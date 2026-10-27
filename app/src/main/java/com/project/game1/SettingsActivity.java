package com.project.game1;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar volumeSeekBar;
    private TextView volumeValueText;
    private EditText usernameInput;
    private Button saveButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeValueText = findViewById(R.id.volumeValueText);
        usernameInput = findViewById(R.id.usernameInput);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

        // Load saved values
        int savedVolume = SettingsManager.getVolume(this);
        volumeSeekBar.setProgress(savedVolume);
        volumeValueText.setText(savedVolume + "%");

        usernameInput.setText(SettingsManager.getUsername(this));

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeValueText.setText(progress + "%");
                SettingsManager.setVolume(SettingsActivity.this, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playPreviewBeep(seekBar.getProgress());
            }
        });

        saveButton.setOnClickListener(v -> saveUsername());
        backButton.setOnClickListener(v -> finish());
    }

    private void saveUsername() {

        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        SettingsManager.setUsername(this, username);
        Toast.makeText(this, "Username saved!", Toast.LENGTH_SHORT).show();
    }

    // Short beep so the player can hear the chosen volume
    private void playPreviewBeep(int volume) {

        if (volume <= 0) {
            return;
        }

        try {
            final ToneGenerator tone =
                    new ToneGenerator(AudioManager.STREAM_MUSIC, volume);

            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150);

            new Handler(Looper.getMainLooper()).postDelayed(tone::release, 300);

        } catch (RuntimeException ignored) {
            // Preview sound is optional; ignore if the device can't play it
        }
    }
}