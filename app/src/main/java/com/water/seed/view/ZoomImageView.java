package com.water.seed.view;
import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.water.seed.Toast;

/**
 * 
 */
public class ZoomImageView extends AppCompatImageView implements OnScaleGestureListener,
		OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener

{
	///------------
	//点击精度，提高删除点击的时候的成功率，越大执行的任务越重
		private final int ACCURACY_CLICK=30;
		//点的半径
		private final int RADIUS=6;
		//点击过滤的精度
		private final int filter_CLICK=20;	
		private List<Point> mAllPoint;
	
		public List<Point> getmAllPoint() {
			return mAllPoint;
		}
		public float scalex=1;
		public float scaley=1;
		private boolean type=true;
		public void setmAllPoint(List<Point> mAllPoint) {
			this.mAllPoint = mAllPoint;
		}		
		public boolean isType() {
			return type;
		}
		public void setType(boolean type) {
			this.type = type;
		}
	
	
	//=-----------
	private static final String TAG = ZoomImageView.class.getSimpleName();
	public static final float SCALE_MAX = 4.0f;
	private static final float SCALE_MID = 2.0f;
	private Paint mPaint;//画圆的画笔

	/**
	 * 初始化时的缩放比例，如果图片宽或高大于屏幕，此值将小于0
	 */
	private float initScale = 1.0f;
	private boolean once = true;

	/**
	 * 用于存放矩阵的9个值
	 */
	private final float[] matrixValues = new float[9];

	/**
	 * 缩放的手势检测
	 */
	private ScaleGestureDetector mScaleGestureDetector = null;
	private final Matrix mScaleMatrix = new Matrix();

	/**
	 * 用于双击检测
	 */
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;

	private int mTouchSlop;

	private float mLastX;
	private float mLastY;

	private boolean isCanDrag;
	private int lastPointerCount;

	private boolean isCheckTopAndBottom = true;
	private boolean isCheckLeftAndRight = true;

	public ZoomImageView(Context context)
	{
		this(context, null);
	}

	public ZoomImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		super.setScaleType(ScaleType.MATRIX);
		mPaint = new Paint();
		mAllPoint = new ArrayList<Point>();
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.RED);
		mGestureDetector = new GestureDetector(context,
				new SimpleOnGestureListener()
				{
					@Override
					public boolean onDoubleTap(MotionEvent e)
					{
						if (isAutoScale == true)
							return true;

						float x = e.getX();
						float y = e.getY();
						Log.e("DoubleTap", getScale() + " , " + initScale);
						if (getScale() < SCALE_MID)
						{
							ZoomImageView.this.postDelayed(
									new AutoScaleRunnable(SCALE_MID, x, y), 16);
							isAutoScale = true;
						} else if (getScale() >= SCALE_MID
								&& getScale() < SCALE_MAX)
						{
							ZoomImageView.this.postDelayed(
									new AutoScaleRunnable(SCALE_MAX, x, y), 16);
							isAutoScale = true;
						} else
						{
							ZoomImageView.this.postDelayed(
									new AutoScaleRunnable(initScale, x, y), 16);
							isAutoScale = true;
						}

						return true;
					}
					@Override
					public boolean onSingleTapConfirmed(MotionEvent e) {
						// TODO Auto-generated method stub
						RectF rectf=getMatrixRectF();
						Log.i("clikeconfim",e.getX()+",---"+e.getY()+"当前比例缩放"+getScale()+"---left"+rectf.left+"---up"+rectf.top+"---right"+rectf.right+"---bottom"+rectf.bottom);
						float x_rel=(e.getX()-rectf.left);
						float y_rel=(e.getY()-rectf.top);
						float x_point=x_rel/getScale();
						float y_point=y_rel/getScale();						
						Point p = new Point((int)x_point,(int)y_point);						
						if(type){			
							if(!hasPoint(p)){						
								addPoint(p);					
								invalidate();
								numlistener.refush();
								 return true;  
							}else{
								 return true;  
							}
						}else{
							if(checkNearestPoint(p)){				
								removePoint(checkwhichdelete());
								invalidate();
								numlistener.refush();
								 return true;  
							}else{
								new Toast(getContext()).show("此点不存在");
								
							}										
						}
						return super.onSingleTapConfirmed(e);
					}
					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						// TODO Auto-generated method stub
						RectF rectf=getMatrixRectF();
						Log.i("clikeUp",e.getX()+",---"+e.getY()+"当前比例缩放"+getScale()+"---left"+rectf.left+"---up"+rectf.top+"---right"+rectf.right+"---bottom"+rectf.bottom);
					
						
						
						
						
						
						
					
						return super.onSingleTapUp(e);
					}
				});
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		this.setOnTouchListener(this);
	}

	/**
	 * 自动缩放的任务
	 * 
	 * @author zhy
	 * 
	 */
	private class AutoScaleRunnable implements Runnable
	{
		static final float BIGGER = 1.07f;
		static final float SMALLER = 0.93f;
		private float mTargetScale;
		private float tmpScale;

		/**
		 * 缩放的中心
		 */
		private float x;
		private float y;

		/**
		 * 传入目标缩放值，根据目标值与当前值，判断应该放大还是缩小
		 * 
		 * @param targetScale
		 */
		public AutoScaleRunnable(float targetScale, float x, float y)
		{
			this.mTargetScale = targetScale;
			this.x = x;
			this.y = y;
			if (getScale() < mTargetScale)
			{
				tmpScale = BIGGER;
			} else
			{
				tmpScale = SMALLER;
			}

		}

		@Override
		public void run()
		{
			// 进行缩放
			mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);

			final float currentScale = getScale();
			// 如果值在合法范围内，继续缩放
			if (((tmpScale > 1f) && (currentScale < mTargetScale))
					|| ((tmpScale < 1f) && (mTargetScale < currentScale)))
			{
				ZoomImageView.this.postDelayed(this, 16);
			} else
			// 设置为目标的缩放比例
			{
				final float deltaScale = mTargetScale / currentScale;
				mScaleMatrix.postScale(deltaScale, deltaScale, x, y);
				checkBorderAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}

		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onScale(ScaleGestureDetector detector)
	{
		float scale = getScale();
		float scaleFactor = detector.getScaleFactor();

		if (getDrawable() == null)
			return true;

		/**
		 * 缩放的范围控制
		 */
		if ((scale < SCALE_MAX && scaleFactor > 1.0f)
				|| (scale > initScale && scaleFactor < 1.0f))
		{
			/**
			 * 最大值最小值判断
			 */
			if (scaleFactor * scale < initScale)
			{
				scaleFactor = initScale / scale;
			}
			if (scaleFactor * scale > SCALE_MAX)
			{
				scaleFactor = SCALE_MAX / scale;
			}
			/**
			 * 设置缩放比例
			 */
			mScaleMatrix.postScale(scaleFactor, scaleFactor,
					detector.getFocusX(), detector.getFocusY());
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
		}
		return true;

	}

	/**
	 * 在缩放时，进行图片显示范围的控制
	 */
	private void checkBorderAndCenterWhenScale()
	{

		RectF rect = getMatrixRectF();
		float deltaX = 0;
		float deltaY = 0;

		int width = getWidth();
		int height = getHeight();

		// 如果宽或高大于屏幕，则控制范围
		if (rect.width() >= width)
		{
			if (rect.left > 0)
			{
				deltaX = -rect.left;
			}
			if (rect.right < width)
			{
				deltaX = width - rect.right;
			}
		}
		if (rect.height() >= height)
		{
			if (rect.top > 0)
			{
				deltaY = -rect.top;
			}
			if (rect.bottom < height)
			{
				deltaY = height - rect.bottom;
			}
		}
		// 如果宽或高小于屏幕，则让其居中
		if (rect.width() < width)
		{
			deltaX = width * 0.5f - rect.right + 0.5f * rect.width();
		}
		if (rect.height() < height)
		{
			deltaY = height * 0.5f - rect.bottom + 0.5f * rect.height();
		}
		Log.e(TAG, "deltaX = " + deltaX + " , deltaY = " + deltaY);

		mScaleMatrix.postTranslate(deltaX, deltaY);

	}

	/**
	 * 根据当前图片的Matrix获得图片的范围
	 * 
	 * @return
	 */
	private RectF getMatrixRectF()
	{
		Matrix matrix = mScaleMatrix;
		RectF rect = new RectF();
		Drawable d = getDrawable();
		if (null != d)
		{
			rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rect);
		}
		return rect;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector)
	{
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector)
	{
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{

		if (mGestureDetector.onTouchEvent(event))
			return true;
		mScaleGestureDetector.onTouchEvent(event);

		float x = 0, y = 0;
		// 拿到触摸点的个数
		final int pointerCount = event.getPointerCount();
		// 得到多个触摸点的x与y均值
		for (int i = 0; i < pointerCount; i++)
		{
			x += event.getX(i);
			y += event.getY(i);
		}
		x = x / pointerCount;
		y = y / pointerCount;

		/**
		 * 每当触摸点发生变化时，重置mLasX , mLastY
		 */
		if (pointerCount != lastPointerCount)
		{
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}

		lastPointerCount = pointerCount;
		RectF rectF = getMatrixRectF();
		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			if (rectF.width() > getWidth() || rectF.height() > getHeight())
			{
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (rectF.width() > getWidth() || rectF.height() > getHeight())
			{
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			Log.e(TAG, "ACTION_MOVE");
			float dx = x - mLastX;
			float dy = y - mLastY;

			if (!isCanDrag)
			{
				isCanDrag = isCanDrag(dx, dy);
			}
			if (isCanDrag)
			{

				if (getDrawable() != null)
				{
					// if (getMatrixRectF().left == 0 && dx > 0)
					// {
					// getParent().requestDisallowInterceptTouchEvent(false);
					// }
					//
					// if (getMatrixRectF().right == getWidth() && dx < 0)
					// {
					// getParent().requestDisallowInterceptTouchEvent(false);
					// }
					isCheckLeftAndRight = isCheckTopAndBottom = true;
					// 如果宽度小于屏幕宽度，则禁止左右移动
					if (rectF.width() < getWidth())
					{
						dx = 0;
						isCheckLeftAndRight = false;
					}
					// 如果高度小雨屏幕高度，则禁止上下移动
					if (rectF.height() < getHeight())
					{
						dy = 0;
						isCheckTopAndBottom = false;
					}
					

					mScaleMatrix.postTranslate(dx, dy);
					checkMatrixBounds();
					setImageMatrix(mScaleMatrix);
				}
			}
			mLastX = x;
			mLastY = y;
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			Log.e(TAG, "ACTION_UP");
			lastPointerCount = 0;
			break;
		}

		return true;
	}

	/**
	 * 获得当前的缩放比例
	 * 
	 * @return
	 */
	public final float getScale()
	{
		mScaleMatrix.getValues(matrixValues);
		return matrixValues[Matrix.MSCALE_X];
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	@Override
	public void onGlobalLayout()
	{
		if (once)
		{
			Drawable d = getDrawable();
			if (d == null)
				return;
			Log.e(TAG, d.getIntrinsicWidth() + " , " + d.getIntrinsicHeight());
			int width = getWidth();
			int height = getHeight();
			// 拿到图片的宽和高
			int dw = d.getIntrinsicWidth();
			int dh = d.getIntrinsicHeight();
			float scale = 1.0f;
			// 如果图片的宽或者高大于屏幕，则缩放至屏幕的宽或者高
			if (dw > width && dh <= height)
			{
				scale = width * 1.0f / dw;
			}
			if (dh > height && dw <= width)
			{
				scale = height * 1.0f / dh;
			}
			// 如果宽和高都大于屏幕，则让其按按比例适应屏幕大小
			if (dw > width && dh > height)
			{
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}
			initScale = scale;

			Log.e(TAG, "initScale = " + initScale);
			mScaleMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
			mScaleMatrix.postScale(scale, scale, getWidth() / 2,
					getHeight() / 2);
			// 图片移动至屏幕中心
			setImageMatrix(mScaleMatrix);
			once = false;
		}

	}

	/**
	 * 移动时，进行边界判断，主要判断宽或高大于屏幕的
	 */
	private void checkMatrixBounds()
	{
		RectF rect = getMatrixRectF();

		float deltaX = 0, deltaY = 0;
		final float viewWidth = getWidth();
		final float viewHeight = getHeight();
		// 判断移动或缩放后，图片显示是否超出屏幕边界
		if (rect.top > 0 && isCheckTopAndBottom)
		{
			deltaY = -rect.top;
		}
		if (rect.bottom < viewHeight && isCheckTopAndBottom)
		{
			deltaY = viewHeight - rect.bottom;
		}
		if (rect.left > 0 && isCheckLeftAndRight)
		{
			deltaX = -rect.left;
		}
		if (rect.right < viewWidth && isCheckLeftAndRight)
		{
			deltaX = viewWidth - rect.right;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	/**
	 * 是否是推动行为
	 * 
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isCanDrag(float dx, float dy)
	{
		return Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		Log.i("clikeUp","刷新了");
		super.onDraw(canvas);
		float nowscale=getScale();
		RectF rectf=getMatrixRectF();
		Log.i("info","scals"+nowscale+"rectf.left"+rectf.left+"rectf.top"+rectf.top);
	
		if (mAllPoint.size() != 0) {
			int cw=canvas.getWidth();
			int ch=canvas.getHeight();
			int len=mAllPoint.size();
			if (len!= 0) {
				for(int i=0;i<len;i++){
						Point point=mAllPoint.get(i) ;
						float x=point.x*nowscale+rectf.left;
						float y=point.y*nowscale+rectf.top;
						canvas.drawCircle(x,y,RADIUS,mPaint);
						
//						canvas.drawCircle(point.x/scalex,point.y/scaley,RADIUS,mPaint);
				}
			}
			Log.i("info","canvas宽度"+cw+"canvas高度"+ch+"w salce"+scalex+"y salce"+scaley);
		}
	}
	
	//-----------------
	/**
	 * 判断这个点是否存在
	 * 
	 * @param point
	 * @return
	 */
	public boolean hasPoint(Point point) {
		if (mAllPoint != null){
			int len =mAllPoint.size();
			for (int i=0;i<len;i++) {
				Point nowpoint =mAllPoint.get(i);
				if(point.x==nowpoint.x&&point.y==nowpoint.y){					
						new Toast(getContext()).show("此点已存在");
						return true;							
				}else{
					continue;
				}				
			}
		}
		return false;
	}
	private List<Point> maybes=new ArrayList<Point>();

	/**
	 * 检测最近的点
	 * 
	 * @param p
	 * @return
	 */
	private boolean checkNearestPoint(Point p) {
		maybes.clear();
		maybes.add(p);
		if (mAllPoint != null) {
			int len =mAllPoint.size();
			for (int i=0;i<len;i++) {

				Point nowpoint =mAllPoint.get(i);
				if(Math.abs(p.x-nowpoint.x)<ACCURACY_CLICK&&Math.abs(p.y-nowpoint.y)<ACCURACY_CLICK){					
						if(maybes==null){
							maybes=new ArrayList<Point>();
						}
					    maybes.add(nowpoint);					
						return true;									
				}else{
					continue;
				}				
			}
		}
		return false;		
	}	
	private Point checkwhichdelete(){
		int len=maybes.size();
		Point max = null;
		Point min=maybes.get(0);
		if(len==2){
			return maybes.get(1);
		}
		for(int i=1;i<len-1;i++){
			
			if(max==null){				
				max=maybes.get(1);
			}
			Point max2=maybes.get(i+1);
			Point linshi=calInterPointDistance(min,max, max2);
			max.set(linshi.x,linshi.y);
			
		}				
		return max;
	}
	/**
	 * 计算两点间的距离，两点之间，直线最短
	 * 
	 * @param p
	 * @param p2
	 * @return
	 */
	public Point calInterPointDistance(Point p, Point p2, Point p3) {
		return (((int) Math.sqrt((p.x - p2.x) * (p.x - p2.x) + (p.y - p2.y) * (p.y - p2.y)))-((int) Math.sqrt((p.x - p3.x) * (p.x - p3.x) + (p.y - p3.y) * (p.y - p3.y))))>0?p3:p2;
	}
	public void addPoint(Point p) {
		if(mAllPoint!=null){			
			mAllPoint.add(p);
		}
	}
	public void removePoint(int position) {
		if(mAllPoint!=null){
		mAllPoint.remove(position);
		}
	}
	public void removePoint(Point p) {
		if(mAllPoint!=null){
		mAllPoint.remove(p);
		}
	}
	private Numlistener numlistener;
     //第二步，设置接口方法
	 public void setOnChangNum(Numlistener numlistener){
         this.numlistener = numlistener;
	 }	  
	
	 public interface Numlistener{
		 public void refush();
	 };
	

}
