package com.example.releasethekraken.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


/**
 * Helper class responsible for managing accessibility settings across the app.
 * Handles toggling features such as increased font size and alternative color themes,
 * and ensures these settings persist across different screens using shared preferences.
 *
 */

public class AccessibilitySettingsHelper {

    private static final String PREF_NAME = "accessibility_prefs";
    private static final String KEY_ACCESSIBILITY = "accessibility_mode";
    private static final String KEY_COLORBLIND = "colorblind_mode";

    private static final float TEXT_SCALE_MULTIPLIER = 1.18f;
    private static final int MIN_TOUCH_TARGET_DP = 56;

    // Safer alternate palette
    private static final int ALT_PRIMARY = Color.parseColor("#0F766E");
    private static final int ALT_ACCENT = Color.parseColor("#C2410C");
    private static final int ALT_TEXT = Color.parseColor("#0F172A");
    private static final int ALT_MUTED = Color.parseColor("#E2E8F0");
    private static final int ALT_DANGER = Color.parseColor("#9A3412");

    private static class ViewState {
        final float textSizeSp;
        final int minHeight;
        final int minWidth;
        final Integer originalTextColor;
        final ColorStateList originalBackgroundTint;
        final ColorStateList originalImageTint;

        ViewState(float textSizeSp,
                  int minHeight,
                  int minWidth,
                  Integer originalTextColor,
                  ColorStateList originalBackgroundTint,
                  ColorStateList originalImageTint) {
            this.textSizeSp = textSizeSp;
            this.minHeight = minHeight;
            this.minWidth = minWidth;
            this.originalTextColor = originalTextColor;
            this.originalBackgroundTint = originalBackgroundTint;
            this.originalImageTint = originalImageTint;
        }
    }

    public static void setAccessibilityMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ACCESSIBILITY, enabled).apply();
    }

    public static boolean isAccessibilityMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ACCESSIBILITY, false);
    }

    public static void setColorBlindMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_COLORBLIND, enabled).apply();
    }

    public static boolean isColorBlindMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_COLORBLIND, false);
    }

    public static void applyAccessibility(View root, Context context) {
        if (root == null || context == null) return;

        boolean accessibilityEnabled = isAccessibilityMode(context);
        boolean colorBlindEnabled = isColorBlindMode(context);

        traverseAndApply(root, context, accessibilityEnabled, colorBlindEnabled);

        root.requestLayout();
        root.invalidate();
    }

    private static void traverseAndApply(View view,
                                         Context context,
                                         boolean accessibilityEnabled,
                                         boolean colorBlindEnabled) {
        if (view == null) return;

        if (view.getTag() == null || !(view.getTag() instanceof ViewState)) {
            float originalSp = -1f;
            Integer originalTextColor = null;
            ColorStateList originalBackgroundTint = null;
            ColorStateList originalImageTint = null;

            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                originalSp = tv.getTextSize() / tv.getResources().getDisplayMetrics().scaledDensity;
                originalTextColor = tv.getCurrentTextColor();
            }

            if (view instanceof MaterialButton) {
                originalBackgroundTint = ((MaterialButton) view).getBackgroundTintList();
            } else if (view instanceof FloatingActionButton) {
                originalBackgroundTint = ((FloatingActionButton) view).getBackgroundTintList();
                originalImageTint = ((FloatingActionButton) view).getImageTintList();
            } else if (view instanceof ImageView) {
                originalImageTint = ((ImageView) view).getImageTintList();
            }

            view.setTag(new ViewState(
                    originalSp,
                    view.getMinimumHeight(),
                    view.getMinimumWidth(),
                    originalTextColor,
                    originalBackgroundTint,
                    originalImageTint
            ));
        }

        ViewState state = (ViewState) view.getTag();

        if (view instanceof TextView && state.textSizeSp > 0) {
            TextView tv = (TextView) view;
            float targetSize = accessibilityEnabled
                    ? state.textSizeSp * TEXT_SCALE_MULTIPLIER
                    : state.textSizeSp;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, targetSize);

            if (state.originalTextColor != null) {
                if (colorBlindEnabled) {
                    tv.setTextColor(remapTextColor(state.originalTextColor));
                } else {
                    tv.setTextColor(state.originalTextColor);
                }
            }
        }

        if (isInteractiveControl(view)) {
            if (accessibilityEnabled) {
                int minTouch = dpToPx(context, MIN_TOUCH_TARGET_DP);
                view.setMinimumHeight(Math.max(state.minHeight, minTouch));
                view.setMinimumWidth(Math.max(state.minWidth, minTouch));
            } else {
                view.setMinimumHeight(state.minHeight);
                view.setMinimumWidth(state.minWidth);
            }
        }

        if (view instanceof MaterialButton) {
            applyButtonPalette((MaterialButton) view, state, colorBlindEnabled);
        } else if (view instanceof FloatingActionButton) {
            applyFabPalette((FloatingActionButton) view, state, colorBlindEnabled);
        } else if (view instanceof ImageButton) {
            applyImageButtonPalette((ImageButton) view, state, colorBlindEnabled);
        } else if (view instanceof SwitchCompat) {
            if (state.originalTextColor != null) {
                ((SwitchCompat) view).setTextColor(colorBlindEnabled ? ALT_TEXT : state.originalTextColor);
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseAndApply(group.getChildAt(i), context, accessibilityEnabled, colorBlindEnabled);
            }
        }
    }

    private static void applyButtonPalette(MaterialButton button, ViewState state, boolean enabled) {
        if (!enabled) {
            button.setBackgroundTintList(state.originalBackgroundTint);
            if (state.originalTextColor != null) {
                button.setTextColor(state.originalTextColor);
            }
            return;
        }

        String text = button.getText() != null ? button.getText().toString().toLowerCase() : "";

        if (text.contains("delete")) {
            button.setBackgroundTintList(ColorStateList.valueOf(ALT_DANGER));
            button.setTextColor(Color.WHITE);
        } else if (text.contains("sign out")) {
            button.setBackgroundTintList(ColorStateList.valueOf(ALT_MUTED));
            button.setTextColor(ALT_TEXT);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(ALT_PRIMARY));
            button.setTextColor(Color.WHITE);
        }
    }

    private static void applyFabPalette(FloatingActionButton fab, ViewState state, boolean enabled) {
        if (!enabled) {
            fab.setBackgroundTintList(state.originalBackgroundTint);
            fab.setImageTintList(state.originalImageTint);
            return;
        }

        fab.setBackgroundTintList(ColorStateList.valueOf(ALT_ACCENT));
        fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));
    }

    private static void applyImageButtonPalette(ImageButton imageButton, ViewState state, boolean enabled) {
        if (state.originalImageTint != null) {
            imageButton.setImageTintList(enabled
                    ? ColorStateList.valueOf(ALT_PRIMARY)
                    : state.originalImageTint);
            return;
        }

        Drawable drawable = imageButton.getDrawable();
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable.mutate());
            DrawableCompat.setTint(drawable, enabled ? ALT_PRIMARY : Color.TRANSPARENT);
            imageButton.setImageDrawable(drawable);
        }
    }

    private static int remapTextColor(int original) {
        if (isDark(original)) return ALT_TEXT;
        return original;
    }

    private static boolean isDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.35;
    }

    private static boolean isInteractiveControl(View view) {
        return view instanceof Button
                || view instanceof MaterialButton
                || view instanceof ImageButton
                || view instanceof FloatingActionButton
                || view instanceof SwitchCompat;
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        context.getResources().getDisplayMetrics()
                )
        );
    }
}