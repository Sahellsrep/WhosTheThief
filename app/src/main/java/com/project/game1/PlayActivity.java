package com.project.game1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class PlayActivity extends AppCompatActivity {

    private Spinner playerCountSpinner;
    private EditText playerNameInput;
    private Button addPlayerButton;
    private Button backButton;
    private Button nextButton;
    private LinearLayout playerList;

    private ArrayList<String> players = new ArrayList<>();

    private int playerCount = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        playerCountSpinner = findViewById(R.id.playerCountSpinner);
        playerNameInput = findViewById(R.id.playerNameInput);
        addPlayerButton = findViewById(R.id.addPlayerButton);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);
        playerList = findViewById(R.id.playerList);

        String[] playerNumbers = {
                "4 Players", "5 Players", "6 Players", "7 Players", "8 Players",
                "9 Players", "10 Players", "11 Players", "12 Players", "13 Players"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, playerNumbers);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        playerCountSpinner.setAdapter(adapter);

        playerCountSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        playerCount = position + 4;

                        players.clear();
                        playerList.removeAllViews();
                        playerNameInput.setText("");

                        updateInput();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        addPlayerButton.setOnClickListener(v -> addPlayer());

        backButton.setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> {

            if (players.size() == playerCount) {

                Intent intent = new Intent(PlayActivity.this, RoleSetupActivity.class);
                intent.putStringArrayListExtra("players", players);
                startActivity(intent);
            }
        });
    }

    private void addPlayer() {

        String name = playerNameInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a player name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (players.size() >= playerCount) {
            return;
        }

        players.add(name);

        TextView playerText = new TextView(this);
        playerText.setText(players.size() + ". " + name);
        playerText.setTextSize(18);
        playerText.setGravity(android.view.Gravity.CENTER);

        int vPad = DisplayUtils.dp(this, 6);
        int hPad = DisplayUtils.dp(this, 10);
        playerText.setPadding(hPad, vPad, hPad, vPad);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        playerList.addView(playerText, params);

        playerNameInput.setText("");

        updateInput();
    }

    private void updateInput() {

        if (players.size() >= playerCount) {

            playerNameInput.setVisibility(View.GONE);
            addPlayerButton.setVisibility(View.GONE);

            nextButton.setEnabled(true);

            Toast.makeText(this, "All players added!", Toast.LENGTH_SHORT).show();

        } else {

            playerNameInput.setVisibility(View.VISIBLE);
            addPlayerButton.setVisibility(View.VISIBLE);

            nextButton.setEnabled(false);

            playerNameInput.setHint("Enter Player " + (players.size() + 1) + " name");
        }
    }
}