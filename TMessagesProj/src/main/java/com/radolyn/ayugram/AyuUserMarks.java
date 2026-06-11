package com.radolyn.ayugram;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.LayoutHelper;

public final class AyuUserMarks {
    public static final long DEVELOPER_ID = 5196899473L;

    private static final long[] DRUNA_IDS = {
            7179502450L,
            5103618641L,
            6689531171L,
            6996532736L,
            6131808782L,
            5391144430L
    };

    public static boolean isDeveloper(long userId) {
        return userId == DEVELOPER_ID;
    }

    public static boolean isDruna(long userId) {
        for (long id : DRUNA_IDS) {
            if (id == userId) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMark(long userId) {
        return isDeveloper(userId) || isDruna(userId);
    }

    public static String getMarkTitle(long userId) {
        if (isDeveloper(userId)) {
            return "\u0420\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a";
        }
        if (isDruna(userId)) {
            return "\u0414\u0440\u0443\u043d";
        }
        return "";
    }

    public static String getMarkDescription(long userId) {
        if (isDeveloper(userId)) {
            return "\u042d\u0442\u043e\u0442 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u0440\u0430\u0437\u0440\u0430\u0431\u043e\u0442\u0447\u0438\u043a\u043e\u043c ChotkoGram.";
        }
        if (isDruna(userId)) {
            return "\u042d\u0442\u043e\u043c\u0443 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044e \u0432\u044b\u0434\u0430\u043d\u0430 \u0441\u0442\u0438\u043b\u044c\u043d\u0430\u044f \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f \u043c\u0435\u0442\u043a\u0430 \u0434\u0440\u0443\u043d\u0430.";
        }
        return "";
    }

    public static Drawable createBadgeDrawable(long userId) {
        if (isDeveloper(userId)) {
            return new BadgeDrawable(true);
        }
        if (isDruna(userId)) {
            return new BadgeDrawable(false);
        }
        return null;
    }

    public static void playDeveloperAnimation(ViewGroup parent) {
        if (parent == null) {
            return;
        }
        DeveloperAnimationView view = new DeveloperAnimationView(parent.getContext());
        parent.addView(view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        view.start(() -> parent.removeView(view));
    }

    private static class BadgeDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean developer;
        private final Path path = new Path();
        private final RectF rect = new RectF();

        BadgeDrawable(boolean developer) {
            this.developer = developer;
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float cx = bounds.centerX();
            float cy = bounds.centerY();
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    developer
                            ? new int[]{0xff49b8ff, 0xff6f5cff, 0xffffd66b}
                            : new int[]{0xff171a24, 0xff303447, 0xffc89532},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            strokePaint.setColor(developer ? 0x99ffffff : 0xffffd35a);
            strokePaint.setStrokeWidth(Math.max(1.2f, radius * 0.13f));
            canvas.drawCircle(cx, cy, radius - strokePaint.getStrokeWidth() / 2f, strokePaint);

            if (developer) {
                strokePaint.setColor(0xffffffff);
                strokePaint.setStrokeWidth(Math.max(2f, radius * 0.22f));
                path.rewind();
                path.moveTo(cx - radius * 0.45f, cy);
                path.lineTo(cx - radius * 0.12f, cy + radius * 0.33f);
                path.lineTo(cx + radius * 0.48f, cy - radius * 0.38f);
                canvas.drawPath(path, strokePaint);
            } else {
                rect.set(cx - radius * .48f, cy - radius * .48f, cx + radius * .48f, cy + radius * .48f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.4f, radius * .15f));
                paint.setColor(0xffffd35a);
                canvas.drawRoundRect(rect, radius * .22f, radius * .22f, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xffffe6a0);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(AndroidUtilities.bold());
                paint.setTextSize(radius * 1.05f);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                canvas.drawText("\u0414", cx, cy - (metrics.ascent + metrics.descent) / 2f, paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            strokePaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            strokePaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return AndroidUtilities.dp(18);
        }

        @Override
        public int getIntrinsicHeight() {
            return AndroidUtilities.dp(18);
        }
    }

    private static class DeveloperAnimationView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] seeds = new float[42];
        private float progress;

        DeveloperAnimationView(android.content.Context context) {
            super(context);
            setWillNotDraw(false);
            for (int i = 0; i < seeds.length; i++) {
                seeds[i] = (float) Math.random();
            }
        }

        void start(Runnable end) {
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1450);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    end.run();
                }
            });
            setScaleX(0.9f);
            setScaleY(0.9f);
            animate().scaleX(1f).scaleY(1f).setDuration(480).setInterpolator(new OvershootInterpolator(1.5f)).start();
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h * 0.32f;
            float alpha = progress < 0.76f ? 1f : (1f - progress) / 0.24f;

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx, cy, Math.max(w, h) * .42f,
                    new int[]{Color.argb((int) (85 * alpha), 255, 214, 107), Color.argb(0, 47, 156, 255)},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(2.5f));
            paint.setColor(Color.argb((int) (210 * alpha), 255, 214, 107));
            canvas.drawCircle(cx, cy, AndroidUtilities.dp(24) + AndroidUtilities.dp(90) * progress, paint);
            paint.setStrokeWidth(AndroidUtilities.dp(1.4f));
            paint.setColor(Color.argb((int) (170 * alpha), 83, 165, 255));
            canvas.drawCircle(cx, cy, AndroidUtilities.dp(54) + AndroidUtilities.dp(130) * progress, paint);

            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < seeds.length; i++) {
                float angle = (float) (Math.PI * 2 * i / seeds.length + seeds[i]);
                float dist = AndroidUtilities.dp(24) + AndroidUtilities.dp(180) * progress * (0.55f + seeds[i] * 0.45f);
                float x = cx + (float) Math.cos(angle) * dist;
                float y = cy + (float) Math.sin(angle) * dist + AndroidUtilities.dp(42) * progress;
                int color = i % 3 == 0 ? 0xffffd66b : i % 3 == 1 ? 0xff55a5ff : 0xffa767ff;
                paint.setColor(Color.argb((int) (225 * alpha), Color.red(color), Color.green(color), Color.blue(color)));
                canvas.drawCircle(x, y, AndroidUtilities.dp(2.3f + 3.2f * seeds[i]), paint);
            }

            paint.setColor(Color.argb((int) (245 * alpha), 255, 230, 160));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(AndroidUtilities.dp(19));
            canvas.drawText("DEVELOPER", cx, cy + AndroidUtilities.dp(7), paint);
        }
    }

    private AyuUserMarks() {
    }
}
