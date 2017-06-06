package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import rx.Observable
import rx.Subscription
import java.util.concurrent.TimeUnit

class WaitingVoteToStart : AppCompatActivity() {
    val TAG = "WaitingVoteToStart"
    var subscription: Subscription? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_vote_to_start)

        // val handler = Handler()
        // handler.postDelayed({
        //     //Do something after 2000ms
        //     val intent = Intent(applicationContext, Voting::class.java)
        //     startActivity(intent)
        //     finish()
        // }, 2000)
    }

    fun subscribe() {
        Log.wtf(TAG, "subscribing...")
        val voteId = StorageService.instance.vote?.id
        subscription = Observable.interval(2, TimeUnit.SECONDS)
                .switchMap {
                    PollService.instance.isPollStarted(voteId as Int)
                }
                .map {
                    val isStarted = it == "true"
                    if (isStarted) {
                        val intent = Intent(applicationContext, Voting::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                .doOnError {

                }
                .onErrorReturn {

                }
                .subscribe {

                }
    }

    fun unsubscribe() {
        Log.wtf(TAG, "Unsubscribing...")
        subscription?.let(Subscription::unsubscribe)
    }

    override fun onResume() {
        super.onResume()
        subscribe()
    }
    override fun onPause() {
        super.onPause()
        unsubscribe()
    }
}
