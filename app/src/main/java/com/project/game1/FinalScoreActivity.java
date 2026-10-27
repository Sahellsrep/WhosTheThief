package com.project.game1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class FinalScoreActivity extends AppCompatActivity {

    private TextView dateText;
    private TextView winnerBanner;
    private LinearLayout rankingContainer;
    private Button mainMenuButton;

    private ArrayList<String> rankedNames = new ArrayList<>();
    private ArrayList<Integer> rankedScores = new ArrayList<>();

    private boolean historyMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_score);

        dateText = findViewById(R.id.dateText);
        winnerBanner = findViewById(R.id.winnerBanner);
        rankingContainer = findViewById(R.id.rankingContainer);
        mainMenuButton = findViewById(R.id.mainMenuButton);

        historyMode = getIntent().getBooleanExtra("historyMode", false);

        ArrayList<String> playersSource;
        ArrayList<Integer> scoresSource;
        long matchDateMillis;

        if (historyMode) {

            playersSource = getIntent().getStringArrayListExtra("historyPlayers");
            scoresSource = getIntent().getIntegerArrayListExtra("historyScores");
            matchDateMillis = getIntent().getLongExtra("historyDate", System.currentTimeMillis());

            if (playersSource == null || scoresSource == null ||
                    playersSource.size() != scoresSource.size() ||
                    playersSource.isEmpty()) {

                Toast.makeText(this, "Could not load match data", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            mainMenuButton.setText("BACK TO HISTORY");

        } else {

            if (GameState.players == null ||
                    GameState.players.isEmpty() ||
                    GameState.cumulativeScores.size() != GameState.players.size()) {

                Toast.makeText(this, "No match data available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            playersSource = GameState.players;
            scoresSource = GameState.cumulativeScores;
            matchDateMillis = System.currentTimeMillis();

            GameHistoryDbHelper dbHelper = new GameHistoryDbHelper(this);
            dbHelper.saveMatch(playersSource, scoresSource);

            mainMenuButton.setText("MAIN MENU");
        }

        dateText.setText(GameHistoryDbHelper.formatDate(matchDateMillis));

        buildRankedList(playersSource, scoresSource);
        showWinnerBanner();
        showRankings();

        mainMenuButton.setOnClickListener(v -> {

            if (historyMode) {
                finish();
            } else {
                goToMainMenu();
            }
        });
    }

    private void buildRankedList(ArrayList<String> playersSource, ArrayList<Integer> scoresSource) {

        rankedNames.clear();
        rankedScores.clear();

        ArrayList<Integer> indexOrder = new ArrayList<>();

        for (int i = 0; i < playersSource.size(); i++) {
            indexOrder.add(i);
        }

        for (int i = 0; i < indexOrder.size(); i++) {
            for (int j = i + 1; j < indexOrder.size(); j++) {

                int scoreI = scoresSource.get(indexOrder.get(i));
                int scoreJ = scoresSource.get(indexOrder.get(j));

                if (scoreJ > scoreI) {
                    int temp = indexOrder.get(i);
                    indexOrder.set(i, indexOrder.get(j));
                    indexOrder.set(j, temp);
                }
            }
        }

        for (int index : indexOrder) {
            rankedNames.add(playersSource.get(index));
            rankedScores.add(scoresSource.get(index));
        }
    }

    private void showWinnerBanner() {

        String winnerName = rankedNames.get(0);
        int winnerScore = rankedScores.get(0);

        winnerBanner.setText("👑 " + winnerName + " WINS with " + winnerScore + " points!");
    }

    private void showRankings() {

        rankingContainer.removeAllViews();

        String[] medals = { "🥇", "🥈", "🥉" };

        int rowPaddingV = DisplayUtils.dp(this, 14);
        int rowPaddingH = DisplayUtils.dp(this, 18);
        int rowMarginV = DisplayUtils.dp(this, 5);
        int rankColumnWidth = DisplayUtils.dp(this, 40);
        int scoreColumnWidth = DisplayUtils.dp(this, 70);

        for (int i = 0; i < rankedNames.size(); i++) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(rowPaddingH, rowPaddingV, rowPaddingH, rowPaddingV);

            boolean isWinner = (i == 0);

            if (isWinner) {
                row.setBackgroundColor(Color.parseColor("#FFD700"));
            } else if (i % 2 == 0) {
                row.setBackgroundColor(Color.parseColor("#2E2E48"));
            } else {
                row.setBackgroundColor(Color.parseColor("#26263F"));
            }

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            rowParams.topMargin = rowMarginV;
            rowParams.bottomMargin = rowMarginV;
            row.setLayoutParams(rowParams);

            TextView rankText = new TextView(this);

            String rankLabel = (i < 3) ? medals[i] : String.valueOf(i + 1) + ".";

            rankText.setText(rankLabel);
            rankText.setTextSize(18);
            rankText.setTextColor(isWinner ? Color.BLACK : Color.WHITE);

            LinearLayout.LayoutParams rankParams =
                    new LinearLayout.LayoutParams(rankColumnWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

            row.addView(rankText, rankParams);

            TextView nameText = new TextView(this);
            nameText.setText(rankedNames.get(i));
            nameText.setTextSize(17);
            nameText.setTextColor(isWinner ? Color.BLACK : Color.WHITE);
            nameText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameText.setMaxLines(1);

            if (isWinner) {
                nameText.setTypeface(null, Typeface.BOLD);
            }

            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );

            row.addView(nameText, nameParams);

            TextView scoreText = new TextView(this);
            scoreText.setText(String.valueOf(rankedScores.get(i)));
            scoreText.setTextSize(17);
            scoreText.setTypeface(null, Typeface.BOLD);
            scoreText.setTextColor(isWinner ? Color.BLACK : Color.parseColor("#FFD700"));
            scoreText.setGravity(Gravity.END);

            LinearLayout.LayoutParams scoreParams =
                    new LinearLayout.LayoutParams(scoreColumnWidth, LinearLayout.LayoutParams.WRAP_CONTENT);

            row.addView(scoreText, scoreParams);

            rankingContainer.addView(row);
        }
    }

    private void goToMainMenu() {

        Intent intent = new Intent(FinalScoreActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}