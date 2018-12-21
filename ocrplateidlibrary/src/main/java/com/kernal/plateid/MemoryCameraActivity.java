package com.kernal.plateid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.format.Time;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import utills.CameraFragment;
import utills.Utils;
import view.VerticalSeekBar;
import view.ViewfinderView;

/**
 *
 *
 * 项目名称：plate_id_sample_service 类名称：MemoryCameraActivity 类描述： 视频扫描界面 扫描车牌并识别
 * （与视频流的拍照识别同一界面） 创建人：张志朋 创建时间：2016-1-29 上午10:55:28 修改人：user 修改时间：2016-1-29
 * 上午10:55:28 修改备注：
 *
 * @version
 *
 */
public class MemoryCameraActivity extends Activity {

	private ImageButton back_btn, flash_btn, back, take_pic;
	private ViewfinderView myview;
	private RelativeLayout re;
	private int width, height;
	private String number = "", color = "";
	private Vibrator mVibrator;
	private PlateRecognitionParameter prp = new PlateRecognitionParameter();;
	private boolean recogType;// 记录进入此界面时是拍照识别还是视频识别 true:视频识别 false:拍照识别
	private String path;// 圖片保存的路徑
	private SensorManager sensorManager;
	private float x,y,z;
	//向左旋转
	public boolean Rotate_left = false;
	//正向旋转
	public boolean Rotate_top = true;
	//向右旋转
	public boolean Rotate_right = false;
	//倒置旋转
	public boolean Rotate_bottom = false;
	private CameraFragment fragment;
	private byte[] feedbackData;
	public int[] areas = new int[4];
	private SeekBar seekBar;
	private VerticalSeekBar verticalSeekBar;
	private LayoutParams layoutParams;
	private int recordProgress;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_carmera);
		recogType = getIntent().getBooleanExtra("camera", false);
		RecogService.initializeType = recogType;
		Point srcPixs=Utils.getScreenSize(this);
		width=srcPixs.x;
		height=srcPixs.y;
		findiew();
		initRecogView();
	}

	/**
	 *
	 * @param feedbackData  被识别的帧数据
	 */
	public void SendfeedbackData(byte[] feedbackData) {
		this.feedbackData = feedbackData;
	}

	void initRecogView(){
		GetScreenDirection();
		if (!recogType) {
			fragment.setRecogModle(true,false);
			// 拍照按钮
			take_pic.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					fragment.setRecogModle(true,true);
				}

			});
		}
	}
	@SuppressLint("NewApi")
	private void findiew() {
		// TODO Auto-generated method stub
		flash_btn = (ImageButton) findViewById(R.id.flash_camera);
		back = (ImageButton) findViewById(R.id.back);
		take_pic = (ImageButton) findViewById(R.id.take_pic_btn);
		re = (RelativeLayout) findViewById(R.id.memory);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		verticalSeekBar = (VerticalSeekBar) findViewById(R.id.verticalSeekBar);
		fragment = (CameraFragment) getFragmentManager().findFragmentById(R.id.sampleFragment);
		if (recogType) {
			take_pic.setVisibility(View.GONE);
		} else {
			take_pic.setVisibility(View.VISIBLE);
		}
		int back_w;
		int back_h;
		int flash_w;
		int flash_h;
		int Fheight;
		int take_h;
		int take_w;
		back.setVisibility(View.VISIBLE);
		back_h = (int) (height * 0.066796875);
		back_w = (int) (back_h * 1);
		layoutParams = new LayoutParams(back_w, back_h);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
				RelativeLayout.TRUE);
		layoutParams.topMargin = (int) (height*0.025);
		layoutParams.leftMargin = (int) (width * 0.050486111111111111111111111111111);
		back.setLayoutParams(layoutParams);

		flash_h = (int) (height * 0.066796875);
		flash_w = (int) (flash_h * 1);
		layoutParams = new LayoutParams(flash_w, flash_h);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
				RelativeLayout.TRUE);

		layoutParams.topMargin = (int) (height*0.025);
		layoutParams.rightMargin = (int) (width * 0.050486111111111111111111111111111);
		flash_btn.setLayoutParams(layoutParams);

		take_h = (int) (height * 0.105859375);
		take_w = (int) (take_h * 1);
		layoutParams = new LayoutParams(take_w, take_h);
		layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				RelativeLayout.TRUE);
		layoutParams.bottomMargin = (int) (height * 0.025);
		take_pic.setLayoutParams(layoutParams);

		layoutParams = new LayoutParams(width*23/24, LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		layoutParams.topMargin = (height*2/3);
		seekBar.setLayoutParams(layoutParams);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				recordProgress = progress;
				fragment.setFocallength((int)(fragment.getFocal()*progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				fragment.setRecogsuspended(true);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				fragment.setRecogsuspended(false);
			}
		});
		layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, height*7/12);
		layoutParams.leftMargin = (width/10);
		layoutParams.topMargin = ( height * 5/24);
		verticalSeekBar.setLayoutParams(layoutParams);
		verticalSeekBar.getFragment(fragment);
		verticalSeekBar.setVisibility(View.GONE);
		verticalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				recordProgress = progress;
				fragment.setFocallength((int)(fragment.getFocal()*progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				fragment.setRecogsuspended(true);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				fragment.setRecogsuspended(false);
			}
		});
				// 竖屏状态下返回按钮
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub\
				fragment.backLastActivtiy();
				MemoryCameraActivity.this.finish();

			}
		});
		// 闪光灯监听事件
		flash_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// b = true;
				// TODO Auto-generated method stub
				fragment.setFlash();

			}

		});
	}

	/**
	 * 根据重力感应  获取屏幕状态
	 */
	public void GetScreenDirection(){
		sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
	}
	private SensorEventListener listener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			x = event.values[SensorManager.DATA_X];
			y = event.values[SensorManager.DATA_Y];
			z = event.values[SensorManager.DATA_Z];
			if(x>7){   //&&y<7
				if(!Rotate_left){
					System.out.println("向左旋转");
					Rotate_bottom = false;
					Rotate_right = false;
					Rotate_top = false;
					Rotate_left =  true;
					rotateAnimation(90,90,take_pic,flash_btn,back);
					ChangView(MemoryCameraActivity.this,false);
				}

			}else if(x<-7){  //&&y<7
				if(!Rotate_right){
					System.out.println("向右旋转");
					Rotate_bottom = false;
					Rotate_right = true;
					Rotate_top = false;
					Rotate_left =  false;
					rotateAnimation(-90,90,take_pic,flash_btn,back);
					ChangView(MemoryCameraActivity.this,false);
				}

			}else if(y<-7){  //&&x<7&&x>-7
				if(!Rotate_bottom){
					System.out.println("倒置旋转");
					Rotate_bottom = true;
					Rotate_right = false;
					Rotate_top = false;
					Rotate_left =  false;
					rotateAnimation(180,90,take_pic,flash_btn,back);
					ChangView(MemoryCameraActivity.this,true);
				}
			}else if(y>7){
				if(!Rotate_top){
					System.out.println("竖屏状态");
					Rotate_bottom = false;
					Rotate_right = false;
					Rotate_top =true;
					Rotate_left =  false;
					rotateAnimation(0,0,take_pic,flash_btn,back);
					ChangView(MemoryCameraActivity.this,true);
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

	/***
	 * 旋转动画
	 * @param toDegrees
	 * 旋转的结束角度
	 * @param
	 *
	 */
	private void rotateAnimation(int toDegrees,int toDegrees2, View view1,View view2,View view3 ) {
		view1.animate().rotation(toDegrees).setDuration(500).start();
		view2.animate().rotation(toDegrees).setDuration(500).start();
		view3.animate().rotation(toDegrees2).setDuration(500).start();
	}

	/**
	 *
	 * @param context   改变屏幕布局  根据横竖屏状态修改布局
	 * @param isPortrait
     */
	public void ChangView(Context context,boolean isPortrait){
		fragment.ChangeState(Rotate_left,Rotate_right,Rotate_top,Rotate_bottom,isPortrait);
		if(Rotate_left){
			seekBar.setVisibility(View.GONE);
			verticalSeekBar.setVisibility(View.VISIBLE);
			layoutParams.leftMargin = (width/10);
			verticalSeekBar.setLayoutParams(layoutParams);
			verticalSeekBar.setProgress(recordProgress);
		}else if(Rotate_right){
			seekBar.setVisibility(View.GONE);
			verticalSeekBar.setVisibility(View.VISIBLE);
			layoutParams.leftMargin = (width*4/5);
			verticalSeekBar.setLayoutParams(layoutParams);
			verticalSeekBar.setProgress(recordProgress);
		}else{
			seekBar.setVisibility(View.VISIBLE);
			verticalSeekBar.setVisibility(View.GONE);
			seekBar.setProgress(recordProgress);
		}

	}
	/**
	 * 拿到结果之后的处理逻辑
	 * @Title: getResult
	 * @Description: TODO(获取结果)
	 * @param @param fieldvalue 调用识别接口返回的数据
	 * @return void 返回类型
	 * @throwsbyte[]picdata
	 */

	public void getResult(String[] fieldvalue,String path) {
		     this.path=path;
			if (fieldvalue[0] != null && !fieldvalue[0].equals("")) {
				/**
				 * 识别到车牌之后的处理方法
				 */
				String []resultString =  fieldvalue[0].split(";");
				String []resultColor = fieldvalue[1].split(";");
				int length = resultString.length;
				if (length == 1) {
					mVibrator = (Vibrator) getApplication()
							.getSystemService(
									Service.VIBRATOR_SERVICE);
					mVibrator.vibrate(100);
					number = fieldvalue[0];
          sendCarNo(number);
					MemoryCameraActivity.this.finish();
				}else {
					String itemString = "";
					String itemColor = "";
					mVibrator = (Vibrator) getApplication()
							.getSystemService(
									Service.VIBRATOR_SERVICE);
					mVibrator.vibrate(100);

					for (int i = 0; i < length; i++) {
						itemString = fieldvalue[0];
						itemColor = fieldvalue[1];
						resultString = itemString.split(";");
						resultColor =  itemColor.split(";");
						number += resultString[i] + ";\n";
						color += resultColor[i] + ";\n";
						itemString = fieldvalue[11];
						resultString = itemString.split(";");
					}
          sendCarNo(number);
					MemoryCameraActivity.this.finish();

				}
			} else{
			    // 未检测到车牌时执行下列代码
				//预览识别执行下列代码 不是预览识别 不做处理等待下一帧
				mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
				mVibrator.vibrate(100);
				number = fieldvalue[0];
				color = fieldvalue[3];
				if (fieldvalue[0] == null) {
					number = "null";
				}
				if (fieldvalue[1] == null) {
					color = "null";
				}
				int left = this.areas[0];
				int top = this.areas[1];
				int w = this.areas[2] - this.areas[0];
				int h = this.areas[3] - this.areas[1];
        sendCarNo(number);
				MemoryCameraActivity.this.finish();

			}
	}
  final int requestCodeOcr = 1001;
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == requestCodeOcr && resultCode == RESULT_OK){
      String carNo = data.getStringExtra("carNo");
      Intent old = getIntent();
      old.putExtra("carNo", carNo);
      setResult(RESULT_OK, old);
      finish();
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			fragment.backLastActivtiy();
		}
		return super.onKeyDown(keyCode, event);
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (sensorManager != null) {
			sensorManager.unregisterListener(listener);
		}
		super.onDestroy();
	}
  private void sendCarNo(String carNo){
    Intent old = getIntent();
    old.putExtra("carNo", carNo);
    setResult(RESULT_OK, old);
    finish();
  }

}
