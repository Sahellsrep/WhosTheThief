package com.project.game1;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GameRoundActivity extends AppCompatActivity {

    private LinearLayout playerContainer;
    private TextView policeText;
    private TextView resultText;
    private TextView scoreboardText;
    private LinearLayout buttonRow;
    private Button nextRoundButton;
    private Button endMatchButton;

    private ArrayList<String> players;
    private ArrayList<String> assignedRoles;
    private ArrayList<Integer> assignedPoints;

    private int policeIndex = -1;
    private int thiefIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game_round);

        playerContainer = findViewById(R.id.playerContainer);
        policeText = findViewById(R.id.policeText);
        resultText = findViewById(R.id.resultText);
        scoreboardText = findViewById(R.id.scoreboardText);
        buttonRow = findViewById(R.id.buttonRow);
        nextRoundButton = findViewById(R.id.nextRoundButton);
        endMatchButton = findViewById(R.id.endMatchButton);

        players = getIntent().getStringArrayListExtra("players");
        assignedRoles = getIntent().getStringArrayListExtra("assignedRoles");
        assignedPoints = getIntent().getIntegerArrayListExtra("assignedPoints");

        if (players == null || assignedRoles == null || assignedPoints == null) {
            Toast.makeText(this, "Game data is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findSpecialRoles();
        showPolice();
        createPlayerButtons();

        nextRoundButton.setOnClickListener(v -> goToNextRound());
        endMatchButton.setOnClickListener(v -> goToFinalScore());
    }

    private void findSpecialRoles() {

        for (int i = 0; i < assignedRoles.size(); i++) {

            String role = assignedRoles.get(i).toLowerCase();

            if (role.equals("police")) {
                policeIndex = i;
            }

            if (role.equals("thief")) {
                thiefIndex = i;
            }
        }
    }

    private void showPolice() {

        if (policeIndex == -1) {
            policeText.setText("POLICE ROLE NOT FOUND");
            return;
        }

        policeText.setText(
                "POLICE\n\n" +
                players.get(policeIndex) +
                "\n\n" +
                "You are the Police.\n" +
                "Find the player who is the Thief."
        );
    }

    private void createPlayerButtons() {

        playerContainer.removeAllViews();

        int horizontalMargin = DisplayUtils.dp(this, 16);
        int verticalMargin = DisplayUtils.dp(this, 8);
        int buttonPadding = DisplayUtils.dp(this, 14);

        for (int i = 0; i < players.size(); i++) {

            if (i == policeIndex) {
                continue;
            }

            final int selectedIndex = i;

            Button playerButton = new Button(this);
            playerButton.setText(players.get(i));
            playerButton.setTextSize(18);
            playerButton.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.leftMargin = horizontalMargin;
            params.rightMargin = horizontalMargin;
            params.topMargin = verticalMargin;
            params.bottomMargin = verticalMargin;

            playerContainer.addView(playerButton, params);

            playerButton.setOnClickListener(v -> checkGuess(selectedIndex));
        }
    }

    private void checkGuess(int selectedIndex) {

        boolean correct = selectedIndex == thiefIndex;

        if (correct) {

            resultText.setText(
                    "CORRECT!\n\n" +
                    players.get(selectedIndex) +
                    " is the THIEF.\n\n" +
                    "Police caught the Thief. Points stay the same."
            );

        } else {

            int policePoints = assignedPoints.get(policeIndex);
            int thiefPoints = assignedPoints.get(thiefIndex);

            assignedPoints.set(policeIndex, thiefPoints);
            assignedPoints.set(thiefIndex, policePoints);

            String guessedName = players.get(selectedIndex);
            String actualThiefName = players.get(thiefIndex);

            resultText.setText(
                    "WRONG GUESS!\n\n" +
                    guessedName +
                    " was NOT the Thief.\n\n" +
                    "Police and Thief points have been swapped."
            );

            showWrongGuessPopup(guessedName, actualThiefName);
        }

        playerContainer.removeAllViews();

        GameState.addRoundPoints(assignedPoints);

        showScoreboard();

        buttonRow.setVisibility(View.VISIBLE);
    }

    private void showWrongGuessPopup(String guessedName, String actualThiefName) {

        new AlertDialog.Builder(this)
                .setTitle("WRONG GUESS!")
                .setMessage(guessedName + " was not the Thief, it was " + actualThiefName + ".")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show();
    }

    private void showScoreboard() {

        StringBuilder builder = new StringBuilder("SCOREBOARD\n\n");

        for (int i = 0; i < GameState.players.size(); i++) {

            builder.append(GameState.players.get(i))
                    .append(": ")
                    .append(GameState.cumulativeScores.get(i))
                    .append("\n");
        }

        scoreboardText.setText(builder.toString());
    }

    private void goToNextRound() {

        Intent intent = new Intent(GameRoundActivity.this, RoundAssignmentActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToFinalScore() {

        Intent intent = new Intent(GameRoundActivity.this, FinalScoreActivity.class);
        startActivity(intent);
        finish();
    }
}