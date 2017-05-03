package info.pipoket.pollinone_raisehand_dev

import android.hardware.*
import android.util.Log
import java.lang.reflect.Type

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

typealias TypeSensorCallback = (Float, Float, Float) -> Unit

class VoteMotionDetector {
    private val mSensorManager: SensorManager

    private val mSensorLinearAccelerometer: Sensor
    private val mSensorGravity: Sensor

    private val mListenerLinearAccelerometer: LinearAccelerometerListener
    private val mListenerGravity: GravityListener

    private var mGravityUpdateCallback: TypeSensorCallback? = null
    private var mAccelerometerUpdateCallback: TypeSensorCallback? = null

    constructor(sensorManager: SensorManager) {
        mSensorManager = sensorManager
        mListenerLinearAccelerometer = LinearAccelerometerListener(this::onAccelerometerUpdate)
        mListenerGravity = GravityListener(this::onGravityUpdate)
        mSensorLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    fun startDetection() {
        mSensorManager.registerListener(mListenerLinearAccelerometer, mSensorLinearAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(mListenerGravity, mSensorGravity,
                SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopDetection() {
        mSensorManager.unregisterListener(mListenerLinearAccelerometer)
        mSensorManager.unregisterListener(mListenerGravity)
    }

    fun setGravityUpdateCallback(callback: TypeSensorCallback) {
        mGravityUpdateCallback = callback
    }

    fun setAccelerometerUpdateCallback(callback: TypeSensorCallback) {
        mAccelerometerUpdateCallback = callback
    }

    fun onAccelerometerUpdate(event: SensorEvent) {
        mAccelerometerUpdateCallback?.invoke(event.values[0], event.values[1], event.values[2])
    }

    fun onGravityUpdate(event: SensorEvent) {
        mGravityUpdateCallback?.invoke(event.values[0], event.values[1], event.values[2])
    }
}