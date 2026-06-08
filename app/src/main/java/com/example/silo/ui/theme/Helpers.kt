package com.example.silo.ui.theme

fun fileEmoji(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "jpg","jpeg","png","gif","webp","heic" -> "🖼"
    "mp4","mov","avi","mkv","webm"         -> "🎬"
    "mp3","wav","flac","aac","ogg"         -> "🎵"
    "pdf"                                  -> "📄"
    "doc","docx"                           -> "📝"
    "xls","xlsx"                           -> "📊"
    "zip","rar","tar","gz","7z"            -> "📦"
    "apk"                                  -> "📱"
    "txt","md"                             -> "📃"
    else                                   -> "📁"
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024L             -> "$bytes B"
    bytes < 1024L * 1024      -> "${"%.1f".format(bytes / 1024f)} KB"
    bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024)} MB"
    else                      -> "${"%.2f".format(bytes / 1024f / 1024 / 1024)} GB"
}
