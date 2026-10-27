package com.project.game1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class JoinGameActivity extends AppCompatActivity {

    private TextView statusText;
    private LinearLayout roomsContainer;
    private EditText ipInput;
    private Button joinManualButton;
    private Button backButton;

    private final LinkedHashMap<String, MultiplayerManager.Room> rooms = new LinkedHashMap<>();
    private MultiplayerManager.Discovery discovery;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String username;
    private boolean joining = false;
    private String lastSignature = "#";

    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            updateRoomList();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        statusText = findViewById(R.id.statusText);
        roomsContainer = findViewById(R.id.roomsContainer);
        ipInput = findViewById(R.id.ipInput);
        joinManualButton = findViewById(R.id.joinManualButton);
        backButton = findViewById(R.id.backButton);

        username = MultiplayerManager.clean(SettingsManager.getUsername(this));

        if (username.isEmpty()) {
            username = SettingsManager.DEFAULT_USERNAME;
        }

        backButton.setOnClickListener(v -> finish());

        joinManualButton.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();

            if (ip.isEmpty()) {
                Toast.makeText(this, "Enter the room address", Toast.LENGTH_SHORT).show();
                return;
            }

            joinRoom(ip);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        rooms.clear();
        lastSignature = "#";

        discovery = new MultiplayerManager.Discovery();
        discovery.start(room -> {
            room.lastSeen = SystemClock.elapsedRealtime();
            rooms.put(room.ip, room);
        });

        handler.post(refresher);
    }

    @Override
    protected void onPause() {
        super.onPause();

        handler.removeCallbacks(refresher);

        if (discovery != null) {
            discovery.stop();
            discovery = null;
        }
    }

    private void updateRoomList() {

        long now = SystemClock.elapsedRealtime();

        Iterator<Map.Entry<String, MultiplayerManager.Room>> it = rooms.entrySet().iterator();

        while (it.hasNext()) {
            if (now - it.next().getValue().lastSeen > 4000) {
                it.remove();
            }
        }

        StringBuilder sig = new StringBuilder();

        for (MultiplayerManager.Room r : rooms.values()) {
            sig.append(r.ip).append(r.hostName).append(r.joined).append("/").append(r.max).append(";");
        }

        if (sig.toString().equals(lastSignature)) return;

        lastSignature = sig.toString();

        roomsContainer.removeAllViews();

        if (rooms.isEmpty()) {
            statusText.setText("Searching for games on this Wi-Fi or hotspot...");
            return;
        }

        statusText.setText("Games found. Tap one to join:");

        for (MultiplayerManager.Room room : new ArrayList<>(rooms.values())) {
            roomsContainer.addView(buildRoomCard(room));
        }
    }

    private LinearLayout buildRoomCard(MultiplayerManager.Room room) {

        int pad = DisplayUtils.dp(this, 16);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackgroundColor(Color.parseColor("#33888888"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.topMargin = DisplayUtils.dp(this, 6);
        params.bottomMargin = DisplayUtils.dp(this, 6);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("👑 " + room.hostName + "'s game");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);

        TextView info = new TextView(this);
        info.setText("Players: " + room.joined + " / " + room.max);
        info.setTextSize(15);
        info.setPadding(0, DisplayUtils.dp(this, 4), 0, 0);

        TextView hint = new TextView(this);
        boolean full = room.joined >= room.max;
        hint.setText(full ? "Room is full" : "Tap to join ›");
        hint.setTextSize(13);
        hint.setAlpha(0.7f);
        hint.setGravity(Gravity.START);
        hint.setPadding(0, DisplayUtils.dp(this, 6), 0, 0);

        card.addView(title);
        card.addView(info);
        card.addView(hint);

        if (!full) {
            card.setOnClickListener(v -> joinRoom(room.ip));
        }

        return card;
    }

    private void joinRoom(String ip) {

        if (joining) return;
        joining = true;

        MultiplayerManager.get().join(ip, username, (ok, err) -> {
            joining = false;

            if (ok) {
                startActivity(new Intent(this, MultiplayerLobbyActivity.class));
            } else {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }
}