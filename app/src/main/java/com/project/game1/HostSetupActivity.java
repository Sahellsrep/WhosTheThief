package com.project.game1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
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

public class HostSetupActivity extends AppCompatActivity {

    private Spinner playerCountSpinner;
    private LinearLayout roleContainer;
    private Button createButton;
    private Button backButton;

    private int playerCount = MultiplayerManager.MIN_PLAYERS;

    private final ArrayList<EditText> nameFields = new ArrayList<>();
    private final ArrayList<EditText> pointFields = new ArrayList<>();
    private final ArrayList<String> savedNames = new ArrayList<>();
    private final ArrayList<String> savedPoints = new ArrayList<>();

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_setup);

        playerCountSpinner = findViewById(R.id.playerCountSpinner);
        roleContainer = findViewById(R.id.roleContainer);
        createButton = findViewById(R.id.createButton);
        backButton = findViewById(R.id.backButton);

        username = MultiplayerManager.clean(SettingsManager.getUsername(this));

        if (username.isEmpty()) {
            username = SettingsManager.DEFAULT_USERNAME;
        }

        int min = MultiplayerManager.MIN_PLAYERS;
        int max = MultiplayerManager.MAX_PLAYERS;

        String[] options = new String[max - min + 1];

        for (int i = 0; i < options.length; i++) {
            options[i] = (min + i) + " Players";
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        playerCountSpinner.setAdapter(adapter);

        playerCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                playerCount = MultiplayerManager.MIN_PLAYERS + position;
                buildRoleInputs();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        createButton.setOnClickListener(v -> createRoom());
        backButton.setOnClickListener(v -> finish());
    }

    private void saveCurrentInputs() {

        savedNames.clear();
        savedPoints.clear();

        for (int i = 0; i < nameFields.size(); i++) {
            savedNames.add(nameFields.get(i).getText().toString());
            savedPoints.add(pointFields.get(i).getText().toString());
        }
    }

    private void buildRoleInputs() {

        saveCurrentInputs();

        nameFields.clear();
        pointFields.clear();
        roleContainer.removeAllViews();

        int fieldHeight = DisplayUtils.dp(this, 52);

        for (int i = 0; i < playerCount; i++) {

            boolean isPolice = (i == 0);
            boolean isThief = (i == 1);

            TextView title = new TextView(this);

            if (isPolice) {
                title.setText("ROLE 1 (POLICE - required)");
            } else if (isThief) {
                title.setText("ROLE 2 (THIEF - required)");
            } else {
                title.setText("ROLE " + (i + 1));
            }

            title.setTextSize(17);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, DisplayUtils.dp(this, 16), 0, DisplayUtils.dp(this, 6));

            roleContainer.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            EditText nameField = new EditText(this);
            nameField.setTextSize(16);
            nameField.setSingleLine(true);
            nameField.setGravity(Gravity.CENTER);
            nameField.setMaxLines(1);

            if (isPolice) {
                nameField.setText("Police");
                nameField.setEnabled(false);
            } else if (isThief) {
                nameField.setText("Thief");
                nameField.setEnabled(false);
            } else {
                nameField.setHint("Role name");
                if (i < savedNames.size()) nameField.setText(savedNames.get(i));
            }

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, fieldHeight);
            nameParams.bottomMargin = DisplayUtils.dp(this, 6);

            roleContainer.addView(nameField, nameParams);

            EditText pointField = new EditText(this);
            pointField.setHint("Points");
            pointField.setTextSize(16);
            pointField.setSingleLine(true);
            pointField.setGravity(Gravity.CENTER);
            pointField.setInputType(
                    android.text.InputType.TYPE_CLASS_NUMBER |
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);

            if (i < savedPoints.size()) {
                pointField.setText(savedPoints.get(i));
            } else if (isPolice) {
                pointField.setText("250");
            } else if (isThief) {
                pointField.setText("0");
            }

            LinearLayout.LayoutParams pointParams = new LinearLayout.LayoutParams(
                    DisplayUtils.dp(this, 140), fieldHeight);
            pointParams.gravity = Gravity.CENTER;

            roleContainer.addView(pointField, pointParams);

            nameFields.add(nameField);
            pointFields.add(pointField);
        }
    }

    private void createRoom() {

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> points = new ArrayList<>();

        for (int i = 0; i < playerCount; i++) {

            String name = nameFields.get(i).getText().toString().trim();
            String pointText = pointFields.get(i).getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter Role " + (i + 1) + " name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (i >= 2 && (name.equalsIgnoreCase("Police") || name.equalsIgnoreCase("Thief"))) {
                Toast.makeText(this,
                        "Police and Thief are already included. Use a different name for Role " + (i + 1),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (name.contains("|") || name.contains("~")) {
                Toast.makeText(this, "Role " + (i + 1) + " has an invalid character",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (pointText.isEmpty()) {
                Toast.makeText(this, "Enter points for Role " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            int pts;

            try {
                pts = Integer.parseInt(pointText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid points for Role " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            names.add(name);
            points.add(pts);
        }

        if (MultiplayerManager.getLocalIp() == null) {
            Toast.makeText(this,
                    "Connect to Wi-Fi or turn on your hotspot first",
                    Toast.LENGTH_LONG).show();
            return;
        }

        MultiplayerManager m = MultiplayerManager.get();
        m.roleNames = names;
        m.rolePoints = points;

        createButton.setEnabled(false);

        m.startHost(username, playerCount, (ok, err) -> {
            createButton.setEnabled(true);

            if (ok) {
                startActivity(new Intent(this, MultiplayerLobbyActivity.class));
            } else {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
        });
    }
}