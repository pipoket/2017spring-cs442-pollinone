package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_create_poll.*

import java.util.ArrayList

import com.cs442.sexysuckzoo.pollinone.service.PollService
import com.cs442.sexysuckzoo.pollinone.service.StorageService

class CreatingPoll : AppCompatActivity() {
    internal var mAllEds: MutableList<EditText> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_poll)
        //clearChoices()
        //addChoice(null)
    }

    private fun createNewChoiceTextView(txt: String): TextView {
        val tv = TextView(applicationContext)
        tv.text = txt
        tv.width = 100
        tv.textSize = 20f
        tv.setTextColor(Color.BLACK)
        return tv
    }

    private fun createNewChoiceEditText(txt: String): EditText {
        val ed = EditText(applicationContext)
        ed.setText("insert new choice")
        ed.setEms(10)
        ed.setTextColor(Color.DKGRAY)
        ed.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        mAllEds.add(ed)
        return ed
    }

    private fun clearChoices() {
        val ll = findViewById(R.id.choiceListLayout) as LinearLayout
        ll.removeAllViews()
        mAllEds.clear()
    }

    fun addChoice(v: View?) {
        val ll = findViewById(R.id.choiceListLayout) as LinearLayout
        run {
            val newChoice = LinearLayout(applicationContext)
            newChoice.setHorizontalGravity(Gravity.RIGHT)
            val tv = createNewChoiceTextView("Choice #" + (mAllEds.size + 1))
            newChoice.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            val childLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            newChoice.addView(tv, childLayoutParam)

            val ed = createNewChoiceEditText("insert new choice")
            newChoice.addView(ed, childLayoutParam)

            ll.addView(newChoice)
        }
    }

    // button callback
    fun cancelCreating(v: View) {
        finish()
    }

    // button callback
    fun createPoll(v: View) {
        val title = inputPollTitle.text.toString()
        val itemCount = inputItemCount.text.toString().toInt()
        if (title.isEmpty()) {
            Toast.makeText(applicationContext, "Input valid title", Toast.LENGTH_LONG).show()
            return
        }
        if (itemCount < 2) {
            Toast.makeText(applicationContext, "Item number should be larger than 2", Toast.LENGTH_LONG).show()
            return
        }
        PollService.instance.createPoll(title, itemCount).map {
            StorageService.instance.vote = it
            val intent = Intent(applicationContext, StartingVote::class.java)
            startActivity(intent)
            finish()
        }.doOnError {
            Toast.makeText(applicationContext, "Failed communication with server", Toast.LENGTH_LONG).show()
        }.onErrorReturn {

        }.subscribe {
        }

    }
}


