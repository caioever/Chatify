package com.daniel0x7cc.chatify.customviews;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class TouchImageView extends AppCompatImageView {

    private Matrix matrix;

    // We can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private static final int CLICK = 3;

    // Remember some things for zooming
    private final PointF last = new PointF();
    private final PointF start = new PointF();
    private float[] m;

    private float saveScale = 1f;
    private int viewWidth;
    private int viewHeight;
    private float origWidth;
    private float origHeight;
    private int oldMeasuredHeight;

    private ScaleGestureDetector mScaleDetector;

    public TouchImageView(final Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    private void sharedConstructing(final Context context) {
        super.setClickable(true);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            final float deltaX = curr.x - last.x;
                            final float deltaY = curr.y - last.y;
                            final float fixTransX = getFixDragTrans(deltaX, viewWidth, origWidth * saveScale);
                            final float fixTransY = getFixDragTrans(deltaY, viewHeight, origHeight * saveScale);
                            matrix.postTranslate(fixTransX, fixTransY);
                            fixTrans();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        mode = NONE;
                        final int xDiff = (int) Math.abs(curr.x - start.x);
                        final int yDiff = (int) Math.abs(curr.y - start.y);
                        if (xDiff < CLICK && yDiff < CLICK) {
                            performClick();
                        }
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }

                setImageMatrix(matrix);
                invalidate();
                return true; // indicate event was handled
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(final ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(final ScaleGestureDetector detector) {
            float minScale = 1f;
            float mScaleFactor = detector.getScaleFactor();
            final float origScale = saveScale;
            saveScale *= mScaleFactor;
            float maxScale = 3f;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2, viewHeight / 2);
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
            }
            fixTrans();
            return true;
        }
    }

    private void fixTrans() {
        matrix.getValues(m);
        final float transX = m[Matrix.MTRANS_X];
        final float transY = m[Matrix.MTRANS_Y];

        final float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
        final float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTrans(final float trans, final float viewSize, final float contentSize) {
        float minTrans;
        float maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;

        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans) {
            return -trans + minTrans;
        }

        if (trans > maxTrans) {
            return -trans + maxTrans;
        }

        return 0;
    }

    private float getFixDragTrans(final float delta, final float viewSize, final float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        //
        // Rescales image on rotation
        //
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0) {
            return;
        }
        oldMeasuredHeight = viewHeight;

        if (saveScale == 1) {
            // Fit to screen.
            float scale;

            final Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
                return;
            }
            final int bmWidth = drawable.getIntrinsicWidth();
            final int bmHeight = drawable.getIntrinsicHeight();

            final float scaleX = (float) viewWidth / (float) bmWidth;
            final float scaleY = (float) viewHeight / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);

            // Center the image
            float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;

            matrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }

}