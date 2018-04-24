package com.deng.fastcounter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ZoomActivity extends AppCompatActivity {

    private ImageView mImageView;
    private Bitmap bitmap;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    // 第一个按下的手指的点
    private PointF startPoint = new PointF();
    // 两个按下的手指的触摸点的中点
    private PointF midPoint = new PointF();
    // 初始的两个手指按下的触摸点的距离
    private float oriDis = 1f;

    private int width;          // 屏幕宽度（像素）
    private int height;         // 屏幕高度（像素）
    private int bmpwidth;       // 图片宽度
    private int bmpheight;      // 图片高度
    private float[] values = new float[9];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        }

        setContentView(R.layout.activity_zoom);

        mImageView = (ImageView) findViewById(R.id.imageView);

        DisplayMetrics dm = new DisplayMetrics();
        //取得窗口属性
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        //窗口的宽度
        width = dm.widthPixels;
        //窗口高度
        height = dm.heightPixels;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.d("Deng", "开始启动图片显示窗口");
        Intent intent = getIntent();
        if (intent != null) {
            MyApplication myApplication = (MyApplication) this.getApplication();
            bitmap = myApplication.getBitmap();
            mImageView.setImageBitmap(bitmap);
            mImageView.setScaleType(ImageView.ScaleType.MATRIX);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();
        //Log.d("Deng", "onTouch: x= " + x + "y=" + y);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //单点触控
                //Log.d("Deng", "ACTION_DOWN");
                matrix.set(mImageView.getImageMatrix());
                savedMatrix.set(matrix);
                startPoint.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: //多点触控
                //Log.d("Deng", "ACTION_POINTER_DOWN");
                oriDis = distance(event);
                if (oriDis > 10f) {
                    savedMatrix.set(matrix);
                    midPoint = midPoint(event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE: // 手指滑动
                if (mode == DRAG) { // 是一个手指拖动
                    Log.d("Deng", "mode = DRAG");
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
                } else if (mode == ZOOM) { // 两个手指滑动
                    Log.d("Deng", "mode = ZOOM");
                    float newDist = distance(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oriDis;
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                Log.d("Deng", "ACTION_UP");
                //复位图片
                PointF p1=getLeftPointF();
                PointF p2=getRightPointF();

                //左边界复位
                if(p1.x>0) {
                    matrix.postTranslate(0 - p1.x, 0);
                    //上边界复位
                    if (p1.y > 0) {
                        matrix.postTranslate(0, 0 - p1.y);
                    }
                    else if(p2.y<height){
                        matrix.postTranslate(0, height - p2.y);
                    }
                }
                else if(p1.y>0) {
                    matrix.postTranslate(0, 0 - p1.y);
                    if(p2.x<width){
                        matrix.postTranslate(width - p2.x, 0);
                    }
                }
                else if (p2.x < width) {
                    matrix.postTranslate(width - p2.x, 0);
                    if (p2.y < height)
                        matrix.postTranslate(0, height - p2.y);
                }
                else if (p2.y < height) {
                    matrix.postTranslate(0, height - p2.y);
                }

                break;
            case MotionEvent.ACTION_POINTER_UP: // 手指放开事件
                //Log.d("Deng", "ACTION_POINTER_UP");
                mode = NONE;
                break;
        }

        mImageView.setImageMatrix(matrix);


        return true;
    }

    //获取图片的上坐标
    private PointF getLeftPointF()
    {
        float[] values = new float[9];
        matrix.getValues(values);
        float leftX=values[2];
        float leftY=values[5];
        Log.d("Deng", "P1坐标：x=" + leftX+  ",y=" +leftY);
        return new PointF(leftX,leftY);
    }
    //获取图片的下坐标
    private PointF getRightPointF()
    {
        Rect rectTemp = mImageView.getDrawable().getBounds();
        float[] values = new float[9];
        matrix.getValues(values);
        float leftX= values[2]+rectTemp.width()*values[0];
        float leftY=values[5]+rectTemp.height()*values[4];
        Log.d("Deng", "P2坐标：x="+ leftX+",y=" +leftY);
        return new PointF(leftX,leftY);
    }

    /**
     * 计算两个手指头之间的中心点的位置
     * x = (x1+x2)/2;
     * y = (y1+y2)/2;
     *
     * @param event 触摸事件
     * @return 返回中心点的坐标
     */
    private PointF midPoint(MotionEvent event) {
        float x = (event.getX(0) + event.getX(1)) / 2;
        float y = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(x, y);
    }

    /**
     * 计算两个手指间的距离
     *
     * @param event 触摸事件
     * @return 放回两个手指之间的距离
     */
    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);        //两点间距离公式
    }
}
