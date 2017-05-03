package info.pipoket.pollinone_raisehand_dev

import android.content.Context
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var mVoteMotionDetector: VoteMotionDetector
    private lateinit var mSound: MediaActionSound

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sensorManager: SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mVoteMotionDetector = VoteMotionDetector(
                sensorManager, this::onHandUpDetected, this::onHandDownDetected)

        // Following callback setters are only for debugging
        mVoteMotionDetector.setAccelerometerUpdateCallback(this::onAccelerometerUpdate)
        mVoteMotionDetector.setGravityUpdateCallback(this::onGravityUpdate)

        // Following initialization is only for debugging
        mSound = MediaActionSound()
        mSound.load(MediaActionSound.START_VIDEO_RECORDING)
        mSound.load(MediaActionSound.SHUTTER_CLICK)
    }

    override fun onDestroy() {
        mSound.release()
        mVoteMotionDetector.stopDetection()
        super.onDestroy()
    }

    override fun onResume() {
        mVoteMotionDetector.startDetection()
        super.onResume()
    }

    override fun onPause() {
        mVoteMotionDetector.stopDetection()
        super.onPause()
    }

    fun onHandUpDetected() {
        mSound.play(MediaActionSound.START_VIDEO_RECORDING)
    }

    fun onHandDownDetected() {
        mSound.play(MediaActionSound.SHUTTER_CLICK)
    }

    fun onAccelerometerUpdate(valueX: Float, valueY: Float, valueZ: Float) {
        // This callback is only for debugging
        val labelX: TextView = findViewById(R.id.textAccXValue) as TextView
        val labelY: TextView = findViewById(R.id.textAccYValue) as TextView
        val labelZ: TextView = findViewById(R.id.textAccZValue) as TextView

        labelX.text = valueX.toString()
        labelY.text = valueY.toString()
        labelZ.text = valueZ.toString()
    }

    fun onGravityUpdate(valueX: Float, valueY: Float, valueZ: Float) {
        // This callback is only for debugging
        val labelX: TextView = findViewById(R.id.textGravityXValue) as TextView
        val labelY: TextView = findViewById(R.id.textGravityYValue) as TextView
        val labelZ: TextView = findViewById(R.id.textGravityZValue) as TextView

        labelX.text = valueX.toString()
        labelY.text = valueY.toString()
        labelZ.text = valueZ.toString()
    }
}
