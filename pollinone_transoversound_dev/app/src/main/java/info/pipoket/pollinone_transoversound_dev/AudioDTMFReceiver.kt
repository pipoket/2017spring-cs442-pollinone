package info.pipoket.pollinone_transoversound_dev

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import org.jtransforms.fft.DoubleFFT_1D



/**
 * Created by pipoket on 2017. 5. 16..
 */

class AudioDTMFReceiver() : Thread() {
    private var mStopped = true

    enum class ReceiverState {
        INIT,
        SEARCH_PREAMBLE,
        RECEIVE_DATA,
        RECEIVE_DONE,
    }

    val TONEMAP = listOf(
            listOf(2692, 5433),   // 0
            listOf(2692, 4190),   // 1
            listOf(2844, 4190),   // 2
            listOf(3105, 4190),   // 3
            listOf(2692, 4549),   // 4
            listOf(2844, 4549),   // 5
            listOf(3105, 4549),   // 6
            listOf(2692, 4925),   // 7
            listOf(2844, 4925),   // 8
            listOf(3105, 4925),   // 9
            listOf(2692, 5433)    // preamble
    )

    private var permGranted = false

    private val mSamplingRate = 44100
    private var mState = ReceiverState.INIT
    private val mThreshold = 1.0

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    init {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
    }

    override fun start() {
        if (mStopped) {
            mStopped = false
            super.start()
        }
    }

    override fun run() {
        Log.i("AudioDTMFReceiver [$mState]", "Running Audio Thread")

        while (mState < ReceiverState.SEARCH_PREAMBLE) {
            if (mStopped)
                return
            Log.v("AudioDTMFReceiver [$mState]", "Waiting for permission")
            sleep(500)
        }

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        val minBufferSize = AudioRecord.getMinBufferSize(
                mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

        var bufferSizeCandidate = 2
        while (bufferSizeCandidate < minBufferSize)
            bufferSizeCandidate *= 2
        val bufferSize = bufferSizeCandidate

        Log.v("AudioDTMFReceiver [$mState]",
                "minBufferSize = $minBufferSize, bufferSize = $bufferSize")

        val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize)

        try {
            val dataBuffer = ShortArray(bufferSize)
            val fftBuffer = DoubleArray(bufferSize * 2)
            val fft = DoubleFFT_1D(bufferSize.toLong())
            recorder.startRecording()

            while (!mStopped && mState < ReceiverState.RECEIVE_DONE) {
                val readSize = recorder.read(dataBuffer, 0, bufferSize)

                for (i in 0..readSize-1) {
                    fftBuffer[(2 * i)] = dataBuffer[i] / 32768.0
                    fftBuffer[(2 * i) + 1] = 0.0
                }
                fft.complexForward(fftBuffer)
                val peakList = searchPeaks(fftBuffer)

/*
                val bytesPerSample = 2            // As it is 16bit PCM
                val amplification: Double = 100.0 // choose a number as you like
                var index = 0
                var floatIndex = 0
                while (index < readSize - bytesPerSample) {
                    var sample: Double = 0.0
                    for (b in 0..bytesPerSample - 1) {
                        var v = dataBuffer[index + b]
                        if (b < bytesPerSample - 1 || bytesPerSample == 1)
                            v = v and 0xFF.toByte()
                        sample += v.toInt().shl(b * 8)
                    }
                    val sample32 = amplification * (sample / 32768.0)
                    dataBuffer[floatIndex] = sample32.toByte()

                    index += bytesPerSample
                    floatIndex += 1
                }
*/


                when (mState) {
                    ReceiverState.SEARCH_PREAMBLE -> searchPreamble(peakList)
                    ReceiverState.RECEIVE_DATA -> receiveData(peakList)
                }
            }
        } catch (x: Throwable) {
            Log.e("AudioDTMFReceiver [$mState]", "Error reading voice audio $x", x)
        } finally {
            recorder.stop()
            recorder.release()
            close()
        }/*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
    }

    fun searchPeaks(fftBuffer: DoubleArray): ArrayList<Double> {
        val peaks = ArrayList<Double>()

        for(i in 0..fftBuffer.size - 1 step 2) {
            val frequency = (i.toDouble() * mSamplingRate) / fftBuffer.size
            if (frequency > mSamplingRate / 2.0)
                break

            if ((frequency < 2700 && frequency > 2690) ||
                    (frequency < 2850 && frequency > 2840) ||
                    (frequency < 3110 && frequency > 3100) ||
                    (frequency < 4195 && frequency > 4185) ||
                    (frequency < 4555 && frequency > 4540) ||
                    (frequency < 4930 && frequency > 4920) ||
                    (frequency < 5440 && frequency > 5430)) {
                val real = fftBuffer[i]
                val imag = fftBuffer[i + 1]
                val lenPow = (real * real) + (imag * imag)
                if (mThreshold < lenPow) {
                    peaks.add(frequency)
                }
            }
        }

        return peaks
    }

    fun searchPreamble(peakList: ArrayList<Double>) {
        if (peakList.isEmpty())
            Log.i("AudioDTMFReceiver [$mState]", "EMPTY!")
        for(peak in peakList)
            Log.i("AudioDTMFReceiver [$mState]", "Peak at $peak")
    }

    fun receiveData(peakList: ArrayList<Double>) {

    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    fun close() {
        mStopped = true
        mState = ReceiverState.INIT
    }

    fun notifyPermissionGrant() {
        if (mState < ReceiverState.SEARCH_PREAMBLE)
            mState = ReceiverState.SEARCH_PREAMBLE
    }

}