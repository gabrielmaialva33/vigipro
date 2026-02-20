package com.vigipro.core.data.audio

import java.nio.ByteBuffer

/**
 * Builds RTP packets for audio streaming over RTSP backchannel.
 * RTP header: 12 bytes (RFC 3550).
 */
object RtpPacketBuilder {

    private const val RTP_VERSION = 2
    private const val RTP_HEADER_SIZE = 12

    /**
     * Build a complete RTP packet with header + payload.
     *
     * @param payloadType 0 = PCMU (mu-law), 8 = PCMA (A-law)
     * @param seq sequence number (wraps at 65535)
     * @param timestamp RTP timestamp (increments by 160 per 20ms frame at 8kHz)
     * @param ssrc synchronization source identifier
     * @param payload G.711 encoded audio data
     */
    fun build(
        payloadType: Int,
        seq: Int,
        timestamp: Long,
        ssrc: Long,
        payload: ByteArray,
    ): ByteArray {
        val packet = ByteArray(RTP_HEADER_SIZE + payload.size)
        val buffer = ByteBuffer.wrap(packet)

        // Byte 0: V=2, P=0, X=0, CC=0 → 0x80
        buffer.put((RTP_VERSION shl 6).toByte())
        // Byte 1: M=0, PT
        buffer.put((payloadType and 0x7F).toByte())
        // Bytes 2-3: sequence number
        buffer.putShort((seq and 0xFFFF).toShort())
        // Bytes 4-7: timestamp
        buffer.putInt((timestamp and 0xFFFFFFFFL).toInt())
        // Bytes 8-11: SSRC
        buffer.putInt((ssrc and 0xFFFFFFFFL).toInt())
        // Payload
        buffer.put(payload)

        return packet
    }

    /**
     * Wrap an RTP packet for TCP interleaved transport (RFC 2326 Section 10.12).
     * Format: '$' (0x24) + channel (1 byte) + length (2 bytes big-endian) + data
     */
    fun wrapTcpInterleaved(channel: Int, rtpPacket: ByteArray): ByteArray {
        val wrapped = ByteArray(4 + rtpPacket.size)
        wrapped[0] = 0x24 // '$'
        wrapped[1] = channel.toByte()
        wrapped[2] = ((rtpPacket.size shr 8) and 0xFF).toByte()
        wrapped[3] = (rtpPacket.size and 0xFF).toByte()
        System.arraycopy(rtpPacket, 0, wrapped, 4, rtpPacket.size)
        return wrapped
    }
}
