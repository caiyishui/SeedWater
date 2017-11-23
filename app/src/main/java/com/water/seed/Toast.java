package com.water.seed;

import android.content.Context;

/**
 * 自定义吐司
 * @author lwh
 */
public class Toast {
	
	private android.widget.Toast mToast;
	private Context mContext;
	
	public Toast(Context context){
		this.mContext = context;
	}
	
	public void show(String msg) {
		if (mToast == null) {
			mToast = android.widget.Toast.makeText(mContext, msg, android.widget.Toast.LENGTH_SHORT);
		} else {
			mToast.setText(msg);
		}
		mToast.show();
	}
	
	public void show(int resId){
		if (mToast == null) {
			mToast = android.widget.Toast.makeText(mContext, resId, android.widget.Toast.LENGTH_SHORT);
		} else {
			mToast.setText(resId);
		}
		mToast.show();
	}
	public void show(String msg,int duration) {
		if (mToast == null) {
			mToast = android.widget.Toast.makeText(mContext, msg,duration);
		} else {
			mToast.setText(msg);
		}
		mToast.show();
	}
	
	public void show(int resId,int duration){
		if (mToast == null) {
			mToast = android.widget.Toast.makeText(mContext, resId,duration);
		} else {
			mToast.setText(resId);
		}
		mToast.show();
	}
}
