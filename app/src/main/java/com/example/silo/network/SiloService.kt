package com.example.silo.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import java.net.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.os.Build
import kotlin.concurrent.thread

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
enum class TransferStatus    { PENDING, IN_PROGRESS, COMPLETE, ERROR }

data class TransferInfo(
    val id:               String,
    val sessionId:        String,
    val fileId:           String,
    val fileName:         String,
    val totalBytes:       Long,
    val bytesTransferred: Long = 0,
    val progress:         Int  = 0,
    val direction:        TransferDirection,
    val status:           TransferStatus = TransferStatus.PENDING
)

data class SiloUiState(
    val deviceName:         String  = "",
    val localIP:            String  = "",
    val isListening:        Boolean = false,
    val connectedSession:   String? = null,
    val pendingPairRequest: PairRequest? = null,
    val activeTransfers:    List<TransferInfo>  = emptyList(),
    val completedTransfers: List<TransferInfo>  = emptyList(),
    val pendingSendFiles:   List<FileQueueItem> = emptyList(),
    val desktopAllowsControl: Boolean = true
)

// ══════════════════════════════════════════════════════════
// Protocol
// ══════════════════════════════════════════════════════════

object SiloProtocol {
    const val PORT_DISCOVERY = 41234
    const val PORT_ANDROID   = 41236
    const val PORT_DESKTOP   = 41235
    const val CHUNK_SIZE     = 60 * 1024
    const val MAX_RETRIES    = 5

    const val DISCOVER       = "SILO_DISCOVER"
    const val HELLO          = "SILO_HELLO"
    const val PAIR_REQ       = "SILO_PAIR_REQ"
    const val PAIR_ACK       = "SILO_PAIR_ACK"
    const val PAIR_DENY      = "SILO_PAIR_DENY"
    const val TRANSFER_START = "SILO_XFER_START"
    const val TRANSFER_ACK   = "SILO_XFER_ACK"
    const val CHUNK          = "SILO_CHUNK"
    const val ACK            = "SILO_ACK"
    const val DONE           = "SILO_DONE"
    const val PING           = "SILO_PING"
    const val PONG           = "SILO_PONG"
    const val DISCONNECT     = "SILO_DISCONNECT"
    const val MOUSE_MOVE     = "SILO_MOUSE_MOVE"
    const val MOUSE_CLICK    = "SILO_MOUSE_CLICK"
    const val KEYBOARD_INPUT = "SILO_KEYBOARD_INPUT"
    const val CTRL_ALLOW     = "SILO_CTRL_ALLOW"

    fun generatePin(): String = (100000..999999).random().toString()
}

// ══════════════════════════════════════════════════════════
// Silo Service  —  plain Thread-based, no coroutines
// ══════════════════════════════════════════════════════════

class SiloService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): SiloService = this@SiloService
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        start()
        return START_NOT_STICKY
    }

    companion object { private const val TAG = "SiloService" }

    private val _uiState = MutableStateFlow(SiloUiState())
    val uiState: StateFlow<SiloUiState> = _uiState.asStateFlow()

    // Sockets
    @Volatile private var discoverySocket: DatagramSocket? = null
    @Volatile private var transferSocket:  DatagramSocket? = null

    // Running flag
    @Volatile private var running = false

    // Session
    @Volatile private var sessionId:   String? = null
    @Volatile private var desktopIP:   String? = null
    @Volatile private var desktopPort: Int?    = null
    @Volatile private var lastPingReceived: Long = 0L

    private val inboundTransfers = ConcurrentHashMap<String, InboundTransfer>()

    // MulticastLock so Wi-Fi chip doesn't drop broadcast packets
    private val multicastLock: WifiManager.MulticastLock by lazy {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.createMulticastLock("silo").apply { setReferenceCounted(true) }
    }

    // ── Start / Stop ──────────────────────────────────────

    fun start() {
        if (running) { Log.w(TAG, "Already running"); return }
        running = true
        Log.d(TAG, "=== SiloService.start() ===")

        startForegroundNotification()

        // Acquire multicast lock
        try {
            if (!multicastLock.isHeld) multicastLock.acquire()
            Log.d(TAG, "MulticastLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "MulticastLock failed: ${e.message}")
        }

        val ip   = getLocalIp()
        val name = Build.MODEL
        Log.d(TAG, "Device: $name  IP: $ip")
        _uiState.update { it.copy(deviceName = name, localIP = ip, isListening = true) }

        // Start two daemon threads — no coroutines, no complexity
        thread("silo-discovery") { runDiscovery(name, ip) }
        thread("silo-transfer")  { runTransfer() }
    }

    private fun startForegroundNotification() {
        val channelId = "silo_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Silo Connection", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("Silo is running")
            .setContentText("Connected in background to PC")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    fun stop() {
        Log.d(TAG, "stop()")
        running = false
        val sid = sessionId
        val ip = desktopIP
        val port = desktopPort
        if (sid != null && ip != null && port != null) {
            sendRaw("${SiloProtocol.DISCONNECT}|$sid".toByteArray(), ip, port)
            Log.d(TAG, "Sent DISCONNECT to $ip:$port")
        }
        try { discoverySocket?.close() } catch (_: Exception) {}
        try { transferSocket?.close()  } catch (_: Exception) {}
        try { if (multicastLock.isHeld) multicastLock.release() } catch (_: Exception) {}
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved: App swiped away")
        stop()
        super.onTaskRemoved(rootIntent)
    }

    /** Called when the user types a PIN and taps Confirm on the phone. */
    fun verifyAndAcceptPairing(req: PairRequest, enteredPin: String): Boolean {
        if (enteredPin != req.pin) return false   // wrong PIN
        sessionId   = req.sessionId
        desktopIP   = req.desktopIP
        desktopPort = req.desktopPort

        // Send PAIR_ACK 3 times (UDP is lossy) — desktop ignores duplicates gracefully
        val ackMsg = "${SiloProtocol.PAIR_ACK}|${req.sessionId}"
        sendViaTransfer(ackMsg, req.desktopIP, req.desktopPort)
        Log.d(TAG, "PAIR_ACK #1 sent to ${req.desktopIP}:${req.desktopPort}")
        thread("silo-pair-ack-retry") {
            Thread.sleep(1000)
            sendViaTransfer(ackMsg, req.desktopIP, req.desktopPort)
            Log.d(TAG, "PAIR_ACK #2 sent to ${req.desktopIP}:${req.desktopPort}")
            Thread.sleep(1000)
            sendViaTransfer(ackMsg, req.desktopIP, req.desktopPort)
            Log.d(TAG, "PAIR_ACK #3 sent to ${req.desktopIP}:${req.desktopPort}")
        }

        _uiState.update { it.copy(pendingPairRequest = null, connectedSession = req.sessionId) }
        lastPingReceived = System.currentTimeMillis()
        thread("silo-keepalive") { runKeepalive(req.sessionId) }
        return true
    }

    fun acceptPairing(req: PairRequest) {
        sessionId   = req.sessionId
        desktopIP   = req.desktopIP
        desktopPort = req.desktopPort
        sendViaTransfer("${SiloProtocol.PAIR_ACK}|${req.sessionId}", req.desktopIP, req.desktopPort)
        _uiState.update { it.copy(pendingPairRequest = null, connectedSession = req.sessionId) }
        lastPingReceived = System.currentTimeMillis()
        thread("silo-keepalive") { runKeepalive(req.sessionId) }
    }

    fun denyPairing(req: PairRequest) {
        sendViaTransfer("${SiloProtocol.PAIR_DENY}|${req.sessionId}|rejected", req.desktopIP, req.desktopPort)
        _uiState.update { it.copy(pendingPairRequest = null) }
    }

    fun sendFile(uri: Uri) {
        val sid = sessionId ?: return
        thread("silo-send") {
            try {
                val cr      = contentResolver
                val cursor = if (uri.scheme == "content") cr.query(uri, null, null, null, null) else null
                cursor?.moveToFirst()
                val nameIdx = cursor?.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) ?: -1
                val sizeIdx = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)         ?: -1
                val name    = if (nameIdx >= 0) cursor?.getString(nameIdx) else uri.lastPathSegment ?: "file"
                var size    = if (sizeIdx >= 0) cursor?.getLong(sizeIdx)   else 0L
                cursor?.close()

                if (size == 0L && uri.scheme == "file") {
                    val f = java.io.File(uri.path!!)
                    if (f.exists()) size = f.length()
                }

                _uiState.update { it.copy(pendingSendFiles = it.pendingSendFiles +
                    FileQueueItem(name ?: "file", size ?: 0L, uri)) }
                sendFileUDP(uri, name ?: "file", size ?: 0L, sid)
                _uiState.update { it.copy(pendingSendFiles = it.pendingSendFiles.filterNot { q -> q.uri == uri }) }
            } catch (e: Exception) { Log.e(TAG, "sendFile error: ${e.message}", e) }
        }
    }

    // ── Discovery Thread ──────────────────────────────────

    private fun runDiscovery(deviceName: String, localIP: String) {
        Log.d(TAG, "Discovery thread started")
        try {
            val sock = DatagramSocket(SiloProtocol.PORT_DISCOVERY)
            sock.broadcast = true
            sock.soTimeout = 0
            discoverySocket = sock
            Log.d(TAG, "Discovery socket bound on port ${SiloProtocol.PORT_DISCOVERY}  local=${sock.localAddress}")

            val buf = ByteArray(4096)
            while (running) {
                val pkt = DatagramPacket(buf, buf.size)
                try {
                    sock.receive(pkt)
                    val msg      = String(pkt.data, 0, pkt.length).trim()
                    val senderIP = pkt.address?.hostAddress ?: continue
                    Log.d(TAG, "Discovery RX from $senderIP: ${msg.take(80)}")

                    if (senderIP.startsWith("127.")) continue

                    if (msg.startsWith(SiloProtocol.DISCOVER)) {
                        val myIP  = getLocalIp()
                        val hello = "${SiloProtocol.HELLO}|$deviceName|$myIP|${SiloProtocol.PORT_ANDROID}"
                        val hBuf  = hello.toByteArray()
                        sock.send(DatagramPacket(hBuf, hBuf.size,
                            InetAddress.getByName(senderIP), SiloProtocol.PORT_DISCOVERY))
                        Log.d(TAG, "Replied HELLO to $senderIP  myIP=$myIP")
                    }
                    if (msg.startsWith(SiloProtocol.CTRL_ALLOW)) {
                        val parts = msg.split("|")
                        if (parts.size >= 3 && parts[1] == sessionId) {
                            val allow = parts[2] == "true"
                            _uiState.update { it.copy(desktopAllowsControl = allow) }
                        }
                    }
                } catch (e: SocketException) {
                    if (!running) break else Log.e(TAG, "Discovery recv error: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Discovery error: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discovery socket FAILED to bind: ${e.message}", e)
        }
        Log.d(TAG, "Discovery thread exited")
    }

    // ── Transfer / Pairing Thread ─────────────────────────

    private fun runTransfer() {
        Log.d(TAG, "Transfer thread started")
        try {
            val sock = DatagramSocket(SiloProtocol.PORT_ANDROID)
            sock.broadcast = true
            sock.soTimeout = 0
            transferSocket = sock
            Log.d(TAG, "Transfer socket bound on port ${SiloProtocol.PORT_ANDROID}")

            val buf = ByteArray(70 * 1024)
            while (running) {
                val pkt = DatagramPacket(buf, buf.size)
                try {
                    sock.receive(pkt)
                    handleTransferPacket(pkt, sock)
                } catch (e: SocketException) {
                    if (!running) break else Log.e(TAG, "Transfer recv error: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Transfer error: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transfer socket FAILED to bind: ${e.message}", e)
        }
        Log.d(TAG, "Transfer thread exited")
    }

    private fun handleTransferPacket(pkt: DatagramPacket, sock: DatagramSocket) {
        val data     = pkt.data.copyOf(pkt.length)
        val fromIP   = pkt.address?.hostAddress ?: return
        val fromPort = pkt.port

        // Binary chunk (header ends at first \n)
        val newlineIdx = data.indexOf('\n'.code.toByte())
        if (newlineIdx != -1) {
            val header = String(data, 0, newlineIdx)
            val parts  = header.split("|")
            if (parts.getOrNull(0) == SiloProtocol.CHUNK && parts.size >= 5) {
                val sid    = parts[1]; val fid = parts[2]
                val idx    = parts[3].toIntOrNull() ?: return
                val total  = parts[4].toIntOrNull() ?: return
                val chunk  = data.copyOfRange(newlineIdx + 1, data.size)
                onChunk(sid, fid, idx, total, chunk)
                val ack = "${SiloProtocol.ACK}|$sid|$fid|$idx".toByteArray()
                sock.send(DatagramPacket(ack, ack.size, pkt.address, fromPort))
                return
            }
        }

        val msg   = String(data).trim()
        val parts = msg.split("|")
        when (parts.getOrNull(0)) {
            SiloProtocol.PAIR_REQ -> {
                if (parts.size >= 4) {
                    val reqSessionId = parts[1]
                    // Ignore duplicate requests if we are already connected to this session
                    // or if it's already the pending request (prevents UI reset while typing)
                    if (reqSessionId == sessionId) return
                    val currentPending = _uiState.value.pendingPairRequest
                    if (currentPending != null && currentPending.sessionId == reqSessionId) return

                    val pin = parts[3]  // PIN sent by the desktop
                    // Use the explicit reply port embedded in the message (parts[4]) if present,
                    // otherwise fall back to the UDP source port.
                    val replyPort = parts.getOrNull(4)?.toIntOrNull() ?: fromPort
                    val req = PairRequest(reqSessionId, parts[2], fromIP, replyPort, pin)
                    Log.d(TAG, "Pair request from $fromIP:$replyPort  desktop=${parts[2]}  pin=$pin")
                    _uiState.update { it.copy(pendingPairRequest = req) }
                }
            }
            SiloProtocol.TRANSFER_START -> {
                if (parts.size >= 6) {
                    val key = "${parts[1]}:${parts[2]}"
                    inboundTransfers[key] = InboundTransfer(
                        sessionId    = parts[1],
                        fileId       = parts[2],
                        fileName     = decodeUrl(parts[3]),
                        fileSize     = parts[4].toLongOrNull() ?: 0L,
                        totalChunks  = parts[5].toIntOrNull() ?: 1,
                        chunks       = ConcurrentHashMap()
                    )
                    addTransfer(TransferInfo(key, parts[1], parts[2], decodeUrl(parts[3]),
                        parts[4].toLongOrNull() ?: 0L, direction = TransferDirection.RECEIVE,
                        status = TransferStatus.IN_PROGRESS))
                    val ack = "${SiloProtocol.TRANSFER_ACK}|${parts[1]}|${parts[2]}".toByteArray()
                    sock.send(DatagramPacket(ack, ack.size, pkt.address, fromPort))
                }
            }
            SiloProtocol.DONE -> {
                val key = "${parts.getOrElse(1){""}}:${parts.getOrElse(2){""}}"
                inboundTransfers.remove(key)?.let { assembleFile(it, key) }
            }
            SiloProtocol.PING -> {
                lastPingReceived = System.currentTimeMillis()
                val pong = "${SiloProtocol.PONG}|${parts.getOrElse(1){""}}" .toByteArray()
                sock.send(DatagramPacket(pong, pong.size, pkt.address, fromPort))
            }
            SiloProtocol.PONG -> {
                lastPingReceived = System.currentTimeMillis()
            }
            SiloProtocol.DISCONNECT -> {
                Log.d(TAG, "Received DISCONNECT. Dropping session.")
                sessionId = null; desktopIP = null; desktopPort = null
                _uiState.update { it.copy(connectedSession = null) }
            }
            SiloProtocol.CTRL_ALLOW -> {
                if (parts.size >= 3 && parts[1] == sessionId) {
                    val allow = parts[2] == "true"
                    _uiState.update { it.copy(desktopAllowsControl = allow) }
                }
            }
        }
    }

    private fun onChunk(sid: String, fid: String, idx: Int, total: Int, data: ByteArray) {
        val key = "$sid:$fid"
        val t   = inboundTransfers[key] ?: return
        t.chunks[idx] = data.copyOf()
        val progress = (t.chunks.size * 100) / t.totalChunks
        updateProgress(key, t.chunks.size.toLong() * SiloProtocol.CHUNK_SIZE, progress)
    }

    // ── File Send ─────────────────────────────────────────

    private fun sendFileUDP(uri: Uri, fileName: String, fileSize: Long, sid: String) {
        val fid   = UUID.randomUUID().toString().take(8)
        val total = ((fileSize + SiloProtocol.CHUNK_SIZE - 1) / SiloProtocol.CHUNK_SIZE).toInt().coerceAtLeast(1)
        val key   = "$sid:$fid"
        val dIP   = desktopIP   ?: return
        val dPort = desktopPort ?: return

        addTransfer(TransferInfo(key, sid, fid, fileName, fileSize,
            direction = TransferDirection.SEND, status = TransferStatus.IN_PROGRESS))

        val startMsg = "${SiloProtocol.TRANSFER_START}|$sid|$fid|${java.net.URLEncoder.encode(fileName,"UTF-8")}|$fileSize|$total|application%2Foctet-stream"
        sendViaTransfer(startMsg, dIP, dPort)
        Thread.sleep(500)

        val stream = contentResolver.openInputStream(uri) ?: return
        val buf    = ByteArray(SiloProtocol.CHUNK_SIZE)
        var idx    = 0; var sent = 0L

        stream.use {
            while (true) {
                val read = it.read(buf)
                if (read == -1) break
                val header = "${SiloProtocol.CHUNK}|$sid|$fid|$idx|$total|\n".toByteArray()
                val packet = header + buf.copyOf(read)
                sendRaw(packet, dIP, dPort)
                Thread.sleep(5) // small pacing delay
                sent += read
                updateProgress(key, sent, ((sent * 100) / fileSize).toInt())
                idx++
            }
        }

        sendViaTransfer("${SiloProtocol.DONE}|$sid|$fid", dIP, dPort)
        completeTransfer(key, fileName, fileSize)
    }

    // ── File Assembly ─────────────────────────────────────

    private fun assembleFile(t: InboundTransfer, key: String) {
        thread("silo-assemble") {
            try {
                val dir  = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(dir, "Silo_${t.fileName}")
                val fos  = java.io.FileOutputStream(file)
                for (i in 0 until t.totalChunks) fos.write(t.chunks[i] ?: ByteArray(0))
                fos.close()
                android.media.MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
                Log.d(TAG, "Saved: ${file.absolutePath}")
                completeTransfer(key, t.fileName, t.fileSize)
            } catch (e: Exception) {
                Log.e(TAG, "assemble error: ${e.message}", e)
            }
        }
    }

    // ── Keepalive ─────────────────────────────────────────

    private fun runKeepalive(sid: String) {
        lastPingReceived = System.currentTimeMillis()
        while (running && sessionId == sid) {
            Thread.sleep(2000)
            if (System.currentTimeMillis() - lastPingReceived > 6000) {
                Log.w(TAG, "Keepalive timeout! No PING/PONG received for 6s. Disconnecting session $sid.")
                sessionId = null; desktopIP = null; desktopPort = null
                _uiState.update { it.copy(connectedSession = null) }
                break
            }
            val ip   = desktopIP   ?: break
            val port = desktopPort ?: break
            sendViaTransfer("${SiloProtocol.PING}|$sid", ip, port)
        }
    }

    // ── Send Helpers ──────────────────────────────────────

    private fun sendViaTransfer(msg: String, ip: String, port: Int) {
        try {
            val buf = msg.toByteArray()
            transferSocket?.send(DatagramPacket(buf, buf.size, InetAddress.getByName(ip), port))
                ?: discoverySocket?.send(DatagramPacket(buf, buf.size, InetAddress.getByName(ip), port))
        } catch (e: Exception) { Log.e(TAG, "send error: ${e.message}") }
    }

    private fun sendRaw(data: ByteArray, ip: String, port: Int) {
        try {
            transferSocket?.send(DatagramPacket(data, data.size, InetAddress.getByName(ip), port))
        } catch (e: Exception) { Log.e(TAG, "sendRaw error: ${e.message}") }
    }

    // ── State Helpers ─────────────────────────────────────

    private fun addTransfer(info: TransferInfo) {
        _uiState.update { it.copy(activeTransfers = it.activeTransfers + info) }
    }
    private fun updateProgress(key: String, bytes: Long, progress: Int) {
        _uiState.update { s -> s.copy(activeTransfers = s.activeTransfers.map {
            if (it.id == key) it.copy(bytesTransferred = bytes, progress = progress,
                status = TransferStatus.IN_PROGRESS) else it }) }
    }
    private fun completeTransfer(key: String, name: String, size: Long) {
        val t = _uiState.value.activeTransfers.find { it.id == key } ?: return
        
        if (t.direction == TransferDirection.RECEIVE) {
            val f = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), name)
            updateProgress(key, size, 100)
            
            val db = com.example.silo.database.SiloDatabase.getDatabase(this)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.historyDao().insert(
                    com.example.silo.database.HistoryEntity(
                        fileName = name,
                        totalBytes = size,
                        direction = "receive",
                        timestamp = System.currentTimeMillis(),
                        savedPath = f.absolutePath
                    )
                )
            }
        } else {
            updateProgress(key, size, 100)
            
            val db = com.example.silo.database.SiloDatabase.getDatabase(this)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                db.historyDao().insert(
                    com.example.silo.database.HistoryEntity(
                        fileName = name,
                        totalBytes = size,
                        direction = "send",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ── IP Detection ──────────────────────────────────────

    fun getLocalIp(): String {
        return try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wm.connectionInfo.ipAddress
            if (ip == 0) fallbackIp() else Formatter.formatIpAddress(ip)
        } catch (e: Exception) { fallbackIp() }
    }

    private fun fallbackIp(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    // ── Mouse Control ──────────────────────────────────────────

    fun sendMouseMove(dx: Float, dy: Float) {
        val sid = sessionId ?: return
        val ip = desktopIP ?: return
        val port = desktopPort ?: return
        thread("mouseMove") {
            try {
                val msg = "${SiloProtocol.MOUSE_MOVE}|$sid|$dx|$dy"
                sendViaTransfer(msg, ip, port)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send mouse move", e)
            }
        }
    }

    fun sendMouseClick(button: String) {
        val sid = sessionId ?: return
        val ip = desktopIP ?: return
        val port = desktopPort ?: return
        thread("mouseClick") {
            try {
                val msg = "${SiloProtocol.MOUSE_CLICK}|$sid|$button"
                sendViaTransfer(msg, ip, port)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send mouse click", e)
            }
        }
    }

    fun sendKeyboard(key: String) {
        val sid = sessionId ?: return
        val ip = desktopIP ?: return
        val port = desktopPort ?: return
        thread("keyboard") {
            try {
                val msg = "${SiloProtocol.KEYBOARD_INPUT}|$sid|${java.net.URLEncoder.encode(key, "UTF-8")}"
                sendViaTransfer(msg, ip, port)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send keyboard input", e)
            }
        }
    }

    // ── Transfers ──────────────────────────────────────────────

    private fun thread(name: String, block: () -> Unit): Thread =
        Thread(block, name).also { it.isDaemon = true; it.start() }

    private fun decodeUrl(s: String) = try {
        java.net.URLDecoder.decode(s, "UTF-8")
    } catch (_: Exception) { s }
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
// Protocol extension helpers
// ══════════════════════════════════════════════════════════

fun SiloProtocol.buildHello(deviceName: String, deviceIP: String) =
    "$HELLO|$deviceName|$deviceIP|$PORT_ANDROID"
fun SiloProtocol.buildPairAck(sessionId: String)  = "$PAIR_ACK|$sessionId"
fun SiloProtocol.buildPairDeny(sessionId: String, reason: String) = "$PAIR_DENY|$sessionId|$reason"
fun SiloProtocol.buildChunkAck(sid: String, fid: String, idx: Int) = "$ACK|$sid|$fid|$idx"
fun SiloProtocol.buildPing(sid: String)  = "$PING|$sid"
fun SiloProtocol.buildPong(sid: String)  = "$PONG|$sid"
