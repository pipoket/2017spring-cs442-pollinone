package info.pipoket.pollinone_raisehand_dev

import android.content.Context
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var mVoteMotionDetector: VoteMotionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sensorManager: SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mVoteMotionDetector = VoteMotionDetector(sensorManager)
        mVoteMotionDetector.setAccelerometerUpdateCallback(this::onAccelerometerUpdate)
        mVoteMotionDetector.setGravityUpdateCallback(this::onGravityUpdate)
    }

    override fun onResume() {
        mVoteMotionDetector.startDetection()
        super.onResume()
    }

    override fun onPause() {
        mVoteMotionDetector.stopDetection()
        super.onPause()
    }

    fun onAccelerometerUpdate(valueX: Float, valueY: Float, valueZ: Float) {
        val labelX: TextView = findViewById(R.id.textAccXValue) as TextView
        val labelY: TextView = findViewById(R.id.textAccYValue) as TextView
        val labelZ: TextView = findViewById(R.id.textAccZValue) as TextView

        labelX.text = valueX.toString()
        labelY.text = valueY.toString()
        labelZ.text = valueZ.toString()
    }

    fun onGravityUpdate(valueX: Float, valueY: Float, valueZ: Float) {
        val labelX: TextView = findViewById(R.id.textGravityXValue) as TextView
        val labelY: TextView = findViewById(R.id.textGravityYValue) as TextView
        val labelZ: TextView = findViewById(R.id.textGravityZValue) as TextView

        labelX.text = valueX.toString()
        labelY.text = valueY.toString()
        labelZ.text = valueZ.toString()
    }
}
