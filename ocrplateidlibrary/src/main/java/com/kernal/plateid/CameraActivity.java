package com.kernal.plateid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import utills.EquipmentUtill;
import utills.FrameCapture;

/**
 * 
*    
* 项目名称：plate_id_sample_service  
* 类名称：CameraActivity  
* 类描述：   老版拍照界面   不推荐使用
* 创建人：user  
* 创建时间：2016-1-25 上午9:57:11  
* 修改人：user  
* 修改时间：2016-1-25 上午9:57:11  
* 修改备注：   
* @version    
*
 */
public class CameraActivity extends Activity implements SurfaceHolder.Callback {

	public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/kernalimage/";
	private int preWidth = 320;
	private int preHeight = 240;
	private int picWidth = 2048;
	private int picHeight = 1536;
	private Camera camera;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private ToneGenerator tone;
	private ImageView imageView;
	private Bitmap bitmap;
	private RelativeLayout rlyaout;
	private String recogPicPath;
	private int width, height;
	private boolean hasFlashLigth = false;
	private long fastClick = 0;
	private ImageButton backImage, restartImage, takeImage, recogImage, lightOn, lightOff;
	private TextView backAndRestartText, takeAndRecogText, lightText;
	private ImageView leftImage, rightImage;
	private boolean taking = false;
	private boolean recoging = false;
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private int uR;
	private int rotationWidth, rotationHeight;
	private float widthScale = (float) 1.0;
	private float heightScale = (float) 1.0;
	private RelativeLayout.LayoutParams frame_left_horizontal_params, frame_right_horizontal_params;
	private RelativeLayout.LayoutParams frame_left_vertical_params, frame_right_vertical_params;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制横屏
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		width = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
		height = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
		if(rotationHeight * 0.75 == rotationWidth || rotationWidth * 0.75 == rotationHeight){//判断是否属于4比3的分辨率并加载指定布局
			setContentView(R.layout.kernal_camera_for_4_3);
		}else{
			setContentView(R.layout.kernal_camera);
		}
		PlateProjectTool.addActivityList(CameraActivity.this);
		picWidth = readIntPreferences("PlateService", "picWidth");
		picHeight = readIntPreferences("PlateService", "picHeight");
		preWidth = readIntPreferences("PlateService", "preWidth");
		preHeight = readIntPreferences("PlateService", "preHeight");

	}

	protected void onStart() {
		super.onStart();
		// System.out.println("onstart");
		findViewAndLayout();

		showFrameImageView();
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(CameraActivity.this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		FeatureInfo[] features = this.getPackageManager().getSystemAvailableFeatures();
		for (FeatureInfo featureInfo : features) {

			if (PackageManager.FEATURE_CAMERA_FLASH.equals(featureInfo.name)) {
				hasFlashLigth = true;
			}
		}
	}

	private void findViewAndLayout() {
		rlyaout = (RelativeLayout) findViewById(R.id.rlyaout);

		imageView = (ImageView) findViewById(R.id.BimageView);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceViwe);

		backImage = (ImageButton) findViewById(R.id.backimage);
		backImage.setBackgroundResource(R.drawable.back);
		backImage.setVisibility(View.VISIBLE);
		restartImage = (ImageButton) findViewById(R.id.restartimage);
		restartImage.setBackgroundResource(R.drawable.rephotograph);
		restartImage.setVisibility(View.INVISIBLE);
		backAndRestartText = (TextView) findViewById(R.id.backandrestarttext);
		backAndRestartText.setVisibility(View.VISIBLE);
		backAndRestartText.setText("返回");
		backAndRestartText.setTextColor(Color.BLACK);
		takeImage = (ImageButton) findViewById(R.id.takeimage);
		takeImage.setBackgroundResource(R.drawable.takepic);
		takeImage.setVisibility(View.VISIBLE);
		recogImage = (ImageButton) findViewById(R.id.recogimage);
		recogImage.setBackgroundResource(R.drawable.recognition);
		recogImage.setVisibility(View.INVISIBLE);
		takeAndRecogText = (TextView) findViewById(R.id.takeandrecogtext);
		takeAndRecogText.setText("拍照");
		takeAndRecogText.setVisibility(View.VISIBLE);
		takeAndRecogText.setTextColor(Color.BLACK);
		lightOn = (ImageButton) findViewById(R.id.lightimage1);
		lightOn.setBackgroundResource(R.drawable.light1);
		lightOn.setVisibility(View.VISIBLE);
		lightOff = (ImageButton) findViewById(R.id.lightimage2);
		lightOff.setBackgroundResource(R.drawable.light2);
		lightOff.setVisibility(View.INVISIBLE);
		lightText = (TextView) findViewById(R.id.lighttext);
		lightText.setText("开闪光灯");
		lightText.setVisibility(View.VISIBLE);
		lightText.setTextColor(Color.BLACK);

		int uiRot = getWindowManager().getDefaultDisplay().getRotation();
		uR= uiRot;
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		rotationWidth = displayMetrics.widthPixels;
		rotationHeight = displayMetrics.heightPixels;
		setViewByUirot(uiRot, displayMetrics.widthPixels, displayMetrics.heightPixels);

		leftImage = (ImageView) findViewById(R.id.cameraframeleft);
		leftImage.setBackgroundResource(R.drawable.frame_left);
		rightImage = (ImageView) findViewById(R.id.cameraframeright);
		rightImage.setBackgroundResource(R.drawable.frame_right);

		int surface_width = (int) ((height * 4) / 3);
		int pic_width = readIntPreferences("PlateService", "picWidth");
		int frame_width = (160 * surface_width) / pic_width;
		if (frame_width <= (int) (height * 0.16)) {
			frame_width = (int) (height * 0.16 + 80);
		} else {
			frame_width = frame_width + 40;
		}

		int vertical_screen = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
		int margin = (int) ((vertical_screen * 1.333 - ((vertical_screen * 1.333 * 150) / picWidth)) / 2);
		int screen_pic_width = (int) ((vertical_screen * 1.333 * 150) / picWidth);
		if (vertical_screen * 0.08 >= screen_pic_width / 2) {
			margin = (int) ((vertical_screen * 1.333 - (vertical_screen * 0.08 * 2 + 20)) / 2);
		} else {
			margin = (int) ((vertical_screen * 1.333 - screen_pic_width * 0.5) / 2);
		}
		margin = margin - 45;
		if (margin < 0) {
			margin = 0;
		}
		frame_left_horizontal_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 0.08),
				(int) (vertical_screen * 0.125));
		frame_left_horizontal_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		frame_left_horizontal_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		frame_left_horizontal_params.leftMargin = margin;

		frame_right_horizontal_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 0.08),
				(int) (vertical_screen * 0.125));
		frame_right_horizontal_params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		if(rotationHeight * 0.75 == rotationWidth || rotationWidth * 0.75 == rotationHeight){//判断是否属于4比3的分辨率并分别配置布局参数
			frame_right_horizontal_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.surfaceViwe);
		}else{
			frame_right_horizontal_params.addRule(RelativeLayout.LEFT_OF, R.id.rlyaout);
		}
		frame_right_horizontal_params.rightMargin = margin;

		margin = (int) ((vertical_screen - ((vertical_screen * 150) / picWidth)) / 2);
		screen_pic_width = (int) ((vertical_screen * 150) / picWidth);
		if (vertical_screen * 0.08 >= screen_pic_width / 2) {
			margin = (int) ((vertical_screen - (vertical_screen * 0.08 * 2 + 20)) / 2);
		} else {
			margin = (int) ((vertical_screen - screen_pic_width * 0.5) / 2);
		}
		margin = margin - 20;
		if (margin < 0) {
			margin = 0;
		}
		int top_margin = (int) ((vertical_screen * 1.333 - vertical_screen * 0.125) / 2);
		frame_left_vertical_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 0.08),
				(int) (vertical_screen * 0.125));
		frame_left_vertical_params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		frame_left_vertical_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		frame_left_vertical_params.leftMargin = margin;
		frame_left_vertical_params.topMargin = top_margin;

		frame_right_vertical_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 0.08),
				(int) (vertical_screen * 0.125));
		frame_right_vertical_params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		frame_right_vertical_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		frame_right_vertical_params.rightMargin = margin;
		frame_right_vertical_params.topMargin = top_margin;

		// RelativeLayout.LayoutParams frame_left = new
		// RelativeLayout.LayoutParams((int) (height * 0.08),
		// (int) (height * 0.125));
		// frame_left.addRule(RelativeLayout.CENTER_VERTICAL,
		// RelativeLayout.TRUE);
		// frame_left.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
		// RelativeLayout.TRUE);
		// frame_left.leftMargin = margin;
		// leftImage.setLayoutParams(frame_left);
		//
		// RelativeLayout.LayoutParams frame_right = new
		// RelativeLayout.LayoutParams((int) (height * 0.08),
		// (int) (height * 0.125));
		// frame_right.addRule(RelativeLayout.CENTER_VERTICAL,
		// RelativeLayout.TRUE);
		// frame_right.addRule(RelativeLayout.LEFT_OF, R.id.rlyaout);
		// frame_right.rightMargin = margin;
		// rightImage.setLayoutParams(frame_right);

		showFrameImageView();
		//返回按钮的监听事件
		backImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				freeCamera();
				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);
				
				CameraActivity.this.Finish();
				
//				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});

		restartImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showFrameImageView();
				takeImage.setVisibility(View.VISIBLE);
				recogImage.setVisibility(View.INVISIBLE);
				restartImage.setVisibility(View.INVISIBLE);
				backImage.setVisibility(View.VISIBLE);
				backAndRestartText.setText("返回");
				takeAndRecogText.setText("拍照");
				if (bitmap != null) {
					if (!bitmap.isRecycled()) {
						bitmap.recycle();
					}
					bitmap = null;
				}
				imageView.setVisibility(View.INVISIBLE);
				imageView.setImageDrawable(null);
				camera.startPreview();
			}
		});
		//拍照按钮点击事件
		takeImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!taking) {
					taking = true;
					takePicture();

					// hepx140807
					// if(recoging){
					// System.out.println("正在保存！！！");
					// }else{
					// saveAndRecogPic();
					// }
				} else {
					System.out.println("正在拍照!!!");
				}
			}
		});

		recogImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (recoging) {
					System.out.println("正在保存！！！");
				} else {
					saveAndRecogPic();
				}
			}
		});
		//点击打开闪光灯并将按扭设置为“关闭闪光灯”状态
		lightOn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				lightText.setText("关闪光灯");
				lightOn.setVisibility(View.INVISIBLE);
				lightOff.setVisibility(View.VISIBLE);
//				if (hasFlashLigth) {   //TODO 合肥掌韵 项目使用  HuangZhen 
					openFlahsLight();
//				}

			}
		});
		//点击关闭闪光灯并将按扭设置为“打开闪光灯”状态
		lightOff.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				lightText.setText("开闪光灯");
				lightOn.setVisibility(View.VISIBLE);
				lightOff.setVisibility(View.INVISIBLE);
//				if (hasFlashLigth) {  			//合肥掌韵 项目使用  HuangZhen TODO
					closeFlashLigth();
//				}
			}
		});

	}
	
	//释放相机
	public void freeCamera() {
		try {
			if (camera != null) {
				camera.cancelAutoFocus();
				camera.setPreviewCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception camera_exception) {
			if (PlateProjectTool.mCamera != null) {
				PlateProjectTool.mCamera.release();
				PlateProjectTool.mCamera = null;
			}
		}

		if (bitmap != null) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}
		imageView.setImageDrawable(null);
	}

	protected int readIntPreferences(String perferencesName, String key) {
		SharedPreferences preferences = getSharedPreferences(perferencesName, MODE_PRIVATE);
		int result = preferences.getInt(key, 0);
		return result;
	}

	private void showFrameImageView() {
		leftImage.setVisibility(View.VISIBLE);
		rightImage.setVisibility(View.VISIBLE);
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
			leftImage.setLayoutParams(frame_left_horizontal_params);
			rightImage.setLayoutParams(frame_right_horizontal_params);
		} else {
			leftImage.setLayoutParams(frame_left_vertical_params);
			rightImage.setLayoutParams(frame_right_vertical_params);
		}
	}

	private void hideFrameImageView() {
		leftImage.setVisibility(View.INVISIBLE);
		rightImage.setVisibility(View.INVISIBLE);
	}

	public boolean isEffectClick() {
		long lastClick = System.currentTimeMillis();
		long diffTime = lastClick - fastClick;
		if (diffTime > 5000) {
			fastClick = lastClick;
			return true;
		}
		return false;
	}

	// 开启设备的闪关灯；
	public void openFlahsLight() {
		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
//			parameters.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
//			parameters.setExposureCompensation(parameters
//                    .getMaxExposureCompensation());
			camera.setParameters(parameters);
//			camera.autoFocus(new Camera.AutoFocusCallback() {
//				public void onAutoFocus(boolean success, Camera camera) {
//				}
//			});
			camera.startPreview();
		}

	}

	public void closeFlashLigth() {
		if (camera != null) {
			camera.stopPreview();
			Camera.Parameters parameters = camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameters);
			camera.startPreview();

		}
	}

	private void saveAndRecogPic() {
		recoging = true;
		try {
			File dir = new File(PATH);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			long datetime = System.currentTimeMillis();
			recogPicPath = PATH + "plateid" + datetime + ".jpg";
			File file = new File(recogPicPath);
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
			bos.flush();
			bos.close();
		} catch (Exception e) {
			recogPicPath = "";
		}
		Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
		intent.putExtra("recogImagePath", recogPicPath);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		if(bitmap!=null&&!bitmap.isRecycled()){
			bitmap.recycle();
			bitmap=null;
			System.gc();
		}
	}

	public void takePicture() {
		if (camera != null) {
			try {
				camera.autoFocus(new AutoFocusCallback() {
					public void onAutoFocus(boolean success, Camera camera) {
						camera.takePicture(shutterCallback, null, PictureCallback);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, R.string.toast_autofocus_failure, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private PictureCallback PictureCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			camera.stopPreview();
			// if (bitmap != null) {
			// if (!bitmap.isRecycled()) {
			// bitmap.recycle();
			// }
			// bitmap = null;
			// }
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inInputShareable = true;
			opts.inPurgeable = true;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
			switch (uR) {
			case 0:
				if (rotationWidth > rotationHeight) {
					System.out.println("---Nothing---");
				}
					else {
						bitmap = getBitmap(bitmap, widthScale, heightScale, true, 90);
				}
					
				break;
			case 1:
				if (rotationWidth > rotationHeight) {
					System.out.println("---Nothing---");
				} 
				else {
					bitmap = getBitmap(bitmap, widthScale, heightScale, false, 270);				
				}
				break;
			case 2:
				if (rotationWidth > rotationHeight) {
					Matrix matrix = new Matrix();
					matrix.preRotate(180);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, false);
					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}
				} else {
					bitmap = getBitmap(bitmap, widthScale, heightScale, false, 270);
				}
				break;
			case 3:
				if (rotationWidth > rotationHeight) {
					Matrix matrix = new Matrix();
					matrix.preRotate(180);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, false);

					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}
				} else {
					bitmap = getBitmap(bitmap, widthScale, heightScale, false, 90);
				}
				break;

			}
			imageView.setImageBitmap(bitmap);
			new FrameCapture(bitmap, "10");

			// hepx140807
			// takeImage.setVisibility(View.INVISIBLE);
			// backImage.setVisibility(View.INVISIBLE);
			// recogImage.setVisibility(View.VISIBLE);
			// restartImage.setVisibility(View.VISIBLE);
			// takeAndRecogText.setText("识别");
			// backAndRestartText.setText("重拍");
			hideFrameImageView();
			taking = false;

			// hepx1408012
			// if (recoging) {
			// System.out.println("正在保存2！！！");
			// } else {
			saveAndRecogPic();
			// }
		}
	} ;
	private Bitmap getBitmap(Bitmap bit,float widthScale,float heightScale,boolean blo,int Rotate){
		Matrix matrix = new Matrix();
		matrix.preRotate(Rotate);
		Bitmap rotate_bitmap = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), bit.getHeight(),
				matrix, blo);
		if (bit != rotate_bitmap) {
			if (!bit.isRecycled()) {
				bit.recycle();
				bit = null;
			}
			bit = rotate_bitmap;
		}

		if (bit.getWidth() > 2048 && bit.getHeight() > 1536) {
			matrix = new Matrix();
			widthScale = (float) 2048 / bit.getWidth();
			heightScale = (float) 1536 / bit.getHeight();
			matrix.postScale(widthScale, heightScale);
			Bitmap cut_bitmap = Bitmap.createBitmap(bit, 0, 0, 2048, 1536, matrix, true);
			if (bit != cut_bitmap) {
				if (!bit.isRecycled()) {
					bit.recycle();
					bit = null;
				}
				bit = cut_bitmap;
			}
		} else if (bit.getWidth() > 2048 && bit.getHeight() <= 1536) {
			matrix = new Matrix();
			widthScale = (float) 2048 / bit.getWidth();
			heightScale = (float) 1.0;
			matrix.postScale(widthScale, heightScale);
			Bitmap cut_bitmap = Bitmap.createBitmap(bit, 0, 0, 2048, bit.getHeight(), matrix, true);
			if (bit != cut_bitmap) {
				if (!bit.isRecycled()) {
					bit.recycle();
					bit = null;
				}
				bit = cut_bitmap;
			}
		} else if (bit.getWidth() <= 2048 && bit.getHeight() > 1536) {
			matrix = new Matrix();
			widthScale = (float) 1.0;
			heightScale = (float) 1536 / bit.getHeight();
			matrix.postScale(widthScale, heightScale);
			Bitmap cut_bitmap = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), 1536, matrix, true);
			if (bit != cut_bitmap) {
				if (!bit.isRecycled()) {
					bit.recycle();
					bit = null;
				}
				bit = cut_bitmap;
			}
		}
		return bit;
	}
	private ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			try {
				if (tone == null) {
					tone = new ToneGenerator(1, ToneGenerator.MIN_VOLUME);
				}
				tone.startTone(ToneGenerator.TONE_PROP_BEEP);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	};

	public void Finish() {
		try {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		} catch (Exception camera_exception) {
			if (PlateProjectTool.mCamera != null) {
				PlateProjectTool.mCamera.release();
				PlateProjectTool.mCamera = null;
			}
		}

		if (bitmap != null) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}

	};

	public void surfaceCreated(SurfaceHolder holder) {
		if (camera == null) {
			try {
				camera = Camera.open();
				PlateProjectTool.mCamera = camera;
			} catch (Exception e) {
				if (PlateProjectTool.mCamera != null) {
					PlateProjectTool.mCamera.release();
					camera = Camera.open();
					PlateProjectTool.mCamera = camera;
				}
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (camera != null) {
			try {
				Camera.Parameters parameters = camera.getParameters();
				parameters.setPictureFormat(PixelFormat.JPEG);								
				 EquipmentUtill er= new EquipmentUtill();
					if(er.CheckPLKTL01H()){
						parameters.setPreviewSize(1920,1080);
						parameters.setPictureSize(640, 480);
					}else{
						parameters.setPreviewSize(preWidth, preHeight);
						parameters.setPictureSize(picWidth, picHeight);
					}								
//				parameters.setExposureCompensation(parameters
//                        .getMaxExposureCompensation());
				camera.setParameters(parameters);
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "相机没有开启",Toast.LENGTH_LONG).show();
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			if (camera != null) {
				camera.cancelAutoFocus();
				camera.setPreviewCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception camera_exception) {
			if (PlateProjectTool.mCamera != null) {
				PlateProjectTool.mCamera.release();
				PlateProjectTool.mCamera = null;
			}
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.e("Camera", "屏幕旋转调用");
		int uiRot = getWindowManager().getDefaultDisplay().getRotation();// 获取屏幕旋转的角度
		uR = uiRot;
		Log.e("Camera", "屏幕旋转角度uiRot===" + uiRot);
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		rotationWidth = displayMetrics.widthPixels;
		rotationHeight = displayMetrics.heightPixels;
		setViewByUirot(uiRot, displayMetrics.widthPixels, displayMetrics.heightPixels);
		showFrameImageView();
		// showTwoImageView();
		// if (BimageView != null && bitmap != null) {
		// BimageView.setVisibility(View.VISIBLE);
		// BimageView.setImageBitmap(bitmap);
		// }
		// if (cameraResultTextView != null &&
		// (cameraResultTextView.getVisibility() == View.VISIBLE)) {
		// if (horizontal) {
		// cameraResultTextView.setLayoutParams(result_text_horizontal_params);
		// } else {
		// cameraResultTextView.setLayoutParams(result_text_vertical_params);
		// }
		// }
	}

	private void setViewByUirot(int uiRot, int width, int height) {

		if (camera == null) {
			try {
				camera = Camera.open();
				PlateProjectTool.mCamera = camera;
			} catch (Exception e) {
				if (PlateProjectTool.mCamera != null) {
					PlateProjectTool.mCamera.release();
					camera = Camera.open();
					PlateProjectTool.mCamera = camera;
				}
			}
		}
		switch (uiRot) {
		case 0:
			setViewAndRotationDouble(width,height,0, 0, 90);
			break;
		case 1:
			setViewAndRotationSingle(width,height,0, 0, 270);
			break;
		case 2:
			setViewAndRotationDouble(width,height,180, 270, 270);
			break;
		case 3:
			setViewAndRotationSingle(width,height,180, 180, 90);
			break;
		}

		camera.startPreview();

	}
	private void setViewAndRotationDouble(int width,int height,int rotation1,int rotation2,int rotation3){
		if (camera != null) {
			camera.stopPreview();
		}
		if (width > height) {
			//uiRot=0 且width > height时，一般是平板电脑   这里是针对平板电脑做的布局
			// horizontal = true;
			camera.setDisplayOrientation(rotation1);
			int layoutLength = (int) (width - ((height * 4) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.rlyaout);
			surfaceView.setLayoutParams(layoutParams);
			// BimageView.setLayoutParams(layoutParams);

			int layout_distance = (int) (height * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			layoutParams.bottomMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);
			lightOff.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);
			layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			layoutParams.bottomMargin = layout_distance / 2 + 20;
			lightText.setLayoutParams(layoutParams);

		} else if (width < height && height * 0.75 == width) {
			//uiRot=0 且width < height时，一般是手机设备   这里是针对手机设备做的布局且屏幕宽高比为4：3
			camera.setDisplayOrientation(rotation2);
			// horizontal = false;
			int layoutLength = (int) (height - ((width * 3) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			surfaceView.setLayoutParams(layoutParams);

			int layout_distance = (int) (width * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.rightMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance * 2,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.backandrestarttext);
			layoutParams.leftMargin = layout_distance / 2;
			lightText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.leftMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);
			lightOff.setLayoutParams(layoutParams);
			
			return;
		}
		
		
		else {
			//uiRot=0 且width < height时，一般是手机设备   这里是针对手机设备做的布局
			camera.setDisplayOrientation(rotation3);
			// horizontal = false;
			int layoutLength = (int) (height - ((width * 4) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.surfaceViwe);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			surfaceView.setLayoutParams(layoutParams);

			int layout_distance = (int) (width * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.rightMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance * 2,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.backandrestarttext);
			layoutParams.leftMargin = layout_distance / 2;
			lightText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.leftMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);
			lightOff.setLayoutParams(layoutParams);
		}
	}
	private void setViewAndRotationSingle(int width,int height,int rotation1,int rotation2,int rotation3){
		
		if (camera != null) {
			camera.stopPreview();
		}if (width > height && width * 0.75 == height){
			camera.setDisplayOrientation(rotation1);
			// horizontal = true;
			int layoutLength = (int) (width - ((height * 3) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			surfaceView.setLayoutParams(layoutParams);
			// BimageView.setLayoutParams(layoutParams);

			int layout_distance = (int) (height * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			layoutParams.bottomMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);
			lightOff.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.lightimage1);
			layoutParams.topMargin = (int) (-height * 0.1);
			lightText.setLayoutParams(layoutParams);
			
			return;
		}
		
		else if (width > height) {

			camera.setDisplayOrientation(rotation2);

			// horizontal = true;
			int layoutLength = (int) (width - ((height * 4) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.rlyaout);
			surfaceView.setLayoutParams(layoutParams);
			// BimageView.setLayoutParams(layoutParams);

			int layout_distance = (int) (height * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.topMargin = layout_distance;
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			layoutParams.bottomMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);
			lightOff.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.lightimage1);
			layoutParams.topMargin = (int) (-height * 0.1);
			lightText.setLayoutParams(layoutParams);

		} else {
			// horizontal = false;
			camera.setDisplayOrientation(rotation3);

			int layoutLength = (int) (height - ((width * 4) / 3));
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.surfaceViwe);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			rlyaout.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			surfaceView.setLayoutParams(layoutParams);
			// BimageView.setLayoutParams(layoutParams);

			int layout_distance = (int) (width * 0.12);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.rightMargin = layout_distance;
			backImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			layoutParams.rightMargin = layout_distance;
			restartImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			takeImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			recogImage.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.leftMargin = layout_distance;
			lightOn.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.leftMargin = layout_distance;
			lightOff.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.backimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.backimage);
			backAndRestartText.setLayoutParams(layoutParams);
			layoutParams = new RelativeLayout.LayoutParams(layout_distance,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.takeimage);
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.takeimage);
			takeAndRecogText.setLayoutParams(layoutParams);

			layoutParams = new RelativeLayout.LayoutParams(layout_distance * 2,
					RelativeLayout.LayoutParams.WRAP_CONTENT); 
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.backandrestarttext);
			layoutParams.leftMargin = layout_distance / 2;
			lightText.setLayoutParams(layoutParams);
		}
}
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			freeCamera();
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			CameraActivity.this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

}
