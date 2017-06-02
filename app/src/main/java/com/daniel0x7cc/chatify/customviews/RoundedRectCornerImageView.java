package com.daniel0x7cc.chatify.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class RoundedRectCornerImageView extends AppCompatImageView {

    private final RectF rect = new RectF();
    private Path path;

    public RoundedRectCornerImageView(Context context) {
        super(context);
        init();
    }

    public RoundedRectCornerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoundedRectCornerImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        path = new Path();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, this.getWidth(), this.getHeight());
        float radius = 18.0f;
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(path);
        super.onDraw(canvas);
    }
}