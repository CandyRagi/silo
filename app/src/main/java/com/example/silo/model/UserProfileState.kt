package com.example.silo.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ── Profile state (held at SiloApp level) ─────────────────

class UserProfileState(context: Context) {

    private val prefs = context.getSharedPreferences("silo_profile", Context.MODE_PRIVATE)

    var displayName by mutableStateOf(prefs.getString("name", "") ?: "")
        private set

    var avatarIndex by mutableStateOf(prefs.getInt("avatar_index", 0).coerceIn(0, 7))
        private set

    fun update(name: String, avatarIndex: Int) {
        displayName      = name
        this.avatarIndex = avatarIndex.coerceIn(0, 7)
        prefs.edit()
            .putString("name", name)
            .putInt("avatar_index", this.avatarIndex)
            .apply()
    }

}
