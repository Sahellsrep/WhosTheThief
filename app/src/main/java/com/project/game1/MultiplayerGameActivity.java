package com.project.game1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;

public class MultiplayerGameActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView roleText;
    private TextView resultText;
    private TextView scoreboardText;
    private Button roleButton;
    private Button nextRoundButton;
    private Button endMatchButton;
    private LinearLayout suspectContainer;
    private LinearLayout buttonRow;

    private MultiplayerManager mgr;
    private ArrayList<String> players;
    private ArrayList<Integer> scores = new ArrayList<>();

    private String myRole = "";
    private int myPoints = 0;
    private boolean roleVisible = false;
    private boolean guessed = false;
    private boolean matchOver = false;

    // host only
    private ArrayList<String> hostRoles = new ArrayList<>();
    private ArrayList<Integer> hostPoints = new ArrayList<>();
    private int policeIdx = -1;
    private int thiefIdx = -1;
    private boolean guessHandled = true;

    private final MultiplayerManager.Listener listener = this::handleMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_game);

        statusText = findViewById(R.id.statusText);
        roleText = findViewById(R.id.roleText);
        resultText = findViewById(R.id.resultText);
        scoreboardText = findViewById(R.id.scoreboardText);
        roleButton = findViewById(R.id.roleButton);
        nextRoundButton = findViewById(R.id.nextRoundButton);
        endMatchButton = findViewById(R.id.endMatchButton);
        suspectContainer = findViewById(R.id.suspectContainer);
        buttonRow = findViewById(R.id.buttonRow);

        mgr = MultiplayerManager.get();
        players = mgr.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            scores.add(0);
        }

        roleButton.setOnClickListener(v -> {
            roleVisible = !roleVisible;
            refreshRole();
        });

        nextRoundButton.setOnClickListener(v -> startRound());

        endMatchButton.setOnClickListener(v -> {
            mgr.broadcast("END");
            finishMatch();
        });

        refreshRole();
        showScoreboard();

        statusText.setText(mgr.isHost ? "Starting round..." : "Waiting for the host...");

        mgr.setListener(listener);

        if (mgr.isHost) {
            startRound();
        }
    }

    // ---------- messages ----------

    private void handleMessage(String from, String msg) {

        if (msg.startsWith("ROLE|")) {
            String[] p = msg.split("\\|");
            applyRole(p[1], Integer.parseInt(p[2]));

        } else if (msg.startsWith("ROUND|")) {
            onRound(msg.substring(6));

        } else if (msg.startsWith("RESULT|")) {
            showResult(msg);

        } else if (msg.equals("END")) {
            finishMatch();

        } else if (msg.startsWith("GUESS|") && mgr.isHost) {
            handleGuess(from, msg.substring(6));

        } else if (msg.startsWith("LEFT|") && mgr.isHost) {
            Toast.makeText(this, msg.substring(5) + " left the game. You can end the match.",
                    Toast.LENGTH_LONG).show();
            nextRoundButton.setVisibility(View.GONE);
            buttonRow.setVisibility(View.VISIBLE);

        } else if (msg.equals("DISCONNECTED") && !mgr.isHost) {
            if (matchOver) return;
            matchOver = true;
            Toast.makeText(this, "The host left the game", Toast.LENGTH_LONG).show();
            mgr.close();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    // ---------- host: round logic ----------

    private int indexOfRole(String role) {
        for (int i = 0; i < hostRoles.size(); i++) {
            if (hostRoles.get(i).equalsIgnoreCase(role)) return i;
        }
        return -1;
    }

    private void startRound() {

        int n = players.size();

        if (mgr.roleNames.size() != n || mgr.rolePoints.size() != n) {
            Toast.makeText(this, "Role setup does not match the player count", Toast.LENGTH_LONG).show();
            return;
        }

        // Shuffle the host's chosen roles among the players
        ArrayList<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        Collections.shuffle(order);

        hostRoles.clear();
        hostPoints.clear();

        for (int i = 0; i < n; i++) {
            int r = order.get(i);
            hostRoles.add(mgr.roleNames.get(r));
            hostPoints.add(mgr.rolePoints.get(r));
        }

        policeIdx = indexOfRole("Police");
        thiefIdx = indexOfRole("Thief");
        guessHandled = false;

        for (int i = 0; i < n; i++) {
            String name = players.get(i);

            if (name.equals(mgr.myName)) {
                applyRole(hostRoles.get(i), hostPoints.get(i));
            } else {
                mgr.sendTo(name, "ROLE|" + hostRoles.get(i) + "|" + hostPoints.get(i));
            }
        }

        String policeName = players.get(policeIdx);

        mgr.broadcast("ROUND|" + policeName);
        onRound(policeName);
    }

    private void handleGuess(String from, String guessName) {

        if (guessHandled) return;
        if (!from.equals(players.get(policeIdx))) return;

        int guessIdx = players.indexOf(guessName);

        if (guessIdx < 0 || guessIdx == policeIdx) return;

        guessHandled = true;

        boolean correct = (guessIdx == thiefIdx);

        ArrayList<Integer> pts = new ArrayList<>(hostPoints);

        if (!correct) {
            int policePts = pts.get(policeIdx);
            pts.set(policeIdx, pts.get(thiefIdx));
            pts.set(thiefIdx, policePts);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("RESULT|").append(correct ? 1 : 0)
                .append("|").append(guessName)
                .append("|").append(players.get(thiefIdx));

        for (int i = 0; i < scores.size(); i++) {
            sb.append("|").append(scores.get(i) + pts.get(i));
        }

        String msg = sb.toString();

        mgr.broadcast(msg);
        showResult(msg);
    }

    // ---------- shared UI ----------

    private void applyRole(String role, int points) {
        myRole = role;
        myPoints = points;
        roleVisible = false;
        refreshRole();
    }

    private void refreshRole() {

        if (myRole.isEmpty()) {
            roleText.setText("Waiting for your role...");
            roleButton.setText("SHOW MY ROLE");
            return;
        }

        if (roleVisible) {
            roleText.setText("ROLE: " + myRole + "\nPOINTS: " + myPoints);
            roleButton.setText("HIDE MY ROLE");
        } else {
            roleText.setText("Role hidden");
            roleButton.setText("SHOW MY ROLE");
        }
    }

    private void onRound(String policeName) {

        guessed = false;

        resultText.setText("");
        buttonRow.setVisibility(View.GONE);
        nextRoundButton.setVisibility(View.VISIBLE);
        suspectContainer.removeAllViews();

        if (policeName.equals(mgr.myName)) {
            statusText.setText("YOU ARE THE POLICE\nTap the player you think is the Thief");
            buildSuspects();
        } else {
            statusText.setText("POLICE: " + policeName + "\nWaiting for the Police to guess...");
        }
    }

    private void buildSuspects() {

        int margin = DisplayUtils.dp(this, 6);

        for (String name : players) {

            if (name.equals(mgr.myName)) continue;

            Button b = new Button(this);
            b.setText(name);
            b.setTextSize(18);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.topMargin = margin;
            params.bottomMargin = margin;

            suspectContainer.addView(b, params);

            b.setOnClickListener(v -> submitGuess(name));
        }
    }

    private void submitGuess(String name) {

        if (guessed) return;
        guessed = true;

        suspectContainer.removeAllViews();
        statusText.setText("You guessed " + name + "...");

        if (mgr.isHost) {
            handleGuess(mgr.myName, name);
        } else {
            mgr.sendToHost("GUESS|" + name);
        }
    }

    private void showResult(String msg) {

        // RESULT|correct|guessed|thief|score1|score2|...
        String[] p = msg.split("\\|");

        boolean correct = p[1].equals("1");
        String guessedName = p[2];
        String thiefName = p[3];

        scores.clear();

        for (int i = 4; i < p.length; i++) {
            scores.add(Integer.parseInt(p[i]));
        }

        suspectContainer.removeAllViews();

        if (correct) {
            statusText.setText("CORRECT!");
            resultText.setText(guessedName + " is the THIEF.\nPolice caught the Thief. Points stay the same.");
        } else {
            statusText.setText("WRONG GUESS!");
            resultText.setText(guessedName + " was NOT the Thief.\nPolice and Thief points were swapped.");

            new AlertDialog.Builder(this)
                    .setTitle("WRONG GUESS!")
                    .setMessage(guessedName + " was not the Thief, it was " + thiefName + ".")
                    .setPositiveButton("OK", null)
                    .setCancelable(false)
                    .show();
        }

        showScoreboard();

        if (mgr.isHost) {
            buttonRow.setVisibility(View.VISIBLE);
        } else {
            resultText.append("\n\nWaiting for the host...");
        }
    }

    private void showScoreboard() {

        StringBuilder b = new StringBuilder("SCOREBOARD\n\n");

        for (int i = 0; i < players.size() && i < scores.size(); i++) {
            b.append(players.get(i)).append(": ").append(scores.get(i)).append("\n");
        }

        scoreboardText.setText(b.toString());
    }

    // ---------- end of match ----------

    private void finishMatch() {

        if (matchOver) return;
        matchOver = true;

        GameState.players = new ArrayList<>(players);
        GameState.cumulativeScores = new ArrayList<>(scores);

        mgr.clearListener(listener);
        mgr.close();

        startActivity(new Intent(this, FinalScoreActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mgr.clearListener(listener);

        if (isFinishing() && !matchOver) {
            mgr.close();
        }
    }
}