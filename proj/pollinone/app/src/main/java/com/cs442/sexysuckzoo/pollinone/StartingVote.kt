package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class StartingVote : AppCompatActivity() {
    private val entries = arrayOf("test userA", "test user B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_vote)

        val ll = findViewById(R.id.linearLayoutEntrees) as LinearLayout
        ll.removeAllViews()

        val childLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        for (entry in entries) {
            ll.addView(getTextView(entry), childLayoutParam)
        }
    }

    private fun getTextView(txt: String): TextView {
        val tv = TextView(applicationContext)
        tv.text = txt
        tv.textSize = 20f
        tv.setTextColor(Color.BLACK)
        return tv
    }

    // Button callback
    fun startVote(v: View) {
        val intent = Intent(applicationContext, VotingHost::class.java)
        startActivity(intent)

        finish()
    }
}
