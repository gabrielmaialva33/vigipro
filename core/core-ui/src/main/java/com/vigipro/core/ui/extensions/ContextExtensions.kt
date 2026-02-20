package com.vigipro.core.ui.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Shares text content via Android share sheet.
 */
fun Context.shareText(text: String, title: String = "Compartilhar") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

/**
 * Copies text to clipboard and shows a toast confirmation.
 */
fun Context.copyToClipboard(text: String, label: String = "VigiPro") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show()
}

/**
 * Masks an email for display: "ga***@gmail.com"
 */
fun String.maskEmail(): String {
    val atIndex = indexOf('@')
    if (atIndex <= 1) return this
    val visible = take(2)
    val domain = substring(atIndex)
    return "$visible***$domain"
}

/**
 * Masks RTSP credentials in a URL for safe logging/display.
 */
fun String.maskRtspCredentials(): String {
    val regex = Regex("""(rtsp://)([^:]+):([^@]+)@""")
    return regex.replace(this) { "${it.groupValues[1]}***:***@" }
}
