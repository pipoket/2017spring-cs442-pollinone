package com.cs442.sexysuckzoo.pollinone

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService
import com.google.gson.JsonArray
import com.google.gson.JsonParser

class VotingResult : AppCompatActivity() {
    internal val TAG = "VotingResult"
    internal var mLinearLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voting_result)

        mLinearLayout = findViewById(R.id.linearLayoutResult) as LinearLayout
        mLinearLayout!!.removeAllViews()

        val vote = StorageService.instance.vote
        vote?.let {
            PollService.instance.closePoll(vote.id, vote.rootCredential as String).map {
                val result: JsonArray = JsonParser().parse(it).getAsJsonArray()
                for (i in 0..(result.size()-1)) {
                    val choice = i+1
                    val count = result.get(i)
                    val s = "Choice #$choice: $count"
                    mLinearLayout!!.addView(getTextView(s))
                }
            }.doOnError {
                Log.wtf(TAG, it)
                Toast.makeText(applicationContext, "failed http communication", Toast.LENGTH_LONG).show()
            }.onErrorReturn {

            }.subscribe {

            }
        }

    }

    private fun getTextView(txt: String): TextView {
        val tv = TextView(applicationContext)
        tv.text = txt
        tv.textSize = 30f
        tv.setTextColor(Color.BLACK)
        return tv
    }
}
