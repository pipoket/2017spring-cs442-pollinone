package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView

class JoiningPoll : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_poll)

        val ll = findViewById(R.id.roomList) as LinearLayout
        run {
            // set joining poll information
            val b = Button(applicationContext)
            b.text = "test poll"
            b.tag = "1"
            b.setOnClickListener { v -> joiningPoll(v) }
            ll.addView(b)
        }
    }

    fun joiningPoll(v: View) {
        if (v.tag === "1") {
            val intent = Intent(applicationContext, WaitingVoteToStart::class.java)
            startActivity(intent)
        }
    }
}
