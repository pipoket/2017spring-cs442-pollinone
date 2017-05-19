package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class VotingHost extends AppCompatActivity {
    Button mButtonNext = null;
    TextView mTextViewChoiceName = null;
    TextView mTextViewCount = null;

    boolean mIsEndOfChoice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting_host);

        mButtonNext = (Button) findViewById(R.id.nextButton);
        mTextViewChoiceName = (TextView) findViewById(R.id.textViewChoice); // Choice #x\n Blah Blah Blah
        mTextViewCount = (TextView) findViewById(R.id.textViewCount); // 10, 투표 한 사람 숫자

        if (mIsEndOfChoice) {
            mButtonNext.setText("Finish");
        }
    }

    public void onNextButtonClick(View v){
        if (mIsEndOfChoice ){
            Intent intent = new Intent(getApplicationContext(), VotingResult.class);
            startActivity(intent);

            finish();
        }
        else {
            mIsEndOfChoice =! mIsEndOfChoice;
            mButtonNext.setText("Finish");
        }
    }
}
