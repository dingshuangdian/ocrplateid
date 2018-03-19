package view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;



public final class ViewfinderView extends View {
	private final Paint paint;
	private final Paint paintLine;
	public Rect frame;
//	int w, h;
	private boolean boo;
	public     int  length =0;
	public int t, b, l, r;
	public int width,height,preWidth,preHeight, surfaceWidth,surfaceHeight;
	public ViewfinderView(Context context, boolean boo, int preWidth,int preHeight) {
		super(context);
//		this.w = w;
//		this.h = h;

		this.boo = boo;
		this.preHeight = preHeight;
		this.preWidth = preWidth;
		paint = new Paint();
		paintLine = new Paint();
	
	}

	@Override
	public void onDraw(Canvas canvas) {
		 width = canvas.getWidth();
		 height = canvas.getHeight();
		 //这个矩形就是中间的扫描框
		
		
		if (boo) {
//			if(height<1080||height>1620){
//				length = height/4;
//			}else{
//				length = 250;
//			}	
			 surfaceWidth = height*preWidth/preHeight;
			
//			
				//确保识别区域在400像素点内时  扫描框的宽度不能采用500像素点了，则按照规则自行获取
				 if((350*surfaceWidth/preWidth)<500){
//					 System.out.println("确保识别区域在400像素点内时  扫描框的宽度不能采用500像素点了，则按照规则自行获取");
					 length = (350*surfaceWidth/preWidth)/2;
				 }else	if(500<=(350*surfaceWidth/preWidth)&&(350*surfaceWidth/preWidth)<=(surfaceWidth/2)){
					//确保识别区域在400像素点内时  扫描框的宽度大于500而小于二分之一surfaceView时  扫描框选用500像素点
//					
					 length=250;
				 }else if((surfaceWidth/2)<(350*surfaceWidth/preWidth)&&(350*surfaceWidth/preWidth)<surfaceWidth){
					//确保识别区域在400像素点内时, 扫描框的宽度大于小于二分之一surfaceView 而小于等于surfaceView时  扫描框宽度根据计算规则自动获取
//					
					 length = (350*surfaceWidth/preWidth)/2;
				 }else if((350*surfaceWidth/preWidth)>=surfaceWidth){
					 length = surfaceWidth/2;
				 }
		} else {
//			if(width<1080||width>1620){
//				length = width/4;
//			}else{
//				length=250;	
//			}

//			 System.out.println("竖屏   surfaceView 的高："+surfaceHeight +" 宽："+surfaceWidth);
				//确保识别区域在400像素点内时  扫描框的宽度不能采用500像素点了，则按照规则自行获取
				 if((350*width/preHeight)<500){
//					 System.out.println("确保识别区域在400像素点内时  扫描框的宽度不能采用500像素点了，则按照规则自行获取");
					 length = (350*width/preHeight)/2;
				 }else	if(500<=(350*width/preHeight)&&(350*width/preHeight)<=(width/2)){
					//确保识别区域在400像素点内时  扫描框的宽度大于500而小于二分之一surfaceView时  扫描框选用500像素点
//					 System.out.println("确保识别区域在400像素点内时  扫描框的宽度大于500而小于二分之一surfaceView时  扫描框选用500像素点");
					 length=250;
				 }else if((width/2)<(350*width/preHeight)&&(350*width/preHeight)<width){
					//确保识别区域在400像素点内时, 扫描框的宽度大于小于二分之一surfaceView 而小于等于surfaceView时  扫描框宽度根据计算规则自动获取
//					 System.out.println("确保识别区域在400像素点内时, 扫描框的宽度大于二分之一surfaceView 而小于等于surfaceView时  扫描框宽度根据计算规则自动获取");
					 length = (350*width/preHeight)/2;
				 }else if((350*width/preHeight)>=width){
					 length = width/2;
				 }
		}
	
//		 if(((width*preHeight)/(2*height))<=400){
//			 System.out.println("扫描框铺满surfaceView时  识别区域也不超过400像素");
//			 length=width/2;
//		 }
//		l = w/2-length;
//		r = w/2+length;
//		t = h/2-length;
//		b = h/2+length;
		l = width/2-length;
		r = width/2+length;
		t = height/2-length;
		b = height/2+length;
		frame = new Rect(l, t, r, b);
		// 画阴影部分，分四部分，从屏幕上方到扫描框的上方，从屏幕左边到扫描框的左边
		// 从扫描框右边到屏幕右边，从扫描框底部到屏幕底部
		paint.setColor(Color.argb(128, 0, 0, 0));
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		paintLine.setColor(Color.rgb(0, 255, 0));
		paintLine.setStrokeWidth(4);
		paintLine.setAntiAlias(true);
		canvas.drawLine(l, t, l +50, t, paintLine);
		canvas.drawLine(l, t, l, t + 50, paintLine);
		canvas.drawLine(r, t, r - 50, t, paintLine);
		canvas.drawLine(r, t, r, t + 50, paintLine);
		canvas.drawLine(l, b, l + 50, b, paintLine);
		canvas.drawLine(l, b, l, b - 50, paintLine);
		canvas.drawLine(r, b, r - 50, b, paintLine);
		canvas.drawLine(r, b, r, b - 50, paintLine);
		// }

		if (frame == null) {
			return;
		}

	}

}
