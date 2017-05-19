package com.cs442.sexysuckzoo.pollinone

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

import com.github.kittinunf.fuel.core.FuelManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FuelManager.instance.basePath = applicationContext.resources.getString(R.string.api_root)
        Log.wtf("basepath", FuelManager.instance.basePath)
    }

    // button callback
    fun joiningPoll(v: View) {
        val intent = Intent(this, JoiningPoll::class.java)
        startActivity(intent)
    }

    // button callback
    fun creatingPoll(v: View) {
        val intent = Intent(this, CreatingPoll::class.java)
        startActivity(intent)
    }
}
