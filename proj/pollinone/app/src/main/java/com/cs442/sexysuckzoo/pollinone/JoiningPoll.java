package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

public class JoiningPoll extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_poll);

        LinearLayout ll = (LinearLayout) findViewById(R.id.roomList);
        {
            // set joining poll information
            Button b = new Button(getApplicationContext());
            b.setText("test poll");
            b.setTag("1");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    joiningPoll(v);
                }
            });
            ll.addView(b);
        }
    }

    public void joiningPoll(View v) {
        if (v.getTag() == "1")
        {
            Intent intent = new Intent(getApplicationContext(), WaitingVoteToStart.class);
            startActivity(intent);
        }
    }
}
