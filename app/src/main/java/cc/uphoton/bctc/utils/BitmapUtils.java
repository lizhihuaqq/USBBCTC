package cc.uphoton.bctc.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import cc.uphoton.bctc.activity.MainActivity;
import cc.uphoton.bctc.activity.ThisApplictation;

import static cc.uphoton.bctc.activity.MainActivity.sp;

public class BitmapUtils {
    public static int[] byteArrayToIntArray(byte[] b) {
        if (b.length % 4 != 0)
            return null;
        int[] a = new int[b.length / 4];
        for (int i1 = 0, i2 = 1, i3 = 2, i4 = 3, i5 = 0; i4 < b.length; i1 += 4, i2 += 4, i3 += 4, i4 += 4, i5++) {
            byte[] ab = new byte[4];
            ab[0] = b[i1];
            ab[1] = b[i2];
            ab[2] = b[i3];
            ab[3] = b[i4];
            a[i5] = byteArrayToInt(ab);
        }
        return a;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static void savePic(ByteBuffer frameData, int width, int height, int bitmapType) {
        if (frameData != null) {
            Bitmap bitmap = null;
            byte[] bytes = null;
            int len = frameData.limit() - frameData.position();
            if (len > 0) {
                bytes = new byte[len];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = frameData.get();
                }

                bitmap = BitmapUtils.convert24bit(bytes, width, height);
                frameData.flip();
                String picName = null;
                if (bitmapType == 0) {
                    picName = "UVC_RGB_" + sp.getString(Const.TIME_STAMP, "rgb") + ".jpg";

                } else if (bitmapType == 1) {
                    picName = "UVC_IR_" + sp.getString(Const.TIME_STAMP, "ir") + ".jpg";

                } else if (bitmapType == 2) {
                    picName = "UVC_Depth_" + sp.getString(Const.TIME_STAMP, "depth") + ".jpg";

                }
                BitmapUtils.saveDepthBitmap(bitmap, picName, MainActivity.getContext());
            }


        }

    }

    public static void savePicBytes(byte[] frameData, int width, int height, int bitmapType) {
        if (frameData != null) {
            Bitmap bitmap = null;

            bitmap = BitmapUtils.convert24bit(frameData, width, height);
            String picName = null;
            if (bitmapType == 0) {
                bitmap=BitmapUtils.mirrorBitmap(bitmap,1);
                picName = sp.getString(Const.TIME_STAMP, "rgb") + "_RGB_" + ".jpg";
            } else if (bitmapType == 1) {
                picName = sp.getString(Const.TIME_STAMP, "rgb") + "_IR" + ".jpg";
            } else if (bitmapType == 2) {
                picName = sp.getString(Const.TIME_STAMP, "depth") + "_Depth" + ".jpg";

            }
            BitmapUtils.saveDepthBitmap(bitmap, picName, MainActivity.getContext());
        }


    }


    //图片字节转换
    public static Bitmap rawByteArray2RGBABitmap(byte[] data, int width, int height) {

        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1)]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        bmp.setPixels(rgba, 0, width, 0, 0, width, height);

        return bmp;

    }

    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        String dateString = formatter.format(currentTime);

        return dateString;
    }

    public static File saveDepthBitmap(Bitmap bmp, String picName, Context ctx) {
        String path = Environment.getExternalStorageDirectory().toString()
                + File.separator
                + "USBBCTC/takePicture";
//        String path = sp.getString(ctx.getString(R.string.savelocation_key), defaultPath) + File.separator;
        File file = new File(path, picName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);//向缓冲区压缩图片
            bos.flush();
            bos.close();
        } catch (Exception e) {
            Log.e("lzh", "Exception===" + e.toString());
        }
        return file;
    }

    /**
     * 24位灰度转Bitmap
     * <p>
     * 图像宽度必须能被4整除
     *
     * @param data   裸数据
     * @param width  图像宽度
     * @param height 图像高度
     * @return
     */
    public static Bitmap convert24bit(byte[] data, int width, int height) {
        byte[] Bits = new byte[data.length * 4]; //RGBA 数组

        int i;
        // data.length / 3 表示 3位为一组
        for (i = 0; i < data.length / 3; i++) {
            // 原理：24位是有彩色的，所以要复制3位，最后一位Alpha = 0xff;
            Bits[i * 4] = data[i * 3];
            Bits[i * 4 + 1] = data[i * 3 + 1];
            Bits[i * 4 + 2] = data[i * 3 + 2];
            Bits[i * 4 + 3] = -1;
        }

        // Bitmap.Config.ARGB_8888 表示：图像模式为8位
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));

        return bmp;
    }

    public static Bitmap convert24bitNew(byte[] data, int width, int height) {
        byte[] Bits = new byte[data.length * 4]; //RGBA 数组

        int i;

        // data.length / 3 表示 3位为一组
        for (i = 0; i < data.length; i++) {
            // 原理：24位是有彩色的，所以要复制3位，最后一位Alpha = 0xff;
            Bits[i * 4 + 0] = data[i];
            Bits[i * 4 + 1] = data[i];
            Bits[i * 4 + 2] = data[i];
            Bits[i * 4 + 3] = -1;
        }

        // Bitmap.Config.ARGB_8888 表示：图像模式为8位
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));

        return bmp;
    }

    public static Bitmap SetRGBData(byte[] rgbData, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        int k = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int blue_color = (int) ((rgbData[k] & 0x1F) * 255 / 31 + 0.5);
                int green_color = (int) ((((rgbData[k] & 0xE0) >> 5) | ((rgbData[k + 1] & 0x07) << 3)) * 255 / 63 + 0.5);
                int red_color = (int) (((rgbData[k + 1] & 0xF8) >> 3) * 255 / 31 + 0.5);
                bmp.setPixel(i, j, Color.rgb(red_color, green_color, blue_color));
                k += 2;
            }
        }
        return bmp;
    }

    /**
     * byteBuffer 转 byte数组
     *
     * @param buffer
     * @return
     */
    public static byte[] bytebuffer2ByteArray(ByteBuffer buffer) {
        //重置 limit 和postion 值
        buffer.flip();
        //获取buffer中有效大小
        int len = buffer.limit() - buffer.position();

        byte[] bytes = new byte[len];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = buffer.get();

        }

        return bytes;
    }
    /**
     * 镜像Bitmap
     */
    public static Bitmap mirrorBitmap(Bitmap bmp, int axis) {
        Matrix m = new Matrix();
        if (axis == 0) { //沿x轴镜像
            m.postScale(1, -1);
        } else if (axis == 1) { //沿y轴镜像
            m.postScale(-1, 1);
        }

        Bitmap result;
        try {
            result = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(),
                    m, true);
        } catch (OutOfMemoryError ex) {
            return null;
        }
        return result;
    }
}
