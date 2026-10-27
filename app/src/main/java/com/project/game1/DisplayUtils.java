package com.project.game1;

import android.content.Context;
import android.util.TypedValue;

public class DisplayUtils {

    public static int dp(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        );
    }
}
