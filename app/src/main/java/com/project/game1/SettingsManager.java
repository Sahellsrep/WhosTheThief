package com.project.game1;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREFS_NAME = "game_settings";
    private static final String KEY_VOLUME = "game_volume";
    private static final String KEY_USERNAME = "multiplayer_username";

    public static final int DEFAULT_VOLUME = 70;
    public static final String DEFAULT_USERNAME = "Player";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getVolume(Context context) {
        return prefs(context).getInt(KEY_VOLUME, DEFAULT_VOLUME);
    }

    public static void setVolume(Context context, int volume) {
        prefs(context).edit().putInt(KEY_VOLUME, volume).apply();
    }

    public static String getUsername(Context context) {
        return prefs(context).getString(KEY_USERNAME, DEFAULT_USERNAME);
    }

    public static void setUsername(Context context, String username) {
        prefs(context).edit().putString(KEY_USERNAME, username).apply();
    }
}