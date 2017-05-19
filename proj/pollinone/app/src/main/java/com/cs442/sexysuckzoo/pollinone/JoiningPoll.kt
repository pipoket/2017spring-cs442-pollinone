package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.google.android.gms.nearby.messages.SubscribeOptions
import org.json.JSONObject

class JoiningPoll : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks  {
    private var mLinearLayout : LinearLayout? = null
    private var mPollList = ArrayList<String>()

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
                var messageAsString = String(message.getContent());
                var json : JSONObject? = JSONObject(messageAsString)

                if (json?.has("roomId") as Boolean) {
                    mPollList.add(json?.getString("roomId"))
                    refreshUI()
                }
                Log.d("JoiningPoll", "Found message: " + messageAsString);
            }

            override fun onLost(message: Message) {
                var messageAsString = String(message.getContent());
                var json : JSONObject? = JSONObject(messageAsString)
                if (json?.has("roomId") as Boolean) {
                    mPollList.remove(json?.getString("roomId"))
                    refreshUI()
                }

                Log.d("JoiningPoll", "Lost sight of message: " + messageAsString);
            }
        }
        refreshUI()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
    }

    override fun onStop() {
        unsubscribe()
        if (mGoogleApiClient?.isConnected() as Boolean) {
            mGoogleApiClient?.disconnect()
        }
        super.onStop()
    }


    fun joiningPoll(v: View) {
        var idx = Integer.parseInt(v.tag as String)
        var roomId = mPollList[idx]
        // TODO : send message to joining the room.
        // TODO : Send an joining request to server
        if (v.tag === "1") {
            val intent = Intent(applicationContext, WaitingVoteToStart::class.java)
            startActivity(intent)
        }
    }

    private fun refreshUI() {
        mLinearLayout?.removeAllViews()
        var idx = 0;
        for (str in mPollList)
        {
            // set joining poll information
            val b = Button(applicationContext)
            b.text = str
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
