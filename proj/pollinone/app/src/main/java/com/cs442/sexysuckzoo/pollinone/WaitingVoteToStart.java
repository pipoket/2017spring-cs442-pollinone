package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class WaitingVoteToStart extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_vote_to_start);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 2000ms
                Intent intent = new Intent(getApplicationContext(), Voting.class);
                startActivity(intent);
                finish();
            }
        }, 2000);

    }
}
