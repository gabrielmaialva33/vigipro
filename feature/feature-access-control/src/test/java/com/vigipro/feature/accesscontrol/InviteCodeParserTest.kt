package com.vigipro.feature.accesscontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeParserTest {

    // --- extractCode ---

    @Test
    fun `extractCode from https URL returns code`() {
        val result = InviteCodeParser.extractCode("https://vigipro.app/invite/ABC12345")
        assertEquals("ABC12345", result)
    }

    @Test
    fun `extractCode from http URL returns code`() {
        val result = InviteCodeParser.extractCode("http://vigipro.app/invite/XYZ99")
        assertEquals("XYZ99", result)
    }

    @Test
    fun `extractCode from URL without protocol returns code`() {
        val result = InviteCodeParser.extractCode("vigipro.app/invite/QWERTY")
        assertEquals("QWERTY", result)
    }

    @Test
    fun `extractCode from URL with trailing slash returns code`() {
        // Regex stops at non-alphanumeric, so trailing chars are ignored
        val result = InviteCodeParser.extractCode("https://vigipro.app/invite/CODE123/")
        assertEquals("CODE123", result)
    }

    @Test
    fun `extractCode from URL with query params returns code only`() {
        val result = InviteCodeParser.extractCode("https://vigipro.app/invite/HELLO?ref=share")
        assertEquals("HELLO", result)
    }

    @Test
    fun `extractCode from raw code returns as-is`() {
        val result = InviteCodeParser.extractCode("ABC12345")
        assertEquals("ABC12345", result)
    }

    @Test
    fun `extractCode trims whitespace from raw code`() {
        val result = InviteCodeParser.extractCode("  CODE123  ")
        assertEquals("CODE123", result)
    }

    @Test
    fun `extractCode from empty string returns empty`() {
        val result = InviteCodeParser.extractCode("")
        assertEquals("", result)
    }

    @Test
    fun `extractCode from unrelated URL returns full value trimmed`() {
        val result = InviteCodeParser.extractCode("https://google.com/something")
        assertEquals("https://google.com/something", result)
    }

    @Test
    fun `extractCode handles mixed case URL`() {
        val result = InviteCodeParser.extractCode("https://VIGIPRO.app/invite/aBcDeF")
        assertEquals("aBcDeF", result)
    }

    @Test
    fun `extractCode from deep link with www prefix`() {
        val result = InviteCodeParser.extractCode("https://www.vigipro.app/invite/TEST01")
        assertEquals("TEST01", result)
    }

    // --- isValidCode ---

    @Test
    fun `isValidCode returns true for alphanumeric codes`() {
        assertTrue(InviteCodeParser.isValidCode("ABC123"))
        assertTrue(InviteCodeParser.isValidCode("abcdef"))
        assertTrue(InviteCodeParser.isValidCode("12345678"))
        assertTrue(InviteCodeParser.isValidCode("A"))
    }

    @Test
    fun `isValidCode returns false for blank`() {
        assertFalse(InviteCodeParser.isValidCode(""))
        assertFalse(InviteCodeParser.isValidCode("   "))
    }

    @Test
    fun `isValidCode returns false for special characters`() {
        assertFalse(InviteCodeParser.isValidCode("ABC-123"))
        assertFalse(InviteCodeParser.isValidCode("code@123"))
        assertFalse(InviteCodeParser.isValidCode("code 123"))
        assertFalse(InviteCodeParser.isValidCode("https://url"))
    }
}
