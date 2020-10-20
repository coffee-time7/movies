package com.qsdbih.movielist.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qsdbih.movielist.R;

import java.util.Objects;

public class ReloadActivity extends AppCompatActivity {
    TextView tvRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_reload);

        Button btnRetry = findViewById(R.id.error_btn_retry);
        tvRetry = findViewById(R.id.error_txt_cause);

        btnRetry.setOnClickListener(v -> {
            if (isNetworkConnected()) {
                Intent i = new Intent(ReloadActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            } else {
                tvRetry.setText(getResources().getString(R.string.error_msg_no_internet));
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}