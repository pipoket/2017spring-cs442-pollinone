package com.cs442.sexysuckzoo.pollinone

import android.hardware.*
import android.os.Handler
import android.util.Log
import android.view.MotionEvent


class LinearAccelerometerListener : SensorEventListener {
    private val TAG: String = javaClass.simpleName
    private var mUpdateCallback: (SensorEvent) -> Unit

    constructor(updateCallback: (SensorEvent) -> Unit) {
        mUpdateCallback = updateCallback
    }

    override fun onSensorChanged(event: SensorEvent) {
        mUpdateCallback(event)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.v(TAG, "onAccuracyChanged()")
    }
}

class GravityListener : SensorEventListener {
    private val TAG: String = javaClass.simpleName
    private var mUpdateCallback: (SensorEvent) -> Unit

    constructor(updateCallback: (SensorEvent) -> Unit) {
        mUpdateCallback = updateCallback
    }

    override fun onSensorChanged(event: SensorEvent) {
        mUpdateCallback(event)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.v(TAG, "onAccuracyChanged()")
    }
}

data class Vector3(val x: Float, val y: Float, val z: Float) {
    val length: Float = Math.sqrt(x.toDouble() * x + y * y + z * z).toFloat()

    operator fun plus(o: Vector3): Vector3 = Vector3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3): Vector3 = Vector3(x - o.x, y - o.y, z - o.z)
    operator fun times(o: Vector3): Float = x * o.x + y * o.y + z * o.z
    operator fun unaryMinus(): Vector3 = Vector3(-x, -y, -z)

    fun getUnitVector(): Vector3 {
        return Vector3(x / length, y / length, z / length)
    }
}

typealias TypeSensorCallback = (Float, Float, Float) -> Unit

class VoteMotionDetector {
    private val TAG: String = javaClass.simpleName

    private val mSensorManager: SensorManager

    private val mSensorLinearAccelerometer: Sensor
    private val mSensorGravity: Sensor

    private val mListenerLinearAccelerometer: LinearAccelerometerListener
    private val mListenerGravity: GravityListener

    private var mGravityUpdateCallback: TypeSensorCallback? = null
    private var mAccelerometerUpdateCallback: TypeSensorCallback? = null
    private val mHandUpCallback: ()->Unit
    private val mHandDownCallback: ()->Unit

    private var mLastAccelerometerValue: SensorEvent? = null
    private var mLastGravityValue: SensorEvent? = null

    private var mDetectFlag = false
    private val mDetectInterval: Long = 60  // milliseconds
    private val mMotionThreshold: Float = 6.0F  // m/s^2
    private val mDotProductThreshold: Float = 0.8F // Size of dot product
    private val mDetectHandler = Handler()

    private enum class VoteMotionEvent {
        STABLE, ACC_UP, ACC_DOWN
    }
    private var mMotionStableCount = 0
    private val mMotionStableThreshold = 4
    private var mMotionState = VoteMotionEvent.STABLE

    constructor(sensorManager: SensorManager,
                handUpCallback: ()->Unit,
                handDownCallback: ()->Unit) {
        mSensorManager = sensorManager
        mListenerLinearAccelerometer = LinearAccelerometerListener(this::onAccelerometerUpdate)
        mListenerGravity = GravityListener(this::onGravityUpdate)
        mHandUpCallback = handUpCallback
        mHandDownCallback = handDownCallback
        mSensorLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    fun startDetection() {
        mSensorManager.registerListener(mListenerLinearAccelerometer, mSensorLinearAccelerometer,
                SensorManager.SENSOR_DELAY_UI)
        mSensorManager.registerListener(mListenerGravity, mSensorGravity,
                SensorManager.SENSOR_DELAY_UI)
        mMotionStableCount = 0
        mMotionState = VoteMotionEvent.STABLE
        mDetectFlag = true;
        detectVoteMotion()
    }

    fun stopDetection() {
        mSensorManager.unregisterListener(mListenerLinearAccelerometer)
        mSensorManager.unregisterListener(mListenerGravity)
        mDetectHandler.removeCallbacks(this::detectVoteMotion)
        mDetectFlag = false
    }

    fun setGravityUpdateCallback(callback: TypeSensorCallback) {
        mGravityUpdateCallback = callback
    }

    fun setAccelerometerUpdateCallback(callback: TypeSensorCallback) {
        mAccelerometerUpdateCallback = callback
    }

    private fun updateMotionState(nextMotionState: VoteMotionEvent) {
        when (mMotionState) {
            VoteMotionEvent.ACC_DOWN -> {
                when (nextMotionState) {
                    VoteMotionEvent.ACC_UP -> mHandDownCallback?.invoke()
                    else -> {}
                }
            }
            VoteMotionEvent.ACC_UP -> {
                when (nextMotionState) {
                    VoteMotionEvent.ACC_DOWN -> mHandUpCallback?.invoke()
                    else -> {}
                }
            }
            else -> {}
        }
        if (mMotionState != nextMotionState) {
            Log.d(TAG, "updateVoteMotionState $mMotionState -> $nextMotionState")
            mMotionState = nextMotionState
        }
    }

    fun detectVoteMotion() {
        if (mLastAccelerometerValue == null || mLastAccelerometerValue == null) {
            mDetectHandler.postDelayed(this::detectVoteMotion, mDetectInterval)
            return
        }

        val aVec = Vector3(
                mLastAccelerometerValue!!.values[0],
                mLastAccelerometerValue!!.values[1],
                mLastAccelerometerValue!!.values[2])
        val gVec = Vector3(
                mLastGravityValue!!.values[0],
                mLastGravityValue!!.values[1],
                mLastGravityValue!!.values[2])

        if (aVec.length < mMotionThreshold) {
            if (++mMotionStableCount > mMotionStableThreshold) updateMotionState(VoteMotionEvent.STABLE)
            if (mDetectFlag) mDetectHandler.postDelayed(this::detectVoteMotion, mDetectInterval)
            return
        }

        val aUnitVec = aVec.getUnitVector()
        val gUnitVec = gVec.getUnitVector()

        val dotVal = gUnitVec * aUnitVec

        if (Math.abs(dotVal) < mDotProductThreshold) {
            if (++mMotionStableCount > mMotionStableThreshold) updateMotionState(VoteMotionEvent.STABLE)
            if (mDetectFlag) mDetectHandler.postDelayed(this::detectVoteMotion, mDetectInterval)
            return
        }
        mMotionStableCount = 0

        if (dotVal < 0) {
            Log.d(TAG, "[Acceleration DOWN] AccLength: ${aVec.length} DotVal: $dotVal")
            updateMotionState(VoteMotionEvent.ACC_DOWN)
        } else {
            Log.d(TAG, "[Acceleration UP] AccLength: ${aVec.length} DotVal: $dotVal")
            updateMotionState(VoteMotionEvent.ACC_UP)
        }

        if (mDetectFlag) mDetectHandler.postDelayed(this::detectVoteMotion, mDetectInterval)
    }

    fun onAccelerometerUpdate(event: SensorEvent) {
        mLastAccelerometerValue = event
        mAccelerometerUpdateCallback?.invoke(event.values[0], event.values[1], event.values[2])
    }

    fun onGravityUpdate(event: SensorEvent) {
        mLastGravityValue = event
        mGravityUpdateCallback?.invoke(event.values[0], event.values[1], event.values[2])
    }
}