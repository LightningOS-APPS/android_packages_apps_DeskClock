/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.widget.toast;

import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

/**
 * Manages visibility of Snackbar and allow preemptive dismiss of current displayed Snackbar.
 */
public final class SnackbarManager {

    private static WeakReference<Snackbar> sSnackbar = null;

    private SnackbarManager() {}

    public static void show(Snackbar snackbar) {
        styleExpressive(snackbar);
        sSnackbar = new WeakReference<>(snackbar);
        snackbar.show();
    }

    public static void dismiss() {
        final Snackbar snackbar = sSnackbar == null ? null : sSnackbar.get();
        if (snackbar != null) {
            snackbar.dismiss();
            sSnackbar = null;
        }
    }
    private static void styleExpressive(Snackbar snackbar) {
        final android.view.View v = snackbar.getView();
        final android.content.Context c = v.getContext();
        final float d = c.getResources().getDisplayMetrics().density;
        final int bgColor = com.android.deskclock.ThemeUtils.resolveColor(c,
                com.android.deskclock.R.attr.colorSurfaceInverse);
        final int textColor = com.android.deskclock.ThemeUtils.resolveColor(c,
                com.android.deskclock.R.attr.colorOnSurfaceInverse);
        final int actionColor = com.android.deskclock.ThemeUtils.resolveColor(c,
                com.android.deskclock.R.attr.colorPrimaryInverse);

        final android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(16 * d);
        bg.setColor(bgColor);
        v.setBackgroundTintList(null);
        v.setBackground(bg);

        if (v.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            final android.view.ViewGroup.MarginLayoutParams lp =
                    (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            final int m = (int) (12 * d);
            lp.setMargins(m, m, m, m);
            v.setLayoutParams(lp);
        }

        final android.widget.TextView text =
                v.findViewById(com.google.android.material.R.id.snackbar_text);
        if (text != null) text.setTextColor(textColor);
        snackbar.setActionTextColor(actionColor);
    }
}
