package info.pipoket.pollinone_transoversound_dev

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.util.Log
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.concurrent.thread


/**
 * Created by pipoket on 2017. 5. 16..
 */

class AudioDTMFReceiver(
        cbStateUpdate: (ReceiverState) -> Unit,
        cbSuccess: (String) -> Unit,
        cbFailure: (ErrorCode) -> Unit
) : Thread() {
    private var mStopped = true
    private val mCbStateUpdate = cbStateUpdate
    private val mCbSuccess = cbSuccess
    private val mCbFailure = cbFailure

    enum class ReceiverState {
        INIT,
        LISTEN,
        PARSE_DATA,
        RECEIVE_DONE,
    }

    enum class ErrorCode {
        USER_CANCELED,
        NO_DETECTION,
        UNKNOWN,
    }

    val TONEMAP: HashMap<Int, String> = hashMapOf(
            872 to "0",
            1044 to "1",
            1173 to "2",
            1313 to "3",
            1475 to "4",
            1658 to "5",
            1862 to "6",
            2088 to "7",
            2347 to "8",
            2627 to "9",
            2950 to "a",
            3316 to "b",
            3725 to "c",
            4177 to "d",
            4694 to "e",
            5264 to "f",
            5770 to "g"
    )
    val REV_TONEMAP: HashMap<String, Int> = hashMapOf (
            "0" to 872,
            "1" to 1044,
            "2" to 1173,
            "3" to 1313,
            "4" to 1475,
            "5" to 1658,
            "6" to 1862,
            "7" to 2088,
            "8" to 2347,
            "9" to 2627,
            "a" to 2950,
            "b" to 3316,
            "c" to 3725,
            "d" to 4177,
            "e" to 4694,
            "f" to 5264,
            "g" to 5770
    )

    private val mSamplingRate = 44100
    private val mSampleDuration = 4 // seconds
    private val mSampleBufferSize = (mSamplingRate * mSampleDuration).toInt()
    private var mState = ReceiverState.INIT

    private val mThreshold = 2.0
    private val mPreambleChar  = "g"
    private val mPayloadSize = 8  // characters

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

        while (mState < ReceiverState.LISTEN) {
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

        val rawSampleBuffer = ShortArray(mSampleBufferSize)

        Log.v("AudioDTMFReceiver [$mState]",
                "minBufferSize = $minBufferSize, bufferSize = $bufferSize")

        val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                mSamplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize)
        recorder.startRecording()
        try {
            while (!mStopped) {

                var sampleBufferPtr = 0
                val dataBuffer = ShortArray(bufferSize)
                while (!mStopped) {
                    val readSize = recorder.read(dataBuffer, 0, bufferSize)
                    val sampleBufferRemSize = mSampleBufferSize - sampleBufferPtr
                    val copySize: Int
                    if (sampleBufferRemSize < readSize)
                        copySize = sampleBufferRemSize
                    else
                        copySize = readSize

                    System.arraycopy(dataBuffer, 0, rawSampleBuffer, sampleBufferPtr, copySize)
                    sampleBufferPtr += copySize

                    if (sampleBufferPtr >= mSampleBufferSize) {
                        mState = ReceiverState.PARSE_DATA
                        mCbStateUpdate(mState)
                        break
                    }
                }

                // Append sample buffer to itself to detect overlapping signal
                val sampleBuffer = ShortArray(mSampleBufferSize * 2)
                System.arraycopy(rawSampleBuffer, 0, sampleBuffer, 0, mSampleBufferSize)
                System.arraycopy(rawSampleBuffer, 0, sampleBuffer, mSampleBufferSize, mSampleBufferSize)

                if (mState == ReceiverState.PARSE_DATA) {
                    thread {
                        val result = parseData(sampleBuffer)
                        if (result != null) {
                            val dataPayload = result!!
                            val dataString = dataPayload.fold("", { acc, s -> val newAcc = acc + s; newAcc })
                            mCbSuccess(dataString)
                            mState = ReceiverState.RECEIVE_DONE
                            close()
                        } else {
                            mState = ReceiverState.LISTEN
                            mCbStateUpdate(mState)
                        }
                    }
                } else {
                    close()
                }
            }
        } catch (x: Throwable) {
            Log.e("AudioDTMFReceiver [$mState]", "Error reading voice audio $x", x)
            mCbFailure(ErrorCode.UNKNOWN)
        } finally {
            recorder.stop()
            recorder.release()
            close()
        }
    }


    fun searchPeaks(fftBuffer: DoubleArray): Pair<Int, Double>? {
        val peaks = ArrayList<Pair<Int, Double>>()

        for(i in 0..fftBuffer.size - 1 step 2) {
            val freq = (i.toDouble() * mSamplingRate) / fftBuffer.size
            if (freq > mSamplingRate / 2.0)
                break
            val intFreq = freq.toInt()

            if (TONEMAP.containsKey(intFreq)) {
                val real = fftBuffer[i]
                val imag = fftBuffer[i + 1]
                val lenPow = (real * real) + (imag * imag)
                if (mThreshold < lenPow)
                    peaks.add(Pair<Int, Double>(intFreq, lenPow))
            }
        }

        Log.i("AudioDTMFReceiver [$mState]", "$peaks")
        if (peaks.size > 2) {
            var maxPow = 0.0
            var maxPeak: Pair<Int, Double>? = null
            peaks.forEach { peak ->
                if (maxPow < peak.second) {
                    maxPow = peak.second
                    maxPeak = peak
                }
            }
            return maxPeak
        }
        else if (peaks.size == 1)
            return peaks[0]
        else
            return null
    }

    fun parseData(sampleBuffer: ShortArray): ArrayList<String>? {
        // Search for the preamble
        val chunkSize = 4096
        val fft = DoubleFFT_1D(chunkSize.toLong())
        var preambleChunkIdx = -1
        val fftBuffer = DoubleArray(chunkSize * 2)

        for (chunkIdx in 0..sampleBuffer.size - 1 step chunkSize)
        {
            for (fftIndex in 0..chunkSize-1) {
                val sampleIndex = chunkIdx + fftIndex
                if (sampleIndex < mSampleBufferSize)
                    fftBuffer[(2 * fftIndex)] = sampleBuffer[sampleIndex] / 32768.0
                else
                    fftBuffer[(2 * fftIndex)] = 0.0

                fftBuffer[(2 * fftIndex) + 1] = 0.0
            }
            fft.complexForward(fftBuffer)
            val peak = searchPeaks(fftBuffer)
            if (peak == null)
                continue
            else {
                if (peak.first == REV_TONEMAP[mPreambleChar]) {
                    preambleChunkIdx = chunkIdx
                    Log.i("AudioDTMFReceiver [$mState]", "Found preamble")
                    break
                }
            }
        }

        if (preambleChunkIdx < 0) {
            Log.i("AudioDTMFReceiver [$mState]", "Preamble not found")
            return null
        }

        // Try to parse out data after the preamble chunk
        var dataIdx = preambleChunkIdx
        var prevDataFreq = 0

        val dataList = ArrayList<String>()
        while (dataIdx < sampleBuffer.size) {
            for (fftIndex in 0..chunkSize-1) {
                val sampleIndex = dataIdx + fftIndex
                if (sampleIndex < mSampleBufferSize)
                    fftBuffer[(2 * fftIndex)] = sampleBuffer[sampleIndex] / 32768.0
                else
                    fftBuffer[(2 * fftIndex)] = 0.0

                fftBuffer[(2 * fftIndex) + 1] = 0.0
            }
            fft.complexForward(fftBuffer)

            val peak = searchPeaks(fftBuffer)
            if (peak == null) {
                prevDataFreq = 0
            } else {
                if (peak.first != REV_TONEMAP[mPreambleChar]) {
                    val dataFreq = peak.first

                    if (prevDataFreq != dataFreq) {
                        val dataChar = TONEMAP[dataFreq]!!
                        dataList.add(dataChar)
                        Log.i("AudioDTMFReceiver [$mState]", "freq: $dataFreq => char: $dataChar")
                        prevDataFreq = dataFreq
                    }
                    if (dataList.size >= mPayloadSize)
                        return dataList
                }
            }

            dataIdx += chunkSize
        }

        return null
    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    fun close() {
        mStopped = true
        mCbStateUpdate(mState)
        if (mState > ReceiverState.INIT && mState < ReceiverState.RECEIVE_DONE)
            mCbFailure(ErrorCode.USER_CANCELED)
        mState = ReceiverState.INIT
    }

    fun notifyPermissionGrant() {
        if (mState < ReceiverState.LISTEN) {
            mState = ReceiverState.LISTEN
            mCbStateUpdate(mState)
        }
    }

}