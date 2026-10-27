package com.project.game1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RoleSetupActivity extends AppCompatActivity {

    private LinearLayout roleList;
    private Button backButton;
    private Button nextButton;

    private ArrayList<String> players;

    private ArrayList<String> roles = new ArrayList<>();
    private ArrayList<Integer> rolePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_setup);

        roleList = findViewById(R.id.roleList);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        players = getIntent().getStringArrayListExtra("players");

        if (players == null || players.size() < 4) {
            Toast.makeText(this, "Player information missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        createRoleInputs();

        backButton.setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> saveRoles());
    }

    private void createRoleInputs() {
        roleList.removeAllViews();

        int rowMargin = DisplayUtils.dp(this, 24);
        int fieldHeight = DisplayUtils.dp(this, 52);
        int pointsWidth = DisplayUtils.dp(this, 140);

        for (int i = 0; i < players.size(); i++) {

            boolean isPolice = (i == 0);
            boolean isThief = (i == 1);

            TextView roleTitle = new TextView(this);

            if (isPolice) {
                roleTitle.setText("ROLE " + (i + 1) + " (POLICE - required)");
            } else if (isThief) {
                roleTitle.setText("ROLE " + (i + 1) + " (THIEF - required)");
            } else {
                roleTitle.setText("ROLE " + (i + 1));
            }

            roleTitle.setTextSize(18);
            roleTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            roleTitle.setGravity(Gravity.CENTER);
            roleTitle.setPadding(0, DisplayUtils.dp(this, 14), 0, DisplayUtils.dp(this, 8));

            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            roleList.addView(roleTitle, titleParams);

            EditText roleName = new EditText(this);
            roleName.setTextSize(16);
            roleName.setSingleLine(true);
            roleName.setTag("role_name_" + i);
            roleName.setGravity(Gravity.CENTER);

            if (isPolice) {
                roleName.setText("Police");
                roleName.setEnabled(false);
                roleName.setTextColor(android.graphics.Color.WHITE);
            } else if (isThief) {
                roleName.setText("Thief");
                roleName.setEnabled(false);
                roleName.setTextColor(android.graphics.Color.WHITE);
            } else {
                roleName.setHint("Role name");
            }

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    fieldHeight
            );

            nameParams.leftMargin = rowMargin;
            nameParams.rightMargin = rowMargin;
            nameParams.bottomMargin = DisplayUtils.dp(this, 8);

            roleList.addView(roleName, nameParams);

            EditText rolePoint = new EditText(this);
            rolePoint.setHint("Points");
            rolePoint.setTextSize(16);
            rolePoint.setSingleLine(true);
            rolePoint.setGravity(Gravity.CENTER);

            rolePoint.setInputType(
                    android.text.InputType.TYPE_CLASS_NUMBER |
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            );

            rolePoint.setTag("role_point_" + i);

            LinearLayout.LayoutParams pointParams = new LinearLayout.LayoutParams(
                    pointsWidth,
                    fieldHeight
            );

            pointParams.gravity = Gravity.CENTER;
            pointParams.bottomMargin = DisplayUtils.dp(this, 16);

            roleList.addView(rolePoint, pointParams);
        }
    }

    private void saveRoles() {
        roles.clear();
        rolePoints.clear();

        for (int i = 0; i < players.size(); i++) {

            EditText roleName = null;
            EditText rolePoint = null;

            for (int j = 0; j < roleList.getChildCount(); j++) {

                View view = roleList.getChildAt(j);

                if (view.getTag() != null) {

                    String tag = view.getTag().toString();

                    if (tag.equals("role_name_" + i)) {
                        roleName = (EditText) view;
                    }

                    if (tag.equals("role_point_" + i)) {
                        rolePoint = (EditText) view;
                    }
                }
            }

            if (roleName == null || rolePoint == null) {
                Toast.makeText(this, "Role fields missing", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = roleName.getText().toString().trim();
            String pointText = rolePoint.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter Role " + (i + 1) + " name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pointText.isEmpty()) {
                Toast.makeText(this, "Enter points for Role " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            int points;

            try {
                points = Integer.parseInt(pointText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid points for Role " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }

            roles.add(name);
            rolePoints.add(points);
        }

        boolean hasPolice = false;
        boolean hasThief = false;

        for (String role : roles) {
            if (role.equalsIgnoreCase("Police")) {
                hasPolice = true;
            }
            if (role.equalsIgnoreCase("Thief")) {
                hasThief = true;
            }
        }

        if (!hasPolice || !hasThief) {
            Toast.makeText(this, "Police and Thief roles are required", Toast.LENGTH_LONG).show();
            return;
        }

        GameState.initialize(players, roles, rolePoints);

        Intent intent = new Intent(RoleSetupActivity.this, RoundAssignmentActivity.class);

        intent.putStringArrayListExtra("players", players);
        intent.putStringArrayListExtra("roles", roles);
        intent.putIntegerArrayListExtra("rolePoints", rolePoints);

        startActivity(intent);
    }
}