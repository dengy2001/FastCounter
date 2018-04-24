package com.deng.fastcounter;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.dnn.Importer;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView mImageView;
    private TextView mTextView;
    private ImageButton ibtnTakephoto;
    private ImageButton ibtnSelectpic;
    private ImageButton ibtnCount;
    private ImageButton ibtnCroppic;

    private SeekBar mSeekBarCircle;
    private SeekBar mSeekBarThreshold;

    private static Uri imageUri;
    private File currentImageFile = null;
    private String mFilePath;

    private final static int CHOOSE_PHOTO = 1;
    private final static int TAKE_PHOTO = 2;

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Bitmap mBitmapResult;       //加了统计圆的图

    private boolean isChooseing = false;

    private MarkPathView mMarkPathView;

    //进行图像识别时的一些参数设定
    private int iRadius = 0;    //标准半径，在填充时稍微收缩显示
    private int iThreshold = 0;
    private TextView mTextViewRadius;
    private TextView mTextViewThreshold;

    private int MY_PERMISSIONS_REQUEST_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mTextView = (TextView) findViewById(R.id.tvCountresult);
        ibtnSelectpic = (ImageButton) findViewById(R.id.imageButtonSelectPic);
        ibtnTakephoto = (ImageButton) findViewById(R.id.imageButtonTakePhoto);
        ibtnCount = (ImageButton) findViewById(R.id.imageButtonCount);
        ibtnCroppic = (ImageButton) findViewById(R.id.imageButtonCropPic);
        mMarkPathView = (MarkPathView) findViewById(R.id.pathView);

        mSeekBarCircle = (SeekBar) findViewById(R.id.seekBarCircleDiameter);
        mSeekBarThreshold = (SeekBar) findViewById(R.id.seekBarThreshold);
        mTextViewRadius = (TextView) findViewById(R.id.textViewCircle);
        mTextViewThreshold = (TextView) findViewById(R.id.textViewThreshold);

        ConstraintLayout.LayoutParams params1;
        params1 = (ConstraintLayout.LayoutParams)mImageView.getLayoutParams();
        mMarkPathView.setLayoutParams(params1);         //将设置path的View完全覆盖在ImageView上面

        mMarkPathView.getBackground().setAlpha(30);
        mMarkPathView.setVisibility(View.INVISIBLE);    //最初不显示出来

        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();

        //调节圆的直径
        mSeekBarCircle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Bitmap tbitmap1;
            Canvas canvas;
            int centerX;
            int centerY;
            int iwidth;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                Paint paint = new Paint();
                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(1);
                //paint.setStyle(Paint.Style.STROKE);
                canvas.drawBitmap(mBitmap, 0, 0, null);
                paint.setColor(Color.GREEN);
                paint.setAlpha(0xA0);
                iRadius = i / 2;
                canvas.drawCircle(centerX, centerY, iRadius, paint);

//                Log.d("Deng", "调节识别圆的直径=" + i / 2);
                //显示出来
                mTextViewRadius.setText("圆 半 径：" + iRadius);
                mImageView.setImageBitmap(tbitmap1);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                tbitmap1 = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                canvas = new Canvas();
                canvas.setBitmap(tbitmap1);
                seekBar.setMax(tbitmap1.getWidth()/5);
                centerX = tbitmap1.getWidth() / 2;
                centerY = tbitmap1.getHeight() / 2;
                iwidth = tbitmap1.getWidth() / 100;
                Log.d("Deng", "开始调节！。。。");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Paint paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                canvas.drawPaint(paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawBitmap(mBitmap, 0, 0, null);    //恢复初始的状态
                Log.d("Deng", "结束调节！。。。");
                Log.d("Deng", "识别圆的半径=" + iRadius);

                //显示出来
                mImageView.setImageBitmap(mBitmap);

                if (!tbitmap1.isRecycled()) {
                    tbitmap1.recycle();
                    Log.d("Deng", "tbitmap1可回收。。。");
                    System.gc();
                }
            }
        });

        //二值化阈值调整
        mSeekBarThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Bitmap tbitmap1;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                iThreshold = i;
                tbitmap1 = RGB2Mono(mBitmap, i);
                mTextViewThreshold.setText("阈 值：" + iThreshold);
                mImageView.setImageBitmap(tbitmap1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                tbitmap1 = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                seekBar.setMax(255);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mImageView.setImageBitmap(mBitmap);
                Log.d("Deng", "二值化阈值=" + iThreshold);
                if (!tbitmap1.isRecycled()) {
                    tbitmap1.recycle();
                    Log.d("Deng", "tbitmap1可回收。。。");
                    System.gc();
                }
            }
        });

        ibtnSelectpic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开相册选照片
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_PHOTO);
            }
        });

        ibtnTakephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //用相机获取图片方法1
                Log.d("Deng","选择拍照获取图片");

                // 获取SD卡路径
                mFilePath = Environment.getExternalStorageDirectory().getPath();
//                mFilePath = MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
                // 保存图片的文件名
                mFilePath = mFilePath + "/" + "tempphoto.jpg";

                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                    requestPermissions(new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                    Log.d("Deng", "调用版本7的拍照！");
                    takePhotoBiggerThan7(new File(mFilePath),(new File(mFilePath)).getAbsolutePath());
                }
                else {
                    // 指定拍照意图
                    Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // 加载路径图片路径
                    Uri mUri = Uri.fromFile(new File(mFilePath));
                    // 指定存储路径，这样就可以保存原图了
                    openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
                    startActivityForResult(openCameraIntent, TAKE_PHOTO);
                }
            }
        });

        ibtnCroppic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //剪裁选区
                isChooseing = !isChooseing;
                if(isChooseing){
                    //隐藏记录path的控件
                    mMarkPathView.setVisibility(View.VISIBLE);
                    ibtnCroppic.setImageResource(R.drawable.croppicok);
                    Log.d("Deng","VISIBLE");
                }
                else{
                    //判断是否有裁剪区域，有则将图形裁剪，并重新显示在ImagView控件中，并清除裁剪区域。
                    if (mMarkPathView.getIsSelected()) {
                        //有选择
                        Log.d("Deng", "选");
                        Matrix m = new Matrix();
                        float [] mValues = new float[9];
                        Matrix m1 = new Matrix();
                        float [] mValues1 = new float[9];


                        int mCenterX,mCenterY;
                        //mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                        Bitmap tbitmap1 = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                        SelectedRectArea sRect = mMarkPathView.getSelectedArea();

                        m = mImageView.getImageMatrix();
                        m.getValues(mValues);
                        float mScale = mValues[0];

                        Canvas canvas = new Canvas();
                        canvas.setBitmap(tbitmap1);
                        Paint mPaint = new Paint();
                        mPaint.setColor(Color.RED);
                        mPaint.setStrokeWidth(1);
                        mCenterX = (int) (sRect.mCenterX / mScale);
                        mCenterY = (int) (sRect.mCenterY / mScale);
                        int mtopleftX = (int) (sRect.mTopLeftX / mScale);
                        int mtopleftY = (int) (sRect.mTopLeftY / mScale);
                        int mViewLeft = (int) (mValues[2] / mScale);        //ImageView显示图片的左边距
                        int mViewTop = (int) (mValues[5] / mScale);         //ImageView显示图片的上边距
                        int newWidth = (int) (sRect.mA / mScale);
                        int newHeight = (int) (sRect.mB / mScale);

                        Log.d("Deng", "mScale=" + mScale);
                        Log.d("Deng", "mCenterX=" + mCenterX);
                        Log.d("Deng", "mCenterY=" + mCenterY);
                        Log.d("Deng", "mViewLeft=" + mViewLeft);
                        Log.d("Deng", "mViewTop=" + mViewTop);


                        //判断是否所有的点都在选择区域中
                        int w = mBitmap.getWidth();
                        int h = mBitmap.getHeight();
                        if (sRect.mTopLeftX/mScale < mViewLeft || sRect.mTopLeftY/mScale < mViewTop ||
                                sRect.mTopRightX/mScale < mViewLeft || sRect.mTopRightY/mScale <mViewTop ||
                                sRect.mBottomRightX/mScale < mViewLeft || sRect.mBottomRightY/mScale < mViewTop ||
                                sRect.mBottomLeftX/mScale <mViewLeft ||sRect.mBottomLeftY/mScale < mViewTop) {
                            //选区超出界限
                            isChooseing = !isChooseing; //恢复状态
                            Toast.makeText(MainActivity.this, "裁剪区域超出范围，请纠正！",Toast.LENGTH_SHORT).show();
                            return;

                        }
                        if (sRect.mTopLeftX/mScale > w + mViewLeft || sRect.mTopLeftY/mScale > h + mViewTop ||
                                sRect.mTopRightX/mScale > w + mViewLeft || sRect.mTopRightY/mScale > h + mViewTop||
                                sRect.mBottomRightX/mScale  > w + mViewLeft|| sRect.mBottomRightY/mScale > h + mViewTop ||
                                sRect.mBottomLeftX/mScale  > w + mViewLeft ||sRect.mBottomLeftY/mScale > h + mViewTop) {
                            //选区超出界限
                            isChooseing = !isChooseing; //恢复状态
                            Toast.makeText(MainActivity.this, "裁剪区域超出范围，请纠正！",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        canvas.rotate(sRect.mAngles,mtopleftX - mViewLeft, mtopleftY - mViewTop);   //旋转画布

                        canvas.drawBitmap(mBitmap,0,0,null);

//                        canvas.drawLine(0,72,144,72,mPaint);
//                        canvas.drawLine(72,0,72,144,mPaint);
//                        //canvas.drawLine(0, 0, mCenterX - mViewLeft, mCenterY - mViewTop, mPaint);
//                        canvas.drawLine(0, 0, mtopleftX - mViewLeft, mtopleftY - mViewTop, mPaint);
//
//                        mPaint.setColor(Color.GREEN);
//                        canvas.drawLine(0, 0, mtopleftX - mViewLeft, mtopleftY - mViewTop, mPaint);

                        mBitmap = Bitmap.createBitmap(tbitmap1,mtopleftX - mViewLeft, mtopleftY - mViewTop,newWidth, newHeight);
                        mImageView.setImageBitmap(mBitmap);
                        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                        if (!tbitmap1.isRecycled()) {
                            tbitmap1.recycle();
                            System.gc();
                        }
                    } else {
                        //没有选择
                        Log.d("Deng", "没有选");
                    }
                    //将记录path的控件显示出来
                    mMarkPathView.setVisibility(View.INVISIBLE);
                    ibtnCroppic.setImageResource(R.drawable.croppic);
                    Log.d("Deng","INVISIBLE");
                }
            }
        });

        //统计钢管的数目
        ibtnCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //识别图像
                if (iRadius == 0) {
                    Toast.makeText(MainActivity.this,"请设定钢管的直径以识别！",Toast.LENGTH_SHORT).show();
                    return;
                }

                ImageKnown();
                mImageView.setImageBitmap(mBitmapResult);
                Toast.makeText(MainActivity.this,"钢管数量统计完成！",Toast.LENGTH_SHORT).show();
            }
        });

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(!isChooseing) {
                    Intent it = new Intent();
                    it.setClass(MainActivity.this, ZoomActivity.class);
                    MyApplication myApplication = (MyApplication) MainActivity.this.getApplication();

                    Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    myApplication.setBitmap(bitmap);

                    startActivity(it);
                    return false;
                }
                else{
                    //创建矩形选择
                    int action = motionEvent.getAction();
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();

                    switch (action)
                    {
                        case MotionEvent.ACTION_DOWN:

                            break;
                        case MotionEvent.ACTION_MOVE:

                            break;
                    }

                    return true;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    //android版本大于7时调用系统相机获得清晰照片
    private void takePhotoBiggerThan7(File mfile,String absolutePath) {
        Log.d("Deng", "版本7的拍照函数开始执行！"+ mfile.getAbsolutePath());
        Uri mCameraTempUri;
        try {
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.DATA, absolutePath);
            MainActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//            mCameraTempUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            mCameraTempUri = FileProvider.getUriForFile(MainActivity.this, "com.deng.fastcounter.fileprovider", mfile);


            if (mCameraTempUri == null) {
                Log.d("Deng", "要保存的Uri没有获取到！");
            } else {
                Log.d("Deng", "要保存的Uri=" + mCameraTempUri.toString());
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (mCameraTempUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraTempUri);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            }
            startActivityForResult(intent, TAKE_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Deng","返回消息好的");
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode ==RESULT_CANCELED) {
                    Toast.makeText(MainActivity.this, "已经取消了从相册选择", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Uri imageUri = data.getData();
                    Log.e("Deng", "从相册选择图片，"+imageUri.toString());
                    mImageView.setImageURI(imageUri);
                    mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    //压缩图片
                    BitmapCompress();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case TAKE_PHOTO:
                Log.d("Deng","照相机返回。");
                FileInputStream is = null;
                try {
                    // 获取输入流
                    is = new FileInputStream(mFilePath);
                    // 把流解析成bitmap,此时就得到了清晰的原图
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    //接下来就可以展示了（或者做上传处理）
                    mImageView.setImageBitmap(bitmap);
                    mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                    //压缩图片
                    BitmapCompress();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    // 关闭流
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                break;

            default:
                break;
        }

    }

    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    Log.i("Deng", "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i("Deng", "加载失败");
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("Deng", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        } else {
            Log.d("Deng", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //识别图像
    private void ImageKnown()
    {
//        int picWidth = mImageView.getDrawable().getIntrinsicWidth();
//        int picHeight = mImageView.getDrawable().getIntrinsicHeight();


        //mBitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();

        SteelPipe tmpsteelPipe = CalcPipeNumber(mBitmap, iThreshold);
        mBitmapResult = Bitmap.createBitmap(tmpsteelPipe.targetBitmap);
        mTextView.setText("计数结果为："+tmpsteelPipe.iPipes+" 根。");

    }

    private Bitmap RGB2Mono(Bitmap bmSrc,int iThreshold) {
        System.gc();
        //rgb图转单色图
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat monoMat = new Mat();

        Bitmap monoGray = null;

        Utils.bitmapToMat(bmSrc, rgbMat);                               //原始RGB图片
        int picWidth = bmSrc.getWidth();

        monoGray = Bitmap.createBitmap(bmSrc.getWidth(), bmSrc.getHeight(), Bitmap.Config.RGB_565);  //中间结果图片
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);      //灰度化

        //Imgproc.blur(rgbMat,rgbMat,new Size(10,10));

        Imgproc.threshold(grayMat,monoMat,iThreshold,255,Imgproc.THRESH_BINARY);

        //膨胀
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        Imgproc.dilate(monoMat,monoMat,element);

        //腐蚀
//        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(9, 9));
//        Imgproc.erode(monoMat,monoMat,element);


        //边缘化
        Imgproc.Canny(monoMat, monoMat, iThreshold*3, 255);






        Utils.matToBitmap(monoMat,monoGray);

        rgbMat.release();
        grayMat.release();
        monoMat.release();


        return monoGray;
    }

    //检测与标记钢管
    private SteelPipe CalcPipeNumber(Bitmap bmSrc,int iThreshold) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat circles = new Mat();

        SteelPipe steelPipe = new SteelPipe();
        steelPipe.srcBitmap = bmSrc;
        Bitmap bmpMono = null;

        Utils.bitmapToMat(bmSrc, rgbMat);                               //原始RGB图片
        int picWidth = bmSrc.getWidth();

        bmpMono = Bitmap.createBitmap(bmSrc.getWidth(), bmSrc.getHeight(), Bitmap.Config.RGB_565);  //中间及结果图片
        bmpMono = RGB2Mono(bmSrc, iThreshold);       //变成灰度图，与预览设定一致
        Utils.bitmapToMat(bmpMono, grayMat);
        Imgproc.cvtColor(grayMat, grayMat, Imgproc.COLOR_BGR2GRAY);

        //HoughCircles参数：1、输入（8位灰度）；2、圆存储用；3、变换方式；4、寻找圆弧圆心的累计分辨率；5、能明显区分的两个不同圆之间的最小距离
        //6、用于Canny的边缘阀值上限，下限被置为上限的一半；7、累加器的阀值；8、最小圆半径；9、最大圆半径；
        Imgproc.HoughCircles(grayMat, circles, Imgproc.CV_HOUGH_GRADIENT, 1, iRadius, 100, 20,
                (int) (iRadius * 0.5), (int) (iRadius * 1.5));
        Mat houghCircles = new Mat();
        houghCircles.create(grayMat.rows(), grayMat.cols(), CvType.CV_8UC1);

        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
        Bitmap tbitmap1 = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas();
        canvas.setBitmap(tbitmap1);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(1);
        //paint.setStyle(Paint.Style.STROKE);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        paint.setColor(Color.GREEN);
        paint.setAlpha(0x50);

        //在图像上绘制圆形
        for (int i = 0; i < circles.cols(); i++) {
            double[] parameters = circles.get(0, i);
            double x, y;
            int r;
            x = parameters[0];
            y = parameters[1];
            r = (int) parameters[2];
            Point center = new Point(x, y);
            //在一副图像上绘制圆形
            //Imgproc.circle(rgbMat, center, iRadius, new Scalar(0,255, 0),-1);   //不透明
            canvas.drawCircle((float) x, (float) y, iRadius, paint);
        }
        steelPipe.iPipes = circles.cols();

        steelPipe.targetBitmap = tbitmap1;

        if (!bmpMono.isRecycled()) {
            bmpMono.recycle();
            System.gc();
        }

        rgbMat.release();
        grayMat.release();
        circles.release();
        System.gc();

        return steelPipe;
    }

    /*
    //图形灰度化
    private Bitmap bitmap2Gray(Bitmap bmSrc) {
        // 得到图片的长和宽
        int width = bmSrc.getWidth();
        int height = bmSrc.getHeight();
        // 创建目标灰度图像
        Bitmap bmpGray = null;
        bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        // 创建画布
        Canvas c = new Canvas(bmpGray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmSrc, 0, 0, paint);
        return bmpGray;
    }
*/
    /*
    //转换成二值图片
    public Bitmap RGB2BW(Bitmap bmSrc,int minV,int maxV) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat edgeMat = new Mat();
        // 创建目标灰度图像
        Bitmap bmpGray = null;
        Bitmap bmpBW = null;

        Utils.bitmapToMat(bmSrc, rgbMat);
        bmpGray = procSrc2Gray(bmSrc);
        Utils.bitmapToMat(bmpGray, grayMat);
        bmpBW = Bitmap.createBitmap(bmSrc.getWidth(), bmSrc.getHeight(), Bitmap.Config.ARGB_8888);
//        Imgproc.Canny(grayMat, grayMat,10,90);
        Imgproc.threshold(grayMat, edgeMat, minV, maxV, Imgproc.THRESH_BINARY);

        Utils.matToBitmap(edgeMat,bmpBW);

        return  bmpBW;
    }
    */
/*
    //转换成灰度图片
    public Bitmap procSrc2Gray(Bitmap bmSrc){
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat edgeMat = new Mat();
        // 创建目标灰度图像
        Bitmap bmpGray = null;

        Utils.bitmapToMat(bmSrc, rgbMat);                               //convert original bitmap to Mat, R G B.
        bmpGray = Bitmap.createBitmap(bmSrc.getWidth(), bmSrc.getHeight(), Bitmap.Config.RGB_565);
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);      //rgbMat to gray grayMat

        Utils.matToBitmap(grayMat,bmpGray); //convert mat to bitmap

        return  bmpGray;
    }
*/
    /*封装钢管类
     */
    class SteelPipe {
        Bitmap srcBitmap;   //原始图
        Bitmap targetBitmap;    //目标图
        int iPipes;             //识别出来的钢管数
    }

    //Bitmap尺寸压缩
    private void BitmapCompress() {
        //本函数直接将mBitmap进行压缩
        if (mBitmap != null) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
            int maxWH = Math.max(w, h);
            float fRatio = (float) 2000 / maxWH;
            if (fRatio < 1) {
                Matrix m = new Matrix();
                m.setScale(fRatio, fRatio);

                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, w, h, m, true);
                Log.d("Deng", "图片压缩过了，Width=" + mBitmap.getWidth() + ",Height=" + mBitmap.getHeight());
                mImageView.setImageBitmap(mBitmap);
            }
        }

    }

}
