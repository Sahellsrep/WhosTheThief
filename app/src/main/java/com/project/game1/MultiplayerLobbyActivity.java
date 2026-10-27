package com.project.game1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MultiplayerLobbyActivity extends AppCompatActivity {

    private TextView infoText;
    private TextView countText;
    private LinearLayout playersContainer;
    private Button readyButton;
    private Button startButton;
    private Button leaveButton;

    private MultiplayerManager mgr;
    private boolean goingToGame = false;

    private final MultiplayerManager.Listener listener = (from, msg) -> {

        if (msg.equals("LOBBY") || msg.startsWith("LEFT|")) {
            refreshPlayers();

        } else if (msg.startsWith("START|")) {
            goToGame();

        } else if (msg.equals("DISCONNECTED")) {
            Toast.makeText(this, "The host closed the room", Toast.LENGTH_LONG).show();
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_lobby);

        infoText = findViewById(R.id.infoText);
        countText = findViewById(R.id.countText);
        playersContainer = findViewById(R.id.playersContainer);
        readyButton = findViewById(R.id.readyButton);
        startButton = findViewById(R.id.startButton);
        leaveButton = findViewById(R.id.leaveButton);

        mgr = MultiplayerManager.get();

        if (mgr.isHost) {
            readyButton.setVisibility(View.GONE);
            infoText.setText("Your game is visible to nearby players in \"Join a Game\".\n"
                    + "Room address (backup): " + MultiplayerManager.getLocalIp());
            startButton.setOnClickListener(v -> startGame());
        } else {
            startButton.setVisibility(View.GONE);
            infoText.setText("Connected! Press READY when you are set.\nThe host will start the game.");
            readyButton.setOnClickListener(v -> toggleReady());
        }

        leaveButton.setOnClickListener(v -> finish());

        refreshPlayers();

        mgr.setListener(listener);
    }

    private void toggleReady() {
        boolean nowReady = !mgr.isReady(mgr.myName);
        mgr.sendToHost("READY|" + (nowReady ? "1" : "0"));
    }

    private void refreshPlayers() {

        ArrayList<String> names = mgr.getPlayers();

        playersContainer.removeAllViews();

        for (int i = 0; i < names.size(); i++) {

            String name = names.get(i);
            boolean ready = mgr.isReady(name);

            String label = (i + 1) + ". " + name;

            if (i == 0) label += "  👑";
            if (name.equals(mgr.myName)) label += "  (you)";

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(Color.parseColor("#22888888"));

            int pad = DisplayUtils.dp(this, 12);
            row.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            rowParams.topMargin = DisplayUtils.dp(this, 4);
            rowParams.bottomMargin = DisplayUtils.dp(this, 4);

            TextView nameView = new TextView(this);
            nameView.setText(label);
            nameView.setTextSize(17);

            row.addView(nameView, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView statusView = new TextView(this);
            statusView.setText(ready ? "✅ Ready" : "⏳ Not ready");
            statusView.setTextSize(15);

            row.addView(statusView);

            playersContainer.addView(row, rowParams);
        }

        int max = mgr.maxPlayers;

        countText.setText("Players: " + names.size() + " / " + max);

        if (mgr.isHost) {

            boolean full = names.size() >= max;
            boolean everyoneReady = mgr.allReady();

            startButton.setEnabled(full && everyoneReady);

            if (!full) {
                startButton.setText("WAITING FOR PLAYERS...");
            } else if (!everyoneReady) {
                startButton.setText("WAITING FOR READY...");
            } else {
                startButton.setText("START GAME");
            }

        } else {
            boolean ready = mgr.isReady(mgr.myName);
            readyButton.setText(ready ? "CANCEL READY" : "READY");
        }
    }

    private void startGame() {

        ArrayList<String> names = mgr.getPlayers();

        if (names.size() < mgr.maxPlayers || !mgr.allReady()) {
            return;
        }

        mgr.markStarted();
        mgr.broadcast("START|" + MultiplayerManager.joinNames(names));

        goToGame();
    }

    private void goToGame() {
        goingToGame = true;

        // messages arriving from now on wait for the game screen
        mgr.setListener(null);

        startActivity(new Intent(this, MultiplayerGameActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mgr.clearListener(listener);

        if (isFinishing() && !goingToGame) {
            mgr.close();
        }
    }
}