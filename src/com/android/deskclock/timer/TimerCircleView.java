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

package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.data.Timer;

/**
 * Custom view that draws timer progress as a circle.
 */
public final class TimerCircleView extends View {

    /** The size of the dot indicating the progress through the timer. */
    private final float mDotRadius;

    /** The color indicating the remaining portion of the timer. */
    private final int mRemainderColor;

    /** The color indicating the completed portion of the timer. */
    private final int mCompletedColor;

    /** The size of the stroke that paints the timer circle. */
    private final float mStrokeSize;

    private final Paint mPaint = new Paint();
    private final Paint mFill = new Paint();
    private final RectF mArcRect = new RectF();

    private Timer mTimer;

    @SuppressWarnings("unused")
    public TimerCircleView(Context context) {
        this(context, null);
    }

    public TimerCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources resources = context.getResources();
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        mDotRadius = mStrokeSize / 2;

        mRemainderColor = ThemeUtils.resolveColor(context, R.attr.colorPrimary);
        mCompletedColor = ThemeUtils.resolveColor(context, android.R.attr.colorBackground);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        mFill.setAntiAlias(true);
        mFill.setColor(mRemainderColor);
        mFill.setStyle(Paint.Style.FILL);
    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        // Compute the size and location of the circle to be drawn.
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mStrokeSize;

        // Reset old painting state.
        mPaint.setColor(mRemainderColor);
        mPaint.setStrokeWidth(mStrokeSize);

        // If the timer is reset, draw a simple white circle.
        final float redPercent;
        if (mTimer.isReset()) {
            // Draw a complete white circle; no red arc required.
            drawWavyArc(canvas, xCenter, yCenter, radius, 0, 360, mPaint);

            // Red percent is 0 since no timer progress has been made.
            redPercent = 0;
        } else if (mTimer.isExpired()) {
            mPaint.setColor(mCompletedColor);

            // Draw a complete circle; no arc required.
            drawWavyArc(canvas, xCenter, yCenter, radius, 0, 360, mPaint);

            // Red percent is 1 since the timer has expired.
            redPercent = 1;
        } else {
            // Draw a combination of red and white arcs to create a circle.
            mArcRect.top = yCenter - radius;
            mArcRect.bottom = yCenter + radius;
            mArcRect.left = xCenter - radius;
            mArcRect.right = xCenter + radius;
            redPercent = Math.min(1, (float) mTimer.getElapsedTime() / (float) mTimer.getTotalLength());
            final float whitePercent = 1 - redPercent;

            // Draw a white arc to indicate the amount of timer that remains.
            drawWavyArc(canvas, xCenter, yCenter, radius, 270, whitePercent * 360, mPaint);

            // Draw a red arc to indicate the amount of timer completed.
            mPaint.setColor(mCompletedColor);
            drawWavyArc(canvas, xCenter, yCenter, radius, 270, -redPercent * 360, mPaint);
        }

        // Draw a dot to indicate current progress through the timer.
        final float dotAngleDegrees = 270 - redPercent * 360;
        final double dotAngleRadians = Math.toRadians(dotAngleDegrees);
        final float dotX = xCenter + (float) (waveRadius(radius, dotAngleRadians) * Math.cos(dotAngleRadians));
        final float dotY = yCenter + (float) (waveRadius(radius, dotAngleRadians) * Math.sin(dotAngleRadians));
        canvas.drawCircle(dotX, dotY, mDotRadius, mFill);

        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }
    private final Path mWavePath = new Path();

    private float waveAmp() {
        return 2.5f * getResources().getDisplayMetrics().density;
    }

    private float waveRadius(float r, double angleRadians) {
        final float amp = waveAmp();
        return (float) (r - amp + amp * Math.sin(20 * angleRadians));
    }

    private void drawWavyArc(Canvas canvas, float cx, float cy, float r,
            float startDeg, float sweepDeg, Paint paint) {
        final int steps = Math.max(2, (int) (Math.abs(sweepDeg) * 2));
        mWavePath.reset();
        for (int i = 0; i <= steps; i++) {
            final double a = Math.toRadians(startDeg + sweepDeg * i / steps);
            final float rr = waveRadius(r, a);
            final float x = (float) (cx + rr * Math.cos(a));
            final float y = (float) (cy + rr * Math.sin(a));
            if (i == 0) mWavePath.moveTo(x, y); else mWavePath.lineTo(x, y);
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(mWavePath, paint);
    }
}
