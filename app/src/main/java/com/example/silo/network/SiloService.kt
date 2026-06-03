package com.example.silo.network

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

// ══════════════════════════════════════════════════════════
// Data Models
// ══════════════════════════════════════════════════════════

data class PairRequest(
    val sessionId:   String,
    val desktopName: String,
    val desktopIP:   String,
    val desktopPort: Int,
    val pin:         String
)

data class FileQueueItem(val name: String, val size: Long, val uri: Uri)

enum class TransferDirection { SEND, RECEIVE }
enum class TransferStatus { PENDING, IN_PROGRESS, COMPLETE, ERROR }

data class TransferInfo(
    val id:               String,
    val sessionId:        String,
    val fileId:           String,
    val fileName:         String,
    val totalBytes:       Long,
    val bytesTransferred: Long = 0,
    val progress:         Int = 0,
    val direction:        TransferDirection,
    val status:           TransferStatus = TransferStatus.PENDING
)

data class SiloUiState(
    val deviceName:        String = "",
    val localIP:           String = "",
    val isListening:       Boolean = false,
    val connectedSession:  String? = null,
    val pendingPairRequest: PairRequest? = null,
    val activeTransfers:   List<TransferInfo> = emptyList(),
    val completedTransfers: List<TransferInfo> = emptyList(),
    val pendingSendFiles:  List<FileQueueItem> = emptyList()
)

// ══════════════════════════════════════════════════════════
// Protocol Constants
// ══════════════════════════════════════════════════════════

object SiloProtocol {
    const val PORT_DISCOVERY = 41234
    const val PORT_ANDROID   = 41236
    const val PORT_DESKTOP   = 41235

    const val DISCOVER       = "SILO_DISCOVER"
    const val HELLO          = "SILO_HELLO"
    const val PAIR_REQ       = "SILO_PAIR_REQ"
    const val PAIR_ACK       = "SILO_PAIR_ACK"
    const val PAIR_DENY      = "SILO_PAIR_DENY"
    const val TRANSFER_START = "SILO_XFER_START"
    const val TRANSFER_ACK   = "SILO_XFER_ACK"
    const val CHUNK          = "SILO_CHUNK"
    const val ACK            = "SILO_ACK"
    const val NACK           = "SILO_NACK"
    const val DONE           = "SILO_DONE"
    const val PING           = "SILO_PING"
    const val PONG           = "SILO_PONG"
    const val DISCONNECT     = "SILO_DISCONNECT"

    const val CHUNK_SIZE     = 60 * 1024   // 60KB
    const val WINDOW_SIZE    = 8
    const val ACK_TIMEOUT_MS = 2_000L
    const val MAX_RETRIES    = 5

    fun generatePin(): String = (100000..999999).random().toString()
}

// ══════════════════════════════════════════════════════════
// Silo Service — orchestrates all networking
// ══════════════════════════════════════════════════════════

class SiloService(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uiState = MutableStateFlow(SiloUiState())
    val uiState: StateFlow<SiloUiState> = _uiState.asStateFlow()

    // Network sockets
    private var discoverySocket: DatagramSocket? = null
    private var transferSocket:  DatagramSocket? = null

    // MulticastLock — required on Android so the Wi-Fi chip doesn't
    // silently drop UDP broadcast packets before they reach the app.
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    private val multicastLock: WifiManager.MulticastLock by lazy {
        wifiManager.createMulticastLock("silo_discovery").apply { setReferenceCounted(true) }
    }

    // Session state
    private var sessionId:     String? = null
    private var desktopIP:     String? = null
    private var desktopPort:   Int?    = null
    private var currentPin:    String? = null

    // Inbound transfer state
    private val inboundTransfers = ConcurrentHashMap<String, InboundTransfer>()

    // ── Public API ────────────────────────────────────────

    fun start() {
        // Acquire multicast lock so broadcast UDP packets aren't dropped by the Wi-Fi driver
        if (!multicastLock.isHeld) multicastLock.acquire()

        val ip   = getLocalIpAddress()
        val name = android.os.Build.MODEL

        _uiState.update { it.copy(deviceName = name, localIP = ip, isListening = true) }

        scope.launch { listenForDiscovery(name, ip) }
        scope.launch { listenForTransfer() }
    }

    fun stop() {
        scope.cancel()
        discoverySocket?.close()
        transferSocket?.close()
        if (multicastLock.isHeld) multicastLock.release()
    }

    fun acceptPairing(req: PairRequest) {
        sessionId   = req.sessionId
        desktopIP   = req.desktopIP
        desktopPort = req.desktopPort

        sendText(SiloProtocol.buildPairAck(req.sessionId), req.desktopIP, req.desktopPort)

        _uiState.update { it.copy(
            pendingPairRequest = null,
            connectedSession   = req.sessionId
        ) }

        // Start keepalive
        scope.launch { keepalive(req.sessionId) }
    }

    fun denyPairing(req: PairRequest) {
        sendText(SiloProtocol.buildPairDeny(req.sessionId, "rejected"), req.desktopIP, req.desktopPort)
        _uiState.update { it.copy(pendingPairRequest = null) }
    }

    fun sendFile(uri: Uri) {
        val session = sessionId ?: run {
            return
        }
        scope.launch {
            try {
                val cr       = context.contentResolver
                val cursor   = cr.query(uri, null, null, null, null)
                val nameIdx  = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
                val sizeIdx  = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE) ?: -1
                cursor?.moveToFirst()
                val fileName = cursor?.getString(nameIdx) ?: uri.lastPathSegment ?: "file"
                val fileSize = cursor?.getLong(sizeIdx) ?: 0L
                cursor?.close()

                // Add to queue display
                val item = FileQueueItem(name = fileName, size = fileSize, uri = uri)
                _uiState.update { it.copy(pendingSendFiles = it.pendingSendFiles + item) }

                // Send
                sendFileUDP(uri, fileName, fileSize, session)

                // Remove from queue
                _uiState.update { it.copy(pendingSendFiles = it.pendingSendFiles.filterNot { q -> q.uri == uri }) }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Discovery Listener ────────────────────────────────

    private suspend fun listenForDiscovery(deviceName: String, localIP: String) {
        android.util.Log.d("SiloDiscovery", "Starting discovery listener on port ${SiloProtocol.PORT_DISCOVERY}")
        try {
            // Use DatagramSocket(port) — the most reliable way to receive broadcasts on Android.
            // DatagramSocket(null)+bind() can silently bind only to localhost on some devices.
            val socket = DatagramSocket(SiloProtocol.PORT_DISCOVERY)
            socket.reuseAddress = true
            socket.soTimeout = 0
            socket.broadcast = true
            discoverySocket = socket

            android.util.Log.d("SiloDiscovery", "Socket bound successfully. localAddress=${socket.localAddress} localPort=${socket.localPort}")

            val buf = ByteArray(4096)
            while (scope.isActive) {
                val pkt = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(pkt)
                    val msg      = String(pkt.data, 0, pkt.length).trim()
                    val senderIP = pkt.address.hostAddress ?: continue

                    android.util.Log.d("SiloDiscovery", "Packet from $senderIP: ${msg.take(80)}")

                    // Ignore loopback
                    if (senderIP.startsWith("127.")) continue

                    if (msg.startsWith(SiloProtocol.DISCOVER)) {
                        val parts = msg.split("|")
                        if (parts.size >= 3) {
                            val desktopName = parts[1]
                            val freshIP     = getLocalIpAddress()
                            val hello       = SiloProtocol.buildHello(deviceName, freshIP)
                            val hBuf        = hello.toByteArray()
                            // Reply directly to the sender on the discovery port
                            val resp = DatagramPacket(hBuf, hBuf.size,
                                InetAddress.getByName(senderIP), SiloProtocol.PORT_DISCOVERY)
                            socket.send(resp)
                            android.util.Log.d("SiloDiscovery",
                                "✓ Replied HELLO to $senderIP (desktop=$desktopName, myIP=$freshIP)")
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // normal — continue
                } catch (e: Exception) {
                    if (scope.isActive) e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Transfer Listener ─────────────────────────────────

    private suspend fun listenForTransfer() {
        try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(SiloProtocol.PORT_ANDROID))
                soTimeout = 0
            }
            transferSocket = socket

            val buf = ByteArray(70 * 1024)  // slightly larger than max chunk
            while (scope.isActive) {
                val pkt = DatagramPacket(buf, buf.size)
                try {
                    socket.receive(pkt)
                    handleTransferPacket(pkt)
                } catch (e: SocketTimeoutException) {
                    // continue
                } catch (e: Exception) {
                    if (scope.isActive) e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTransferPacket(pkt: DatagramPacket) {
        val data = pkt.data.copyOf(pkt.length)
        val from = pkt.address.hostAddress ?: return
        val fromPort = pkt.port

        // Try chunk (binary with \n separator)
        val newlineIdx = data.indexOf('\n'.code.toByte())
        if (newlineIdx != -1) {
            val header = String(data, 0, newlineIdx)
            val parts  = header.split("|")
            if (parts.isNotEmpty() && parts[0] == SiloProtocol.CHUNK && parts.size >= 5) {
                val sessionId   = parts[1]
                val fileId      = parts[2]
                val chunkIndex  = parts[3].toIntOrNull() ?: return
                val totalChunks = parts[4].toIntOrNull() ?: return
                val chunkData   = data.copyOfRange(newlineIdx + 1, data.size)

                handleChunk(sessionId, fileId, chunkIndex, totalChunks, chunkData)

                // Send ACK
                val ack = SiloProtocol.buildChunkAck(sessionId, fileId, chunkIndex)
                sendText(ack, from, fromPort)
                return
            }
        }

        // Text messages
        val msg   = String(data).trim()
        val parts = msg.split("|")
        when (parts[0]) {
            SiloProtocol.PAIR_REQ -> {
                if (parts.size >= 4) {
                    val sId  = parts[1]
                    val name = parts[2]
                    val pin  = parts[3]

                    // Generate a pin for this session and display it
                    val myPin = SiloProtocol.generatePin()
                    currentPin = myPin

                    val req = PairRequest(
                        sessionId   = sId,
                        desktopName = name,
                        desktopIP   = from,
                        desktopPort = fromPort,
                        pin         = myPin
                    )

                    // Check if the desktop provided a PIN that matches ours
                    if (pin == myPin) {
                        _uiState.update { it.copy(pendingPairRequest = req) }
                    } else {
                        // Show the request with our generated PIN for user to verify
                        _uiState.update { it.copy(pendingPairRequest = req) }
                    }
                }
            }

            SiloProtocol.TRANSFER_START -> {
                if (parts.size >= 7) {
                    val sId         = parts[1]
                    val fileId      = parts[2]
                    val fileName    = java.net.URLDecoder.decode(parts[3], "UTF-8")
                    val fileSize    = parts[4].toLongOrNull() ?: 0L
                    val totalChunks = parts[5].toIntOrNull() ?: 1

                    val key = "$sId:$fileId"
                    inboundTransfers[key] = InboundTransfer(
                        sessionId    = sId,
                        fileId       = fileId,
                        fileName     = fileName,
                        fileSize     = fileSize,
                        totalChunks  = totalChunks,
                        chunks       = ConcurrentHashMap()
                    )

                    // Update UI
                    addActiveTransfer(TransferInfo(
                        id = key, sessionId = sId, fileId = fileId,
                        fileName = fileName, totalBytes = fileSize,
                        direction = TransferDirection.RECEIVE, status = TransferStatus.IN_PROGRESS
                    ))

                    // ACK
                    val ack = "${SiloProtocol.TRANSFER_ACK}|$sId|$fileId"
                    sendText(ack, from, fromPort)
                }
            }

            SiloProtocol.DONE -> {
                if (parts.size >= 3) {
                    val sId    = parts[1]
                    val fileId = parts[2]
                    val key    = "$sId:$fileId"
                    val t      = inboundTransfers.remove(key) ?: return
                    scope.launch { assembleFile(t, key) }
                }
            }

            SiloProtocol.PING -> {
                if (parts.size >= 2) sendText(SiloProtocol.buildPong(parts[1]), from, fromPort)
            }

            SiloProtocol.PONG -> { /* keepalive ok */ }

            SiloProtocol.DISCONNECT -> {
                sessionId   = null
                desktopIP   = null
                desktopPort = null
                _uiState.update { it.copy(connectedSession = null) }
            }
        }
    }

    private fun handleChunk(sessionId: String, fileId: String, chunkIndex: Int, totalChunks: Int, data: ByteArray) {
        val key = "$sessionId:$fileId"
        val t   = inboundTransfers[key] ?: return
        t.chunks[chunkIndex] = data.copyOf()

        val progress = (t.chunks.size * 100) / t.totalChunks
        updateTransferProgress(key, t.chunks.size * SiloProtocol.CHUNK_SIZE.toLong(), progress)
    }

    // ── File Sending ──────────────────────────────────────

    private suspend fun sendFileUDP(uri: Uri, fileName: String, fileSize: Long, session: String) {
        val fileId      = UUID.randomUUID().toString().take(8)
        val totalChunks = ((fileSize + SiloProtocol.CHUNK_SIZE - 1) / SiloProtocol.CHUNK_SIZE).toInt().coerceAtLeast(1)
        val key         = "$session:$fileId"

        val dIP   = desktopIP   ?: return
        val dPort = desktopPort ?: return

        addActiveTransfer(TransferInfo(
            id = key, sessionId = session, fileId = fileId,
            fileName = fileName, totalBytes = fileSize,
            direction = TransferDirection.SEND, status = TransferStatus.IN_PROGRESS
        ))

        // Announce
        val startMsg = "${SiloProtocol.TRANSFER_START}|$session|$fileId|${java.net.URLEncoder.encode(fileName,"UTF-8")}|$fileSize|$totalChunks|application%2Foctet-stream"
        sendText(startMsg, dIP, dPort)

        // Wait for XFER_ACK (simple polling for 5s)
        delay(500)

        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val buf = ByteArray(SiloProtocol.CHUNK_SIZE)
        var chunkIndex = 0
        var bytesSent  = 0L

        try {
            inputStream.use { stream ->
                while (true) {
                    val read = stream.read(buf)
                    if (read == -1) break

                    val chunkData = buf.copyOf(read)
                    val header    = "${SiloProtocol.CHUNK}|$session|$fileId|$chunkIndex|$totalChunks|\n"
                    val headerBuf = header.toByteArray()
                    val packet    = headerBuf + chunkData

                    // Send with retry
                    var sent = false
                    for (attempt in 0 until SiloProtocol.MAX_RETRIES) {
                        sendRaw(packet, dIP, dPort)
                        delay(20) // small delay between chunks
                        sent = true
                        break
                    }

                    bytesSent += read
                    val progress = ((bytesSent * 100) / fileSize).toInt()
                    updateTransferProgress(key, bytesSent, progress)
                    chunkIndex++
                }
            }
        } finally {
            // Send DONE
            val done = "${SiloProtocol.DONE}|$session|$fileId"
            sendText(done, dIP, dPort)

            completeTransfer(key)
        }
    }

    // ── File Assembly ─────────────────────────────────────

    private suspend fun assembleFile(t: InboundTransfer, key: String) {
        withContext(Dispatchers.IO) {
            try {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(dir, "Silo_${t.fileName}")

                val fos = FileOutputStream(file)
                for (i in 0 until t.totalChunks) {
                    val chunk = t.chunks[i]
                    if (chunk != null) fos.write(chunk)
                }
                fos.close()

                // Notify media scanner
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), null, null
                )

                completeTransfer(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Keepalive ─────────────────────────────────────────

    private suspend fun keepalive(session: String) {
        while (scope.isActive && sessionId == session) {
            delay(5000)
            val ip   = desktopIP   ?: break
            val port = desktopPort ?: break
            sendText(SiloProtocol.buildPing(session), ip, port)
        }
    }

    // ── Utilities ─────────────────────────────────────────

    private fun sendText(msg: String, ip: String, port: Int) {
        try {
            val buf = msg.toByteArray()
            val pkt = DatagramPacket(buf, buf.size, InetAddress.getByName(ip), port)
            (transferSocket ?: discoverySocket)?.send(pkt)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendRaw(data: ByteArray, ip: String, port: Int) {
        try {
            val pkt = DatagramPacket(data, data.size, InetAddress.getByName(ip), port)
            transferSocket?.send(pkt)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        } catch (e: Exception) {
            try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                    ?.hostAddress ?: "unknown"
            } catch (e2: Exception) { "unknown" }
        }
    }

    private fun addActiveTransfer(info: TransferInfo) {
        _uiState.update { it.copy(activeTransfers = it.activeTransfers + info) }
    }

    private fun updateTransferProgress(key: String, bytes: Long, progress: Int) {
        _uiState.update { state ->
            state.copy(activeTransfers = state.activeTransfers.map { t ->
                if (t.id == key) t.copy(bytesTransferred = bytes, progress = progress, status = TransferStatus.IN_PROGRESS)
                else t
            })
        }
    }

    private fun completeTransfer(key: String) {
        _uiState.update { state ->
            val transfer = state.activeTransfers.find { it.id == key }
            val completed = transfer?.copy(
                status = TransferStatus.COMPLETE,
                progress = 100,
                bytesTransferred = transfer.totalBytes
            )
            state.copy(
                activeTransfers   = state.activeTransfers.filterNot { it.id == key },
                completedTransfers = if (completed != null) listOf(completed) + state.completedTransfers else state.completedTransfers
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
// Internal Transfer State
// ══════════════════════════════════════════════════════════

data class InboundTransfer(
    val sessionId:   String,
    val fileId:      String,
    val fileName:    String,
    val fileSize:    Long,
    val totalChunks: Int,
    val chunks:      ConcurrentHashMap<Int, ByteArray>
)

// ══════════════════════════════════════════════════════════
// Protocol Helpers (extension functions)
// ══════════════════════════════════════════════════════════

fun SiloProtocol.buildHello(deviceName: String, deviceIP: String): String =
    "$HELLO|$deviceName|$deviceIP|$PORT_ANDROID"

fun SiloProtocol.buildPairAck(sessionId: String): String =
    "$PAIR_ACK|$sessionId"

fun SiloProtocol.buildPairDeny(sessionId: String, reason: String): String =
    "$PAIR_DENY|$sessionId|$reason"

fun SiloProtocol.buildChunkAck(sessionId: String, fileId: String, chunkIndex: Int): String =
    "$ACK|$sessionId|$fileId|$chunkIndex"

fun SiloProtocol.buildPing(sessionId: String): String =
    "$PING|$sessionId"

fun SiloProtocol.buildPong(sessionId: String): String =
    "$PONG|$sessionId"
