package app.crossword.yourealwaysbe.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import androidx.core.content.ContextCompat;

import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Created by rcooper on 10/26/14.
 */
public class CircleProgressBar extends View {
    private static Typeface icons1;
    private static Typeface icons4;
    private int nullColor;
    private int inProgressColor;
    private int doneColor;
    private int errorColor;
    private int height;
    private int width;
    private int percentFilled;
    private boolean complete;
    private DisplayMetrics metrics;
    private float circleStroke;
    private float circleFine;


    public CircleProgressBar(Context context) {
        super(context);
        initMetrics(context);
        loadColors(context);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMetrics(context);
        loadColors(context);
    }

    public CircleProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMetrics(context);
        loadColors(context);
    }

    private void loadColors(Context context) {
        nullColor = ContextCompat.getColor(context, R.color.progressNull);
        inProgressColor = ContextCompat.getColor(context, R.color.progressInProgress);
        doneColor = ContextCompat.getColor(context, R.color.progressDone);
        errorColor = ContextCompat.getColor(context, R.color.progressError);
    }

    private final void initMetrics(Context context){
        metrics = context.getResources().getDisplayMetrics();
        circleStroke = metrics.density * 6F;
        circleFine = metrics.density * 2f;
        if(icons1 == null) {
            icons1 = Typeface.createFromAsset(context.getAssets(), "icons1.ttf");
            icons4 = Typeface.createFromAsset(context.getAssets(), "icons4.ttf");
        }
    }

    public void setPercentFilled(int percentFilled) {
        this.percentFilled = percentFilled;
        this.invalidate();
    }

    public int getPercentFilled() {
        return percentFilled;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
        this.invalidate();
    }

    public boolean getComplete() {
        return complete;
    }
    @Override
    protected void onMeasure(int widthSpecId, int heightSpecId) {
        this.height = View.MeasureSpec.getSize(heightSpecId);
        this.width = View.MeasureSpec.getSize(widthSpecId);
        setMeasuredDimension(this.width, this.height);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float halfStroke = circleStroke / 2;
        float textSize = halfWidth * 0.75f;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(nullColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);

        if (this.complete) {
            paint.setColor(doneColor);
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(halfWidth, halfWidth, halfWidth - halfStroke - metrics.density * 2f, paint);
            paint.setTypeface(icons1);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("4", halfWidth, halfHeight + textSize / 2f, paint);
        } else if (this.percentFilled < 0) {
            paint.setColor(errorColor);
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(halfWidth, halfWidth, halfWidth - halfStroke - metrics.density * 2f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("?", halfWidth, halfWidth + textSize / 3f, paint);
        } else if (this.percentFilled == 0) {
//            paint.setStrokeWidth(circleFine);
//            canvas.drawCircle(halfWidth, halfHeight, halfWidth - metrics.density * 4f, paint);
            paint.setTypeface(icons4);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("W", halfWidth, halfWidth + textSize / 2.5f, paint);
        } else {
            paint.setColor(inProgressColor);
            paint.setStrokeWidth(circleFine);
            canvas.drawCircle(halfWidth, halfWidth, halfWidth - halfStroke - 1f, paint);
            paint.setStrokeWidth(circleStroke);

            RectF rect = new RectF(0 + circleStroke ,0 + circleStroke ,
                    width - circleStroke , width - circleStroke);
            canvas.drawArc(rect, -90,  360F * percentFilled / 100F, false, paint);
            paint.setStyle(Paint.Style.FILL);
            textSize = halfWidth * 0.5f;
            paint.setTextSize(textSize);
            canvas.drawText(percentFilled+"%", halfWidth, halfHeight + textSize / 3f, paint);
        }
    }
}
