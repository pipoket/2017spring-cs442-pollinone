package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_voting_host.*
import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import rx.Observable
import rx.Subscription
import java.util.concurrent.TimeUnit

class VotingHost : AppCompatActivity() {
    internal var mButtonNext: Button? = null
    internal var mTextViewChoiceName: TextView? = null
    internal var mTextViewCount: TextView? = null

    internal var mIsEndOfChoice = false
    internal var subscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voting_host)

        mButtonNext = findViewById(R.id.nextButton) as Button
        mTextViewChoiceName = findViewById(R.id.textViewChoice) as TextView // Choice #x\n Blah Blah Blah
        mTextViewCount = findViewById(R.id.textViewCount) as TextView // 10, 투표 한 사람 숫자
    }

    override fun onResume() {
        super.onResume()
        subscription = StorageService.instance.vote?.let {
            val vote = it
            Observable.interval(1, TimeUnit.SECONDS).switchMap {
                PollService.instance.count(vote.id, vote.rootCredential!!)
            }.map {
                mTextViewCount?.text = it.toString()
            }.doOnError{

            }.onErrorReturn {

            }.subscribe {

            }
        }
    }

    override fun onPause() {
        super.onPause()
        subscription?.let {
            if (!it.isUnsubscribed) {
                it.unsubscribe()
            }
        }
        subscription = null
    }

    fun onNextButtonClick(v: View) {
        val vote = StorageService.instance.vote
        vote?.let {
            PollService.instance.collectPoll(vote.id, vote.rootCredential as String).map{
                StorageService.instance.vote = it
                if (it.status == "finished") {
                    val intent = Intent(applicationContext, VotingResult::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val currentItem = it.currentItem as Int + 1
                    textViewChoice.text = "Choice #$currentItem"
                    mTextViewCount?.text = "0"
                    if (currentItem == vote.itemCount) {
                        mButtonNext!!.text = "Finish"
                    }
                }
            }.doOnError {

            }.onErrorReturn {

            }.subscribe {

            }
        }
    }
}
