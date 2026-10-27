package com.project.game1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GameHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private TextView emptyText;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_history);

        historyContainer = findViewById(R.id.historyContainer);
        emptyText = findViewById(R.id.emptyText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {

        GameHistoryDbHelper dbHelper = new GameHistoryDbHelper(this);
        ArrayList<GameHistoryDbHelper.MatchRecord> records = dbHelper.getAllMatches();

        historyContainer.removeAllViews();

        if (records.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        emptyText.setVisibility(View.GONE);

        for (GameHistoryDbHelper.MatchRecord record : records) {
            historyContainer.addView(buildHistoryCard(record));
        }
    }

    private LinearLayout buildHistoryCard(GameHistoryDbHelper.MatchRecord record) {

        int cardPadding = DisplayUtils.dp(this, 18);
        int cardMarginH = DisplayUtils.dp(this, 16);
        int cardMarginV = DisplayUtils.dp(this, 8);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
        card.setBackgroundColor(Color.parseColor("#2E2E48"));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        cardParams.leftMargin = cardMarginH;
        cardParams.rightMargin = cardMarginH;
        cardParams.topMargin = cardMarginV;
        cardParams.bottomMargin = cardMarginV;
        card.setLayoutParams(cardParams);

        TextView dateLabel = new TextView(this);
        dateLabel.setText(GameHistoryDbHelper.formatDate(record.dateMillis));
        dateLabel.setTextSize(14);
        dateLabel.setTextColor(Color.parseColor("#AAAAAA"));

        card.addView(dateLabel);

        int winnerIndex = findWinnerIndex(record.scores);

        LinearLayout winnerRow = new LinearLayout(this);
        winnerRow.setOrientation(LinearLayout.HORIZONTAL);
        winnerRow.setGravity(Gravity.CENTER_VERTICAL);
        winnerRow.setPadding(0, DisplayUtils.dp(this, 10), 0, DisplayUtils.dp(this, 6));

        TextView winnerLabel = new TextView(this);
        winnerLabel.setText("👑 " + record.players.get(winnerIndex));
        winnerLabel.setTextSize(18);
        winnerLabel.setTypeface(null, Typeface.BOLD);
        winnerLabel.setTextColor(Color.parseColor("#FFD700"));
        winnerLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        winnerLabel.setMaxLines(1);

        LinearLayout.LayoutParams winnerLabelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );

        winnerRow.addView(winnerLabel, winnerLabelParams);

        TextView winnerScoreLabel = new TextView(this);
        winnerScoreLabel.setText(record.scores.get(winnerIndex) + " pts");
        winnerScoreLabel.setTextSize(16);
        winnerScoreLabel.setTypeface(null, Typeface.BOLD);
        winnerScoreLabel.setTextColor(Color.WHITE);

        winnerRow.addView(winnerScoreLabel);
        card.addView(winnerRow);

        StringBuilder namesBuilder = new StringBuilder();

        for (int i = 0; i < record.players.size(); i++) {
            if (i > 0) {
                namesBuilder.append(", ");
            }
            namesBuilder.append(record.players.get(i));
        }

        TextView playersSummary = new TextView(this);
        playersSummary.setText(record.players.size() + " players: " + namesBuilder.toString());
        playersSummary.setTextSize(13);
        playersSummary.setTextColor(Color.parseColor("#CCCCCC"));
        playersSummary.setPadding(0, DisplayUtils.dp(this, 6), 0, 0);
        playersSummary.setMaxLines(3);
        playersSummary.setEllipsize(android.text.TextUtils.TruncateAt.END);

        card.addView(playersSummary);

        TextView tapHint = new TextView(this);
        tapHint.setText("Tap to view full results ›");
        tapHint.setTextSize(12);
        tapHint.setTextColor(Color.parseColor("#888888"));
        tapHint.setPadding(0, DisplayUtils.dp(this, 10), 0, 0);

        card.addView(tapHint);

        card.setOnClickListener(v -> openMatchDetail(record));

        return card;
    }

    private int findWinnerIndex(ArrayList<Integer> scores) {

        int winnerIndex = 0;

        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(winnerIndex)) {
                winnerIndex = i;
            }
        }

        return winnerIndex;
    }

    private void openMatchDetail(GameHistoryDbHelper.MatchRecord record) {

        Intent intent = new Intent(GameHistoryActivity.this, FinalScoreActivity.class);

        intent.putExtra("historyMode", true);
        intent.putExtra("historyDate", record.dateMillis);
        intent.putStringArrayListExtra("historyPlayers", record.players);
        intent.putIntegerArrayListExtra("historyScores", record.scores);

        startActivity(intent);
    }
}