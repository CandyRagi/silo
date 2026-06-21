package com.example.silo.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ── Avatar palette ─────────────────────────────────────────

val avatarPalette = listOf(
    Color(0xFF6366F1),  // indigo
    Color(0xFF8B5CF6),  // violet
    Color(0xFF3B82F6),  // blue
    Color(0xFF10B981),  // emerald
    Color(0xFFF59E0B),  // amber
    Color(0xFFEF4444),  // red
    Color(0xFFEC4899),  // pink
    Color(0xFF14B8A6),  // teal
)

// ── Profile state (held at SiloApp level) ─────────────────

class UserProfileState(context: Context) {

    private val prefs = context.getSharedPreferences("silo_profile", Context.MODE_PRIVATE)

    var displayName by mutableStateOf(prefs.getString("name", "") ?: "")
        private set

    var avatarColorIndex by mutableStateOf(prefs.getInt("avatar_index", 0))
        private set

    val avatarColor: Color get() = avatarPalette[avatarColorIndex.coerceIn(0, avatarPalette.lastIndex)]

    fun update(name: String, colorIndex: Int) {
        displayName      = name
        avatarColorIndex = colorIndex
        prefs.edit()
            .putString("name", name)
            .putInt("avatar_index", colorIndex)
            .apply()
    }

    var allowAudioStreaming by mutableStateOf(prefs.getBoolean("allow_audio", true))
        private set

    fun setAudioStreaming(allow: Boolean) {
        allowAudioStreaming = allow
        prefs.edit().putBoolean("allow_audio", allow).apply()
    }
}
