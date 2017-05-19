package com.cs442.sexysuckzoo.pollinone;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VotingResult extends AppCompatActivity {
    LinearLayout mLinearLayout = null;

    String[] result = {"Choice#1 : 20", "Choice#2 : 3110" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting_result);

        mLinearLayout = (LinearLayout) findViewById(R.id.linearLayoutResult);
        mLinearLayout.removeAllViews();

        for (String s : result){
            mLinearLayout.addView(getTextView(s));
        }

    }

    private TextView getTextView(String txt) {
        TextView tv = new TextView(getApplicationContext());
        tv.setText(txt);
        tv.setTextSize(30);
        tv.setTextColor(Color.BLACK);
        return tv;
    }
}
