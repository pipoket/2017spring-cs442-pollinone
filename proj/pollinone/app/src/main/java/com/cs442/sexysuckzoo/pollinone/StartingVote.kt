package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.nearby.messages.MessageListener
import org.json.JSONObject


class StartingVote : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private val entries = ArrayList<String>()
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mActiveMessage: Message? = null
    private var mMessageListener: MessageListener? = null

    private var roomId = "Vote #1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_vote)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build()

        entries.add("Waiting for users to join")
        refreshUI()

        // subscribe model
        mMessageListener = object : MessageListener()
        {
            override fun onFound(message: Message) {
                var messageAsString = String(message.getContent());
                var json : JSONObject? = JSONObject(messageAsString)

                if (json?.has("roomId") as Boolean) {
                    // this message is sent from host. ignore it.
                }
                else {
                    // TODO : add the user and count them.
                    var userId = json?.getString("userId")
                    entries.add(userId)
                    refreshUI()
                }
                Log.d("StartVote", "Found message: " + messageAsString);
            }

            override fun onLost(message: Message) {
                var messageAsString = String(message.getContent());
                Log.d("StartVote", "Lost sight of message: " + messageAsString);
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
    }

    override fun onStop() {
        unpublish();
        unsubscribe();
        if (mGoogleApiClient?.isConnected() as Boolean) {
            mGoogleApiClient?.disconnect()
        }
        super.onStop()
    }

    private fun getTextView(txt: String): TextView {
        val tv = TextView(applicationContext)
        tv.text = txt
        tv.textSize = 20f
        tv.setTextColor(Color.BLACK)
        return tv
    }
    private fun refreshUI() {
        val ll = findViewById(R.id.linearLayoutEntrees) as LinearLayout
        ll.removeAllViews()

        val childLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        for (entry in entries) {
            ll.addView(getTextView(entry), childLayoutParam)
        }
    }

    // Button callback
    fun startVote(v: View) {
        val intent = Intent(applicationContext, VotingHost::class.java)
        startActivity(intent)

        finish()
    }

    private fun publish(message: String) {
        Log.i("StartVote", "Publishing message: " + message)
        mActiveMessage = Message(message.toByteArray())
        Nearby.Messages.publish(mGoogleApiClient, mActiveMessage)
    }

    private fun unpublish() {
        Log.i("StartVote", "Unpublishing.")
        if (mActiveMessage != null) {
            Nearby.Messages.unpublish(mGoogleApiClient, mActiveMessage)
            mActiveMessage = null
        }
    }

    private fun unsubscribe() {
        Log.i("StartVote", "Unsubscribing.")
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener)
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    override fun onConnected(var1: Bundle?) {
        var json : JSONObject? = JSONObject()
        json?.put("roomId", roomId)
        publish(json?.toString() as String)
    }

    override fun onConnectionSuspended(var1: Int) {}
}
