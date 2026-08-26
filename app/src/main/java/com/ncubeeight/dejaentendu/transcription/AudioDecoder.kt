package com.ncubeeight.dejaentendu.transcription

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/**
 * Decodes a compressed audio file (m4a/mp3/mp4 — whatever the user
 * imported) into the raw, headerless 16-bit PCM mono 16kHz format that
 * ML Kit GenAI Speech Recognition's AudioSource.fromPfd requires.
 *
 * Uses the platform's own MediaExtractor/MediaCodec rather than a library —
 * androidx.media3's Transformer was tried first, but it only exports MP4
 * containers (confirmed via its own docs, 2026-08-26); it has no
 * raw/headerless PCM or WAV output.
 *
 * Single-pass streaming: downmix + resample happen per MediaCodec output
 * buffer as it arrives, writing straight to the output file. An earlier
 * version buffered the whole native-rate decode to a temp file and
 * resampled it in one shot — simpler, but it OOM'd on a real ~25-minute
 * recording (a `readBytes()` of the full native-rate PCM tried to allocate
 * ~150MB in one go). This version never holds more than one decode
 * buffer + a few bytes of carried resampler state in memory, regardless
 * of recording length. Confirmed via a real crash on-device, 2026-08-26.
 */
object AudioDecoder {
    private const val TARGET_SAMPLE_RATE = 16_000

    fun decodeToRawPcm16Mono16k(inputPath: String, outputFile: File) {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val candidate = extractor.getTrackFormat(i)
            val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = candidate
                break
            }
        }
        val trackFormat = requireNotNull(format) { "No audio track found in $inputPath" }
        extractor.selectTrack(trackIndex)

        val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
        val sourceSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val resampler = StreamingResampler(sourceSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble())

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(trackFormat, null, null, 0)
        codec.start()

        outputFile.outputStream().buffered(1 shl 16).use { out ->
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputBufferId = codec.dequeueInputBuffer(10_000)
                    if (inputBufferId >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferId)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inputBufferId, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferId >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outputBufferId)!!
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        val interleaved = ShortArray(bufferInfo.size / 2)
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(interleaved)

                        val mono = if (sourceChannelCount > 1) {
                            downmixToMono(interleaved, sourceChannelCount)
                        } else {
                            interleaved
                        }

                        resampler.process(mono, out)
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
    }

    private fun downmixToMono(interleaved: ShortArray, channelCount: Int): ShortArray {
        val frameCount = interleaved.size / channelCount
        val mono = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0
            for (c in 0 until channelCount) sum += interleaved[i * channelCount + c]
            mono[i] = (sum / channelCount).toShort()
        }
        return mono
    }

    /**
     * Linear-interpolation resampler that consumes one chunk (MediaCodec
     * output buffer) at a time, carrying its fractional read position and
     * last sample across calls so there's no discontinuity at chunk
     * boundaries — adequate for speech recognition input, not audiophile
     * quality.
     */
    private class StreamingResampler(private val ratio: Double) {
        private var pos = 0.0
        private var previousLast: Short = 0
        private val writeBuffer = ByteArray(2)

        fun process(chunk: ShortArray, out: java.io.OutputStream) {
            if (chunk.isEmpty()) return
            while (true) {
                val index = floor(pos).toInt()
                if (index + 1 >= chunk.size) break
                val frac = pos - index
                val sample0 = if (index < 0) previousLast else chunk[index]
                val sample1 = chunk[index + 1]
                val sample = (sample0 + (sample1 - sample0) * frac).toInt().toShort()
                writeBuffer[0] = (sample.toInt() and 0xFF).toByte()
                writeBuffer[1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                out.write(writeBuffer)
                pos += ratio
            }
            pos -= chunk.size
            previousLast = chunk.last()
        }
    }
}
