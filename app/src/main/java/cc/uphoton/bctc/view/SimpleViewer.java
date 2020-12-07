package cc.uphoton.bctc.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.uphoton.rlsense.librlsense.DataProcess;
import com.uphoton.rlsense.librlsense.RlStreamCapture;
import com.uphoton.rlsense.librlsense.RlTypes;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import cc.uphoton.bctc.activity.MainActivity;
import cc.uphoton.bctc.activity.ThisApplictation;
import cc.uphoton.bctc.utils.BitmapUtils;
import cc.uphoton.bctc.utils.Const;
import cc.uphoton.bctc.utils.MyUtils;

import static cc.uphoton.bctc.activity.MainActivity.getContext;
import static cc.uphoton.bctc.activity.MainActivity.mCaptureStateChangeListener;
import static cc.uphoton.bctc.activity.MainActivity.mTakePicListener;
import static cc.uphoton.bctc.activity.MainActivity.sp;
import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_DEPTH;
import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_IR;
import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_RGB;
import static cc.uphoton.bctc.utils.BitmapUtils.savePic;


public class SimpleViewer extends Thread {

    private boolean mShouldRun = false;

    private int mStreamType;
    private GLPanel mColorGLPanel;
    private GLPanel mDepthGLPanel;
    private GLPanel mIRGLPanel;
    private DecodePanel mDecodePanel;
    private RlStreamCapture mCapture;
    private RlTypes.RlImageFrameMode mCurrentMode;

    public SimpleViewer(RlStreamCapture capture, int streamType) {
        mCapture = capture;
        mStreamType = streamType;
    }

    public void setColorGLPanel(GLPanel GLPanel) {
        this.mColorGLPanel = GLPanel;
    }

    public void setDepthGLPanel(GLPanel GLPanel) {
        this.mDepthGLPanel = GLPanel;
    }

    public void setIRGLPanel(GLPanel GLPanel) {
        this.mIRGLPanel = GLPanel;
    }

    public void setDecodePanel(DecodePanel decodePanel) {
        this.mDecodePanel = decodePanel;
    }

    @Override
    public void run() {
        super.run();

        //open stream.
        mCapture.start(mStreamType);
        //get current framemode.
        mCurrentMode = mCapture.getFrameMode(mStreamType);
        //start read frame.
        while (mShouldRun) {
            if (mStreamType == RlTypes.RlStreamType.RL_STREAM_COLOR.toNative()) {
                RlTypes.RlImageFrame colorFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_COLOR, 50);
                if (colorFrame == null) {
                    continue;
                }
                drawColor(colorFrame);
                colorFrame.release();
            } else if (mStreamType == RlTypes.RlStreamType.RL_STREAM_DEPTH_FLOOD_IR_MIX.toNative()) {
                RlTypes.RlImageFrame depthFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_DEPTH, 40);
                if (depthFrame != null) {
                    drawDepth(depthFrame);
                    depthFrame.release();
                }

                RlTypes.RlImageFrame irFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_IR, 40);

                if (irFrame != null) {
                    drawIr(irFrame);
                    irFrame.release();
                }
            } else if (mStreamType == RlTypes.RlStreamType.RL_STREAM_DEPTH.toNative()) {
                RlTypes.RlImageFrame depthFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_DEPTH, 40);
//                Log.e("lzh", "depth======" + depthFrame);
                if (depthFrame != null) {
                    drawDepth(depthFrame);
                    depthFrame.release();
                }
            } else if (mStreamType == 3001) {
                // dot ir
                if (ir) {
                    RlTypes.RlImageFrame irFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_IR, 40);
                    if (irFrame != null) {
                        drawIr(irFrame);

                        irFrame.release();
                    }
                } else {
                    RlTypes.RlImageFrame depthFrame = mCapture.pollFrame(RlTypes.RlFrameType.RL_FRAME_DEPTH, 40);
                    if (depthFrame != null) {
                        drawDepth(depthFrame);

                        depthFrame.release();
                    }
                }


            } else {

            }

        }
    }

    private Handler mHandle = new Handler();
    boolean ir = true;

    public void changeStreamType() {
        if (ir) {
            mCapture.stop(mStreamType);
            mHandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCapture.start(RlTypes.RlStreamType.RL_STREAM_DEPTH.toNative());
                    ir = false;
                    sp.edit().putString(Const.PREVIEW, Const.Depth_PREVIEW).commit();
                }
            }, 200);
//            Log.e("lzh", "要去preview Depth");

        } else {
            mCapture.stop(RlTypes.RlStreamType.RL_STREAM_DEPTH.toNative());
            mHandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ir = true;
                    sp.edit().putString(Const.PREVIEW, Const.IR_PREVIEW).commit();
                    mCapture.start(mStreamType);
                }
            }, 200);
//            Log.e("lzh", "要去preview IR");
        }
    }

    private void drawIr(RlTypes.RlImageFrame nextFrame) {
        ByteBuffer frameData = nextFrame.getData();
        int width = nextFrame.getWidth();
        int height = nextFrame.getHeight();
        frameData = DataProcess.ir2RGB888(frameData, width, height);
        if (sp.getBoolean(Const.TAKE_PIC_IR, false)) {
//            Log.e("lzh", "drawIr===" + frameData);

            if (frameData != null) {
                byte[] bytes = null;
                int len = frameData.limit() - frameData.position();
                if (len > 0) {
                    bytes = new byte[len];
                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = frameData.get();
                    }
                }
                frameData.flip();
                ThisApplictation.setData(CODE_BITMAP_IR, bytes);
                sp.edit().putBoolean(Const.TAKE_PIC_IR, false).commit();
                if (sp.getBoolean(Const.IS_ONLY_TAKE_PIC_IR, false)) {
                    sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_IR, false).commit();
                   if(mTakePicListener!=null){
                       mTakePicListener.setOnTakePicFinish(Const.CaptureType.IR);
                   }
                } else {
//                    Log.e("lzh", "IR 取图完成了===" + sp.getString(Const.TIME_STAMP, Const.TIME_Default));
                    if(mCaptureStateChangeListener!=null){
                        mCaptureStateChangeListener.setOnCaptureFinished(Const.CaptureType.IR);
                    }
                }

            } else {
                Toast.makeText(MainActivity.getContext(), "IR 数据为空", Toast.LENGTH_SHORT).show();
            }
        }
        //存储IR图片

      /*  if (mIRGLPanel != null) {
            mIRGLPanel.paint(null, frameData, width, height);
        }*/
    }

    private void drawDepth(RlTypes.RlImageFrame nextFrame) {
//        Log.e("lzh","drawDepth");
        ByteBuffer frameData = nextFrame.getData();
        int width = nextFrame.getWidth();
        int height = nextFrame.getHeight();
        frameData = DataProcess.depth2RGB888(frameData, width, height);
        if (sp.getBoolean(Const.TAKE_PIC_DEPTH, false)) {
//            Log.e("lzh", "取深度图了" + frameData);
//            Log.e("lzh", "drawDepth===" + frameData);
            if (frameData != null) {
                byte[] bytes = null;
                int len = frameData.limit() - frameData.position();
                if (len > 0) {
                    bytes = new byte[len];
                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = frameData.get();
                    }
                }
                frameData.flip();
                ThisApplictation.setData(CODE_BITMAP_DEPTH, bytes);
                sp.edit().putBoolean(Const.TAKE_PIC_DEPTH, false).commit();
                if(mCaptureStateChangeListener!=null){
                    mCaptureStateChangeListener.setOnCaptureFinished(Const.CaptureType.DEPTH);
                }
//                Intent intent = new Intent();
//                intent.setAction(Const.COLLECT_Depth_FINISH_ACTION);
//                getContext().sendBroadcast(intent);
//                }
            } else {
                Toast.makeText(MainActivity.getContext(), "Depth 数据为空", Toast.LENGTH_SHORT).show();
            }
        }
      /*  if (mDepthGLPanel != null) {
            mDepthGLPanel.paint(null, frameData, width, height);
        }*/

    }

    private void drawColor(RlTypes.RlImageFrame nextFrame) {
        ByteBuffer frameData = nextFrame.getData();
        int width = nextFrame.getWidth();
        int height = nextFrame.getHeight();
        if (sp.getBoolean(Const.TAKE_PIC_RGB, false)) {
            sp.edit().putBoolean(Const.TAKE_PIC_RGB, false).commit();
            if (frameData != null) {
                byte[] bytes = null;
                int len = frameData.limit() - frameData.position();
                if (len > 0) {
                    bytes = new byte[len];
                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = frameData.get();
                    }
                }
                frameData.flip();
                ThisApplictation.setData(CODE_BITMAP_RGB, bytes);
                if (sp.getBoolean(Const.IS_ONLY_TAKE_PIC_RGB, false)) {
                    if(mTakePicListener!=null){
                        mTakePicListener.setOnTakePicFinish(Const.CaptureType.RGB);
                    }

                    sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_RGB, false).commit();
                } else {
                    if(mCaptureStateChangeListener!=null){
                        mCaptureStateChangeListener.setOnCaptureFinished(Const.CaptureType.RGB);
                    }
//                    Intent intent = new Intent();
//                    intent.setAction(Const.COLLECT_RGB_FINISH_ACTION);
//                    getContext().sendBroadcast(intent);
                }
            } else {
                Toast.makeText(MainActivity.getContext(), "RGB 数据为空", Toast.LENGTH_SHORT).show();
            }
        }
        //draw color image.
        if (mColorGLPanel != null) {
            mColorGLPanel.paint(null, frameData, width, height);
        }
    }


    public static int BITMAP_TYPE_RGB = 0;
    public static int BITMAP_TYPE_IR = 1;
    public static int BITMAP_TYPE_DEPTH = 2;


    public void onPause() {
        if (mColorGLPanel != null) {
            mColorGLPanel.onPause();
        }

        if (mDepthGLPanel != null) {
            mDepthGLPanel.onPause();
        }

        if (mIRGLPanel != null) {
            mIRGLPanel.onPause();
        }
    }

    public void onResume() {
        if (mColorGLPanel != null) {
            mColorGLPanel.onResume();
        }

        if (mDepthGLPanel != null) {
            mDepthGLPanel.onResume();
        }

        if (mIRGLPanel != null) {
            mIRGLPanel.onResume();
        }
    }

    public void onStart() {
        if (!mShouldRun) {
            mShouldRun = true;

            //start read thread
            this.start();
        }
    }

    public void onDestroy() {
        mShouldRun = false;
        //destroy stream.
        mCapture.stop(mStreamType);
        mCapture.stop(RlTypes.RlStreamType.RL_STREAM_DEPTH.toNative());
        mCapture.stop(RlTypes.RlStreamType.RL_STREAM_COLOR.toNative());
    }
}
