package com.vigipro.core.data.audio

/**
 * G.711 audio codec — encodes 16-bit linear PCM to 8-bit compressed.
 * A-law (PCMA, RTP payload type 8) and mu-law (PCMU, RTP payload type 0).
 * Standard for ONVIF backchannel audio at 8kHz mono.
 */
object G711Codec {

    private const val ALAW_MAX = 0xFFF
    private const val MULAW_MAX = 0x1FFF
    private const val MULAW_BIAS = 0x84

    fun encodePcmToAlaw(pcm16: ShortArray): ByteArray {
        val encoded = ByteArray(pcm16.size)
        for (i in pcm16.indices) {
            encoded[i] = linearToAlaw(pcm16[i].toInt())
        }
        return encoded
    }

    fun encodePcmToMulaw(pcm16: ShortArray): ByteArray {
        val encoded = ByteArray(pcm16.size)
        for (i in pcm16.indices) {
            encoded[i] = linearToMulaw(pcm16[i].toInt())
        }
        return encoded
    }

    private fun linearToAlaw(pcmVal: Int): Byte {
        var sample = pcmVal
        val sign: Int
        if (sample >= 0) {
            sign = 0xD5 // even bit complement
        } else {
            sign = 0x55
            sample = -sample - 1
        }

        if (sample > ALAW_MAX) sample = ALAW_MAX

        val compressedByte: Int = if (sample >= 256) {
            val exponent = alawCompressTable[(sample shr 8) and 0x7F]
            val mantissa = (sample shr (exponent + 3)) and 0x0F
            (exponent shl 4) or mantissa
        } else {
            sample shr 4
        }

        return (compressedByte xor sign).toByte()
    }

    private fun linearToMulaw(pcmVal: Int): Byte {
        var sample = pcmVal
        val sign: Int

        // Get the sample into sign-magnitude
        sign = (sample shr 8) and 0x80 // set aside the sign
        if (sign != 0) sample = -sample // get magnitude
        if (sample > MULAW_MAX) sample = MULAW_MAX // clip the magnitude

        // Convert from 16 bit linear to ulaw
        sample += MULAW_BIAS
        val exponent = mulawCompressTable[(sample shr 7) and 0xFF]
        val mantissa = (sample shr (exponent + 3)) and 0x0F
        val ulawByte = (sign or (exponent shl 4) or mantissa).inv()

        return ulawByte.toByte()
    }

    private val alawCompressTable = intArrayOf(
        1, 1, 2, 2, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4,
        5, 5, 5, 5, 5, 5, 5, 5,
        5, 5, 5, 5, 5, 5, 5, 5,
        6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7,
    )

    private val mulawCompressTable = intArrayOf(
        0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    )
}
