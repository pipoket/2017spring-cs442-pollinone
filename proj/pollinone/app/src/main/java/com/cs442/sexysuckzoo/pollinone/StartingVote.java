package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StartingVote extends AppCompatActivity {
    private String[] entries = {"test userA", "test user B"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_vote);

        LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayoutEntrees);
        ll.removeAllViews();

        LinearLayout.LayoutParams childLayoutParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        for (String entry : entries){
            ll.addView(getTextView(entry), childLayoutParam);
        }
    }

    private TextView getTextView(String txt) {
        TextView tv = new TextView(getApplicationContext());
        tv.setText(txt);
        tv.setTextSize(20);
        tv.setTextColor(Color.BLACK);
        return tv;
    }

    // Button callback
    public void startVote(View v){
        Intent intent = new Intent(getApplicationContext(), VotingHost.class);
        startActivity(intent);

        finish();
    }
}
