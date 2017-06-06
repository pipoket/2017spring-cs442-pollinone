package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import com.cs442.sexysuckzoo.pollinone.model.Vote
import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.SubscribeOptions
import org.json.JSONObject

class JoiningPoll : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks  {
    private var mLinearLayout : LinearLayout? = null
    private var mPollList = ArrayList<Vote>()

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mMessageListener: MessageListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_poll)

        mLinearLayout = findViewById(R.id.roomList) as LinearLayout

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build()

        // subscribe model
        mMessageListener = object : MessageListener()
        {
            override fun onFound(message: Message) {
                val messageAsString = String(message.content)
                val json : JSONObject? = JSONObject(messageAsString)

                if (json?.has("roomKey") as Boolean) {
                    // mPollList.add(json.getString("roomKey"))
                    val key = json.getString("roomKey")
                    PollService.instance.fetchPoll(key).map {
                        mPollList.add(it)
                        refreshUI()
                    }.doOnError {

                    }.onErrorReturn {

                    }.subscribe {

                    }
                }
                Log.d("JoiningPoll", "Found message: " + messageAsString)
            }

            override fun onLost(message: Message) {
                val messageAsString = String(message.content)
                val json : JSONObject? = JSONObject(messageAsString)
                if (json?.has("roomKey") as Boolean) {
                    val key = json.getString("roomKey")
                    mPollList.removeIf {
                        it.key == key
                    }
                    // mPollList.remove(json.getString("roomId"))
                    refreshUI()
                }

                Log.d("JoiningPoll", "Lost sight of message: " + messageAsString);
            }
        }

        // mock
        mPollList.add(Vote(16, "HelloWorld", "y9", null, null, null, null))
        refreshUI()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
    }

    override fun onStop() {
        if (mGoogleApiClient?.isConnected as Boolean) {
            unsubscribe()
            mGoogleApiClient?.disconnect()
        }
        super.onStop()
    }


    fun joiningPoll(v: View) {
        val idx = v.tag as Int
        val vote = mPollList[idx]
        PollService.instance.joinPoll(vote.id, vote.key).map {
            StorageService.instance.member = it
            val intent = Intent(applicationContext, WaitingVoteToStart::class.java)
            startActivity(intent)
            finish()
        }.doOnError {

        }.onErrorReturn {

        }.subscribe {

        }
        // TODO : send message to joining the room.
        // TODO : Send an joining request to server
        // if (v.tag === "1") {
        //     val intent = Intent(applicationContext, WaitingVoteToStart::class.java)
        //     startActivity(intent)
        // }
    }

    private fun refreshUI() {
        mLinearLayout?.removeAllViews()
        for ((idx, vote) in mPollList.withIndex())
        {
            // set joining poll information
            val b = Button(applicationContext)
            b.text = vote.title
            b.tag = idx
            b.setOnClickListener { v -> joiningPoll(v) }
            mLinearLayout?.addView(b)
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.e("JoiningPoll", "onConnectionFailed")
    }
    override fun onConnected(var1: Bundle?) {
        subscribe()
    }
    override fun onConnectionSuspended(var1: Int) {}

    // Subscribe to receive messages.
    private fun subscribe() {
        Log.i("JoiningPoll", "Subscribing.")
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, SubscribeOptions.DEFAULT)
    }

    private fun unsubscribe() {
        Log.i("JoiningPoll", "Unsubscribing.")
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
    }
}
