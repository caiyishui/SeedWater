package com.water.seed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.water.seed.view.CaptureView;
import com.water.seed.view.ZoomImageView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private CaptureView cv_main_camera;
    private Camera mCamera;
    WindowManager wm;
    private float scalex = 1;
    private float scaley = 1;
    private ZoomImageView mv_main_camera;
    private Bitmap smallbitmap;
    private List<Point> points = new ArrayList<Point>();
    private List<Point> pointsinit = new ArrayList<Point>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn_main_tapkpic);
        cv_main_camera = findViewById(R.id.cv_main_camera);
        mv_main_camera = findViewById(R.id.mv_main_camera);
        wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePic();
            }
        });
    }

    private void takePic() {

        mCamera = cv_main_camera.openCamera();
        initParameters(mCamera);
        mCamera.takePicture(null, null, new PictureListener());
        mv_main_camera.setVisibility(View.VISIBLE);
    }

    public void initParameters(Camera mCamera) {
        Camera.Parameters parameters = mCamera.getParameters();
        //设置近似屏幕的分辨率
        Camera.Size s = getCurrentScreenSize(parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(s.width, s.height);
        Camera.Size s2 = getBestPictureSize(parameters.getSupportedPictureSizes(), s);
        parameters.setPictureSize(s2.width, s2.height);
        mCamera.setParameters(parameters);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cv_main_camera != null) {
            cv_main_camera.resetCamera();
        }
    }

    /**
     * 设置最合适的尺寸
     *
     * @param localSizes
     * @param s
     * @return
     */
    private Camera.Size getBestPictureSize(List<Camera.Size> localSizes, Camera.Size s) {

        Camera.Size biggestSize = null;
        Camera.Size fitSize = null;// 优先选预览界面的尺寸
        Camera.Size previewSize = s;
        float previewSizeScale = 0;
        if (previewSize != null) {
            previewSizeScale = previewSize.width / (float) previewSize.height;
        }

        if (localSizes != null) {
            int cameraSizeLength = localSizes.size();
            for (int n = 0; n < cameraSizeLength; n++) {
                Camera.Size size = localSizes.get(n);
                if (biggestSize == null) {
                    biggestSize = size;
                } else if (size.width >= biggestSize.width && size.height >= biggestSize.height) {
                    biggestSize = size;
                }

                // 选出与预览界面等比的最高分辨率
                if (previewSizeScale > 0
                        && size.width >= previewSize.width && size.height >= previewSize.height) {
                    float sizeScale = size.width / (float) size.height;
                    if (sizeScale == previewSizeScale) {
                        if (fitSize == null) {
                            fitSize = size;
                        } else if (size.width >= fitSize.width && size.height >= fitSize.height) {
                            fitSize = size;
                        }
                    }
                }
            }

            // 如果没有选出fitSize, 那么最大的Size就是FitSize
            if (fitSize == null) {
                fitSize = biggestSize;
            }

        }
        return fitSize;
    }

    private Camera.Size getCurrentScreenSize(List<Camera.Size> localSizes) {
        Camera.Size biggestSize = null;
        Camera.Size fitSize = null;// 优先选屏幕分辨率
        Camera.Size targetSize = null;// 没有屏幕分辨率就取跟屏幕分辨率相近(大)的size
        Camera.Size targetSiz2 = null;// 没有屏幕分辨率就取跟屏幕分辨率相近(小)的size
        if (localSizes != null) {
            int cameraSizeLength = localSizes.size();
            for (int n = 0; n < cameraSizeLength; n++) {
                Camera.Size size = localSizes.get(n);
                if (biggestSize == null ||
                        (size.width >= biggestSize.width && size.height >= biggestSize.height)) {
                    biggestSize = size;
                }

                if (size.width == getDisplay().heightPixels
                        && size.height == getDisplay().widthPixels) {
                    fitSize = size;
                } else if (size.width == getDisplay().heightPixels
                        || size.height == getDisplay().widthPixels) {
                    if (targetSize == null) {
                        targetSize = size;
                    } else if (size.width < getDisplay().heightPixels
                            || size.height < getDisplay().widthPixels) {
                        targetSiz2 = size;
                    }
                }
            }

            if (fitSize == null) {
                fitSize = targetSize;
            }

            if (fitSize == null) {
                fitSize = targetSiz2;
            }

            if (fitSize == null) {
                fitSize = biggestSize;
            }

        }

        return fitSize;
    }

    public DisplayMetrics getDisplay() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    static {
        System.loadLibrary("waterseed-lib");


    }

    /**
     * 获取种子数量
     */
//	public native static int getSeedNum(int[] pixels,int width,int height);
    public native static ArrayList<Point> getPointArray(int[] pixels, int width, int height);

    /**
     * jpeg是获取经过压缩成jpg格式的图像数据
     *
     * @author Administrator
     */
    private class PictureListener implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {


            int disw = wm.getDefaultDisplay().getWidth() / 5 * 4;
            int dish = wm.getDefaultDisplay().getHeight() / 5 * 4;
            ;
//			float disw=cv_main_camera.getWidth();
//			float dish=cv_main_camera.getHeight();
            if (data == null) {
                Log.i("PictureListener", "没有有jpg原始到图像");
                return;
            } else {
                Log.i("PictureListener", "有jpg原始到图像");
            }

            int length = data.length;
//			//处理事件
            Bitmap bitmap = getBitmapFromByte(data);
            if (bitmap == null) {

                return;
            }

            //设置图片到Zoom空间上
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            scalex = (float) ((float) w / (float) disw);
            scaley = (float) ((float) h / (float) dish);
            mv_main_camera.scalex = scalex;
            mv_main_camera.scaley = scaley;
            smallbitmap = small(bitmap);
            int sw = smallbitmap.getWidth();
            int sh = smallbitmap.getHeight();
            mv_main_camera.setImageBitmap(smallbitmap);
            cv_main_camera.setVisibility(View.GONE);


            Log.i("length", length + "7878787878787878787" + "disw:" + disw + "dish:" + dish + "~~~~~" + "~~~~~" + w + "~~~~" + h);
            int[] pixels = new int[sw * sh];
            smallbitmap.getPixels(pixels, 0, sw, 0, 0, sw, sh);
            List<Point> pointsget = getPointArray(pixels, sw, sh);
            int seedNumsize = pointsget.size();
            points = pointsget;
            int seedNum = 0;
            if (pointsget != null) {
                seedNum = pointsget.size();
            }
            mv_main_camera.setmAllPoint(pointsget);

            Toast.makeText(MainActivity.this, "个数：" + seedNumsize, Toast.LENGTH_SHORT).show();


        }

    }

    public Bitmap getBitmapFromByte(byte[] imgByte) {
        InputStream input = null;
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        input = new ByteArrayInputStream(imgByte);
        SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(
                input, null, options));
        bitmap = (Bitmap) softRef.get();
        if (imgByte != null) {
            imgByte = null;
        }

        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bitmap;

    }

    private Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(1 / scalex, 1 / scaley); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }


}
