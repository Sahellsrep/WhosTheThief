package com.project.game1;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.SeekBar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class GameApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ClickSound.init(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityResumed(Activity activity) {
                installClickListener(activity);
            }

            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }

    private void installClickListener(final Activity activity) {

        final Window window = activity.getWindow();
        final Window.Callback original = window.getCallback();

        // Already installed on this window
        if (original == null || Proxy.isProxyClass(original.getClass())) {
            return;
        }

        Window.Callback proxy = (Window.Callback) Proxy.newProxyInstance(
                Window.Callback.class.getClassLoader(),
                new Class[]{Window.Callback.class},
                (p, method, args) -> {

                    if (method.getName().equals("dispatchTouchEvent")) {

                        MotionEvent e = (MotionEvent) args[0];

                        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {

                            View hit = findClickable(window.getDecorView(), e.getRawX(), e.getRawY());

                            if (hit != null) {
                                ClickSound.play(activity);
                            }
                        }
                    }

                    try {
                        return method.invoke(original, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                }
        );

        window.setCallback(proxy);
    }

    // Finds the clickable view under the finger, ignoring text fields and sliders
    private View findClickable(View view, float rawX, float rawY) {

        if (view.getVisibility() != View.VISIBLE) {
            return null;
        }

        int[] loc = new int[2];
        view.getLocationOnScreen(loc);

        boolean inside = rawX >= loc[0] && rawX <= loc[0] + view.getWidth()
                && rawY >= loc[1] && rawY <= loc[1] + view.getHeight();

        if (!inside) {
            return null;
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View found = findClickable(group.getChildAt(i), rawX, rawY);

                if (found != null) {
                    return found;
                }
            }
        }

        if (view instanceof EditText || view instanceof SeekBar) {
            return null;
        }

        if (view.isClickable() && view.isEnabled()) {
            return view;
        }

        return null;
    }
}