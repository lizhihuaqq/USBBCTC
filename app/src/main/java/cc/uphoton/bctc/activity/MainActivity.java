package cc.uphoton.bctc.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.uphoton.rlsense.librlsense.RlContext;
import com.uphoton.rlsense.librlsense.RlDevice;
import com.uphoton.rlsense.librlsense.RlStreamCapture;
import com.uphoton.rlsense.librlsense.RlTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import cc.uphoton.bctc.R;
import cc.uphoton.bctc.bean.LivenessResultBean;
import cc.uphoton.bctc.utils.BitmapUtils;
import cc.uphoton.bctc.utils.Const;
import cc.uphoton.bctc.utils.CrashHandler;
import cc.uphoton.bctc.utils.MyUtils;
import cc.uphoton.bctc.view.GLPanel;
import cc.uphoton.bctc.view.SimpleViewer;

import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_DEPTH;
import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_IR;
import static cc.uphoton.bctc.activity.ThisApplictation.CODE_BITMAP_RGB;
import static cc.uphoton.bctc.utils.BitmapUtils.savePic;
import static cc.uphoton.bctc.utils.BitmapUtils.savePicBytes;
import static cc.uphoton.bctc.utils.Const.SAVE_FILE_DEFAULT;
import static cc.uphoton.bctc.utils.Const.THRESHOLD_VALUE;
import static cc.uphoton.bctc.utils.Const.THRESHOLD_VALUE_DEFAULT;
import static cc.uphoton.bctc.view.SimpleViewer.BITMAP_TYPE_IR;
import static cc.uphoton.bctc.view.SimpleViewer.BITMAP_TYPE_RGB;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Button mCheckMultiBtn;
    private Button mCheckOnceBtn;

    private GLPanel mGLColorPanel;
    private GLPanel mGLDepthPanel;
    private GLPanel mGLIrPanel;
    private Button mTakePicButton;
    private Surface mSurface;

    private RlContext mRlContext = null;
    private RlDevice mRlDevice = null;
    private RlStreamCapture mCapture = null;

    private SimpleViewer mColorViewer;
    //private SimpleViewer mDepthViewer;
    private SimpleViewer mDepthIrViewer;

    private static final int DEVICE_OPEN_SUCCESS = 0;
    private static final int DEVICE_OPEN_FALIED = 1;
    private static final int DEVICE_DISCONNECT = 2;

    private static final int REQUEST_CAMERA_CODE = 0x007;

    private CheckBox mCheckBoxTrue;
    private CheckBox mCheckBoxFalse;
    private CheckBox mCheckBoxAll;
    private long startCheckTime, endCheckTime;

    private int expectDepthFrameWidth = 640;
    private int expectDepthFrameHeight = 480;

    private RlTypes.RlDeviceState deviceState = RlTypes.RlDeviceState.RL_DEVICE_STATE_DISCONNECT;

    private Handler MainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DEVICE_OPEN_FALIED:
                case DEVICE_DISCONNECT:
                    showMessageDialog();
                    break;
                case DEVICE_OPEN_SUCCESS:
                    runViewer();
                    break;
            }
        }
    };

    private void showMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("The device is not connected!!!");
        builder.setPositiveButton("quit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.dismiss();
                finish();
            }
        });
        builder.show();
    }

    private void runViewer() {
        mCapture = mRlDevice.getStreamCapture();
        mColorViewer = new SimpleViewer(mCapture, RlTypes.RlStreamType.RL_STREAM_COLOR.toNative());
        mGLColorPanel.setVisibility(View.VISIBLE);
        mColorViewer.setColorGLPanel(mGLColorPanel);
        mColorViewer.onStart();

//        mDepthIrViewer = new SimpleViewer(mCapture, RlTypes.RlStreamType.RL_STREAM_DEPTH_FLOOD_IR_MIX.toNative());
        mGLDepthPanel.setVisibility(View.GONE);
        mGLIrPanel.setVisibility(View.VISIBLE);
        mDepthIrViewer = new SimpleViewer(mCapture, 3001);
        mDepthIrViewer.setIRGLPanel(mGLIrPanel);
//        mDepthIrViewer.setDepthGLPanel(mGLDepthPanel);
        mDepthIrViewer.onStart();
    }

    public boolean isCameraPermission(Activity context, int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
                return false;
            }
        }
        return true;
    }


    //请求权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // not process now
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private static Context mContext;

    public static Context getContext() {

        return mContext;
    }


    private class OpenDeviceRunnable implements Runnable {

        @Override
        public void run() {
            mRlContext = new RlContext(MainActivity.this);
            ArrayList<RlDevice> devices = mRlContext.getDeviceList();
            if (devices.size() > 0) {
                mRlDevice = devices.get(0);
                mRlDevice.requestPermission(RlTypes.RlPermissionType.RL_PERMISSION_SENSOR_ALL, mRlSensorPermissionListener);

            }
        }
    }

    private RlDevice.RlSensorPermissionListener mRlSensorPermissionListener = new RlDevice.RlSensorPermissionListener() {
        @Override
        public void onRlSensorPermissionGranted(RlTypes.RlPermissionType rlPermissionType) {
            Log.d(TAG, "permissionGranted");
            if (rlPermissionType == RlTypes.RlPermissionType.RL_PERMISSION_SENSOR_ALL) {
                Log.d(TAG, "sensor all permissionGranted");
            } else if (rlPermissionType == RlTypes.RlPermissionType.RL_PERMISSION_SENSOR_3D) {
                Log.d(TAG, "sensor 3d  permissionGranted");
            } else if (rlPermissionType == RlTypes.RlPermissionType.RL_PERMISSION_SENSOR_COLOR) {
                Log.d(TAG, "sensor color permissionGranted");
            }
            int ret = mRlDevice.open();
            if (ret < 0) {
                MainHandler.sendEmptyMessage(DEVICE_OPEN_FALIED);
            } else {
                MainHandler.sendEmptyMessage(DEVICE_OPEN_SUCCESS);
            }
        }

        @Override
        public void onRlSensorPermissionDenied(RlTypes.RlPermissionType rlPermissionType) {

        }
    };

    public static SharedPreferences sp;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private CrashHandler mCrashHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCrashHandler = CrashHandler.getCrashHandler();
        mCrashHandler.init(this);
        setContentView(R.layout.activity_main_usb);
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);
        new initTask().execute();
        onNewIntent(getIntent());
//        renameTask();

    }
  /*  public void renameTask(){

        String parentPath="/storage/emulated/0/图像质量分析/";
        File file=new File(parentPath);
        String[] fileNameList=file.list();
        for(int i=0;i<fileNameList.length;i++) {
            Log.e("lzh", "fileNameList==" + fileNameList[i].substring(0,3));
           String srtString=parentPath+fileNameList[i];
           String desString=parentPath+fileNameList[i].substring(0,3)+".jpg";
           File srcFile=new File(srtString);
           srcFile.renameTo(new File(desString));
        }
    }*/

    //获取usb插拔广播
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
//            Log.e("lzh", "action==" + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {   // 插入
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {  // 拔出
                Toast.makeText(MainActivity.this, "USB拔出", Toast.LENGTH_SHORT).show();
                if (mDepthIrViewer != null) {
                    mDepthIrViewer.onPause();
                }

                if (mColorViewer != null) {
                    mColorViewer.onPause();
                }

                if (mDepthIrViewer != null) {
                    mDepthIrViewer.onDestroy();
                }

                //destroy color viewer.
                if (mColorViewer != null) {
                    mColorViewer.onDestroy();
                }
                if (mRlDevice != null) {
                    mRlDevice.close();
                }
                if (mRlContext != null) {
                    mRlContext.destroy();
                    mRlContext = null;
                }
            }
        }
    };

    private TextView mSumCountTextView;
    private TextView mTrueCountTextView;
    private TextView mFalseCountTextView;
    private TextView mMultiScoreTextView;
    private TextView mNoDetectCountTextView;
    private BroadcastReceiver mFinishDetectReceiver;
    private EditText mThresholdEditText;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mContext = this;
//        mFinishDetectReceiver = new FinishTakePicReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        // 2. 设置接收广播的类型
//        intentFilter.addAction(Const.COLLECT_IR_FINISH_ACTION);// 只有持有相同的action的接受者才能接收此广播
//        intentFilter.addAction(Const.COLLECT_Depth_FINISH_ACTION);// 只有持有相同的action的接受者才能接收此广播
//        intentFilter.addAction(Const.COLLECT_RGB_FINISH_ACTION);// 只有持有相同的action的接受者才能接收此广播
//        // 3. 动态注册：调用Context的registerReceiver（）方法
//        registerReceiver(mFinishDetectReceiver, intentFilter);

        sp = getSharedPreferences("sp", MODE_PRIVATE);
        KeyPointsPaint.setColor((Color.RED));
        KeyPointsPaint.setStyle(Paint.Style.FILL);
        KeyPointsPaint.setStrokeWidth(2);
        drawView = findViewById(R.id.points_view_rgb);
        drawView.setZOrderOnTop(true);
        drawView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mDrawSurfaceHolder = drawView.getHolder();
        mThresholdEditText = findViewById(R.id.et_threshold);
        mThresholdEditText.setText(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT));
        mThresholdEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                sp.edit().putString(THRESHOLD_VALUE, mThresholdEditText.getText().toString()).commit();
                return false;
            }
        });
        mNoDetectCountTextView = findViewById(R.id.tv_no_detect_count);
        mMultiScoreTextView = findViewById(R.id.tv_multi_score);
        mSumCountTextView = findViewById(R.id.tv_sum_count);
        mTrueCountTextView = findViewById(R.id.tv_true_count);
        mFalseCountTextView = findViewById(R.id.tv_false_count);
        mCheckMultiBtn = findViewById(R.id.btn_check_multi);
        mCheckOnceBtn = findViewById(R.id.btn_check_once);
        mCheckOnceBtn.setOnClickListener(this);
        mCheckMultiBtn.setOnClickListener(this);
        mCheckBoxTrue = findViewById(R.id.cb_true);
        mCheckBoxFalse = findViewById(R.id.cb_false);
        mCheckBoxAll = findViewById(R.id.cb_all);
        boolean is_save_all_checked = sp.getBoolean(Const.SAVE_ALL, true);
        boolean is_save_true_checked = sp.getBoolean(Const.SAVE_TRUE, true);
        boolean is_save_false_checked = sp.getBoolean(Const.SAVE_FALSE, true);
        mCheckBoxAll.setChecked(is_save_all_checked);
        mCheckBoxTrue.setChecked(is_save_true_checked);
        mCheckBoxFalse.setChecked(is_save_false_checked);
        mCheckBoxAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckBoxAll.setChecked(isChecked);
                sp.edit().putBoolean(Const.SAVE_ALL, isChecked).commit();
            }
        });
        mCheckBoxTrue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckBoxTrue.setChecked(isChecked);
                sp.edit().putBoolean(Const.SAVE_TRUE, isChecked).commit();
            }
        });
        mCheckBoxFalse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCheckBoxFalse.setChecked(isChecked);
                sp.edit().putBoolean(Const.SAVE_FALSE, isChecked).commit();
            }
        });
        mGLColorPanel = (GLPanel) findViewById(R.id.sv_color_view);
//        mGLColorPanel.setRotationY();
        mGLDepthPanel = (GLPanel) findViewById(R.id.sv_depth_view);
        mGLIrPanel = (GLPanel) findViewById(R.id.sv_ir_view);
        mTakePicButton = (Button) findViewById(R.id.btn_take_pic);
        mTakePicButton.setOnClickListener(this);
        new Thread(new OpenDeviceRunnable()).start();
        captureFinish();
        takePicFinish();
    }

    public void captureFinish() {
        setOnCaptureFinishedListener(new CaptureStateChangeListener() {
            @Override
            public void setOnCaptureFinished(Const.CaptureType type) {
                switch (type) {
                    case IR:
                        isFinishCollectIR = true;
                        if (sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && !sp.getBoolean(Const.TAKE_PIC_IR, false)) {
                            mDepthIrViewer.changeStreamType();
                        }
                        break;
                    case RGB:
                        isFinishCollectRGB=true;
                        break;
                    case DEPTH:
                        isFinishCollectDepth = true;
                        byte[] imgDepth = ThisApplictation.getData(CODE_BITMAP_DEPTH);
                        Bitmap bitmapDepth = BitmapUtils.convert24bit(imgDepth, 480, 640);
                        MyUtils.saveTrueBitmap(bitmapDepth, sp.getString(Const.TIME_STAMP, "dafault") + "_4.jpg", MainActivity.this);
                        if (Const.Depth_PREVIEW.equals(sp.getString(Const.PREVIEW, "depth"))) {
                            if (!sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && !sp.getBoolean(Const.TAKE_PIC_IR, false)) {
                                livenessResultBean = ThisApplictation.getBean();
                                String pathDepth = Environment.getExternalStorageDirectory().toString()
                                        + File.separator
                                        + "USBBCTC/live/" + sp.getString(Const.TIME_STAMP, "default") + "_4.jpg";
                                File file = new File(pathDepth);

                                if (livenessResultBean != null) {
                                    if (livenessResultBean.has_face_flag == 1) {
                                        livenessScore = livenessResultBean.livness_probs[1];
                                        if (livenessScore < Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                                            refreshPicCount(2);
                                            if (file.exists()) {
                                                file.delete();
                                            }
                                        } else {
                                            refreshPicCount(1);
                                        }
                                        Bundle bundle = new Bundle();
                                        bundle.putString("multiScore", livenessScore + "");
                                        bundle.putString("irface", livenessResultBean.ir_width + "×" + livenessResultBean.ir_height);
                                        Message message = new Message();
                                        message.setData(bundle);
                                        mHandle.sendMessage(message);
                                        scoresData = scoresData + sp.getString(Const.TIME_STAMP, Const.TIME_Default) + "活体概率：" + livenessResultBean.livness_probs[1] + "    IR 人脸宽高：" + livenessResultBean.ir_width + "×" + livenessResultBean.ir_height;
                                        if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
                                            MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
                                        }
                                    } else {
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                        scoresData = scoresData + sp.getString(Const.TIME_STAMP, "default") + "未检测到人脸";
                                        if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
                                            MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
                                        }
                                        Bundle bundle = new Bundle();
                                        bundle.putString("multiScore", "未检测到人脸");
                                        bundle.putString("irface", 0 + "×" + 0);
                                        Message message = new Message();
                                        message.setData(bundle);
                                        mHandle.sendMessage(message);
                                        refreshPicCount(0);
                                    }
                                }
                                endCheckTime = System.currentTimeMillis() - startCheckTime;
                                sumCostTime = sumCostTime + sp.getString(Const.TIME_STAMP, "default") + "sum time：" + endCheckTime;
                                MyUtils.writeTxtToFile(sumCostTime, "/storage/emulated/0/USBBCTC/", "TimeCost.txt");
                                if (isRun) {
                                    collectOneTime();
                                }
                            }
                        }
                        if (!sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && sp.getBoolean(Const.TAKE_PIC_IR, false)) {
//                    Log.e("lzh", "从Depth切换IR Preview了");
                            mDepthIrViewer.changeStreamType();
                        }
                        break;
                }
                if (isFinishCollectRGB && isFinishCollectIR) {
                    isFinishCollectRGB = false;
                    isFinishCollectIR = false;
                    new ReceiveTask().execute();
                }
            }
        });
    }
    public void takePicFinish(){
        setOnTakePicFinishedListener(new TakePicListener() {
            @Override
            public void setOnTakePicFinish(Const.CaptureType type) {
                switch (type) {
                    case IR:
                        isOnceTakeIR=true;
                        break;
                    case RGB:
                        isOnceTakeRGB=true;
                        break;
                    case DEPTH:
                        break;
                }
                if (isOnceTakeIR && isOnceTakeRGB) {
                    isOnceTakeRGB = false;
                    isOnceTakeIR = false;
                    byte[] imgIr = ThisApplictation.getData(CODE_BITMAP_IR);
                    byte[] imgRgb = ThisApplictation.getData(CODE_BITMAP_RGB);
                    if (imgIr != null) {
                        savePicBytes(imgIr, 400, 640, BITMAP_TYPE_IR);
                    }
                    if (imgRgb != null) {
                        savePicBytes(imgRgb, 480, 640, BITMAP_TYPE_RGB);
                    }
                }
            }
        });
    }

    private class initTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            initLiveness(Const.REF_DEFAULT_PATH, Const.YML_PATH_NEW);
            return null;
        }
    }

    private boolean isRun = false;
    private boolean isFaceModelexists = false;
    private boolean isLiveModelexists = false;
    private String captureTimeStamp;
    private int mPicCount = 0; //已采集图片个数
    private int mTruePicCount = 0; //已采集图片个数
    private int mFalsePicCount = 0; //已采集图片个数
    private int mNoDetectPicCount = 0; //已采集图片个数


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_check_once:
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_IR, false).commit();
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_RGB, false).commit();
                if (!new File(Const.YML_PATH_NEW).exists()) {
                    Toast.makeText(MainActivity.this, "请先设置Yml文件", Toast.LENGTH_SHORT).show();
                } else {
                    mPicCount = 0;
                    mTruePicCount = 0;
                    mFalsePicCount = 0;
                    drawView.setVisibility(View.VISIBLE);
                    collectOneTime();
                }
                break;
            case R.id.btn_check_multi:
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_IR, false).commit();
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_RGB, false).commit();
                if (!new File(Const.YML_PATH_NEW).exists()) {
                    Toast.makeText(MainActivity.this, "请先设置Yml文件", Toast.LENGTH_SHORT).show();
                } else {
                    if (!isRun) {
                        mPicCount = 0;
                        mTruePicCount = 0;
                        mFalsePicCount = 0;
                        drawView.setVisibility(View.VISIBLE);
                        mCheckMultiBtn.setText("点击停止多次检测");
                        isRun = true;
                        collectOneTime();
                        Toast.makeText(MainActivity.this, "开始多次检测,点击可停止", Toast.LENGTH_SHORT).show();
                    } else {
                        drawView.setVisibility(View.INVISIBLE);
                        mCheckMultiBtn.setText("点击开始多次检测");
                        isRun = false;
                        Toast.makeText(MainActivity.this, "停止多次检测", Toast.LENGTH_SHORT).show();
                    }
                }

                break;
            case R.id.btn_take_pic:
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_IR, true).commit();
                sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_RGB, true).commit();
                collectOneTime();
                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();

                break;

        }
    }

    private void collectOneTime() {
        sumCostTime = "";
        scoresData = "";
        startCheckTime = System.currentTimeMillis();
        drawView.setVisibility(View.VISIBLE);
        sp.edit().putString(Const.TIME_STAMP, BitmapUtils.getStringDate()).commit();
//        Log.e("lzh", "collectOneTime:" + sp.getString(Const.TIME_STAMP, Const.TIME_Default));
        sp.edit().putBoolean(Const.TAKE_PIC_IR, true).commit();
        sp.edit().putBoolean(Const.TAKE_PIC_DEPTH, true).commit();
        sp.edit().putBoolean(Const.TAKE_PIC_RGB, true).commit();
    }

    //preview 采集图片监听器
    public interface CaptureStateChangeListener {
        void setOnCaptureFinished(Const.CaptureType type);
    }

    public static CaptureStateChangeListener mCaptureStateChangeListener = null;

    public static void setOnCaptureFinishedListener(CaptureStateChangeListener listener) {
        mCaptureStateChangeListener = listener;
    }

    //preview 拍照监听器
    public interface TakePicListener {
        void setOnTakePicFinish(Const.CaptureType type);
    }

    public static TakePicListener mTakePicListener = null;

    public static void setOnTakePicFinishedListener(TakePicListener listener) {
        mTakePicListener = listener;
    }


    private boolean isFinishCollectIR = false;
    private boolean isFinishCollectRGB = false;
    private boolean isFinishCollectDepth = false;
    private boolean isOnceTakeIR = false;
    private boolean isOnceTakeRGB = false;
    private boolean isOnceTakeDepth = false;
    private String sumCostTime = "";
    private String scoresData = "";

//    private class FinishTakePicReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
////            Log.e("lzh", sp.getString(Const.TIME_STAMP, Const.TIME_Default) + "==action====" + intent.getAction());
//            if (Const.COLLECT_IR_FINISH_ACTION.equals(intent.getAction())) {
//                isFinishCollectIR = true;
//                if (sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && !sp.getBoolean(Const.TAKE_PIC_IR, false)) {
//                    mDepthIrViewer.changeStreamType();
//                }
//            } else if (Const.COLLECT_Depth_FINISH_ACTION.equals(intent.getAction())) {
//                isFinishCollectDepth = true;
//                byte[] imgDepth = ThisApplictation.getData(CODE_BITMAP_DEPTH);
//                Bitmap bitmapDepth = BitmapUtils.convert24bit(imgDepth, 480, 640);
//                MyUtils.saveTrueBitmap(bitmapDepth, sp.getString(Const.TIME_STAMP, "dafault") + "_4.jpg", MainActivity.this);
//                if (Const.Depth_PREVIEW.equals(sp.getString(Const.PREVIEW, "depth"))) {
//                    if (!sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && !sp.getBoolean(Const.TAKE_PIC_IR, false)) {
//                        livenessResultBean = ThisApplictation.getBean();
//                        String pathDepth = Environment.getExternalStorageDirectory().toString()
//                                + File.separator
//                                + "USBBCTC/live/" + sp.getString(Const.TIME_STAMP, "default") + "_4.jpg";
//                        File file = new File(pathDepth);
//
//                        if (livenessResultBean != null) {
//                            if (livenessResultBean.has_face_flag == 1) {
//                                livenessScore = livenessResultBean.livness_probs[1];
//                                if (livenessScore < Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
//                                    refreshPicCount(2);
//                                    if (file.exists()) {
//                                        file.delete();
//                                    }
//                                } else {
//                                    refreshPicCount(1);
//                                }
//                                Bundle bundle = new Bundle();
//                                bundle.putString("multiScore", livenessScore + "");
//                                bundle.putString("irface", livenessResultBean.ir_width + "×" + livenessResultBean.ir_height);
//                                Message message = new Message();
//                                message.setData(bundle);
//                                mHandle.sendMessage(message);
//                                scoresData = scoresData + sp.getString(Const.TIME_STAMP, Const.TIME_Default) + "活体概率：" + livenessResultBean.livness_probs[1] + "    IR 人脸宽高：" + livenessResultBean.ir_width + "×" + livenessResultBean.ir_height;
//                                if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
//                                    MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
//                                }
//                            } else {
//                                if (file.exists()) {
//                                    file.delete();
//                                }
//                                scoresData = scoresData + sp.getString(Const.TIME_STAMP, "default") + "未检测到人脸";
//                                if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
//                                    MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
//                                }
//                                Bundle bundle = new Bundle();
//                                bundle.putString("multiScore", "未检测到人脸");
//                                bundle.putString("irface", 0 + "×" + 0);
//                                Message message = new Message();
//                                message.setData(bundle);
//                                mHandle.sendMessage(message);
//                                refreshPicCount(0);
//                            }
//                        }
//                        endCheckTime = System.currentTimeMillis() - startCheckTime;
//                        sumCostTime = sumCostTime + sp.getString(Const.TIME_STAMP, "default") + "sum time：" + endCheckTime;
//                        MyUtils.writeTxtToFile(sumCostTime, "/storage/emulated/0/USBBCTC/", "TimeCost.txt");
//                        if (isRun) {
//                            collectOneTime();
//                        }
//                    }
//                }
//                if (!sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && sp.getBoolean(Const.TAKE_PIC_IR, false)) {
////                    Log.e("lzh", "从Depth切换IR Preview了");
//                    mDepthIrViewer.changeStreamType();
//                }
//            } else if (Const.COLLECT_RGB_FINISH_ACTION.equals(intent.getAction())) {
//                isFinishCollectRGB = true;
//            }
//            if (isFinishCollectRGB && isFinishCollectIR) {
//                isFinishCollectRGB = false;
//                isFinishCollectIR = false;
//                new ReceiveTask().execute();
//            }
//        }
//    }

    private class ReceiveTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            byte[] imgIr = ThisApplictation.getData(CODE_BITMAP_IR);
            byte[] imgRgb = ThisApplictation.getData(CODE_BITMAP_RGB);
            if (imgIr != null && imgRgb != null) {

                Bitmap bitmapIR = BitmapUtils.convert24bit(imgIr, 400, 640);
                ThisApplictation.setBitmap(CODE_BITMAP_IR, bitmapIR);
                Bitmap bitmapRGB = BitmapUtils.convert24bit(imgRgb, 480, 640);
                ThisApplictation.setBitmap(CODE_BITMAP_RGB, bitmapRGB);
//                Log.e("lzh", "开始去检测===" + sp.getString(Const.TIME_STAMP, Const.TIME_Default));
                detectFaceAndLive(imgIr, imgRgb);
            } else {
                mHandle.sendEmptyMessage(CODE_IMGE_EMPTY_TOAST);

            }

            return null;
        }
    }


    private int x, y, width, height;
    private Bitmap depthFaceBitmap;
    private Bitmap depthVGABitmap;
    private Bitmap irResultBitmap;
    private Bitmap irFaceBitmap;
    private Bitmap irSourceBitmap;
    private Bitmap rgbVGABitmap;
    private Bitmap rgbFaceBitmap;
    private LivenessResultBean livenessResultBean;
    private float livenessScore;

    private void detectFaceAndLive(byte[] imgObj, byte[] imgRGB) {
        rgbVGABitmap = null;
        rgbFaceBitmap = null;
        irFaceBitmap = null;
        depthFaceBitmap = null;
        depthVGABitmap = null;
        irResultBitmap = null;
        rgbVGABitmap = ThisApplictation.getBitmap(CODE_BITMAP_RGB);
        irSourceBitmap = ThisApplictation.getBitmap(CODE_BITMAP_IR);
        depthVGABitmap = ThisApplictation.getBitmap(CODE_BITMAP_DEPTH);

        String scoresData = "";
        livenessScore = 0;


        livenessResultBean = new LivenessResultBean();
//        Log.e("lzh","开始活体检测");
        if (imgObj != null && imgRGB != null) {
            livenessResultBean = (LivenessResultBean) FaceDetectLivenessFromByte(imgObj, imgRGB, Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT)));

            //整体检测结束
            ThisApplictation.setBean(livenessResultBean);
            String pathDepth = Environment.getExternalStorageDirectory().toString()
                    + File.separator
                    + "USBBCTC/live/" + sp.getString(Const.TIME_STAMP, "default") + "_4.jpg";
            File file = new File(pathDepth);
            if (livenessResultBean.has_face_flag == 1) {
                livenessScore = livenessResultBean.livness_probs[1];
                if (livenessScore < Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                    new ConnectThread(socket, "false").start();
                    if (file.exists()) {
                        file.delete();
                    }
                } else {
                    new ConnectThread(socket, "true").start();
                }

                if (livenessResultBean.ir_face != null && livenessResultBean.ir_width > 0 && livenessResultBean.ir_height > 0) {
                    try {
                        irFaceBitmap = Bitmap.createBitmap(livenessResultBean.ir_width, livenessResultBean.ir_height, Bitmap.Config.ARGB_8888);
                        irFaceBitmap.setPixels(livenessResultBean.ir_face, 0, livenessResultBean.ir_width, 0, 0, livenessResultBean.ir_width, livenessResultBean.ir_height);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (livenessResultBean.rgb_face != null && livenessResultBean.rgb_width > 0 && livenessResultBean.rgb_height > 0) {
                    try {
                        rgbFaceBitmap = Bitmap.createBitmap(livenessResultBean.rgb_width, livenessResultBean.rgb_height, Bitmap.Config.ARGB_8888);
                        rgbFaceBitmap.setPixels(livenessResultBean.rgb_face, 0, livenessResultBean.rgb_width, 0, 0, livenessResultBean.rgb_width, livenessResultBean.rgb_height);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                mHandle.sendEmptyMessage(CODE_START_CHECK_RESULT);
            } else {
                if (file.exists()) {
                    file.delete();
                }
                mHandle.sendEmptyMessage(CODE_START_CHECK_RESULT_FALSE);
                new ConnectThread(socket, "false").start();
            }
            //保存IR图片
            //镜像RGB VGA图片
            if (rgbVGABitmap != null) {
                rgbVGABitmap = BitmapUtils.mirrorBitmap(rgbVGABitmap, 1);
            }
            Bitmap[] bitmaps = {rgbVGABitmap, rgbFaceBitmap, irFaceBitmap, irSourceBitmap, depthVGABitmap};
            new SaveResultTask().execute(bitmaps);
       /* if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
            mHandle.sendEmptyMessage(CODE_CHECK_FINISH);
        }*/
            if (!sp.getBoolean(Const.CLICK_ONCE, false) && Const.IR_PREVIEW.equals(sp.getString(Const.PREVIEW, "ir"))) {
                if (!sp.getBoolean(Const.TAKE_PIC_DEPTH, false) && !sp.getBoolean(Const.TAKE_PIC_IR, false)) {
                    endCheckTime = System.currentTimeMillis() - startCheckTime;
                    sumCostTime = sumCostTime + sp.getString(Const.TIME_STAMP, "default") + "sum time：" + endCheckTime;
                    MyUtils.writeTxtToFile(sumCostTime, "/storage/emulated/0/USBBCTC/", "TimeCost.txt");
                    if (livenessResultBean != null) {
                        if (livenessResultBean.has_face_flag == 1) {
                            livenessScore = livenessResultBean.livness_probs[1];
                            if (livenessScore < Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                                refreshPicCount(2);
                            } else {
                                refreshPicCount(1);
                            }

                            Bundle bundle = new Bundle();
                            bundle.putString("multiScore", livenessScore + "");
                            bundle.putString("irface", livenessResultBean.ir_width + "×" + livenessResultBean.ir_height);
                            Message message = new Message();
                            message.setData(bundle);
                            mHandle.sendMessage(message);
                            scoresData = scoresData + sp.getString(Const.TIME_STAMP, Const.TIME_Default) + "活体概率：" + livenessResultBean.livness_probs[1] + "    IR 人脸宽高：" + livenessResultBean.ir_width + "×" + livenessResultBean.ir_height;
                            if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
                                MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
                            }
                        } else {
                            scoresData = scoresData + sp.getString(Const.TIME_STAMP, Const.TIME_Default) + "未检测到人脸";
                            if (!sp.getBoolean(Const.CLICK_ONCE, false)) {
                                MyUtils.writeTxtToFile(scoresData, "/storage/emulated/0/USBBCTC/", "LivenessScores.txt");
                            }
                            Bundle bundle = new Bundle();
                            bundle.putString("multiScore", "未检测到人脸");
                            bundle.putString("irface", 0 + "×" + 0);
                            Message message = new Message();
                            message.setData(bundle);
                            mHandle.sendMessage(message);
                            refreshPicCount(0);
                        }
                    }
                    if (isRun) {
                        mHandle.sendEmptyMessage(CODE_CHECK_FINISH);
                    }
                }
            }
        }


    }

    private Canvas canvas;
    private Paint KeyPointsPaint = new Paint();
    private SurfaceHolder mDrawSurfaceHolder;
    private SurfaceView drawView;

    private void DrawFaceResult(int[] faceboxes, Bitmap bitmap, boolean isLive) {

        canvas = null;
        try {
            canvas = mDrawSurfaceHolder.lockCanvas();
            if (isLive) {
                KeyPointsPaint.setColor((Color.GREEN));
                //绿色
            } else {
                KeyPointsPaint.setColor((Color.RED));
                //红色
            }
            if (canvas == null) {
                return;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // 屏幕长宽
            DisplayMetrics metric = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metric);
            int screenW = metric.widthPixels / 2;
            int screenH = metric.heightPixels;


            int imgW = bitmap.getWidth();
            int imgH = bitmap.getHeight();

            int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
            Rect frame = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            int statusBarHeight = frame.top;

            int previewWidth = screenW;
            int previewHeight = screenH - contentTop - statusBarHeight;
            previewHeight = 400;
            float kx = ((float) previewWidth) / imgW;
            float ky = (float) previewHeight / imgH;
//            kx = 0.8f;
            // 绘制人脸关键点
            for (int i = 0; i < faceboxes.length; i++) {
                if (faceboxes[2] > 400) {
                    faceboxes[2] = 400;
                }
                if (faceboxes[3] > 640) {
                    faceboxes[3] = 640;
                }
                if (faceboxes[0] < 0) {

                    faceboxes[0] = 0;
                }
                if (faceboxes[1] < 0) {

                    faceboxes[1] = 0;
                }
                float left = faceboxes[0] - 40;
                if (left < 0) {
                    left = 0;
                }
                float top = faceboxes[1];
                float right = faceboxes[2] + left;
                float bottom = faceboxes[3] + top;
                canvas.drawLine(left * kx, top * ky,
                        right * kx, top * ky, KeyPointsPaint);
                canvas.drawLine(right * kx, top * ky,
                        right * kx, bottom * ky, KeyPointsPaint);
                canvas.drawLine(right * kx, bottom * ky,
                        left * kx, bottom * ky, KeyPointsPaint);
                canvas.drawLine(left * kx, bottom * ky,
                        left * kx, top * ky, KeyPointsPaint);
            }
        } catch (Throwable t) {
//            Log.e("lzh", "Draw result error:" + t.getMessage());
        } finally {
            if (canvas != null) {

                mDrawSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

    }

    private class SaveResultTask extends AsyncTask<Bitmap, Void, Void> {
        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            boolean is_save_all_checked = sp.getBoolean(Const.SAVE_ALL, true);
            boolean is_save_true_checked = sp.getBoolean(Const.SAVE_TRUE, true);
            boolean is_save_false_checked = sp.getBoolean(Const.SAVE_FALSE, true);
            String timeFlag = sp.getString(Const.TIME_STAMP, Const.TIME_Default);

            if (is_save_all_checked && livenessScore > Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                if (bitmaps[0] != null) {
                    MyUtils.saveTrueBitmap(bitmaps[0], timeFlag + "_1.jpg", MainActivity.this);
                }
                if (bitmaps[1] != null) {
                    MyUtils.saveTrueBitmap(bitmaps[1], timeFlag + "_2.jpg", MainActivity.this);
                }
                if (bitmaps[2] != null) {
                    MyUtils.saveTrueBitmap(bitmaps[2], timeFlag + "_ROI.jpg", MainActivity.this);
                }
                if (bitmaps[3] != null) {
                    MyUtils.saveTrueBitmap(bitmaps[3], timeFlag + "_3.jpg", MainActivity.this);
                }
                if (bitmaps[4] != null) {
                    MyUtils.saveTrueBitmap(bitmaps[4], timeFlag + "_4.jpg", MainActivity.this);
                }

            } else {
                if (is_save_true_checked && livenessScore > Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                    if (bitmaps[0] != null) {
                        MyUtils.saveTrueBitmap(bitmaps[0], timeFlag + "_1.jpg", MainActivity.this);
                    }
                    if (bitmaps[1] != null) {
                        MyUtils.saveTrueBitmap(bitmaps[1], timeFlag + "_2.jpg", MainActivity.this);
                    }
                    if (bitmaps[3] != null) {
                        MyUtils.saveTrueBitmap(bitmaps[3], timeFlag + "_3.jpg", MainActivity.this);
                    }
                }
            }
            if (is_save_false_checked && livenessScore < Float.parseFloat(sp.getString(THRESHOLD_VALUE, THRESHOLD_VALUE_DEFAULT))) {
                if (livenessScore > 0) {
                    if (bitmaps[0] != null) {
                        //rgbVGA
                        MyUtils.saveFalseBitmap(bitmaps[0], timeFlag + "_0.jpg", MainActivity.this);
                    }
                    if (bitmaps[3] != null) {
                        //irSource
                        MyUtils.saveFalseBitmap(bitmaps[3], timeFlag + "_IR.jpg", MainActivity.this);
                    }
                    //IR face
                    if (bitmaps[2] != null) {
                        MyUtils.saveFalseBitmap(bitmaps[2], timeFlag + "_ROI.jpg", MainActivity.this);
                    }
                }
            }
            if ((is_save_false_checked || is_save_true_checked || is_save_all_checked) && livenessScore == 0) {

                if (bitmaps[0] != null) {
                    //rgbVGA
                    MyUtils.saveNoDetectFaceBitmap(bitmaps[0], timeFlag + "_RGB.jpg", MainActivity.this);
                }
                if (bitmaps[3] != null) {
                    //irSource
                    MyUtils.saveNoDetectFaceBitmap(bitmaps[3], timeFlag + "_IR.jpg", MainActivity.this);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
//            Log.e("lzh", "保存完成耗时---" + end_save);

        }
    }

    private static int CODE_START_CHECK_RESULT = 1000;
    private static int CODE_START_CHECK_RESULT_FALSE = 1001;
    private static int CODE_CHECK_FINISH = 1002;
    private static int CODE_CHECK_FINISH_SOCKET = 1003;
    private static int CODE_NO_MATERIAL = 1004;
    private static int CODE_IMGE_EMPTY_TOAST = 1005;

    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CODE_IMGE_EMPTY_TOAST) {
                Toast.makeText(MainActivity.this, "取得模组数据IR或者RGB为空", Toast.LENGTH_SHORT).show();
            } else if (msg.what == CODE_NO_MATERIAL) {
                Toast.makeText(MainActivity.this, "请先设置新材料图片", Toast.LENGTH_SHORT).show();
            } else if (msg.what == CODE_CHECK_FINISH_SOCKET) {
                drawView.setVisibility(View.INVISIBLE);
//                isSocketStartCheck = false;
            } else if (isRun && msg.what == CODE_CHECK_FINISH) {
//                循环多次检测
                collectOneTime();
            } else if (sp.getBoolean(Const.CLICK_ONCE, false) && msg.what == CODE_START_CHECK_RESULT) {
                sp.edit().putBoolean(Const.CLICK_ONCE, false).commit();
             /*   Intent intent = new Intent(MainActivity.this, CheckResultActivity.class);
                startActivity(intent);*/

            } else if (sp.getBoolean(Const.CLICK_ONCE, false) && msg.what == CODE_START_CHECK_RESULT_FALSE) {
                sp.edit().putBoolean(Const.CLICK_ONCE, false).commit();
                Toast.makeText(MainActivity.this, "未检测到人脸，请重试", Toast.LENGTH_SHORT).show();
            } else {
                Bundle receiveBundle = msg.getData();
                String receiverMsg = receiveBundle.getString("data");
                String time = receiveBundle.getString("time");
                String order = receiveBundle.getString("order");
                //收到串口发的开始检测的指令后开始活体检测
                if (order != null && order.contains(Const.START_CHECK_ORDER)) {
//                    String order_finish_time=order.
//                new Thread(new ScanThread()).start();
//                    if (!isSocketStartCheck) {
                    sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_IR, false).commit();
                    sp.edit().putBoolean(Const.IS_ONLY_TAKE_PIC_RGB, false).commit();
                    collectOneTime();
//                    }
                }
                /*if (!TextUtils.isEmpty(time)) {
                    mTimeCostTextView.setVisibility(View.VISIBLE);
                    mTimeCostTextView.setText(time);

                } else {
                    mTimeCostTextView.setVisibility(View.GONE);
                }*/
                if (receiverMsg != null && receiverMsg.contains("failed")) {
                    Toast.makeText(MainActivity.this, receiverMsg, Toast.LENGTH_SHORT).show();
                }
                String collectPicCount = receiveBundle.getString("picCount");
                String trueCount = receiveBundle.getString("trueCount");
                String falseCount = receiveBundle.getString("falseCount");
                String multiScore = receiveBundle.getString("multiScore");
                String noDetectCount = receiveBundle.getString("noDetectCount");
                String irFace = receiveBundle.getString("irface");
                if (multiScore != null) {
//                    Log.e("lzh","活体概率=="+multiScore);
                    if ("未检测到人脸".equals(multiScore)) {
                        drawView.setVisibility(View.INVISIBLE);
                    }
                    mMultiScoreTextView.setText(multiScore);
                }
               /* if (irFace != null) {
                    mIRFaceTextView.setText(irFace);

                }*/
                if (collectPicCount != null) {
                    mSumCountTextView.setText(collectPicCount);
                }
                if (trueCount != null) {
                    mTrueCountTextView.setText(trueCount);
                }
                if (falseCount != null) {
                    mFalseCountTextView.setText(falseCount);
                }
                if (noDetectCount != null) {
                    mNoDetectCountTextView.setText(noDetectCount);
                }


            }
        }
    };


    private void refreshPicCount(int islive) {
        if (islive == 1) {
            mTruePicCount++;
        } else if (islive == 2) {
            mFalsePicCount++;
        } else if (islive == 0) {
            mNoDetectPicCount++;
        }
        mPicCount++;
        Bundle bundle = new Bundle();
        bundle.putString("picCount", mPicCount + "");
        bundle.putString("trueCount", mTruePicCount + "");
        bundle.putString("falseCount", mFalsePicCount + "");
        bundle.putString("noDetectCount", mNoDetectPicCount + "");
        Message message = new Message();
        message.setData(bundle);
        mHandle.sendMessage(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFlag = true;
        new ServerThread().start();
        //如果用0710大电流版本
//        new ConnectionThread("30888888").start();
        new ConnectionThread("38888888").start();
        if (mDepthIrViewer != null) {
            mDepthIrViewer.onResume();
        }
//        if(mIrViewer != null) {
//            mIrViewer.onResume();
//        }

        if (mColorViewer != null) {
            mColorViewer.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isConnect = false;
        sp.edit().clear().commit();
        new ConnectionThread("48888888").start();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                MyUtils.writeTxtToFile(BitmapUtils.getStringDate() + "--onPause--serverSocket---close", "/storage/emulated/0/SocketLog/", "socketLog.txt");
                Log.e("lzh", "--onPause--serverSocket---close");
                isFlag = false;
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                serverSocket.close();
                serverSocket = null;
            } catch (Exception e) {
                Log.e("lzh", "onDestroy--Exception" + e.toString());
                e.printStackTrace();
            }
        }
        if (isRun) {
            drawView.setVisibility(View.GONE);
            mCheckMultiBtn.setText("点击开始多次检测");
            isRun = false;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDepthIrViewer != null) {
            mDepthIrViewer.onPause();
        }

//        if(mIrViewer != null) {
//            mIrViewer.onPause();
//        }

        if (mColorViewer != null) {
            mColorViewer.onPause();
        }

        if (mDepthIrViewer != null) {
            mDepthIrViewer.onDestroy();
        }

//        if(mIrViewer != null) {
//            mIrViewer.onDestroy();
//        }

        //destroy color viewer.
        if (mColorViewer != null) {
            mColorViewer.onDestroy();
        }

        if (mRlDevice != null) {
            mRlDevice.close();
        }

        if (mRlContext != null) {
            mRlContext.destroy();
            mRlContext = null;
        }

        //finish
        finish();

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
      /*  if (mFinishDetectReceiver != null) {
            unregisterReceiver(mFinishDetectReceiver);
        }*/

        if (mUsbReceiver != null) {
            unregisterReceiver(mUsbReceiver);
        }
    }

    ServerSocket serverSocket = null;
    public final int port = 8688;
    private Socket socket;
    private boolean isFlag = true;
    public boolean isConnect = false;

    private class ServerThread extends Thread {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (isFlag) {
//                   if(!isConnect){
//                        Socket soc = new Socket();
//                        SocketAddress endpoint = new InetSocketAddress(IP_ADDRESS, port);
//                        try {
//                            soc.connect(endpoint, 200);
//                        } catch (Exception e) {
//                            Log.e("lzh", "connect server error==" + e.toString());
//                            e.printStackTrace();
//                        }
//                    }
//                    Log.e("lzh", "server accept before" + MyUtils.getStringDate());
                    socket = serverSocket.accept();
                    isConnect = true;
//                    Log.e("lzh", "server accept success" + MyUtils.getStringDate());
                    MyUtils.writeTxtToFile(BitmapUtils.getStringDate() + "--onResume--serverSocket---start", "/storage/emulated/0/SocketLog/", "socketLog.txt");
                    new ConnectThread(socket, "").start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                isFlag = false;
                Log.e("lzh", "serverSocket---error--" + e.toString());
                if (serverSocket != null) {
                    try {
                        Log.e("lzh", "serverSocket关闭");
                        socket.close();
                        serverSocket.close();
                        serverSocket = null;
                        socket = null;
                        isFlag = false;
                        if (e.toString() != null && e.toString().contains("EADDRINUSE")) {
                            if (serverSocket == null && socket == null) {
                                Log.e("lzh", "端口占用关闭后重新打开");
                            }
                            SystemClock.sleep(20);
                            isFlag = true;
                            new ServerThread().start();
                            new ConnectionThread("38888888").start();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        MyUtils.writeTxtToFile(BitmapUtils.getStringDate() + e.toString(), "storage/emulated/0/SocketLog/", "socketLog.txt");
                    }
                }
            }
        }
    }

    //向客户端发送信息,发送
    class ConnectThread extends Thread {
        Socket socket = null;
        String messageData = null;

        public ConnectThread(Socket socket, String message) {
            super();
//            Log.e("lzh", "socket--" + socket);
            this.messageData = message;
//            Log.e("lzh", "messageData--" + messageData);
//            System.out.println("lzh---写数据--" + messageData);
            if ("true".equals(message) || "false".equals(message)) {
                MyUtils.writeTxtToFile(BitmapUtils.getStringDate() + "--send to client live result:" + message, "/storage/emulated/0/SocketLog/", "socketLog.txt");
            }

            this.socket = socket;

        }

        @Override
        public void run() {
            OutputStream output;
            if (socket != null && !socket.isClosed()) {

                try {
                    socket.setSoTimeout(150);
                    output = socket.getOutputStream();
                    output.write(messageData.getBytes());
                    output.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int i = 0;
                boolean isSocketConnectted = true;
                while (isSocketConnectted) {

                    i++;
                    InputStream input;
                    int count = 0;
                    try {
                        input = socket.getInputStream();
                        // simply for java.util.ArrayList
                        if (count == 0) {
                            count = input.available();
                        }
                        if (count != 0) {
                            byte[] b = new byte[count];
                            int readed = input.read(b);
//                            input.close();
                            String messageRecv = new String(b, 0, readed);
                            Bundle bundle = new Bundle();
//                            Log.e("lzh", "messageRecv==" + messageRecv);
                            bundle.putString("order", messageRecv);
                            Message message = new Message();
                            message.setData(bundle);
                            mHandle.sendMessage(message);
//                            System.out.println("lzh---收到数据--" + messageRecv);
                        }
                    } catch (Exception e) {
                        isSocketConnectted = false;
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private Socket soc;
    private String messageRecv;
    private static String IP_ADDRESS = "127.0.0.1";
    private static int PORT = 8666;

    //新建一个子线程，实现socket通信
    class ConnectionThread extends Thread {
        String message = null;

        public ConnectionThread(String msg) {
//            Log.e("lzh", "msg--" + msg);
            message = msg;
        }

        @Override
        public void run() {
            try {
                Socket soc = new Socket();
                SocketAddress endpoint = new InetSocketAddress(IP_ADDRESS, PORT);
                try {
                    soc.connect(endpoint, 200);
//                    soc.setSoTimeout(1000);
                } catch (Exception e) {
//                    soc.close();
                    Log.e("lzh", "结构光error--" + e.toString());
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("data", "connect failed");
                    Message message = new Message();
                    message.setData(bundle);
                    mHandle.sendMessage(message);
                }
                OutputStream output;
                try {
                    output = soc.getOutputStream();
                    soc.setSoTimeout(100);
                    output.write(message.getBytes());
                    output.flush();
                } catch (Exception e) {
                    Log.e("lzh", "Exception222--" + e.toString());
                    e.printStackTrace();
                    Bundle bundle = new Bundle();
                    bundle.putString("data", "write failed");
                    Message message = new Message();
                    message.setData(bundle);
                    mHandle.sendMessage(message);
                }

                int i = 0;
                boolean isSocketConnect = true;
                while (isSocketConnect) {
                    i++;
                    if (i > 500) {
                        break;
                    }
                    InputStream input;
                    int count = 0;
                    try {
                        input = soc.getInputStream();
                        soc.setSoTimeout(100);
                        if (count == 0) {
                            count = input.available();
                        }
                        if (count != 0) {
                            byte[] b = new byte[count];
                            int readed = input.read(b);
                            messageRecv = new String(b, 0, readed);
                            Bundle bundle = new Bundle();
                            bundle.putString("data", messageRecv);
                            Message message = new Message();
                            message.setData(bundle);
                            mHandle.sendMessage(message);
                            break;
                        }
                    } catch (Exception e) {
                        isSocketConnect = false;
                        Log.e("lzh", "Exception333--" + e.toString());
                        e.printStackTrace();
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("data", "read failed");
                        Message message = new Message();
                        message.setData(bundle2);
                        mHandle.sendMessage(message);
                    }
                }
                soc.close();

            } catch (IOException e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }

    }


    public native String stringFromJNI();

    public static native Object FaceDetectLiveness(int[] imgObj, int[] imgRGB, String depth_data_path, float default_liveness);

    public static native void initLiveness(String ref_path, String ymlPath);

    public static native int[] AdjustLightness(int[] irObj);

    public static native void CopyMnnData(AssetManager ass, String filename, String outPutFile);

    public static native Object FaceDetectLivenessFromByte(byte[] imgObj, byte[] imgRGB, float livenessDefault);

}
