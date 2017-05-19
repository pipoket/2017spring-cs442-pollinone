package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // button callback
    public void joiningPoll(View v) {
        Intent intent = new Intent(this, SearchingPoll.class);
        startActivity(intent);
    }

    // button callback
    public void creatingPoll(View v) {
        Intent intent = new Intent(this, CreatingPoll.class);
        startActivity(intent);
    }
}
