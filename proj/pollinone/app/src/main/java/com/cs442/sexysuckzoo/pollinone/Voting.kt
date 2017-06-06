package com.cs442.sexysuckzoo.pollinone

import android.content.Context
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.cs442.sexysuckzoo.pollinone.model.Member
import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import kotlinx.android.synthetic.main.activity_voting.*


class Voting : AppCompatActivity() {
    val TAG = "Voting"
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
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                vote()
            } else {
                withdraw()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mVoteMotionDetector.startDetection()
    }

    override fun onPause() {
        super.onPause()
        mVoteMotionDetector.stopDetection()
    }

    fun refreshUI(member: Member) {
        // @TODO: Feedback to user which selection has been chosen
        StorageService.instance.member?.item = member.item
    }

    fun vote() {
        val voteId = StorageService.instance.vote?.id as Int
        val credential = StorageService.instance.member?.credential as String
        PollService.instance.vote(voteId, credential).map {
            refreshUI(it)
        }.doOnError {
            Log.wtf(TAG, it.message)
            Toast.makeText(applicationContext, "Failed voting", Toast.LENGTH_LONG).show()
            toggleButton.toggle()
        }.onErrorReturn {

        }.subscribe {

        }
    }

    fun withdraw() {
        val voteId = StorageService.instance.vote?.id as Int
        val credential = StorageService.instance.member?.credential as String
        PollService.instance.withdraw(voteId, credential).map {
            refreshUI(it)
        }.doOnError {
            Toast.makeText(applicationContext, "Failed withdrawing", Toast.LENGTH_LONG).show()
            toggleButton.toggle()
        }.onErrorReturn {

        }.subscribe {

        }
    }

    fun onHandUpDetected() {
        if (!toggleButton.isChecked) {
            toggleButton.toggle()
            vote()
        }
    }

    fun onHandDownDetected() {
        if (toggleButton.isChecked) {
            toggleButton.toggle()
            withdraw()
        }
    }
}
