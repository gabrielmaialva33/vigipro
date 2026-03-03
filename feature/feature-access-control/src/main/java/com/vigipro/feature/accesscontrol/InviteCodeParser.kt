package com.vigipro.feature.accesscontrol

object InviteCodeParser {

    private val URL_PATTERN = Regex("(?i)vigipro\\.app/invite/([A-Za-z0-9]+)")

    /**
     * Extracts the invite code from a scanned value.
     * Handles full URLs (https://vigipro.app/invite/ABC123) and raw codes.
     */
    fun extractCode(scannedValue: String): String {
        val match = URL_PATTERN.find(scannedValue)
        return match?.groupValues?.get(1) ?: scannedValue.trim()
    }

    /**
     * Validates that a code looks plausible (non-empty alphanumeric).
     */
    fun isValidCode(code: String): Boolean {
        return code.isNotBlank() && code.matches(Regex("^[A-Za-z0-9]+$"))
    }
}
