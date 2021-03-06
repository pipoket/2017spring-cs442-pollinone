package com.cs442.sexysuckzoo.pollinone

import android.content.Context
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator;
import android.util.Log
import android.widget.Toast
import android.widget.ToggleButton
import com.cs442.sexysuckzoo.pollinone.model.Member
import com.cs442.sexysuckzoo.pollinone.model.Vote

import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import kotlinx.android.synthetic.main.activity_voting.*
import rx.Observable
import rx.Subscription
import java.util.concurrent.TimeUnit


class Voting : AppCompatActivity() {
    val TAG = "Voting"
    private lateinit var mSensorManager: SensorManager
    private lateinit var mVoteMotionDetector: VoteMotionDetector

    var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voting)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mVoteMotionDetector = VoteMotionDetector(
                mSensorManager,
                this::onHandUpDetected,
                this::onHandDownDetected
        )
        // toggleButton.setOnCheckedChangeListener { _, isChecked ->
        //     if (isChecked) {
        //         vote()
        //     } else {
        //         withdraw()
        //     }
        // }
        toggleButton.setOnClickListener { v ->
            val isChecked = (v as ToggleButton).isChecked
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
        var vote: Vote? = null
        val member = StorageService.instance.member!!

        subscription = Observable.interval(1, TimeUnit.SECONDS).switchMap {
            vote = StorageService.instance.vote!!
            PollService.instance.fetchPoll(vote!!.id, member.credential)
        }.map {
            if (it.status != "voting") {
                finish()
            }
            else if (it.currentItem != vote!!.currentItem) {
                StorageService.instance.vote = it
                toggleButton.isChecked = false
                val choice = it.currentItem!! + 1
                currentChoiceTextView.text = "Choice #$choice"
            }
        }.subscribe {

        }
    }

    override fun onPause() {
        super.onPause()
        mVoteMotionDetector.stopDetection()
        subscription?.let {
            if (!it.isUnsubscribed) {
                it.unsubscribe()
            }
        }
        subscription = null
    }

    fun refreshUI(member: Member) {
        // @TODO: Feedback to user which selection has been chosen
        StorageService.instance.member?.item = member.item
        if (member.item == null) {
            currentVoteTextView.text = "Now voting for nothing"
        } else {
            val item = (member.item as Int) + 1
            currentVoteTextView.text = "Now voting for item #$item"
        }
    }

    fun vote() {
        val voteId = StorageService.instance.vote?.id as Int
        val credential = StorageService.instance.member?.credential as String
        PollService.instance.vote(voteId, credential).map {
            refreshUI(it)

            val v = getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(500)
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
