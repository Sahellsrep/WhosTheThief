package com.project.game1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LocalMultiplayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_multiplayer);

        TextView usernameText = findViewById(R.id.usernameText);
        Button hostButton = findViewById(R.id.hostButton);
        Button joinButton = findViewById(R.id.joinButton);
        Button backButton = findViewById(R.id.backButton);

        String username = MultiplayerManager.clean(SettingsManager.getUsername(this));

        if (username.isEmpty()) {
            username = SettingsManager.DEFAULT_USERNAME;
        }

        usernameText.setText("Playing as: " + username + "\n(change it in Settings)");

        hostButton.setOnClickListener(v ->
                startActivity(new Intent(this, HostSetupActivity.class)));

        joinButton.setOnClickListener(v ->
                startActivity(new Intent(this, JoinGameActivity.class)));

        backButton.setOnClickListener(v -> finish());
    }
}