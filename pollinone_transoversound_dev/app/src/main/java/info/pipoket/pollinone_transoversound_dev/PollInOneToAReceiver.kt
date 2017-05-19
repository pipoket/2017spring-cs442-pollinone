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

class PollInOneToAReceiver(
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

    private val TONEMAP = ToneMap.High

    private val mParseChunkSize = 1024
    private val mSamplingRate = 44100
    private val mSampleDuration =  1.5 // seconds
    private val mSampleBufferSize = (mSamplingRate * mSampleDuration).toInt()
    private var mState = ReceiverState.INIT

    private val mThreshold = 1.0
    private val mPreambleChar  = "g"
    private val mPayloadSize = 8  // characters

    // Both are in seconds
    private val mToneLength: Double = mParseChunkSize.toDouble() / mSamplingRate
    private val mGuardLengthMultiplier = 3

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
                    recorder.read(dataBuffer, 0, bufferSize)
                    val sampleBufferRemSize = mSampleBufferSize - sampleBufferPtr
                    val copySize: Int
                    if (sampleBufferRemSize < bufferSize)
                        copySize = sampleBufferRemSize
                    else
                        copySize = bufferSize

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
                            Log.i("ReedSolomon", "Got result $result")
                            val payload = result!!

                            val rawDataArray = IntArray(6, { idx ->
                                if (idx < 4) {
                                    Log.i("ReedSolomon", "$idx -> ${payload[idx].toCharArray()[0].toInt()}")
                                    payload[idx].toCharArray()[0].toInt()
                                } else if (idx == 4) {
                                    val chksum = payload[4] + payload[5]
                                    Log.i("ReedSolomon", "$idx -> ${chksum.toInt(16)}")
                                    chksum.toInt(16)
                                } else {
                                    val chksum = payload[6] + payload[7]
                                    Log.i("ReedSolomon", "$idx -> ${chksum.toInt(16)}")
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

        //Log.i("AudioDTMFReceiver [$mState]", "$peaks")
        if (peaks.size > 1) {
            var maxPow = 0.0
            var maxPeak: Pair<Int, Double>? = null
            peaks.forEach { peak ->
                if (maxPow < peak.second) {
                    maxPow = peak.second
                    maxPeak = peak
                }
            }
            //Log.i("AudioDTMFReceiver [$mState]", "$maxPeak")
            return maxPeak
        } else if (peaks.size == 1) {
            //Log.i("AudioDTMFReceiver [$mState]", "${peaks[0]}")
            return peaks[0]
        } else {
            //Log.i("AudioDTMFReceiver [$mState]", "NULL")
            return null
        }
    }

    fun parseData(sampleBuffer: ShortArray): ArrayList<String>? {
        // Search for the preamble
        val fft = DoubleFFT_1D(mParseChunkSize.toLong())
        var preambleChunkIdx = -1
        val fftBuffer = DoubleArray(mParseChunkSize * 2)

        for (chunkIdx in 0..sampleBuffer.size - 1 step mParseChunkSize) {
            for (fftIndex in 0..mParseChunkSize - 1) {
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
                if (peak.first == 3746 || peak.first == 7536) {
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
        var dataPos: Double = 0.0
        var prevDataFreq = 0
        var flagPreamble = true
        var flagSecondTry = false

        val dataList = ArrayList<String>()
        while (dataIdx < sampleBuffer.size) {
            for (fftIndex in 0..mParseChunkSize - 1) {
                val sampleIndex = dataIdx + fftIndex
                if (sampleIndex < sampleBuffer.size)
                    fftBuffer[(2 * fftIndex)] = sampleBuffer[sampleIndex] / 32768.0
                else
                    fftBuffer[(2 * fftIndex)] = 0.0

                fftBuffer[(2 * fftIndex) + 1] = 0.0
            }
            fft.complexForward(fftBuffer)

            val peak = searchPeaks(fftBuffer)
            Log.i("AudioDTMFReceiver [$mState]", "$peak")
            if (flagPreamble) {
                if (peak == null)
                    flagPreamble = false
            } else {
                // Handle repeated preamble as an error case
                if (peak?.first == 3746 || peak?.first == 7536)
                    break

                if (dataList.isEmpty()) {
                    if (peak != null) {
                        val dataChar = TONEMAP[peak.first]!!
                        dataList.add(dataChar)
                        Log.i("AudioDTMFReceiver [$mState]", "Detected bit: $dataChar (total: ${dataList.size})")
                        dataIdx += mGuardLengthMultiplier * mParseChunkSize
                        continue
                    }
                } else {
                    // Handle this as an error case
                    if (peak == null) {
                        if (flagSecondTry) {
                            dataList.add("0")
                            Log.i("AudioDTMFReceiver [$mState]", "Detection failure: 0 (total: ${dataList.size})")
                            dataIdx += mGuardLengthMultiplier * mParseChunkSize
                            flagSecondTry = false
                        } else {
                            dataIdx += mParseChunkSize
                            flagSecondTry = true
                        }
                    } else {
                        val dataChar = TONEMAP[peak.first]!!
                        dataList.add(dataChar)
                        Log.i("AudioDTMFReceiver [$mState]", "Detected bit: $dataChar (total: ${dataList.size})")
                        dataIdx += mGuardLengthMultiplier * mParseChunkSize
                        flagSecondTry = false
                    }

                    if (dataList.size >= mPayloadSize)
                        return dataList

                    continue
                }
            }

            dataIdx += mParseChunkSize
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