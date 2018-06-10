package com.github.biasai.repositories.https.warebia.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.github.biasai.repositories.https.warebia.utils.AssetsUtils;
import com.github.biasai.repositories.https.warebia.utils.ProportionUtils;


/**
 * 圆形进度条控件
 * Created by 彭治铭 on 2017/9/24.
 */

public class ProgressView extends View {
    public ProgressView(Context context) {
        super(context);
        init();
    }

    public ProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    Bitmap dst, load;
    int width, height;

    private void init() {
        dst = AssetsUtils.getInstance().getBitmapFromAssets("biasai/progress/circleprgoress.png", 0, true);
        int dstWidth = ProportionUtils.getInstance().adapterInt(150);
        float bias = (float) dstWidth / dst.getWidth();
        dst = Bitmap.createScaledBitmap(dst, dstWidth, dstWidth, true);
        load = AssetsUtils.getInstance().getBitmapFromAssets("biasai/progress/loading.png", 0, true);
        load = Bitmap.createScaledBitmap(load, (int) (load.getWidth() * bias), (int) (load.getHeight() * bias), true);
        width = dstWidth * 2;
        height = width;
        centerX = width / 2;
        centerY = height / 2;
        dstX = (width - dst.getWidth()) / 2;
        dstY = (height - dst.getHeight()) / 2;
        loadX = (width - load.getWidth()) / 2;
        loadY = (width - load.getHeight()) / 2;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColorFilter(new LightingColorFilter(Color.TRANSPARENT, 0xFFFFFFFF));//原有图片总感觉会失真，效果不好，所以直接变成白色。效果会好点。
        setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    Paint paint;
    public int degress = 0;
    int centerX;
    int centerY;
    int dstX, dstY;
    int loadX, loadY;

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.save();
        canvas.rotate(degress += 3, centerX, centerY);
        canvas.drawBitmap(dst, dstX, dstY, paint);
        canvas.restore();
        canvas.drawBitmap(load, loadX, loadY, paint);
        invalidate();
    }

}
