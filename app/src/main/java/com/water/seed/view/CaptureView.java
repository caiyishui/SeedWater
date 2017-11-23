package com.water.seed.view;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * 录像控件
 * 
 * @author lwh
 */
public class CaptureView extends SurfaceView {
	// sufaceview的关加
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Context context;
	private static final int MY_PERMISSIONS_REQUEST_CALL_CAMERA = 1;//请求码，自己定义
	

	@SuppressWarnings("deprecation")
	public CaptureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
		mHolder = getHolder();
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.addCallback(new CaptureCallback());
	}

	/**
	 * 打开默认摄像头（后摄像头），一般Android手机必须有一个后置摄像头，而前摄像头不一定有
	 * 
	 * @return
	 */
	public synchronized Camera openCamera() {
	
//            callCamera();
            if (mCamera == null) {
            	mCamera = Camera.open();
            }
            return mCamera;
      
		
		
	}

    public void onRequestPermissionsResultcallback() {

        		//成功，开启摄像头
        		if (mCamera == null) {
        			mCamera = openCamera();
        		}
        		try {
        			mCamera.setPreviewDisplay(mHolder);
        			// 创建surface的时候初始化相机
        			initCamera();
        			
        			mCamera.startPreview();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        	
       
    }
	private class CaptureCallback implements SurfaceHolder.Callback {
		/**
		 * 预览界面创建时调用，每次界面改变后都会重新创建，需要获取相机资源并设置SurfaceHolder。
		 *@author  water
		 * QQ376596444
		 *2016年8月17日 
		 * @version 1.0
		 */
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
//			//检查权限
//	        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//	        //如果没有授权，则请求授权
//	            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CALL_CAMERA);
//	        } else { 
//			 if (Build.VERSION.SDK_INT >= 23) {  
//		            /** 
//		             * 请求权限是一个异步任务  不是立即请求就能得到结果 在结果回调中返回 
//		             */  
//				 ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);  
//		        } else { 
					if (mCamera == null) {
						mCamera = openCamera();
					}
					try {
						mCamera.setPreviewDisplay(mHolder);
						// 创建surface的时候初始化相机
						initCamera();
		
						mCamera.startPreview();
					} catch (IOException e) {
						e.printStackTrace();
					}
//	        }

		}

		
		/**
		 * 当surfaceview改变的时候就设置自动对焦
		 * 预览界面发生变化时调用，每次界面发生变化之后需要重新启动预览。
		 * @author water QQ376596444 2016年8月17日
		 * @version 1.0
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			// 实现自动对焦
			if(mCamera!=null){
				mCamera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						if (success) {
							initCamera();// 实现相机的参数初始化
								
								camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
						}
					}
	
				});
			}
		}

		/**
		 * 当surfaceview销毁的时候调用移除监听，不然会出bug释放资源
		 * 预览销毁时调用，停止预览，释放相应资源。
		 * @author water QQ376596444 2016年8月17日
		 * @version 1.0
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mCamera != null) {
				holder.removeCallback(this);
				mCamera.stopPreview();
				mCamera.release();// 释放资源，以便提高下次使用的效率
				mCamera = null;
			}
		}

		// @Override
		// public void onAutoFocus(boolean success, Camera camera) {
		// }

	}
	/**
	 * 初始化相机
	 * 
	 * @author water QQ376596444 2016年8月17日
	 * @version 1.0
	 */
	@SuppressWarnings("deprecation")

	private void initCamera() {
		Parameters params = mCamera.getParameters();
		params.setPictureFormat(PixelFormat.JPEG);
		
	


		// 设置相机持续对焦
		if (params.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
		setDispaly(params, mCamera);
		mCamera.setParameters(params);

		// mCamera.autoFocus(this);
		// mCamera.setDisplayOrientation(90);
		mCamera.startPreview();// 开始预览
		mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
	}

	/**
	 * 重置相机
	 */
	public void resetCamera() {
		//  停止预览的时候已经致空了
		if (mCamera == null) {
			mCamera = openCamera();
		}
		// 添加管家的监听，之前停止预览的时候移除了
		if (mHolder!= null) {
			mHolder.addCallback(new CaptureCallback());
		}
		//开启预览
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.startPreview();
		}
	}



	// 控制图像的正确显示方向
	@SuppressWarnings("deprecation")
	private void setDispaly(Parameters parameters, Camera camera) {
		if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
			setDisplayOrientation(camera, 0);
		} else {
			parameters.setRotation(0);
		}
	}
	// 实现的图像的正确显示
	private void setDisplayOrientation(Camera camera, int i) {
		Method downPolymorphic;
		try {
			downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
			if (downPolymorphic != null) {
				downPolymorphic.invoke(camera, new Object[] { i });
			}
		} catch (Exception e) {
			Log.e("Came_e", "图像出错");
		}
	}

	
}
