package com.deng.fastcounter;

import android.app.Application;
import android.graphics.Bitmap;

/**
 * Created by Dengyong on 2018-3-29.
 */

public class MyApplication extends Application {
    /*
    /*用于传递图片用于放大观察
     */
    private static Bitmap sBitmap;

    public static Bitmap getBitmap() {
        return sBitmap;
    }

    public static void setBitmap(Bitmap bitmap) {
        sBitmap = bitmap;
    }


}
