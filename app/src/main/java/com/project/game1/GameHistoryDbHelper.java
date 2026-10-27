package com.project.game1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class GameHistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "game_history.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "match_history";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE_MILLIS = "date_millis";
    private static final String COLUMN_PLAYERS = "players";
    private static final String COLUMN_SCORES = "scores";

    // Unlikely to appear inside a player's name, safe as a list separator
    private static final String SEPARATOR = "\u0001";

    public GameHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_HISTORY + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_DATE_MILLIS + " INTEGER, " +
                        COLUMN_PLAYERS + " TEXT, " +
                        COLUMN_SCORES + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public long saveMatch(ArrayList<String> players, ArrayList<Integer> scores) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE_MILLIS, System.currentTimeMillis());
        values.put(COLUMN_PLAYERS, joinStrings(players));
        values.put(COLUMN_SCORES, joinInts(scores));

        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();

        return id;
    }

    public ArrayList<MatchRecord> getAllMatches() {

        ArrayList<MatchRecord> records = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_HISTORY,
                null,
                null,
                null,
                null,
                null,
                COLUMN_DATE_MILLIS + " DESC"
        );

        if (cursor != null) {

            while (cursor.moveToNext()) {

                long dateMillis = cursor.getLong(
                        cursor.getColumnIndexOrThrow(COLUMN_DATE_MILLIS)
                );

                String playersRaw = cursor.getString(
                        cursor.getColumnIndexOrThrow(COLUMN_PLAYERS)
                );

                String scoresRaw = cursor.getString(
                        cursor.getColumnIndexOrThrow(COLUMN_SCORES)
                );

                MatchRecord record = new MatchRecord();
                record.dateMillis = dateMillis;
                record.players = splitStrings(playersRaw);
                record.scores = splitInts(scoresRaw);

                records.add(record);
            }

            cursor.close();
        }

        db.close();

        return records;
    }

    private String joinStrings(ArrayList<String> list) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(list.get(i));
        }

        return builder.toString();
    }

    private String joinInts(ArrayList<Integer> list) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(list.get(i));
        }

        return builder.toString();
    }

    private ArrayList<String> splitStrings(String raw) {

        ArrayList<String> list = new ArrayList<>();

        if (raw == null || raw.isEmpty()) {
            return list;
        }

        String[] parts = raw.split(SEPARATOR);

        for (String part : parts) {
            list.add(part);
        }

        return list;
    }

    private ArrayList<Integer> splitInts(String raw) {

        ArrayList<Integer> list = new ArrayList<>();

        if (raw == null || raw.isEmpty()) {
            return list;
        }

        String[] parts = raw.split(SEPARATOR);

        for (String part : parts) {
            list.add(Integer.parseInt(part));
        }

        return list;
    }

    public static String formatDate(long millis) {

        SimpleDateFormat format =
                new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

        return format.format(new Date(millis));
    }

    public static class MatchRecord {
        public long dateMillis;
        public ArrayList<String> players;
        public ArrayList<Integer> scores;
    }
}