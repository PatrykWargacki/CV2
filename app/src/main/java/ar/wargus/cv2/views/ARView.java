package ar.wargus.cv2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import org.opencv.core.Rect2d;

public class ARView extends View {
	private SparseArray<Rect2d> objectsToDraw = new SparseArray<>();
	private Paint paint;
	private float widthTransform = 1;
	private float heightTransform = 1;
	
	public ARView(Context context) {
		super(context);
		init();
	}
	
	public ARView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ARView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public ARView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}
	
	private void init(){
		setWillNotDraw(false);
		paint = new Paint();
//		paint.setARGB(0, 0, 255, 0);
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(5f);
		paint.setStyle(Paint.Style.STROKE);
	}
	
//	private float[] sceneCornersData;
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawLine(0, 0,
		                100, 100,
		                paint);
		
		for (int i = 0; i < objectsToDraw.size(); i++) {
			Rect2d rect = objectsToDraw.valueAt(i);
			if(rect == null) continue;
			canvas.drawRect((float) rect.x,
			                (float) rect.y,
			                (float) (rect.x + rect.width),
			                (float) (rect.y + rect.height),
			                paint);
//			Mat objectCorner = objectsToDraw[i].second.clone();
//			 transformacja z lokalizacji na obraziePrzetworzonym do obrazuNaPodglądzie
//			sceneCornersData = new float[(int) (objectCorner.total() * objectCorner.channels())];
//			objectCorner.get(0, 0, sceneCornersData);
			
//			for(int o = 0; o < sceneCornersData.length%2; o++){
//				sceneCornersData[o]     *= widthTransform;
//				sceneCornersData[o + 1] *= heightTransform;
//			}
//			canvas.drawLines(sceneCornersData,
//			                 paint);
//			canvas.drawLine(sceneCornersData[0], sceneCornersData[1],
//			                sceneCornersData[2], sceneCornersData[3],
//			                paint);
//			canvas.drawLine(sceneCornersData[2], sceneCornersData[3],
//			                sceneCornersData[4], sceneCornersData[5],
//			                paint);
//			canvas.drawLine(sceneCornersData[4], sceneCornersData[5],
//			                sceneCornersData[6], sceneCornersData[7],
//			                paint);
//			canvas.drawLine(sceneCornersData[6], sceneCornersData[7],
//			                sceneCornersData[0], sceneCornersData[1],
//			                paint);
		}
	}
	
	public void setObjectsToDraw(SparseArray<Rect2d> objectsToDraw) {
		this.objectsToDraw = objectsToDraw;
		this.invalidate();
	}
	
	public void setSizeTransforms(float widthTransform,
	                              float heightTransform) {
		this.widthTransform = widthTransform;
		this.heightTransform = heightTransform;
	}
}
