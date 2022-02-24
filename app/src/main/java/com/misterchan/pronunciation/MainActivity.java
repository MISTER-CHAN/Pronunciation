package com.misterchan.pronunciation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    Button bQuery;
    EditText etText;
    LinearLayout llResult;
    Spinner sSchema;
    Spinner sURL;

    private final View.OnClickListener onQueryButtonClickListener = view -> {
        String chars = etText.getText().toString();
        if (chars.length() == 0) {
            return;
        }
        view.setEnabled(false);
        bQuery.setText("加載中");
        llResult.removeAllViews();
        new QueryingThread(this).start();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bQuery = findViewById(R.id.b_query);
        etText = findViewById(R.id.et_text);
        llResult = findViewById(R.id.ll_result);
        sSchema = findViewById(R.id.s_schema);
        sURL = findViewById(R.id.s_url);

        bQuery.setOnClickListener(onQueryButtonClickListener);

        sSchema.setAdapter(new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, Schemas.ALL));
        sURL.setAdapter(new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, URLs.ALL));

    }
}