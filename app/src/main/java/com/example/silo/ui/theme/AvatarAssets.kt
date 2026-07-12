package com.example.silo.ui.theme

import com.example.silo.R

/**
 * Bundled default profile pictures. These are the exact same 8 images (same
 * filenames, minus extension) as Silo Desktop's pfp folder, so an avatar
 * chosen on either device can be identified by a shared name like "avatar3"
 * and resolved locally on each platform.
 */
val avatarDrawables = listOf(
    R.drawable.avatar1,
    R.drawable.avatar2,
    R.drawable.avatar3,
    R.drawable.avatar4,
    R.drawable.avatar5,
    R.drawable.avatar6,
    R.drawable.avatar7,
    R.drawable.avatar8,
)

/** The shared cross-platform name for the avatar at [index] (0-based), e.g. "avatar1". */
fun avatarName(index: Int): String = "avatar${index.coerceIn(0, avatarDrawables.lastIndex) + 1}"
