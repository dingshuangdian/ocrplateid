package com.kernal.plateid;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CameraRecogPlateID extends Activity implements SurfaceHolder.Callback {
	public static final String TAG = "CameraRecogPlateID_OPT";
	public static final String PATH = Environment.getExternalStorageDirectory().toString();
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	public int preWidth = 640;
	public int preHeight = 480;
	public int picWidth = 2048;
	public int picHeight = 1536;
	private String authfile;
	private String sn;
	private String server;
	private String devcode;
	private String datefile;
	private String lpFileName;
	private String pic;
	private int quality = 75;
	private boolean usepara;
	private int nPlateLocate_Th;// 识别阈值(取值范围0-9,5:默认阈值0:最宽松的阈值9:最严格的阈值)
	private int nOCR_Th;
	private int bIsAutoSlope;// 是否要倾斜校正
	private int nSlopeDetectRange;// 倾斜校正的范围(取值范围0-16)
	private int nContrast;// 清晰度指数(取值范围0-9,最模糊时设为1;最清晰时设为9)
	private int bIsNight;// 是否夜间模式：1是；0不是
	private String szProvince;// 省份顺序
	private int individual;// 是否开启个性化车牌:0是；1不是
	private int tworowyellow;// 双层黄色车牌是否开启:2是；3不是
	private int armpolice;// 单层武警车牌是否开启:4是；5不是
	private int tworowarmy;// 双层军队车牌是否开启:6是；7不是
	private int tractor; // 农用车车牌是否开启:8是；9不是
	private int onlytworowyellow;// 只识别双层黄牌是否开启:10是；11不是
	private int embassy;// 使馆车牌是否开启:12是；13不是
	private int onlylocation;// 只定位车牌是否开启:14是；15不是
	private int armpolice2;// 双层武警车牌是否开启:16是；17不是
	private String deviceId;
	private String androidId;
//	private boolean photograph;
	private Camera camera;
	private Bitmap bitmap;
	private ToneGenerator tone;

	public AuthService.MyBinder authBinder;
	public RecogService.MyBinder recogBinder;
	private int ReturnAuthority = -1;
	int nRet = -1;
	int imageformat = 1;
	int bVertFlip = 0;
	int bDwordAligned = 1;
	boolean bGetVersion = false;
	String[] fieldvalue = new String[14];
	private int rotation;
	private int rotationWidth, rotationHeight;
	private boolean recog_bool = false;
	private Boolean saveBoolean = true;
	private List<String> focusModes;
	private String picPathString;
	private float widthScale = (float) 1.0;
	private float heightScale = (float) 1.0;
	private Boolean horizontal = true;
	private Boolean isCatchPicture = false;
	private final int SYSTEM_RESULT_CODE = 2;

	private ImageView BimageView;
	private TextView cameraResultTextView;
	private RelativeLayout.LayoutParams result_text_horizontal_params, result_text_vertical_params;
	private ImageView leftImage, rightImage;
	private RelativeLayout rlyaout;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private ImageButton backImage, restartImage, takeImage, recogImage, lightImage1, lightImage2;
	private RelativeLayout.LayoutParams frame_left_horizontal_params, frame_right_horizontal_params;
	private RelativeLayout.LayoutParams frame_left_vertical_params, frame_right_vertical_params;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		int recogcamera_layout_id = getResources().getIdentifier("kernal_camera_opt", "layout", this.getPackageName());
		setContentView(recogcamera_layout_id);

		TelephonyManager telephonyManager;
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		// 获取deviceId
		StringBuilder sb = new StringBuilder();
		sb.append(telephonyManager.getDeviceId());
		deviceId = sb.toString();// 由15位数字组成
		// 获取androId
		StringBuilder sb1 = new StringBuilder();
		sb1.append(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
		androidId = sb1.toString();// 由16位数字和字母组成
		Intent intentget = this.getIntent();
		picWidth = intentget.getIntExtra("srcwidth", 2048);// 2048 1024
		picHeight = intentget.getIntExtra("srcheight", 1536);// 1536 768
		preWidth = intentget.getIntExtra("WIDTH", 640);
		preHeight = intentget.getIntExtra("HEIGHT", 480);
		sn = intentget.getStringExtra("sn");
		server = intentget.getStringExtra("server");
		authfile = intentget.getStringExtra("authfile");
		datefile = intentget.getStringExtra("datefile");
		devcode = intentget.getStringExtra("devcode");
		lpFileName = intentget.getStringExtra("lpFileName");
		pic = lpFileName;
		quality = intentget.getIntExtra("quality", 75);
		usepara = intentget.getBooleanExtra("usepara", false);
		szProvince = intentget.getStringExtra("szProvince");
		nPlateLocate_Th = intentget.getIntExtra("nPlateLocate_Th", 7);
		nOCR_Th = intentget.getIntExtra("nOCR_Th", 5);
		bIsAutoSlope = intentget.getIntExtra("bIsAutoSlope", 1);
		nSlopeDetectRange = intentget.getIntExtra("nSlopeDetectRange", 0);
		nContrast = intentget.getIntExtra("nContrast", 9);
		bIsNight = intentget.getIntExtra("bIsNight", 0);
		individual = intentget.getIntExtra("individual", 0);
		tworowyellow = intentget.getIntExtra("tworowyellow", 3);
		armpolice = intentget.getIntExtra("armpolice", 5);
		tworowarmy = intentget.getIntExtra("tworowarmy", 7);
		tractor = intentget.getIntExtra("tractor", 9);
		onlytworowyellow = intentget.getIntExtra("onlytworowyellow", 11);
		embassy = intentget.getIntExtra("embassy", 13);
		onlylocation = intentget.getIntExtra("onlylocation", 15);
		armpolice2 = intentget.getIntExtra("armpolice2", 17);

		// 考虑用户传lpFileName和没传lpFileName的情况，没传就不保存文件，直接覆盖。
		if (null != lpFileName && !lpFileName.equals("")) {
			Log.i(TAG, "lpFileName is not null");
			File lpFile = new File(lpFileName);
			String lpFilePath = lpFile.getParent();
			File lpPath = new File(lpFilePath);
			if (!lpPath.exists()) {
				boolean success = lpPath.mkdirs();
				Log.i(TAG, "success:" + success);
			}
		} else {
			Log.i(TAG, "lpFileName is null");
			String defaultPath = PATH + "/wtimage/wt.jpg";
			lpFileName = defaultPath;
			File file = new File(lpFileName);
			String parentPath = file.getParent();
			File parentFile = new File(parentPath);
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
		}
		Log.i(TAG, "authfile=" + authfile);
		if (null != authfile && !authfile.equals("") && authfile.equals("guangda")) {
			if (deviceId.equals("null")) {
				authfile = getExtPath() + "/kernal/" + androidId + "_cp.txt";
			} else {
				authfile = getExtPath() + "/kernal/" + deviceId + "_cp.txt";
			}
			Log.i(TAG, "authfile =" + authfile);
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		checkCamerParameter();
		findView();
		if (isCatchPicture) {
			surfaceHolder = surfaceView.getHolder();
			surfaceHolder.addCallback(CameraRecogPlateID.this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			FeatureInfo[] features = this.getPackageManager().getSystemAvailableFeatures();
			for (FeatureInfo featureInfo : features) {
				// Log.i(TAG, "featureInfo ="+featureInfo.name);
				if (PackageManager.FEATURE_CAMERA_FLASH.equals(featureInfo.name)) {
					// hasFlashLigth = true;
				}
			}
		} else {
			Intent sysCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
			sysCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(lpFileName)));
			startActivityForResult(sysCameraIntent, SYSTEM_RESULT_CODE);
		
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected void onStop() {
		super.onStop();
		BimageView.setImageDrawable(null);
		if (camera != null) {
			camera.release();
			camera = null;
		}
		if (bitmap != null) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (SYSTEM_RESULT_CODE == requestCode && resultCode == Activity.RESULT_OK) {
			Intent recogIntent = new Intent(getApplicationContext(), RecogService.class);
			// Log.i(TAG, "onActivityResult pic="+pic);
			bindService(recogIntent, recogConn, Service.BIND_AUTO_CREATE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
//		if (camera != null) {
//			camera.release();
//			camera = null;
//		}
		int uiRot = getWindowManager().getDefaultDisplay().getRotation();
//		checkCamerParameter();
		System.out.println("uiRot===" + uiRot);
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		rotation = uiRot;
		rotationWidth = displayMetrics.widthPixels;
		rotationHeight = displayMetrics.heightPixels;
		setViewByRotation(uiRot, displayMetrics.widthPixels, displayMetrics.heightPixels);
		showTwoImageView();
		if (BimageView != null && bitmap != null) {
			BimageView.setVisibility(View.VISIBLE);
			BimageView.setImageBitmap(bitmap);
		}
		if (cameraResultTextView != null && (cameraResultTextView.getVisibility() == View.VISIBLE)) {
			if (horizontal) {
				cameraResultTextView.setLayoutParams(result_text_horizontal_params);
			} else {
				cameraResultTextView.setLayoutParams(result_text_vertical_params);
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (camera != null) {
			try {
				Camera.Parameters parameters = camera.getParameters();
				parameters.setPictureFormat(PixelFormat.JPEG);
				parameters.setPreviewSize(preWidth, preHeight);
				parameters.setPictureSize(picWidth, picHeight);
				camera.setParameters(parameters);
				camera.setPreviewDisplay(holder);
				focusModes = parameters.getSupportedFocusModes();
				camera.startPreview();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (camera == null) {
			try {
				camera = Camera.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			freeCamera();
			Intent intent = new Intent(getApplicationContext(), CameraRecogPlateID.class);
			startActivity(intent);
			CameraRecogPlateID.this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	// 授权验证服务绑定后的操作与start识别服务
	public ServiceConnection authConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			authBinder = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "authConn onServiceConnected");
			Log.i(TAG, "authfile=" + authfile + "\nsn=" + sn);
			authBinder = (AuthService.MyBinder) service;
			// Toast.makeText(getApplicationContext(),
			// R.string.auth_check_service_bind_success,
			// Toast.LENGTH_SHORT).show();
			try {
				// sn：采用序列号方式激活时设置此参数，否则写""
				// authfile：采用激活文件方式激活时设置此参数，否则写""
				// 以上俩个参数都不为""时按序列号方式激活；当sn和authfile为""时会在根目录下找激活文件xxxxxxxxxxxxxxx_cp.txt
				// ReturnAuthority = authBinder.getAuth(sn, authfile);
				PlateAuthParameter pap = new PlateAuthParameter();
				pap.authFile = authfile;
				pap.dataFile = datefile;
				pap.devCode = Devcode.DEVCODE;
				pap.sn = sn;
				pap.server = server;
				ReturnAuthority = authBinder.getAuth(pap);
				Log.i(TAG, "ReturnAuthority=" + ReturnAuthority);
				nRet = ReturnAuthority;
				// Toast.makeText(getApplicationContext(),
				// "ReturnAuthority="+ReturnAuthority,
				// Toast.LENGTH_SHORT).show();
				if (ReturnAuthority != 0) {
				} else {
				}
			} catch (Exception e) {
				// int failed_check_failure =
				// getResources().getIdentifier("failed_check_failure",
				// "string", getPackageName());
				// Toast.makeText(getApplicationContext(), failed_check_failure,
				// Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				// Log.i(TAG, "e=" + e.toString());
			} finally {
				if (authBinder != null) {
					unbindService(authConn);// 解绑授权验证服务
				}
			}
		}
	};

	// 在本页识别时识别服务绑定后的操作
	public ServiceConnection recogConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			recogConn = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			recogBinder = (RecogService.MyBinder) service;
			int inRet = recogBinder.getInitPlateIDSDK();
			// nRet = recogBinder.getInitPlateIDSDK();
			if (inRet != 0) {
				// Toast.makeText(getApplicationContext(), "验证授权或初始化失败:" + nRet,
				// Toast.LENGTH_SHORT).show();

			} else {
				if (usepara == false) {
					recogBinder.setRecogArgu(lpFileName, imageformat, bGetVersion, bVertFlip, bDwordAligned);
				} else {
					System.out.println("usepara");
					// 设置识别用到的参数，采用直接设置参数对象的方式设置识别参数，所有参数定义请查看文档
					PlateCfgParameter cfgparameter = new PlateCfgParameter();
					cfgparameter.armpolice = armpolice;
					cfgparameter.armpolice2 = armpolice2;
					cfgparameter.bIsAutoSlope = bIsAutoSlope;
					cfgparameter.bIsNight = bIsNight;
					cfgparameter.embassy = embassy;
					cfgparameter.individual = individual;
					cfgparameter.nContrast = nContrast;
					cfgparameter.nOCR_Th = nOCR_Th;
					cfgparameter.nPlateLocate_Th = nPlateLocate_Th;
					cfgparameter.nSlopeDetectRange = nSlopeDetectRange;
					cfgparameter.onlylocation = onlylocation;
					cfgparameter.tworowyellow = tworowyellow;
					cfgparameter.tworowarmy = tworowarmy;
					if (szProvince == null)
						szProvince = "";
					cfgparameter.szProvince = szProvince;
					cfgparameter.onlytworowyellow = onlytworowyellow;
					cfgparameter.tractor = tractor;
					recogBinder.setRecogArgu(cfgparameter, imageformat, bVertFlip, bDwordAligned);
				}
				nRet = recogBinder.getnRet();
				// fieldvalue = recogBinder.doRecog(pic, width, height);
				PlateRecognitionParameter prp = new PlateRecognitionParameter();
				prp.dataFile = datefile;
				prp.devCode = Devcode.DEVCODE;
				prp.pic = lpFileName;
				fieldvalue = recogBinder.doRecogDetail(prp);
				nRet = recogBinder.getnRet();
			}
			// 解绑识别服务。
			if (recogBinder != null) {
				unbindService(recogConn);
			}
			// 用户不指定lpFileName时删除所拍图片
			if (null != pic && !pic.equals("")) {
				// System.out.println("null != lpFileName && !lpFileName.equals");
			} else {
				// System.out.println("lpFileName="+lpFileName);
				File picFile = new File(lpFileName);
				if (picFile.exists()) {
					picFile.delete();
				}
			}
			// 返回识别结果
			// Log.i(TAG, "pic="+pic);
			Intent intentReturn = new Intent();
			intentReturn.putExtra("ReturnAuthority", ReturnAuthority);
			intentReturn.putExtra("nRet", nRet);
			intentReturn.putExtra("ReturnLPFileName", lpFileName);
			intentReturn.putExtra("fieldvalue", fieldvalue);
			setResult(Activity.RESULT_OK, intentReturn);
			finish();
		}
	};

	private void findView() {
		int cameraresulttext = getResources().getIdentifier("cameraresulttext", "id", this.getPackageName());
		cameraResultTextView = (TextView) findViewById(cameraresulttext);
		cameraResultTextView.setBackgroundColor(Color.BLACK);
		cameraResultTextView.setTextColor(Color.WHITE);
		cameraResultTextView.setVisibility(View.INVISIBLE);
		cameraResultTextView.setMovementMethod(new ScrollingMovementMethod());

		int bimageView = getResources().getIdentifier("BimageView", "id", this.getPackageName());
		BimageView = (ImageView) findViewById(bimageView);
		int rlyaouts = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
		rlyaout = (RelativeLayout) findViewById(rlyaouts);
		int surfaceViwe = getResources().getIdentifier("surfaceViwe", "id", this.getPackageName());
		surfaceView = (SurfaceView) findViewById(surfaceViwe);
		int backimage = getResources().getIdentifier("backimage", "id", this.getPackageName());
		backImage = (ImageButton) findViewById(backimage);
		int reback = getResources().getIdentifier("reback", "drawable", this.getPackageName());
		backImage.setBackgroundResource(reback);
		backImage.setVisibility(View.VISIBLE);
		int restartimage = getResources().getIdentifier("restartimage", "id", this.getPackageName());
		restartImage = (ImageButton) findViewById(restartimage);
		int retake = getResources().getIdentifier("retake", "drawable", this.getPackageName());
		restartImage.setBackgroundResource(retake);
		restartImage.setVisibility(View.INVISIBLE);
		// int backandrestarttext = getResources().getIdentifier("retake", "id",
		// this.getPackageName());
		// backAndRestartText = (TextView) findViewById(backandrestarttext);
		// backAndRestartText.setVisibility(View.VISIBLE);
		// backAndRestartText.setText("返回");
		int takeimage = getResources().getIdentifier("takeimage", "id", this.getPackageName());
		takeImage = (ImageButton) findViewById(takeimage);
		int take = getResources().getIdentifier("take", "drawable", this.getPackageName());
		takeImage.setBackgroundResource(take);
		takeImage.setVisibility(View.VISIBLE);
		int recogimage = getResources().getIdentifier("recogimage", "id", this.getPackageName());
		recogImage = (ImageButton) findViewById(recogimage);
		int discern = getResources().getIdentifier("discern", "drawable", this.getPackageName());
		recogImage.setBackgroundResource(discern);
		recogImage.setVisibility(View.INVISIBLE);
		// takeAndRecogText = (TextView) findViewById(R.id.takeandrecogtext);
		// takeAndRecogText.setText("拍照");
		// takeAndRecogText.setVisibility(View.VISIBLE);
		int lightimage1 = getResources().getIdentifier("lightimage1", "id", this.getPackageName());
		lightImage1 = (ImageButton) findViewById(lightimage1);
		int flash_close = getResources().getIdentifier("flash_close", "drawable", this.getPackageName());
		lightImage1.setBackgroundResource(flash_close);
		lightImage1.setVisibility(View.VISIBLE);
		int lightimage2 = getResources().getIdentifier("lightimage2", "id", this.getPackageName());
		lightImage2 = (ImageButton) findViewById(lightimage2);
		int flash_open = getResources().getIdentifier("flash_open", "drawable", this.getPackageName());
		lightImage2.setBackgroundResource(flash_open);
		lightImage2.setVisibility(View.INVISIBLE);
		// lightText = (TextView) findViewById(R.id.lighttext);
		// lightText.setText("开闪光灯");
		// lightText.setVisibility(View.VISIBLE);
		int uiRot = getWindowManager().getDefaultDisplay().getRotation();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		rotation = uiRot;
		rotationWidth = displayMetrics.widthPixels;
		rotationHeight = displayMetrics.heightPixels;
		setViewByRotation(uiRot, displayMetrics.widthPixels, displayMetrics.heightPixels);

		int cameraframeleft = getResources().getIdentifier("cameraframeleft", "id", this.getPackageName());
		leftImage = (ImageView) findViewById(cameraframeleft);
		int frame_left = getResources().getIdentifier("frame_left", "drawable", this.getPackageName());
		leftImage.setBackgroundResource(frame_left);
		int cameraframeright = getResources().getIdentifier("cameraframeright", "id", this.getPackageName());
		rightImage = (ImageView) findViewById(cameraframeright);
		int frame_right = getResources().getIdentifier("frame_right", "drawable", this.getPackageName());
		rightImage.setBackgroundResource(frame_right);

		// int horizontal_screen = Math.max(displayMetrics.widthPixels,
		// displayMetrics.heightPixels);
		int vertical_screen = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);

		result_text_horizontal_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 1.333 * 0.5),
				(int) (vertical_screen * 0.3));
		result_text_horizontal_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		result_text_horizontal_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		result_text_horizontal_params.leftMargin = (int) (vertical_screen * 1.333 * 0.25);

		result_text_vertical_params = new RelativeLayout.LayoutParams((int) (vertical_screen * 0.7),
				(int) (vertical_screen * 0.3));
		int rlyaoutss = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
		result_text_vertical_params.addRule(RelativeLayout.ABOVE, rlyaoutss);
		result_text_vertical_params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		result_text_vertical_params.leftMargin = (int) (vertical_screen * 0.15);

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
		frame_right_horizontal_params.addRule(RelativeLayout.LEFT_OF, rlyaoutss);
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

		showTwoImageView();

		backImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				freeCamera();
				finish();
			}

		});

		restartImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showTwoImageView();
				takeImage.setVisibility(View.VISIBLE);
				recogImage.setVisibility(View.INVISIBLE);
				restartImage.setVisibility(View.INVISIBLE);
				backImage.setVisibility(View.VISIBLE);
				// backAndRestartText.setText("返回");
				// takeAndRecogText.setText("拍照");
				if (bitmap != null) {
					if (!bitmap.isRecycled()) {
						bitmap.recycle();
					}
					bitmap = null;
				}
				BimageView.setVisibility(View.INVISIBLE);
				BimageView.setImageDrawable(null);
				camera.startPreview();
			}
		});

		takeImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent authIntent = new Intent(CameraRecogPlateID.this, AuthService.class);
				bindService(authIntent, authConn, Service.BIND_AUTO_CREATE);
				if (recog_bool) {
					System.out.println("正在识别中......");
				} else {
					// takeAndRecogText.setText("拍照");
					if (cameraResultTextView != null && (cameraResultTextView.getVisibility() == View.VISIBLE)) {
						if (bitmap != null) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
							}
							bitmap = null;
						}
						BimageView.setImageDrawable(null);
						cameraResultTextView.setVisibility(View.INVISIBLE);
						camera.startPreview();
						showTwoImageView();
					} else {
						camera.startPreview();
						showTwoImageView();
						if (saveBoolean) {
							saveBoolean = false;
							takeAndSavePicture();
						} else {
							System.out.println("正在运行中......");
						}
					}
				}
			}
		});

		recogImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				saveBoolean = false;
				recog_bool = true;
				takeImage.setVisibility(View.VISIBLE);
				recogImage.setVisibility(View.INVISIBLE);
				restartImage.setVisibility(View.INVISIBLE);
				backImage.setVisibility(View.VISIBLE);
				// takeAndRecogText.setText("清空");
				// backAndRestartText.setText("返回");
				hideTwoImageView();
				camera.stopPreview();

				BimageView.setImageDrawable(null);
				new Thread() {

					public void run() {
						if (bitmap != null) {
							bitmap.recycle();
							bitmap = null;
						}
						Intent recogIntent = new Intent(getApplicationContext(), RecogService.class);
						bindService(recogIntent, recogConn, Service.BIND_AUTO_CREATE);// 启动识别服务
						saveBoolean = true;

					}

				}.start();
			}
		});

		lightImage1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// lightText.setText("关闪光灯");
				Camera.Parameters parameters = camera.getParameters();
				parameters.set("flash-mode", "on");
				camera.setParameters(parameters);
				lightImage1.setVisibility(View.INVISIBLE);
				lightImage2.setVisibility(View.VISIBLE);
			}
		});

		lightImage2.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				// lightText.setText("开闪光灯");
				Camera.Parameters parameters = camera.getParameters();
				parameters.set("flash-mode", "off");
				camera.setParameters(parameters);
				lightImage1.setVisibility(View.VISIBLE);
				lightImage2.setVisibility(View.INVISIBLE);
			}
		});

	}

	public void freeCamera() {
		if (camera != null) {
			camera.release();
			camera = null;
		}

		if (bitmap != null) {
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}
		BimageView.setImageDrawable(null);
		unBindPlateRecogService();
	}

	private void unBindPlateRecogService() {
		try {
			if (recogBinder != null) {
				unbindService(recogConn);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取外置存储卡路径
	public String getExtPath() {
		for (String path : getStoragePath()) {
			if (path.contains("ext")) {
				return path;
			}
		}
		return "";
	}

	// 获取储存卡(内、外置)路径
	public List<String> getStoragePath() {
		List<String> mounts = new ArrayList<String>();
		try {
			Runtime runtime = Runtime.getRuntime();
			Process proc = runtime.exec("mount");
			InputStream is = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			String line;
			BufferedReader br = new BufferedReader(isr);
			while ((line = br.readLine()) != null) {
				if (line.contains("secure"))
					continue;
				if (line.contains("asec"))
					continue;
				if (line.contains("fat")) {
					String columns[] = line.split(" ");
					if (columns != null && columns.length > 1) {
						if (null != columns[1] || columns[1].equals("")) {
							mounts.add(columns[1]);
						}
					}
				} else if (line.contains("fuse")) {
					String columns[] = line.split(" ");
					if (columns != null && columns.length > 1) {
						if (null != columns[1] || columns[1].equals("")) {
							mounts.add(columns[1]);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mounts;
	}

	private void showTwoImageView() {
		if (cameraResultTextView != null && (cameraResultTextView.getVisibility() == View.VISIBLE)) {
			leftImage.setVisibility(View.INVISIBLE);
			rightImage.setVisibility(View.INVISIBLE);
		} else {
			leftImage.setVisibility(View.VISIBLE);
			rightImage.setVisibility(View.VISIBLE);
		}
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
			leftImage.setLayoutParams(frame_left_horizontal_params);
			rightImage.setLayoutParams(frame_right_horizontal_params);
		} else {
			leftImage.setLayoutParams(frame_left_vertical_params);
			rightImage.setLayoutParams(frame_right_vertical_params);
		}
	}

	public void takeAndSavePicture() {

		if (camera != null) {
			try {

				if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
					camera.autoFocus(new AutoFocusCallback() {

						public void onAutoFocus(boolean success, Camera camera) {
							camera.takePicture(shutterCallback, null, PictureCallback);
						}
					});
				} else {
					camera.takePicture(shutterCallback, null, PictureCallback);
					Toast.makeText(getBaseContext(), "不支持自动对焦", Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {

			}
		}
	}

	private ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			if (tone == null)
				tone = new ToneGenerator(1, ToneGenerator.MIN_VOLUME);
			tone.startTone(ToneGenerator.TONE_PROP_BEEP);
		}
	};
	private PictureCallback PictureCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			camera.stopPreview();
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inInputShareable = true;
			opts.inPurgeable = true;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
			switch (rotation) {
			case 0:
				if (rotationWidth > rotationHeight) {
					System.out.println("---Nothing---");
				} else {
					Matrix matrix = new Matrix();
					matrix.preRotate(90);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, true);
					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}

					if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1.0;
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, bitmap.getHeight(), matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 1.0;
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					}
				}
				break;
			case 1:
				if (rotationWidth > rotationHeight) {
					System.out.println("---Nothing---");
				} else {

					Matrix matrix = new Matrix();
					matrix.preRotate(270);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, false);
					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}

					if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1.0;
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, bitmap.getHeight(), matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 1.0;
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					}

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

					Matrix matrix = new Matrix();
					matrix.preRotate(270);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, false);
					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}

					if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1.0;
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, bitmap.getHeight(), matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 1.0;
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					}

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

					Matrix matrix = new Matrix();
					matrix.preRotate(90);
					Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
							matrix, false);
					if (bitmap != rotate_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = rotate_bitmap;
					}

					if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
						matrix = new Matrix();
						widthScale = (float) 2048 / bitmap.getWidth();
						heightScale = (float) 1.0;
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048, bitmap.getHeight(), matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					} else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
						matrix = new Matrix();
						widthScale = (float) 1.0;
						heightScale = (float) 1536 / bitmap.getHeight();
						matrix.postScale(widthScale, heightScale);
						Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), 1536, matrix, true);
						if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
					}

				}
				break;

			}

			/* 创建文件 */
			String sdStatus = Environment.getExternalStorageState();
			if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
				picPathString = Environment.getExternalStorageDirectory().toString() + "/wtimage/";
			}
			if(picPathString!=null){
            	File dir = new File(picPathString);
                if (dir != null && !dir.exists()) {
                    dir.mkdir();
                }
            }
			File file = new File(lpFileName);
			try {
				file.createNewFile();
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
				bos.flush();
				bos.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

			BimageView.setImageBitmap(bitmap);
			takeImage.setVisibility(View.INVISIBLE);
			recogImage.setVisibility(View.VISIBLE);
			restartImage.setVisibility(View.VISIBLE);
			backImage.setVisibility(View.INVISIBLE);
			// takeAndRecogText.setText("识别");
			// backAndRestartText.setText("重拍");
			hideTwoImageView();
			saveBoolean = true;
		}
	};

	private void hideTwoImageView() {
		leftImage.setVisibility(View.INVISIBLE);
		rightImage.setVisibility(View.INVISIBLE);
	}

	private void setViewByRotation(int rotation, int width, int height) {
		if (camera == null) {
			try {
				camera = Camera.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		switch (rotation) {
		case 0:

			if (camera != null) {
				camera.stopPreview();
			}
			if (width > height) {
				horizontal = true;
				camera.setDisplayOrientation(0);
				int layoutLength = (int) (width - ((height * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				int rlyaouts = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.LEFT_OF, rlyaouts);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				// RelativeLayout.TRUE);
				// layoutParams.bottomMargin = layout_distance;
				// lightText.setLayoutParams(layoutParams);

			} else {
				camera.setDisplayOrientation(90);
				horizontal = false;
				int layoutLength = (int) (height - ((width * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				int surfaceViwe = getResources().getIdentifier("surfaceViwe", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.BELOW, surfaceViwe);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.topMargin=-layoutLength;
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance*2,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_TOP,
				// R.id.backandrestarttext);
				// layoutParams.leftMargin = layout_distance/2;
				// lightText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				// layoutParams.addRule(RelativeLayout.ABOVE, R.id.lighttext);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				int takeimage = getResources().getIdentifier("takeimage", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				// layoutParams.addRule(RelativeLayout.ABOVE, R.id.lighttext);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);
			}
			break;

		case 1:

			if (camera != null) {
				camera.stopPreview();
			}
			if (width > height) {
				camera.setDisplayOrientation(0);
				horizontal = true;
				int layoutLength = (int) (width - ((height * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				int rlyaoutss = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.LEFT_OF, rlyaoutss);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				// RelativeLayout.TRUE);
				// layoutParams.bottomMargin = layout_distance;
				// lightText.setLayoutParams(layoutParams);

			} else {
				horizontal = false;
				camera.setDisplayOrientation(270);

				int layoutLength = (int) (height - ((width * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				int surfaceViwe = getResources().getIdentifier("surfaceViwe", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.BELOW, surfaceViwe);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.topMargin=-layoutLength;
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance*2,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_TOP,
				// R.id.backandrestarttext);
				// layoutParams.leftMargin = layout_distance/2;
				// lightText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				int takeimage = getResources().getIdentifier("takeimage", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

			}
			break;
		case 2:
			if (camera != null) {
				camera.stopPreview();
			}

			if (width > height) {
				horizontal = true;
				camera.setDisplayOrientation(180);

				int layoutLength = (int) (width - ((height * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				int rlyaouts = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.LEFT_OF, rlyaouts);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				// RelativeLayout.TRUE);
				// layoutParams.bottomMargin = layout_distance;
				// lightText.setLayoutParams(layoutParams);

			} else {

				camera.setDisplayOrientation(270);
				horizontal = false;
				int layoutLength = (int) (height - ((width * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				int surfaceViwe = getResources().getIdentifier("surfaceViwe", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.BELOW, surfaceViwe);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.topMargin=-layoutLength;
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance*2,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_TOP,
				// R.id.backandrestarttext);
				// layoutParams.leftMargin = layout_distance/2;
				// lightText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				int takeimage = getResources().getIdentifier("takeimage", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

			}
			break;
		case 3:

			if (camera != null) {
				camera.stopPreview();
			}
			if (width > height) {

				camera.setDisplayOrientation(180);

				horizontal = true;
				int layoutLength = (int) (width - ((height * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width - layoutLength, height);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				int rlyaouts = getResources().getIdentifier("rlyaout", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.LEFT_OF, rlyaouts);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
				layoutParams.bottomMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
				// RelativeLayout.TRUE);
				// layoutParams.bottomMargin = layout_distance;
				// lightText.setLayoutParams(layoutParams);

			} else {
				horizontal = false;
				camera.setDisplayOrientation(90);

				int layoutLength = (int) (height - ((width * 4) / 3));
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				int surfaceViwe = getResources().getIdentifier("surfaceViwe", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.BELOW, surfaceViwe);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.topMargin=-layoutLength;
				rlyaout.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(width, height - layoutLength);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				BimageView.setLayoutParams(layoutParams);
				layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
				surfaceView.setLayoutParams(layoutParams);

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

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.backimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.backimage);
				// backAndRestartText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				takeImage.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				recogImage.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.BELOW, R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_LEFT,
				// R.id.takeimage);
				// layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,
				// R.id.takeimage);
				// takeAndRecogText.setLayoutParams(layoutParams);

				// layoutParams = new
				// RelativeLayout.LayoutParams(layout_distance*2,
				// RelativeLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				// RelativeLayout.TRUE);
				// layoutParams.addRule(RelativeLayout.ALIGN_TOP,
				// R.id.backandrestarttext);
				// layoutParams.leftMargin = layout_distance/2;
				// lightText.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				int takeimage = getResources().getIdentifier("takeimage", "id", this.getPackageName());
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage2.setLayoutParams(layoutParams);

				layoutParams = new RelativeLayout.LayoutParams(layout_distance, layout_distance);
				layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
				layoutParams.addRule(RelativeLayout.ALIGN_TOP, takeimage);
				layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				layoutParams.leftMargin = layout_distance;
				lightImage1.setLayoutParams(layoutParams);

			}
			break;
		}

		camera.startPreview();
	}

	public void finish() {
		super.finish();
		try {
			if (camera != null) {
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void checkCamerParameter() {
		// Log.i(TAG, "checkCamerParameter");
		// Log.i(TAG, "isCatchPicture="+isCatchPicture);
		Camera camera = null;
		try {
			camera = Camera.open();
			if (camera != null) {
				Camera.Parameters parameters = camera.getParameters();
				List<Camera.Size> list = parameters.getSupportedPreviewSizes();
				Camera.Size size;

				int length = list.size();
				int previewWidth = 0;
				int previewheight = 0;
				int second_previewWidth = 0;
				int second_previewheight = 0;
				if (length == 1) {
					size = list.get(0);
					previewWidth = size.width;
					previewheight = size.height;
				} else {
					for (int i = 0; i < length; i++) {
						size = list.get(i);
						if (size.width <= 2048 && size.height <= 1536) {
							second_previewWidth = size.width;
							second_previewheight = size.height;
							if (previewWidth < second_previewWidth) {
								previewWidth = second_previewWidth;
								previewheight = second_previewheight;
							}
						}
					}
				}

				List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
				preWidth = previewSizes.get(1).width;
				preHeight = previewSizes.get(1).height;
				List<Camera.Size> PictureSizes = parameters.getSupportedPictureSizes();
				for (int i = 0; i < PictureSizes.size(); i++) {
					if (PictureSizes.get(i).width == 2048 && PictureSizes.get(i).height == 1536) {
						if (isCatchPicture == true) {
							break;
						}
						isCatchPicture = true;
						picWidth = 2048;
						picHeight = 1536;
					}
					if (PictureSizes.get(i).width == 1600 && PictureSizes.get(i).height == 1200) {
						isCatchPicture = true;
						picWidth = 1600;
						picHeight = 1200;
					}
					if (PictureSizes.get(i).width == 1280 && PictureSizes.get(i).height == 960) {
						isCatchPicture = true;
						picWidth = 1280;
						picHeight = 960;
						break;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			camera.stopPreview();
			camera.release();
			camera = null;
		} finally {
			if (camera != null) {
				try {
					camera.release();
					camera = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Log.i(TAG, "isCatchPicture="+isCatchPicture);
		}
	}
	
}
