package com.kairos.os.domain.usecases

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorder {
    companion object {
        const val SAMPLE_RATE = 16000
        const val MAX_DURATION_SEC = 28
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val pcmBuffer = ByteArrayOutputStream()

    @Volatile
    private var isRecording = false

    fun start(scope: CoroutineScope, onRmsChanged: (Float) -> Unit): Boolean {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize <= 0) return false

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            pcmBuffer.reset()
            audioRecord = record
            record.startRecording()
            isRecording = true

            val maxBytes = SAMPLE_RATE * 2 * MAX_DURATION_SEC
            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                while (isRecording && pcmBuffer.size() < maxBytes) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    pcmBuffer.write(buffer, 0, read)
                    val rmsDb = computeRmsDb(buffer, read)
                    withContext(Dispatchers.Main) { onRmsChanged(rmsDb) }
                }
                if (pcmBuffer.size() >= maxBytes) {
                    stopRecordingInternal()
                }
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun stopAndGetWav(): ByteArray? {
        stopRecordingInternal()
        val pcm = pcmBuffer.toByteArray()
        pcmBuffer.reset()
        if (pcm.isEmpty()) return null
        return pcmToWav(pcm, SAMPLE_RATE)
    }

    fun cancel() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        pcmBuffer.reset()
        releaseAudioRecord()
    }

    private fun stopRecordingInternal() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        releaseAudioRecord()
    }

    private fun releaseAudioRecord() {
        val record = audioRecord ?: return
        audioRecord = null
        try {
            record.stop()
        } catch (_: Exception) {
        }
        record.release()
    }

    private fun computeRmsDb(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        var i = 0
        while (i + 1 < length) {
            val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
            val signed = sample.toShort().toInt()
            sum += signed * signed
            i += 2
        }
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f
        val rms = sqrt(sum / sampleCount)
        return if (rms < 1.0) 0f else (20 * log10(rms / 32767.0)).toFloat()
    }

    private fun pcmToWav(pcm: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcm.size
        val totalSize = 36 + dataSize

        val header = ByteArrayOutputStream(44)
        header.write("RIFF".toByteArray())
        header.write(intToLittleEndian(totalSize))
        header.write("WAVE".toByteArray())
        header.write("fmt ".toByteArray())
        header.write(intToLittleEndian(16))
        header.write(shortToLittleEndian(1))
        header.write(shortToLittleEndian(channels.toShort()))
        header.write(intToLittleEndian(sampleRate))
        header.write(intToLittleEndian(byteRate))
        header.write(shortToLittleEndian(blockAlign))
        header.write(shortToLittleEndian(bitsPerSample.toShort()))
        header.write("data".toByteArray())
        header.write(intToLittleEndian(dataSize))
        header.write(pcm)
        return header.toByteArray()
    }

    private fun intToLittleEndian(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 24 and 0xFF).toByte()
        )
    }

    private fun shortToLittleEndian(value: Short): ByteArray {
        val intVal = value.toInt()
        return byteArrayOf(
            (intVal and 0xFF).toByte(),
            (intVal shr 8 and 0xFF).toByte()
        )
    }
}
