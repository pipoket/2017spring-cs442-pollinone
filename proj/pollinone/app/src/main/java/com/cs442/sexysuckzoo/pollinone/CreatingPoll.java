package com.cs442.sexysuckzoo.pollinone;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CreatingPoll extends AppCompatActivity {
    List<EditText> mAllEds = new ArrayList<EditText>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_poll);
        clearChoices();
        addChoice(null);
    }

    private TextView createNewChoiceTextView(String txt){
        TextView tv = new TextView(getApplicationContext());
        tv.setText(txt);
        tv.setWidth(100);
        tv.setTextSize(20);
        tv.setTextColor(Color.BLACK);
        return tv;
    }
    private EditText createNewChoiceEditText(String txt) {
        EditText ed = new EditText(getApplicationContext());
        ed.setText("insert new choice");
        ed.setEms(10);
        ed.setTextColor(Color.DKGRAY);
        ed.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        mAllEds.add(ed);
        return ed;
    }
    private void clearChoices() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.choiceListLayout);
        ll.removeAllViews();
        mAllEds.clear();;
    }

    public void addChoice(View v) {
        LinearLayout ll = (LinearLayout) findViewById(R.id.choiceListLayout);
        {
            LinearLayout newChoice = new LinearLayout(getApplicationContext());
            newChoice.setHorizontalGravity(Gravity.RIGHT);
            TextView tv = createNewChoiceTextView("Choice #" + (mAllEds.size() + 1));
            newChoice.setLayoutParams(new
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams childLayoutParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

            newChoice.addView(tv, childLayoutParam );

            EditText ed = createNewChoiceEditText("insert new choice");
            newChoice.addView(ed, childLayoutParam );

            ll.addView(newChoice);
        }
    }

    // bUtton callback
    public void cancleCreating(View v){
        finish();
    }

    // button callback
    public void createPoll(View v){
        Intent intent = new Intent(getApplicationContext(), JoiningPoll.class);
        startActivity(intent);

        finish();
    }
}


