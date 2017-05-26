package info.pipoket.pollinone_transoversound_dev

import android.media.AudioFormat
import android.util.Log
import com.google.zxing.common.reedsolomon.GenericGF
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder
import android.media.AudioTrack
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioManager
import android.media.AudioFormat.ENCODING_PCM_8BIT
import android.media.AudioRecord


/**
 * Created by pipoket on 2017. 5. 20..
 */
class PollInOneToASender {
    private val REV_TONEMAP = ToneMap.revTrueHigh

    private val mSamplingRate = 44100
    private val mSendChunkSize = 1024
    private val mToneCount = 1
    private val mPreambleCount = 3
    private val mDelayCount = 4


    private val mThreshold = 1.0
    private val mPreambleChar  = "g"
    private val mPayloadSize = 8  // characters

    fun sendData(number: Int) {
        var rawHexStrNumber = number.toString(16)
        while (rawHexStrNumber.length < 4) {
           rawHexStrNumber = "0$rawHexStrNumber"
        }

        val hexStrNumber = rawHexStrNumber
        val dataArray = IntArray(6, { idx ->
            if (idx < 4) {
                hexStrNumber[idx].toInt()
            } else {
                0
            }
        })


        val encoder = ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256)
        encoder.encode(dataArray, 2)

        var dataToSend = ""
        for(i in 0..dataArray.size - 1) {
            if (i < 4) {
                dataToSend += dataArray[i].toChar()
            } else {
                var conv = dataArray[i].toString(16)
                while (conv.length < 2) {
                    conv = "0$conv"
                }
                dataToSend += conv
            }
        }

        Log.i("PollInOneTOASender", "$dataToSend")
        playSound(dataToSend)
    }

    fun close() {

    }


    private fun playSound(data: String) {
        // AudioTrack definition
        val minBufferSize = AudioTrack.getMinBufferSize(
                mSamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT)

        var bufferSizeCandidate = 2
        while (bufferSizeCandidate < minBufferSize)
            bufferSizeCandidate *= 2
        val bufferSize = bufferSizeCandidate

        val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSamplingRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
        )

        val wholeBuffer = ShortArray(
                (mSendChunkSize * mPreambleCount) +
                        (mSendChunkSize * mDelayCount) +
                        (((mSendChunkSize * mToneCount) + (mSendChunkSize * mDelayCount)) * data.length)
        )
        var wholeBufferPtr = 0;

        // Preamble
        let {
            val sound = DoubleArray(mSendChunkSize * mPreambleCount)
            val buffer = ShortArray(mSendChunkSize * mPreambleCount)
            val frequency = REV_TONEMAP[mPreambleChar]!!
            for (i in 0..sound.size - 1) {
                sound[i] = Math.sin((2.0 * Math.PI * i / (mSamplingRate.toDouble() / frequency)))
                buffer[i] = (sound[i] * Short.MAX_VALUE).toShort()
            }
            System.arraycopy(buffer, 0, wholeBuffer, wholeBufferPtr, mSendChunkSize * mPreambleCount)
            wholeBufferPtr += mSendChunkSize * mPreambleCount
        }

        // Preamble delay
        let {
            val buffer = ShortArray(mSendChunkSize * mDelayCount)
            for (i in 0..buffer.size - 1)
                buffer[i] = 0
            System.arraycopy(buffer, 0, wholeBuffer, wholeBufferPtr, mSendChunkSize * mDelayCount)
            wholeBufferPtr += mSendChunkSize * mDelayCount
        }

        // Data
        for (c in 0..data.length - 1) {
            let {
                val sound = DoubleArray(mSendChunkSize)
                val buffer = ShortArray(mSendChunkSize)
                val frequency = REV_TONEMAP[data.get(c).toString()]!!
                for (i in 0..sound.size - 1) {
                    sound[i] = Math.sin((2.0 * Math.PI * i / (mSamplingRate.toDouble() / frequency)))
                    buffer[i] = (sound[i] * Short.MAX_VALUE).toShort()
                }
                System.arraycopy(buffer, 0, wholeBuffer, wholeBufferPtr, mSendChunkSize)
                wholeBufferPtr += mSendChunkSize
            }

            let {
                val buffer = ShortArray(mSendChunkSize * mDelayCount)
                for (i in 0..buffer.size - 1)
                    buffer[i] = 0
                System.arraycopy(buffer, 0, wholeBuffer, wholeBufferPtr, mSendChunkSize * mDelayCount)
                wholeBufferPtr += mSendChunkSize * mDelayCount
            }
        }

        audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume())
        audioTrack.play()

        audioTrack.write(wholeBuffer, 0, wholeBuffer.size)
        audioTrack.stop()
        audioTrack.release()

    }
}