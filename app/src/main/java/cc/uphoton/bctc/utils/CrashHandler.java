package cc.uphoton.bctc.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CrashHandler implements Thread.UncaughtExceptionHandler {


    private static final String TAG = "CrashHandler";

    private static CrashHandler crashHandler;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private Context mContext;

    /**
     * 采集信息
     */
    private Map<String, String> mInfo = new HashMap<>();

    /**
     * 文件日期
     */
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 单例
     */
    private CrashHandler() {

    }

    /**
     * 懒汉式单例
     *
     * @return
     */
    public static CrashHandler getCrashHandler() {
        if (crashHandler == null) {
            synchronized (CrashHandler.class) {
                if (crashHandler == null) {
                    crashHandler = new CrashHandler();
                }
            }
        }

        return crashHandler;
    }


    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        if (!handlerException(throwable)) {
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(thread, throwable);
            }
        } else {
            try {
                Thread.sleep(1000);
                android.os.Process.killProcess(android.os.Process.myPid());
                // 退出程序
                System.exit(0);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
    }

    private boolean handlerException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        new Thread() {
            @Override
            public void run() {

                Looper.prepare();
                Toast.makeText(mContext, "异常退出", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
        collectDeviceInfo(mContext);
        saveCrashInfo2File(throwable);
        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                mInfo.put("versionName", versionName);
                mInfo.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                mInfo.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : mInfo.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            String time = dateFormat.format(new Date());
            String fileName = time + ".txt";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                String path = Environment.getExternalStorageDirectory().toString()+"/crash_demo";
                String path = Environment.getExternalStorageDirectory().toString()
                        + File.separator
                        + "CrashLog"
                        + File.separator;
                //AppSettings.CrashLogPath;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                //FileWriter fileWriter = new FileWriter(path + fileName);
                FileOutputStream fos = new FileOutputStream(path + fileName);
                //fileWriter.write(sb.toString());
                fos.write(sb.toString().getBytes("UTF-8"));
                fos.close();
                //fileWriter.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e("lzh", "an error occured while writing file...", e);
        }
        return null;
    }


}
