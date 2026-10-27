package com.project.game1;

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
import java.util.Collections;

public class RoundAssignmentActivity extends AppCompatActivity {

    private LinearLayout playerContainer;
    private Button startGameButton;

    private ArrayList<String> players;
    private ArrayList<String> roles;
    private ArrayList<Integer> rolePoints;

    private ArrayList<String> shuffledRoles = new ArrayList<>();
    private ArrayList<Integer> shuffledPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_round_assignment);

        playerContainer = findViewById(R.id.playerContainer);
        startGameButton = findViewById(R.id.startGameButton);

        players = getIntent().getStringArrayListExtra("players");
        roles = getIntent().getStringArrayListExtra("roles");
        rolePoints = getIntent().getIntegerArrayListExtra("rolePoints");

        if (players == null || roles == null || rolePoints == null) {
            players = GameState.players;
            roles = GameState.roles;
            rolePoints = GameState.rolePoints;
        }

        if (players == null || roles == null || rolePoints == null) {
            showMissingData();
            return;
        }

        if (players.size() < 4) {
            showMissingData();
            return;
        }

        if (roles.size() != players.size()) {
            showMissingData();
            return;
        }

        if (rolePoints.size() != players.size()) {
            showMissingData();
            return;
        }

        createRandomAssignments();
        createPlayerContainers();

        startGameButton.setVisibility(View.GONE);
        startGameButton.setOnClickListener(v -> startGame());
    }

    private void showMissingData() {
        Toast.makeText(this, "Game data is missing", Toast.LENGTH_LONG).show();
        finish();
    }

    private void createRandomAssignments() {
        shuffledRoles.clear();
        shuffledPoints.clear();

        ArrayList<RoleData> roleDataList = new ArrayList<>();

        for (int i = 0; i < roles.size(); i++) {
            roleDataList.add(new RoleData(roles.get(i), rolePoints.get(i)));
        }

        Collections.shuffle(roleDataList);

        for (RoleData roleData : roleDataList) {
            shuffledRoles.add(roleData.name);
            shuffledPoints.add(roleData.points);
        }
    }

    private void createPlayerContainers() {
        playerContainer.removeAllViews();

        int horizontalMargin = DisplayUtils.dp(this, 16);
        int verticalMargin = DisplayUtils.dp(this, 10);
        int boxPadding = DisplayUtils.dp(this, 20);

        for (int i = 0; i < players.size(); i++) {

            final int playerIndex = i;

            LinearLayout playerBox = new LinearLayout(this);
            playerBox.setOrientation(LinearLayout.VERTICAL);
            playerBox.setGravity(Gravity.CENTER);
            playerBox.setPadding(boxPadding, boxPadding, boxPadding, boxPadding);
            playerBox.setBackgroundResource(R.drawable.card_background);

            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            boxParams.leftMargin = horizontalMargin;
            boxParams.rightMargin = horizontalMargin;
            boxParams.topMargin = verticalMargin;
            boxParams.bottomMargin = verticalMargin;

            playerContainer.addView(playerBox, boxParams);

            TextView playerName = new TextView(this);
            playerName.setText(players.get(i));
            playerName.setTextSize(22);
            playerName.setGravity(Gravity.CENTER);
            playerName.setTypeface(null, android.graphics.Typeface.BOLD);

            playerBox.addView(playerName);

            playerBox.setOnClickListener(v -> showRole(playerBox, playerIndex));
        }
    }

    private void showRole(LinearLayout playerBox, int playerIndex) {

        if (playerBox.getTag() != null) {
            return;
        }

        playerBox.setTag("opened");
        playerBox.removeAllViews();

        TextView roleText = new TextView(this);
        roleText.setText(shuffledRoles.get(playerIndex));
        roleText.setTextSize(34);
        roleText.setGravity(Gravity.CENTER);
        roleText.setTypeface(null, android.graphics.Typeface.BOLD);
        roleText.setPadding(0, DisplayUtils.dp(this, 10), 0, DisplayUtils.dp(this, 10));

        playerBox.addView(roleText);

        TextView pointsText = new TextView(this);
        pointsText.setText("POINTS\n" + shuffledPoints.get(playerIndex));
        pointsText.setTextSize(22);
        pointsText.setGravity(Gravity.CENTER);
        pointsText.setPadding(0, 0, 0, DisplayUtils.dp(this, 14));

        playerBox.addView(pointsText);

        Button doneButton = new Button(this);
        doneButton.setText("DONE");
        doneButton.setTextSize(18);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        buttonParams.gravity = Gravity.CENTER;

        playerBox.addView(doneButton, buttonParams);

        doneButton.setOnClickListener(v -> {

            playerContainer.removeView(playerBox);

            if (playerContainer.getChildCount() == 0) {

                Toast.makeText(this, "Everyone has seen their role!", Toast.LENGTH_SHORT).show();
                startGameButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startGame() {
        Intent intent = new Intent(RoundAssignmentActivity.this, GameRoundActivity.class);

        intent.putStringArrayListExtra("players", players);
        intent.putStringArrayListExtra("assignedRoles", shuffledRoles);
        intent.putIntegerArrayListExtra("assignedPoints", shuffledPoints);

        startActivity(intent);
    }

    private static class RoleData {
        String name;
        int points;

        RoleData(String name, int points) {
            this.name = name;
            this.points = points;
        }
    }
}