package cc.uphoton.bctc.activity;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.YuvImage;

import java.nio.ByteBuffer;

import cc.uphoton.bctc.bean.LivenessResultBean;
import cc.uphoton.bctc.utils.CrashHandler;

public class ThisApplictation extends Application {
    private CrashHandler crashHandler;
    private static Bitmap mIRBitmap, mRGBBitmap, mFGBitmap;
    private static YuvImage mIRImage, mRGBImage, mFGImage;
    private static byte[] mIRData, mRGBData, mFGData;

    public boolean IsNew1 = false;
    public boolean IsNew2 = true;
    public static final int CODE_BITMAP_IR = 1;
    public static final int CODE_BITMAP_DEPTH  = 3;
    public static final int CODE_BITMAP_RGB = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        crashHandler = CrashHandler.getCrashHandler();
        crashHandler.init(this);
    }


    public static Bitmap getBitmap(int index) {
        switch (index) {
            case CODE_BITMAP_IR:
                return mIRBitmap;
            case CODE_BITMAP_RGB:
                return mRGBBitmap;
            case CODE_BITMAP_DEPTH:
                return mFGBitmap;
            default:
                return null;
        }
    }

    public static YuvImage getImage(int index) {
        switch (index) {
            case CODE_BITMAP_IR:
                return mIRImage;
            case CODE_BITMAP_RGB:
                return mRGBImage;
            case CODE_BITMAP_DEPTH:
                return mFGImage;
            default:
                return null;
        }
    }

    public static void setBitmap(int index, Bitmap bmp) {
        switch (index) {
            case CODE_BITMAP_IR:
                mIRBitmap = bmp;
                break;
            case CODE_BITMAP_RGB:
                mRGBBitmap = bmp;
                break;
            case CODE_BITMAP_DEPTH:
                mFGBitmap = bmp;
                break;
            default:
                return;
        }
    }

    public static void setImage(int index, YuvImage image) {
        switch (index) {
            case CODE_BITMAP_IR:
                mIRImage = image;
                break;
            case CODE_BITMAP_RGB:
                mRGBImage = image;
                break;
            case CODE_BITMAP_DEPTH:
                mFGImage = image;
                break;
            default:
                return;
        }
    }
    public static ByteBuffer mIRBuffer;
    public static ByteBuffer mRGBBuffer;
    public static ByteBuffer mFGBuffer;
    public static void setByteBuffer(int index, ByteBuffer buffer){
        switch (index) {
            case CODE_BITMAP_IR:
                mIRBuffer = buffer;
                break;
            case CODE_BITMAP_RGB:
                mRGBBuffer = buffer;
                break;
            case CODE_BITMAP_DEPTH:
                mFGBuffer = buffer;
                break;
            default:
                return;
        }
    }
    public static ByteBuffer getByteBuffer(int index) {
        switch (index) {
            case CODE_BITMAP_IR:
                return mIRBuffer;
            case CODE_BITMAP_RGB:
                return mRGBBuffer;
            case CODE_BITMAP_DEPTH:
                return mFGBuffer;
            default:
                return null;
        }
    }




    public static void setData(int index, byte[] data) {
        switch (index) {
            case CODE_BITMAP_IR:
                mIRData = data;
                break;
            case CODE_BITMAP_RGB:
                mRGBData = data;
                break;
            case CODE_BITMAP_DEPTH:
                mFGData = data;
                break;
            default:
                return;
        }
    }
    private static LivenessResultBean livenessResultBean=new LivenessResultBean();
    public static void setBean( LivenessResultBean bean){
        livenessResultBean=bean;
    }
    public static LivenessResultBean getBean(){
        return livenessResultBean;
    }

    public static byte[] getData(int index) {
        switch (index) {
            case CODE_BITMAP_IR:
                return mIRData;
            case CODE_BITMAP_RGB:
                return mRGBData;
            case CODE_BITMAP_DEPTH:
                return mFGData;
            default:
                return null;
        }
    }
}
