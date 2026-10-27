package com.project.game1;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class ClickSound {

    private static SoundPool pool;
    private static int soundId;
    private static boolean loaded = false;

    public static void init(Context context) {

        if (pool != null) {
            return;
        }

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        pool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        pool.setOnLoadCompleteListener((p, id, status) -> loaded = (status == 0));

        soundId = pool.load(context.getApplicationContext(), R.raw.click, 1);
    }

    public static void play(Context context) {

        if (!loaded) {
            return;
        }

        float volume = SettingsManager.getVolume(context) / 100f;

        if (volume <= 0f) {
            return;
        }

        pool.play(soundId, volume, volume, 1, 0, 1f);
    }
}