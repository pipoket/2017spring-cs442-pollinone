package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.cs442.sexysuckzoo.pollinone.model.Vote
import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import com.cs442.sexysuckzoo.pollinone.transoversound.PollInOneToASender
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.messages.*
import org.json.JSONObject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_start_vote.*


class StartingVote : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private val entries = ArrayList<String>()
    private var mGoogleApiClient: GoogleApiClient? = null

    // a message to publish
    private var mPubMessage: Message? = null
    private var mSoundBroadcaster: PollInOneToASender? = null

    //@TODO: provide proper room id
    private var roomId : String? = "Vote #1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_vote)

        roomId = StorageService.instance.vote?.key
        if (mGoogleApiClient != null) {
            return
        }
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build()
        mSoundBroadcaster = PollInOneToASender({
          soundBroadcastStopped()
        })

        entries.add("Waiting for users to join")
        refreshUI()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
        mSoundBroadcaster?.close()
    }

    override fun onStop() {
        unpublish()
        if (mGoogleApiClient?.isConnected() as Boolean) {
            mGoogleApiClient?.disconnect()
        }
        mSoundBroadcaster?.close()
        mSoundBroadcaster?.close()
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
        val vote = StorageService.instance.vote
        vote?.let {
            PollService.instance.startPoll(vote.id, vote.rootCredential as String).map {
                StorageService.instance.vote = it
                val intent = Intent(applicationContext, VotingHost::class.java)
                startActivity(intent)
                finish()
            }.doOnError {
                Toast.makeText(applicationContext, "failed http communication", Toast.LENGTH_LONG).show()
            }.onErrorReturn {
            }.subscribe {

            }
        }
    }

    // Button callback
    fun publish(maybeView:View?) {
        // Nearby Message API
        var json : JSONObject? = JSONObject()
        json?.put("roomKey", roomId)
        publish(json?.toString() as String)
    }

    private val TTL_IN_SECONDS = 3 * 60 // Three minutes.
    private val PUB_SUB_STRATEGY = Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build()

    private fun publish(message: String) {
        var TAG :String = "StartVote"

        val options = PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(object : PublishCallback() {
                    override fun onExpired() {
                        super.onExpired()
                        Log.i(TAG, "No longer publishing")
                    }
                }).build()

        if (mPubMessage != null)
        {
            unpublish()
        }
        if (mGoogleApiClient == null)
            return

        mPubMessage = DeviceMessage.newNearbyMessage(message)

        Log.i(TAG, "Publishing")

        Nearby.Messages.publish(mGoogleApiClient, mPubMessage, options)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        Log.i(TAG, "Published successfully.")
                    } else {
                        Log.i(TAG, "Published failed.")
                    }
                }

        publishSound()
    }

    private fun publishSound() {
        // Sound broadcasting
        try {
            val ridHigh = roomId!![0].toInt()
            val ridLow = roomId!![1].toInt()
            val rid = ridHigh.shl(8) + ridLow
            mSoundBroadcaster?.sendData(rid)
        } finally {}

    }
    private fun unpublish() {
        Log.i("StartVote", "Unpublishing.")
        if (mPubMessage != null) {
            Nearby.Messages.unpublish(mGoogleApiClient, mPubMessage)
            mPubMessage = null
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    override fun onConnected(var1: Bundle?) {
        var json : JSONObject? = JSONObject()
        json?.put("roomKey", roomId)
        publish(json?.toString() as String)
    }

    override fun onConnectionSuspended(var1: Int) {}

    // Transmit over Sound callbacks
    fun soundBroadcastStopped() {
    }

}
