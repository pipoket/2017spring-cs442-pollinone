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
import java.nio.ByteOrder.LITTLE_ENDIAN
import android.R.attr.order
import android.support.annotation.NonNull
import java.util.*


/**
 * Created by pipoket on 2017. 5. 16..
 */

class PollInOneToAReceiver(
        cbStateUpdate: (ReceiverState) -> Unit,
        cbSuccess: (Pair<String, Int>) -> Unit,
        cbFailure: (ErrorCode) -> Unit
) : Thread() {
    private val mRandom = Random()
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

    private val TONEMAP_TOP = ToneMap.top
    private val TONEMAP_BOTTOM = ToneMap.bottom

    private val mParseChunkSize = 1024
    private val mSamplingRate = 44100
    private val mSampleDuration = 1.15 // seconds
    private val mSampleBufferSize = (mSamplingRate * mSampleDuration).toInt()
    private var mState = ReceiverState.INIT

    private val mThreshold = 0.1
    private val mPreambleThreshold = 1.0
    private val mPayloadSize = 8  // characters

    // Both are in seconds
    private val mToneLengthMultiplier = 2
    private val mGuardLengthMultiplier = 4

    // Normalizer variables
    val mNormalizerDBLevel = Math.pow(10.0, -1.0 / 20.0).toFloat()
    var mNormalizerMax = 0
    var mNormalizerMin = 0
    var mNormalizerAlignment = 0
    var mNormalizerCount = 1

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

                    // Normalize recorded buffer
                    normalize(dataBuffer, copySize)

                    // Copy to the aggregation buffer
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
                            mCbSuccess(result)
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

    private fun normalize(dataBuffer: ShortArray, bufferSize: Int) {
        for (i in 0..bufferSize - 1) {
            val sample = dataBuffer[i]
            val max = mNormalizerMax
            val min = mNormalizerMin
            val count = mNormalizerCount
            val alignment = mNormalizerAlignment

            mNormalizerMax = Math.max(max, sample.toInt())
            mNormalizerMin = Math.min(min, sample.toInt())
            mNormalizerAlignment = (count - 1) / count * alignment + sample / count
            mNormalizerCount++
        }
        val extent = Math.max(Math.abs(mNormalizerMax), Math.abs(mNormalizerMin))
        val factor = mNormalizerDBLevel * java.lang.Short.MAX_VALUE / extent
        for (i in 0..bufferSize - 1) {
            dataBuffer[i] = ((dataBuffer[i] - mNormalizerAlignment) * factor).toShort()
        }
    }

    fun applyReedSolomon(payload: ArrayList<String>): Pair<String, Int>? {
        val rawDataArray = IntArray(6, {idx ->
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
            val dataInt = dataString.toInt(16)
            return Pair(dataString, dataInt)
        } catch (x: Throwable) {
            Log.e("AudioDTMFReceiver [$mState]", "Error while final ReedSolomon parsing", x)
        }

        return null
    }

    fun searchPeaks(fftBuffer: DoubleArray, flagPreamble: Boolean): Pair<Int, Double>? {
        var peakTop: Pair<Int, Double>? = null
        var peakBottom: Pair<Int, Double>? = null

        val threshold: Double
        if (flagPreamble)
            threshold = mPreambleThreshold
        else
            threshold = mThreshold

        for(i in 0..fftBuffer.size - 1 step 2) {
            val freq = (i.toDouble() * mSamplingRate) / fftBuffer.size
            if (freq > mSamplingRate / 2.0)
                break
            val intFreq = freq.toInt()

            if (TONEMAP_TOP.containsKey(intFreq)) {
                val real = fftBuffer[i]
                val imag = fftBuffer[i + 1]
                val lenPow = (real * real) + (imag * imag)
                if (lenPow >= threshold && peakTop?.second ?: 0.0 < lenPow) {
                    peakTop = Pair(intFreq, lenPow)
                    //Log.i("AudioDTMFReceiver [$mState]", "PeakTop: $peakTop")
                }
            }

            if (TONEMAP_BOTTOM.containsKey(intFreq)) {
                val real = fftBuffer[i]
                val imag = fftBuffer[i + 1]
                val lenPow = (real * real) + (imag * imag)
                if (lenPow >= threshold && peakBottom?.second ?: 0.0 < lenPow) {
                    peakBottom = Pair(intFreq, lenPow)
                    //Log.i("AudioDTMFReceiver [$mState]", "PeakBottom: $peakBottom")
                }
            }
        }

        Log.i("AudioDTMFReceiver [$mState]", "Peak RESULT===>: $peakTop, $peakBottom")
        if (peakTop != null && peakBottom != null) {
            if (TONEMAP_TOP[peakTop.first] == TONEMAP_BOTTOM[peakBottom.first])
                return peakTop
            else
                return null
        } else {
            return peakTop
        }
    }

    fun applyHannWindow(buffer: ShortArray, index: Int, fftSize: Int): Double {
        return (buffer[index] / 32768.0) * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * index.toDouble() / fftSize))
    }

    fun parseData(sampleBuffer: ShortArray): Pair<String, Int>? {
        Log.i("AudioDTMFReceiver [$mState]", "============== parseData() ==============")
        // Search for the preamble
        val fft = DoubleFFT_1D(mParseChunkSize.toLong())
        var preambleChunkIdx = -1
        val fftBuffer = DoubleArray(mParseChunkSize * 2)

        for (chunkIdx in 0..sampleBuffer.size - 1 step mParseChunkSize) {
            for (fftIndex in 0..mParseChunkSize - 1) {
                val sampleIndex = chunkIdx + fftIndex
                if (sampleIndex < mSampleBufferSize)
                    fftBuffer[(2 * fftIndex)] = applyHannWindow(sampleBuffer, sampleIndex, mParseChunkSize)
                else
                    fftBuffer[(2 * fftIndex)] = 0.0

                fftBuffer[(2 * fftIndex) + 1] = 0.0
            }
            fft.complexForward(fftBuffer)
            val peak = searchPeaks(fftBuffer, true)
            if (peak == null)
                continue
            else {
                if (peak.first == ToneMap.bottomPreambleFreq ||
                        peak.first == ToneMap.topPreambleFreq) {
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
        var flagPreamble = true
        var flagSecondTry = false

        val dataList = ArrayList<String>()
        while (dataIdx < sampleBuffer.size) {
            for (fftIndex in 0..mParseChunkSize - 1) {
                val sampleIndex = dataIdx + fftIndex
                if (sampleIndex < sampleBuffer.size)
                    fftBuffer[(2 * fftIndex)] = applyHannWindow(sampleBuffer, sampleIndex, mParseChunkSize)
                else
                    fftBuffer[(2 * fftIndex)] = 0.0

                fftBuffer[(2 * fftIndex) + 1] = 0.0
            }
            fft.complexForward(fftBuffer)

            val peak = searchPeaks(fftBuffer, flagPreamble)
            Log.i("AudioDTMFReceiver [$mState]", "$peak")
            if (flagPreamble) {
                if (peak == null) {
                    flagPreamble = false
                }
            } else {
                // If the preamble is seen again, flush existing data and start again
                if (peak?.first == ToneMap.bottomPreambleFreq ||
                        peak?.first == ToneMap.topPreambleFreq) {
                    Log.i("AudioDTMFReceiver [$mState]", "Detected Preamble!!: clean buffer and start again")
                    flagPreamble = true
                    dataList.clear()
                    continue
                }

                // Handle this as an error case
                if (peak == null) {
                    if (flagSecondTry) {
                        val fillChar = mRandom.nextInt(16).toString(16)
                        dataList.add(fillChar)

                        Log.i("AudioDTMFReceiver [$mState]", "Detection failure: $fillChar (total: ${dataList.size})")

                        dataIdx += (mGuardLengthMultiplier - 1) * mParseChunkSize
                        flagSecondTry = false
                    } else {
                        dataIdx += mParseChunkSize
                        flagSecondTry = true
                    }
                } else {
                    val dataChar = TONEMAP_TOP[peak.first]!!

                    if (dataList.isNotEmpty() && dataChar == dataList.last() && peak.second < 1.0) {
                        dataIdx += mParseChunkSize / 2
                        continue
                    }

                    dataList.add(dataChar)
                    Log.i("AudioDTMFReceiver [$mState]", "Detected bit: $dataChar (total: ${dataList.size})")
                    dataIdx += (mToneLengthMultiplier - 1) * mParseChunkSize
                    dataIdx += mGuardLengthMultiplier * mParseChunkSize
                    flagSecondTry = false
                }

                if (dataList.size >= mPayloadSize) {
                    return applyReedSolomon(dataList)
                }

                continue
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