package com.project.game1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.project.game1.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.playButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PlayActivity.class)));

        binding.historyButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GameHistoryActivity.class)));

        binding.settingsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        binding.localMultiplayerButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LocalMultiplayerActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}