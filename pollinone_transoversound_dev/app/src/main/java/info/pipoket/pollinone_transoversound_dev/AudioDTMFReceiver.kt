package info.pipoket.pollinone_transoversound_dev

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.util.Log
import com.google.zxing.common.reedsolomon.GenericGF
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder
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
            990 to "0",
            1108 to "1",
            1216 to "2",
            1324 to "3",
            1442 to "4",
            1550 to "5",
            1658 to "6",
            1776 to "7",
            1884 to "8",
            1991 to "9",
            2110 to "a",
            2217 to "b",
            2325 to "c",
            2433 to "d",
            2551 to "e",
            2659 to "f",
            2767 to "g"
    )
    val REV_TONEMAP: HashMap<String, Int> = hashMapOf (
            "0" to 990,
            "1" to 1108,
            "2" to 1216,
            "3" to 1324,
            "4" to 1442,
            "5" to 1550,
            "6" to 1658,
            "7" to 1776,
            "8" to 1884,
            "9" to 1991,
            "a" to 2110,
            "b" to 2217,
            "c" to 2325,
            "d" to 2433,
            "e" to 2551,
            "f" to 2659,
            "g" to 2767
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
                            val payload = result!!

                            val rawDataArray = IntArray(6, { idx ->
                                if (idx < 4) {
                                    Log.v("ReedSolomon", "${payload[idx].toCharArray()[0].toInt()}")
                                    payload[idx].toCharArray()[0].toInt()
                                } else if (idx == 4) {
                                    val chksum = payload[4] + payload[5]
                                    Log.v("ReedSolomon", "${chksum.toInt(16)}")
                                    chksum.toInt(16)
                                } else {
                                    val chksum = payload[6] + payload[7]
                                    Log.v("ReedSolomon", "${chksum.toInt(16)}")
                                    chksum.toInt(16)
                                }
                            })

                            try {
                                val decoder = ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256)
                                decoder.decode(rawDataArray, 2)
                                val dataString = String(rawDataArray, 0, 4)
                                mCbSuccess(dataString)
                                mState = ReceiverState.RECEIVE_DONE
                                close()
                            } catch (x: Throwable) {
                                mState = ReceiverState.LISTEN
                                mCbStateUpdate(mState)
                            }
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
                if (peak.first == REV_TONEMAP[mPreambleChar]) {
                    if (!dataList.isEmpty())
                        dataList.clear()
                } else {
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