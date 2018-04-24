package com.deng.fastcounter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Dengyong on 2018-4-9.
 */

public class MarkPathView extends View {

    private boolean isSelected = false;     //已经建立了选区
    private boolean isStart = false;        //第一次是否按下

    private SelectArea mSelectArea = new SelectArea();

    private float iconScale;                //选区的3个图标缩放比例
    int width;                              //屏幕宽度
    int height;                             //屏幕高度

    private static final int NONE = 0;
    private static final int DRAG = 1;      //移动
    private static final int ROTATE = 2;    //旋转
    private static final int ZOOM = 3;    //缩放
    private int mode = NONE;

    private float downX,downY,upX,upY;
    private float []values = new float[9];      //用于坐标变换计算
    private PointF []tmpRect = new PointF[4];   //临时用于获取Rect的点

    private float tempAngle = 0;        //临时变换角度
    private float beginAngle = 0;       //旋转开始前的角度
    private PointF beginRotatePoint = new PointF();     //旋转起始点
    private PointF []beginRect = new PointF[4];   //用于获取Rect的移动的起点

    public MarkPathView(Context context){
        super(context);

        initSelect(context);
    }

    public MarkPathView(Context context, AttributeSet attributeSet){
        super(context,attributeSet);

        initSelect(context);
    }

    private void initSelect(Context context) {
        //画笔1，内框
        mSelectArea.paintLine1 = new Paint();
        mSelectArea.paintLine1.setAntiAlias(true);
        mSelectArea.paintLine1.setStrokeWidth(5);
        mSelectArea.paintLine1.setColor(Color.BLUE);
        mSelectArea.paintLine1.setStyle(Paint.Style.STROKE);

        //画笔2，外框
        mSelectArea.paintLine2 = new Paint();
        mSelectArea.paintLine2.setAntiAlias(true);
        mSelectArea.paintLine2.setStrokeWidth(1);
        mSelectArea.paintLine2.setColor(Color.BLACK);
        mSelectArea.paintLine2.setStyle(Paint.Style.STROKE);

        //外框上的3个图标
        mSelectArea.mBitmapClose = BitmapFactory.decodeResource(this.getContext().getResources(),R.drawable.selectclose);
        mSelectArea.mBitmapRotate = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.selectrotate);
        mSelectArea.mBitmapScale = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.selectscale);
//        Log.d("Deng", "Width=" + mSelectArea.mBitmapClose.getWidth());
//        Log.d("Deng", "Width=" + mSelectArea.mBitmapClose.getHeight());


        //把选择框边的图片大小设定为总屏宽的 1/20
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        width = outMetrics.widthPixels;
        height = outMetrics.heightPixels;
        mSelectArea.iconDiameter = (float) width / 20 ;  //图标直径
        Matrix matrix = new Matrix();
        iconScale = mSelectArea.iconDiameter / mSelectArea.mBitmapClose.getWidth();
        matrix.postScale(iconScale, iconScale);
        mSelectArea.mBitmapClose = Bitmap.createBitmap(mSelectArea.mBitmapClose, 0, 0, mSelectArea.mBitmapClose.getWidth(),
                mSelectArea.mBitmapClose.getHeight(), matrix, true);
        mSelectArea.mBitmapRotate = Bitmap.createBitmap(mSelectArea.mBitmapRotate, 0, 0, mSelectArea.mBitmapRotate.getWidth(),
                mSelectArea.mBitmapRotate.getHeight(), matrix, true);
        mSelectArea.mBitmapScale = Bitmap.createBitmap(mSelectArea.mBitmapScale, 0, 0, mSelectArea.mBitmapScale.getWidth(),
                mSelectArea.mBitmapScale.getHeight(), matrix, true);

        //框线间距
        mSelectArea.disLines = 20;
        mSelectArea.degree = 0;     //初始0°
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(isStart) {

            //画线
            canvas.drawLine(mSelectArea.inTopLeft.x, mSelectArea.inTopLeft.y, mSelectArea.inTopRight.x,
                    mSelectArea.inTopRight.y, mSelectArea.paintLine1);
            canvas.drawLine(mSelectArea.inTopRight.x, mSelectArea.inTopRight.y, mSelectArea.inBottomRight.x,
                    mSelectArea.inBottomRight.y, mSelectArea.paintLine1);
            canvas.drawLine(mSelectArea.inBottomRight.x, mSelectArea.inBottomRight.y, mSelectArea.inBottomLeft.x,
                    mSelectArea.inBottomLeft.y, mSelectArea.paintLine1);
            canvas.drawLine(mSelectArea.inBottomLeft.x, mSelectArea.inBottomLeft.y, mSelectArea.inTopLeft.x,
                    mSelectArea.inTopLeft.y, mSelectArea.paintLine1);

            canvas.drawLine(mSelectArea.outTopLeft.x, mSelectArea.outTopLeft.y, mSelectArea.outTopRight.x,
                    mSelectArea.outTopRight.y, mSelectArea.paintLine2);
            canvas.drawLine(mSelectArea.outTopRight.x, mSelectArea.outTopRight.y, mSelectArea.outBottomRight.x,
                    mSelectArea.outBottomRight.y, mSelectArea.paintLine2);
            canvas.drawLine(mSelectArea.outBottomRight.x, mSelectArea.outBottomRight.y, mSelectArea.outBottomLeft.x,
                    mSelectArea.outBottomLeft.y, mSelectArea.paintLine2);
            canvas.drawLine(mSelectArea.outBottomLeft.x, mSelectArea.outBottomLeft.y, mSelectArea.outTopLeft.x,
                    mSelectArea.outTopLeft.y, mSelectArea.paintLine2);

//            Log.d("Deng", "inTopLeft.x=" + mSelectArea.inTopLeft.x);
//            Log.d("Deng", "outTopLeft.x=" + mSelectArea.outTopLeft.x);
//            Log.d("Deng", "inTopLeft.y=" + mSelectArea.inTopLeft.y);
//            Log.d("Deng", "outTopLeft.y=" + mSelectArea.outTopLeft.y);
//
//            Log.d("Deng", "inTopRight.x=" + mSelectArea.inTopRight.x);
//            Log.d("Deng", "outTopRight.x=" + mSelectArea.outTopRight.x);
//            Log.d("Deng", "inTopRight.y=" + mSelectArea.inTopRight.y);
//            Log.d("Deng", "outTopRight.y=" + mSelectArea.outTopRight.y);

            canvas.drawBitmap(mSelectArea.mBitmapClose, mSelectArea.outTopLeft.x - mSelectArea.iconDiameter / 2,
                    mSelectArea.outTopLeft.y - mSelectArea.iconDiameter / 2, null);
            canvas.drawBitmap(mSelectArea.mBitmapRotate, mSelectArea.outTopRight.x - mSelectArea.iconDiameter / 2,
                    mSelectArea.outTopRight.y - mSelectArea.iconDiameter / 2, null);
            canvas.drawBitmap(mSelectArea.mBitmapScale, mSelectArea.outBottomRight.x - mSelectArea.iconDiameter / 2,
                    mSelectArea.outBottomRight.y - mSelectArea.iconDiameter / 2, null);

        }
    }

    //第一次创建选区矩形，内部
    private void fillFirstRect(float downX, float downY, float upX, float upY) {
        //首次，旋转角度为0
        mSelectArea.degree = 0;
        if (upX >= downX && upY >= downY) {
            //向右下滑动
            mSelectArea.inTopLeft = new PointF(downX, downY);
            mSelectArea.inTopRight = new PointF(upX, downY);
            mSelectArea.inBottomRight = new PointF(upX, upY);
            mSelectArea.inBottomLeft = new PointF(downX, upY);
        }
        if (upX >= downX && upY <= downY) {
            //右上滑动
            mSelectArea.inTopLeft = new PointF(downX, upY);
            mSelectArea.inTopRight = new PointF(upX, upY);
            mSelectArea.inBottomRight = new PointF(upX, downY);
            mSelectArea.inBottomLeft = new PointF(downX, downY);
        }

        if (upX <= downX && upY <= downY) {
            //左上滑动
            mSelectArea.inTopLeft = new PointF(upX, upY);
            mSelectArea.inTopRight = new PointF(downX, upY);
            mSelectArea.inBottomRight = new PointF(downX, downY);
            mSelectArea.inBottomLeft = new PointF(upX, downY);
        }

        if (upX <= downX && upY >= downY) {
            //左下滑动
            mSelectArea.inTopLeft = new PointF(upX, downY);
            mSelectArea.inTopRight = new PointF(downX, downY);
            mSelectArea.inBottomRight = new PointF(downX, upY);
            mSelectArea.inBottomLeft = new PointF(upX, upY);
        }
    }

    //填充矩形，先填充内部，然后填充外部
    private void fillRects(){

        float disA = (float) Math.sqrt((mSelectArea.inTopRight.x - mSelectArea.inTopLeft.x) * (mSelectArea.inTopRight.x - mSelectArea.inTopLeft.x)
                    + (mSelectArea.inTopRight.y - mSelectArea.inTopLeft.y) * (mSelectArea.inTopRight.y - mSelectArea.inTopLeft.y));
        //计算矩形的第二边
        float disB = (float) Math.sqrt((mSelectArea.inBottomLeft.x - mSelectArea.inTopLeft.x) * (mSelectArea.inBottomLeft.x - mSelectArea.inTopLeft.x)
                + (mSelectArea.inBottomLeft.y - mSelectArea.inTopLeft.y) * (mSelectArea.inBottomLeft.y - mSelectArea.inTopLeft.y));

        tmpRect = calcRect(mSelectArea.inTopLeft, disA, disB, mSelectArea.degree);
        mSelectArea.inTopLeft = tmpRect[0];
        mSelectArea.inTopRight = tmpRect[1];
        mSelectArea.inBottomRight = tmpRect[2];
        mSelectArea.inBottomLeft = tmpRect[3];

        //计算出外框第1点坐标
        Matrix matrix = new Matrix();
        matrix.postTranslate((float)( 1.414 * mSelectArea.disLines), 0);
        matrix.postRotate(-135 + mSelectArea.degree, 0, 0);
        matrix.getValues(values);
        mSelectArea.outTopLeft.x = mSelectArea.inTopLeft.x + values[2];
        mSelectArea.outTopLeft.y = mSelectArea.inTopLeft.y + values[5];
        tmpRect = calcRect(mSelectArea.outTopLeft, disA + 2 * mSelectArea.disLines,
                disB + 2 * mSelectArea.disLines, mSelectArea.degree);
        mSelectArea.outTopLeft = tmpRect[0];
        mSelectArea.outTopRight = tmpRect[1];
        mSelectArea.outBottomRight = tmpRect[2];
        mSelectArea.outBottomLeft = tmpRect[3];

    }
    //计算带角度的矩形的4个点坐标
    //参数：1、左上角点；2、A边；3、B边；4、角度。
    private PointF[] calcRect(PointF pTopLeft, float disA, float disB, float angle0) {
        PointF[] rtnPoint = new PointF[4];
        rtnPoint[0] = new PointF();
        rtnPoint[1] = new PointF();
        rtnPoint[2] = new PointF();
        rtnPoint[3] = new PointF();

        //第1个点
        rtnPoint[0] = pTopLeft;

        //计算框线点坐标，首先确定Topleft点的坐标，其他3点由此基点计算产生
        Matrix matrix = new Matrix();

        //第二个点
        matrix.reset();
        matrix.postTranslate(disA, 0);
        matrix.postRotate(angle0, 0, 0);
        matrix.getValues(values);
        rtnPoint[1].x = rtnPoint[0].x + values[2];
        rtnPoint[1].y = rtnPoint[0].y + values[5];

        //第四个点
        matrix.reset();
        matrix.postTranslate(0, disB);
        matrix.postRotate(angle0, 0, 0);
        matrix.getValues(values);
        rtnPoint[3].x = rtnPoint[0].x + values[2];
        rtnPoint[3].y = rtnPoint[0].y + values[5];

        //第三个点
        rtnPoint[2].x = rtnPoint[3].x + rtnPoint[1].x - rtnPoint[0].x;
        rtnPoint[2].y = rtnPoint[3].y + rtnPoint[1].y - rtnPoint[0].y;

        return rtnPoint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mode = NONE;  //重置状态
                if(!isStart) {
                    downX = event.getX();
                    downY = event.getY();
                    isStart = true;
                }
                if(isSelected){
                    //已经有选择区了
                    PointF sP = new PointF(event.getX(), event.getY());
                    if (isInCircle(sP, mSelectArea.outTopLeft, mSelectArea.iconDiameter / 2)) {
                        //点在关闭图标上了
                        isStart = false;
                        isSelected = false;
                    }
                    if (isInCircle(sP, mSelectArea.outTopRight, mSelectArea.iconDiameter / 2)) {
                        //点在旋转图标上了
                        mode = ROTATE;
                        beginAngle = mSelectArea.degree;
                        beginRotatePoint = mSelectArea.outTopRight;
                    }
                    if (isInCircle(sP, mSelectArea.outBottomRight, mSelectArea.iconDiameter / 2)) {
                        //点在缩放图标上了
                        mode = ZOOM;
                        downX = event.getX();
                        downY = event.getY();
                        Log.d("Deng", "缩放");
                    }
                    if (isInRect(sP) && mode == NONE) {
                        //点在框内部，移动
                        mode = DRAG;
                        downX = event.getX();
                        downY = event.getY();

                        beginRect[0] = new PointF(mSelectArea.inTopLeft.x,mSelectArea.inTopLeft.y);
                        beginRect[1] = new PointF(mSelectArea.inTopRight.x,mSelectArea.inTopRight.y);
                        beginRect[2] = new PointF(mSelectArea.inBottomRight.x,mSelectArea.inBottomRight.y);
                        beginRect[3] = new PointF(mSelectArea.inBottomLeft.x,mSelectArea.inBottomLeft.y);

                    }

                }

//                Log.d("Deng","down:isStart="+isStart+",isSelected="+isSelected);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if(isStart && (!isSelected)){
                    //第一次创建选区中
                    upX = event.getX();
                    upY = event.getY();
                    fillFirstRect(downX, downY, upX, upY);
                    fillRects();
                }
                if(mode == ROTATE){
                    //点在旋转上移动
                    PointF sP = new PointF(event.getX(), event.getY());
                    tempAngle = rotateDegree(mSelectArea.inTopLeft, beginRotatePoint, sP);
                    mSelectArea.degree = beginAngle + tempAngle;
//                    Log.d("Deng", "当前旋转角度为：" + mSelectArea.degree + "，初始角度为：" + beginAngle);

                    fillRects();
                }
                if (mode == ZOOM) {
                    //点在缩放上
                    upX = event.getX();
                    upY = event.getY();
                    float disAll = (float) (Math.sqrt((upX - mSelectArea.inTopLeft.x) * (upX - mSelectArea.inTopLeft.x) + (upY - mSelectArea.inTopLeft.y) * (upY - mSelectArea.inTopLeft.y)));
                    float disOld = (float) (Math.sqrt((mSelectArea.inBottomRight.x - mSelectArea.inTopLeft.x) * (mSelectArea.inBottomRight.x - mSelectArea.inTopLeft.x) +
                            (mSelectArea.inBottomRight.y - mSelectArea.inTopLeft.y) * (mSelectArea.inBottomRight.y - mSelectArea.inTopLeft.y)));
                    float disAOld = (float) (Math.sqrt((mSelectArea.inTopRight.x - mSelectArea.inTopLeft.x) * (mSelectArea.inTopRight.x - mSelectArea.inTopLeft.x) +
                            (mSelectArea.inTopRight.y - mSelectArea.inTopLeft.y) * (mSelectArea.inTopRight.y - mSelectArea.inTopLeft.y)));
                    float disBOld = (float) (Math.sqrt((mSelectArea.inBottomLeft.x - mSelectArea.inTopLeft.x) * (mSelectArea.inBottomLeft.x - mSelectArea.inTopLeft.x) +
                            (mSelectArea.inBottomLeft.y - mSelectArea.inTopLeft.y) * (mSelectArea.inBottomLeft.y - mSelectArea.inTopLeft.y)));

                    float zoomRate = disAll/ disOld;

                    tmpRect = calcRect(mSelectArea.inTopLeft, disAOld * zoomRate, disBOld * zoomRate, mSelectArea.degree);
                    mSelectArea.inTopLeft = tmpRect[0];
                    mSelectArea.inTopRight = tmpRect[1];
                    mSelectArea.inBottomRight = tmpRect[2];
                    mSelectArea.inBottomLeft = tmpRect[3];
                    fillRects();
//                    Log.d("Deng", "mSelectArea.inBottomLeft.y=" + mSelectArea.inBottomLeft.y);
                }
                if (mode == DRAG) {
                    //点在移动上
                    upX = event.getX();
                    upY = event.getY();
                    mSelectArea.inTopLeft.x = beginRect[0].x + upX - downX;
                    mSelectArea.inTopLeft.y = beginRect[0].y +upY - downY;
                    mSelectArea.inTopRight.x = beginRect[1].x +upX - downX;
                    mSelectArea.inTopRight.y = beginRect[1].y +upY - downY;
                    mSelectArea.inBottomRight.x = beginRect[2].x +upX - downX;
                    mSelectArea.inBottomRight.y = beginRect[2].y +upY - downY;
                    mSelectArea.inBottomLeft.x = beginRect[3].x +upX - downX;
                    mSelectArea.inBottomLeft.y = beginRect[3].y +upY - downY;
                    fillRects();
                    Log.d("Deng", "移动：upX=" + upX + ",upY=" + upY + ",downX=" + downX + ",downY=" + downY + "，开始移动坐标X=" + beginRect[0].x);
                }

//                Log.d("Deng","move:isStart="+isStart+",isSelected="+isSelected+",MODE="+mode);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(!isSelected && isStart) {
                    //第一次创建选区
                    upX = event.getX();
                    upY = event.getY();
                    isSelected = true;
                    fillFirstRect(downX, downY, upX, upY);
                }
                if(mode == ROTATE){
                    tempAngle = 0;
//                    mode = NONE;
                }
//                if (mode == ZOOM) {
//                    mode = NONE;
//                }
//                if (mode == DRAG) {
//                    mode = NONE;
//                }

                //Log.d("Deng","move:isStart="+isStart+",isSelected="+isSelected+",MODE="+mode);
                invalidate();
                break;
        }
        return true;
    }

    public boolean getIsSelected() {
        if (isSelected) {
            return true;
        }
        return false;
    }

    public SelectedRectArea getSelectedArea() {
        SelectedRectArea mS = new SelectedRectArea();
        mS.mTopLeftX = mSelectArea.inTopLeft.x;
        mS.mTopLeftY = mSelectArea.inTopLeft.y;
        mS.mTopRightX = mSelectArea.inTopRight.x;
        mS.mTopRightY = mSelectArea.inTopRight.y;
        mS.mBottomRightX = mSelectArea.inBottomRight.x;
        mS.mBottomRightY = mSelectArea.inBottomRight.y;
        mS.mBottomLeftX = mSelectArea.inBottomLeft.x;
        mS.mBottomLeftY = mSelectArea.inBottomLeft.y;

        mS.mAngles = - mSelectArea.degree;
        mS.mA = (float) Math.sqrt((mSelectArea.inTopRight.x-mSelectArea.inTopLeft.x)*(mSelectArea.inTopRight.x-mSelectArea.inTopLeft.x)+
                (mSelectArea.inTopRight.y-mSelectArea.inTopLeft.y)*(mSelectArea.inTopRight.y-mSelectArea.inTopLeft.y));
        mS.mB = (float) Math.sqrt((mSelectArea.inBottomLeft.x-mSelectArea.inTopLeft.x)*(mSelectArea.inBottomLeft.x-mSelectArea.inTopLeft.x)+
                (mSelectArea.inBottomLeft.y-mSelectArea.inTopLeft.y)*(mSelectArea.inBottomLeft.y-mSelectArea.inTopLeft.y));
        mS.mCenterX = (float) ((mSelectArea.inTopRight.x + mSelectArea.inTopLeft.x) / 2);
        mS.mCenterY = (float) ((mSelectArea.inTopRight.y + mSelectArea.inTopLeft.y) / 2);

//        mS.mTopLeftX = 19;
//        mS.mTopLeftY = 21;
//        mS.mAngles = 31;
//        mS.mA = 100;
//        mS.mB = 80;
        return mS;
    }

    /**
     * 封装一个矩形选择区域
     */
    class  SelectArea {
        Paint paintLine1;
        Paint paintLine2;
        PointF inTopLeft = new PointF(0,0);
        PointF inTopRight = new PointF(0,0);
        PointF inBottomLeft = new PointF(0,0);
        PointF inBottomRight = new PointF(0,0);

        PointF outTopLeft = new PointF(0, 0);
        PointF outTopRight = new PointF(0, 0);
        PointF outBottomLeft = new PointF(0, 0);
        PointF outBottomRight = new PointF(0, 0);

        float degree;                           //旋转角度
        float disLines;                         //框线间距
        float iconDiameter;                      //图标直径

        Bitmap mBitmapClose;
        Bitmap mBitmapScale;
        Bitmap mBitmapRotate;
    }

    //判断点是否在圆形的点击区内
    private boolean isInCircle(PointF onPoint,PointF circleCenter,float circleRadius){
        float x = onPoint.x - circleCenter.x;
        float y = onPoint.y - circleCenter.y;
        if ((float) Math.sqrt(x * x + y * y) < circleRadius) {
            return true;
        }
        return false;
    }

    //判断点是否在矩形内
    private boolean isInRect(PointF onPoint){
        boolean isOnleft = false;   //点在线哪一侧
//点在线哪一侧
// Tmp = (y1 – y2) * x + (x2 – x1) * y + x1 * y2 – x2 * y1
//        Tmp > 0 在左侧
//                Tmp = 0 在线上
//        Tmp < 0 在右侧

        //判断线1，topleft-topright
        float x = onPoint.x;
        float y = onPoint.y;
        float x1 = mSelectArea.inTopLeft.x;
        float y1 = mSelectArea.inTopLeft.y;
        float x2 = mSelectArea.inTopRight.x;
        float y2 = mSelectArea.inTopRight.y;
        float tmp = (y1 - y2) * x + (x2 - x1) * y + x1 * y2 - x2 * y1;
        if (tmp > 0) {
            isOnleft = true;
        } else {
            isOnleft = false;
        }

        //判断线2
        x1 = mSelectArea.inTopRight.x;
        y1 = mSelectArea.inTopRight.y;
        x2 = mSelectArea.inBottomRight.x;
        y2 = mSelectArea.inBottomRight.y;
        tmp = (y1 - y2) * x + (x2 - x1) * y + x1 * y2 - x2 * y1;
        if (tmp > 0) {
            if (!isOnleft) {
                return false;
            }
        } else {
            if (isOnleft) {
                return false;
            }
        }
        //判断线3
        x1 = mSelectArea.inBottomRight.x;
        y1 = mSelectArea.inBottomRight.y;
        x2 = mSelectArea.inBottomLeft.x;
        y2 = mSelectArea.inBottomLeft.y;
        tmp = (y1 - y2) * x + (x2 - x1) * y + x1 * y2 - x2 * y1;
        if (tmp > 0) {
            if (!isOnleft) {
                return false;
            }
        } else {
            if (isOnleft) {
                return false;
            }
        }
        //判断线4
        x1 = mSelectArea.inBottomLeft.x;
        y1 = mSelectArea.inBottomLeft.y;
        x2 = mSelectArea.inTopLeft.x;
        y2 = mSelectArea.inTopLeft.y;
        tmp = (y1 - y2) * x + (x2 - x1) * y + x1 * y2 - x2 * y1;
        if (tmp > 0) {
            if (!isOnleft) {
                return false;
            }
        } else {
            if (isOnleft) {
                return false;
            }
        }

        //在内部
        return true;
    }

    //计算旋转角度
    private float rotateDegree(PointF orP,PointF sP,PointF endP){
        double rd = 0;
        double dis_a = (double) Math.sqrt((sP.x - orP.x)*(sP.x - orP.x) + (sP.y-orP.y)*(sP.y-orP.y));
        double dis_b = (double) Math.sqrt((endP.x - orP.x)*(endP.x - orP.x) + (endP.y-orP.y)*(endP.y-orP.y));
        double dis_c = (double) Math.sqrt((endP.x - sP.x)*(endP.x - sP.x) + (endP.y-sP.y)*(endP.y-sP.y));
        //c边对应的弧度
        rd = (double) Math.acos((dis_a * dis_a + dis_b * dis_b - dis_c * dis_c) / (2 * dis_a * dis_b));
//        Log.d("Deng", "角度=" +(rd * 180)/Math.PI );
//        Log.d("Deng", "旋转点:x=" + orP.x + ",y=" + orP.y + "；开始点：x=" + sP.x + ",y=" + sP.y + "结束点：x=" + endP.x + ",y=" + endP.y);

        float direction;
        direction = (sP.x - orP.x) * (endP.y - orP.y) - (sP.y - orP.y) * (endP.x - orP.x);
        if (direction < 0){
            //逆时针
            return (float) -((rd * 180)/Math.PI);
        }
        return (float) ((rd * 180)/Math.PI);

    }

}

