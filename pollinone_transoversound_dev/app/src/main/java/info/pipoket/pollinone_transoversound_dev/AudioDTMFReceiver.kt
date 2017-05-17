package info.pipoket.pollinone_transoversound_dev

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlin.experimental.and

/**
 * Created by pipoket on 2017. 5. 16..
 */

class AudioDTMFReceiver : Thread() {
    private var stopped = false

    enum class ReceiverState {
        SEARCH_PREAMBLE,
        RECEIVE_DATA,
        RECEIVE_DONE,
    }

    private val mSamplingRate = 44100
    private val mBlockSize = 1024
    private var mState = ReceiverState.SEARCH_PREAMBLE

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    init {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        start()
    }

    override fun run() {
        Log.i("AudioDTMFReceiver [$state]", "Running Audio Thread")

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        val minBufferSize = AudioRecord.getMinBufferSize(
                mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

        val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize)

        try {
            val dataBuffer = ByteArray(mBlockSize)
            val fftBuffer = ArrayList<Complex>(mBlockSize)
            recorder.startRecording()

            while (!stopped && mState < ReceiverState.RECEIVE_DONE) {
                Log.i("Map", "Writing new data to buffer");
                recorder.read(dataBuffer, 0, mBlockSize)


                val bytesPerSample = 2            // As it is 16bit PCM
                val amplification: Double = 100.0 // choose a number as you like
                var index = 0
                var floatIndex = 0
                while(index < mBlockSize - bytesPerSample + 1) {
                    var sample: Double = 0.0;
                    for (b in 0..bytesPerSample) {
                        var v = dataBuffer[index + b];
                        if (b < bytesPerSample - 1 || bytesPerSample == 1)
                            v = v and 0xFF.toByte()
                        sample += v.toInt().shl(b * 8)
                    }
                    val sample32 = amplification * (sample / 32768.0);
                    dataBuffer[floatIndex] = sample32.toByte();

                    index += bytesPerSample
                    floatIndex += 1
                }

                for (i in 0..mBlockSize)
                    fftBuffer[i] = Complex(dataBuffer[i].toDouble(), 0.0)
                val fftResult = FFT.fft(fftBuffer)

                for(i in 0..mBlockSize/2) {
                    val frequency = i * mSamplingRate / mBlockSize;
                    Log.d("FFT", " $frequency: ${fftResult[i]}")
                }


                when (mState) {
                    ReceiverState.SEARCH_PREAMBLE -> searchPreamble()
                    ReceiverState.RECEIVE_DATA -> receiveData()
                }
            }
        } catch (x: Throwable) {
            Log.w("AudioDTMFReceiver [$state]", "Error reading voice audio $x")
        } finally {
            recorder.stop()
            recorder.release()
        }/*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
    }

    fun searchPreamble() {

    }

    fun receiveData() {

    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    fun close() {
        stopped = true
    }

}