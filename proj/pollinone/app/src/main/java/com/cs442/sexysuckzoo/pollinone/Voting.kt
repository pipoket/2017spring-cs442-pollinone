package com.cs442.sexysuckzoo.pollinone

import android.content.Context
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_voting.*


class Voting : AppCompatActivity() {
    private lateinit var mSensorManager: SensorManager
    private lateinit var mVoteMotionDetector: VoteMotionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voting)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mVoteMotionDetector = VoteMotionDetector(
                mSensorManager,
                this::onHandUpDetected,
                this::onHandDownDetected
        )
    }

    override fun onResume() {
        super.onResume()
        mVoteMotionDetector.startDetection()
    }

    override fun onPause() {
        super.onPause()
        mVoteMotionDetector.stopDetection()
    }

    fun onHandUpDetected() {
        if (!toggleButton.isChecked) {
            toggleButton.toggle()
        }
    }

    fun onHandDownDetected() {
        if (toggleButton.isChecked) {
            toggleButton.toggle()
        }
    }
}
