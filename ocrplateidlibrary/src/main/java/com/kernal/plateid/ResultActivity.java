package com.kernal.plateid;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;

/**
 * 
 * 
 * 项目名称：plate_id_sample_service 类名称：ResultActivity 类描述： 结果显示界面 显示识别结果等信息 创建人：张志朋
 * 创建时间：2016-1-29 上午10:54:23 修改人：user 修改时间：2016-1-29 上午10:54:23 修改备注：
 * 
 * @version
 * 
 */
public class ResultActivity extends Activity {

	public static final String PATH = Environment.getExternalStorageDirectory()
			.toString();
	public RecogService.MyBinder recogBinder;
	private int iInitPlateIDSDK = -1;
	private int imageformat = 0;
	private int bVertFlip = 0;
	private int bDwordAligned = 1;
	private String[] fieldvalue = new String[14];
	private int nRet = -1;
	private String recogPicPath;
	private boolean bGetVersion = false;
	private int width = 1080;
	private int height = 1920;
	private EditText resultEditText;
	private ImageView resultImageView;
	private int screen_height;
	private int screen_width;
	private int image_layout_height;
	private int image_layout_width;
	private Bitmap bitmap;
	private String pic;
	private final int SYSTEM_RESULT_CODE = 2;
	private final int SELECT_RESULT_CODE = 3;

	int[] fieldname = { R.string.plate_number, R.string.plate_color,
			R.string.plate_color_code, R.string.plate_type_code,
			R.string.plate_reliability, R.string.plate_brightness_reviews,
			R.string.plate_move_orientation, R.string.plate_leftupper_pointX,
			R.string.plate_leftupper_pointY, R.string.plate_rightdown_pointX,
			R.string.plate_rightdown_pointY, R.string.plate_elapsed_time,
			R.string.plate_light, R.string.plate_car_color };

	public ServiceConnection recogConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			
			recogConn = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// System.out.println("ResultActivity onServiceConnected");
			recogBinder = (RecogService.MyBinder) service;
			iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();
			if (iInitPlateIDSDK != 0) {

				nRet = iInitPlateIDSDK;
				String[] str = { "" + iInitPlateIDSDK };
				getResult(str);
			} else {
				// recogBinder.setRecogArgu(recogPicPath,
				// imageformat,
				// bGetVersion, bVertFlip, bDwordAligned);
				PlateCfgParameter cfgparameter = new PlateCfgParameter();
				cfgparameter.armpolice = 4;// 单层武警车牌是否开启:4是；5不是
				cfgparameter.armpolice2 = 16;// 双层武警车牌是否开启:16是；17不是
				cfgparameter.embassy = 12;// 使馆车牌是否开启:12是；13不是
				cfgparameter.individual = 0;// 是否开启个性化车牌:0是；1不是
				// cfgparameter.nContrast = 9;//
				// 清晰度指数(取值范围0-9,最模糊时设为1;最清晰时设为9)
				cfgparameter.nOCR_Th = 0;
				cfgparameter.nPlateLocate_Th = 5;// 识别阈值(取值范围0-9,5:默认阈值0:最宽松的阈值9:最严格的阈值)
				cfgparameter.onlylocation = 15;// 只定位车牌是否开启:14是；15不是
				cfgparameter.tworowyellow = 2;// 双层黄色车牌是否开启:2是；3不是
				cfgparameter.tworowarmy = 6;// 双层军队车牌是否开启:6是；7不是
				cfgparameter.szProvince = "";// 省份顺序
				cfgparameter.onlytworowyellow = 11;// 只识别双层黄牌是否开启:10是；11不是
				cfgparameter.tractor = 8;// 农用车车牌是否开启:8是；9不是
				cfgparameter.bIsNight = 1;// 是否夜间模式：1是；0不是
				cfgparameter.newEnergy  = 24; //新能源车牌开启    
				cfgparameter.consulate = 22;  //领事馆车牌开启
				// //废弃参数
				recogBinder.setRecogArgu(cfgparameter, imageformat, bVertFlip,
						bDwordAligned);

				// fieldvalue =
				// recogBinder.doRecog(recogPicPath, width,
				// height);
				PlateRecognitionParameter prp = new PlateRecognitionParameter();
				prp.height = height;// 图像高度
				prp.width = width;// 图像宽度
				prp.pic = recogPicPath;// 图像文件
				// prp.dataFile = dataFile;
				// prp.isCheckDevType =
				// true;//检查设备型号授权所用参数,另一个参数为devCode
				prp.devCode = Devcode.DEVCODE;
				// prp.versionfile =
				// Environment.getExternalStorageDirectory().toString()+"/AndroidWT/wtversion.lsc";;
				System.out.println("图像宽高"+height+"    "+width);
				fieldvalue = recogBinder.doRecogDetail(prp);
				nRet = recogBinder.getnRet();
				if (nRet != 0) {
					String[] str = { "" + nRet };
					getResult(str);
				} else {
					getResult(fieldvalue);
				}

			}

			if (recogBinder != null) {
				unbindService(recogConn);
			
			}
		}
	};
	private LayoutParams layoutParams;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// PlateProjectTool.addActivityList(ResultActivity.this);
		setContentView(R.layout.result);
		Intent intent = getIntent();
		RecogService.initializeType = false;
		recogPicPath = intent.getStringExtra("recogImagePath");
		resultImageView = (ImageView) findViewById(R.id.lastfinallyshowimage);
		resultEditText = (EditText) findViewById(R.id.edit_file);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screen_height = dm.heightPixels;
		screen_width = dm.widthPixels;
		System.out.println("识别图片的路径" + recogPicPath);
		float height_width = (float) 0.75;
		File file = new File(recogPicPath);
		if (file.exists()) {
			try {
				BitmapFactory.Options opts = new BitmapFactory.Options();
				FileInputStream input = new FileInputStream(file);
				opts.inInputShareable = true;
				opts.inPurgeable = true;				
				bitmap = BitmapFactory.decodeStream(input, null, opts);
				height_width = (float) bitmap.getHeight() / bitmap.getWidth();
				input.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			height_width = (float) 0.75;
		}

		// 目前只支持1920*1080的图片
	
		if ((bitmap.getHeight() <= 1920 && bitmap.getWidth() <= 1080)||bitmap.getHeight() <= 1080&& bitmap.getWidth() <= 1920) {
			height = bitmap.getHeight();
			width  = bitmap.getWidth();
			new Thread() {
				public void run() {
					Intent recogIntent = new Intent(getApplicationContext(),
							RecogService.class);
					bindService(recogIntent, recogConn,
							Service.BIND_AUTO_CREATE);
				};

			}.start();
		} else {
			Toast.makeText(ResultActivity.this, "读取图片错误，图片超过1920*1080分辨率",  
					Toast.LENGTH_LONG).show();
			Intent intentPIC  = new Intent("kernal.plateid.MainActivity");
			startActivity(intentPIC);
			finish();
		}

		if (screen_height * 0.75 == screen_width
				|| screen_width * 0.75 == screen_height) {// 判断是否属于4比3的分辨率并分别配置布局参数
			layoutParams = new LayoutParams(screen_height - 60,
					(int) (screen_height * 0.75));
		} else {
			if (height_width > 1.4)
				height_width = (float) 1.4;
			layoutParams = new LayoutParams(screen_height - 60,
					(int) ((screen_width - 60) * height_width));

		}
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
				RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
				RelativeLayout.TRUE);

		layoutParams.leftMargin = 30;
		layoutParams.rightMargin = 30;
		layoutParams.topMargin = 30;

		resultImageView.setLayoutParams(layoutParams);

		resultImageView.setBackgroundDrawable(new BitmapDrawable(bitmap));

		// hepx140807
		Button takePic = (Button) this.findViewById(R.id.takePic);
		Button backIndex = (Button) this.findViewById(R.id.backIndex);
		backIndex.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(ResultActivity.this, MainActivity.class);
				ResultActivity.this.finish();
				startActivity(intent);
			}
		});
		takePic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {

				if (readIntPreferences("PlateService", "picWidth") != 0
						&& readIntPreferences("PlateService", "picHeight") != 0
						&& readIntPreferences("PlateService", "preWidth") != 0
						&& readIntPreferences("PlateService", "preHeight") != 0
						&& readIntPreferences("PlateService", "preMaxWidth") != 0
						&& readIntPreferences("PlateService", "preMaxHeight") != 0) {
					Intent camera_intent = new Intent(ResultActivity.this,
							CameraActivity.class);
					camera_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(camera_intent);

				} else {
					Intent sysCameraIntent = new Intent(
							MediaStore.ACTION_IMAGE_CAPTURE);
					String sdStatus = Environment.getExternalStorageState();
					String sdDirString = PATH + "/kernalimage";
					long datetime = System.currentTimeMillis();
					pic = sdDirString + "/plateid" + datetime + ".jpg";
					sysCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
							Uri.fromFile(new File(pic)));
					startActivityForResult(sysCameraIntent, SYSTEM_RESULT_CODE);
					sysCameraIntent = new Intent(
							MediaStore.ACTION_IMAGE_CAPTURE);
					sysCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,
							Uri.fromFile(new File(pic)));
					startActivityForResult(sysCameraIntent, SYSTEM_RESULT_CODE);
				}
			}
		});

	}

	protected int readIntPreferences(String perferencesName, String key) {
		SharedPreferences preferences = getSharedPreferences(perferencesName,
				MODE_PRIVATE);
		int result = preferences.getInt(key, 0);
		return result;
	}

	private void getResult(String[] fieldvalue) {

		if (nRet != 0) {
			String nretString = nRet + "";
			if (nretString.equals("-1001")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_readJPG_error));
			} else if (nretString.equals("-10001")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_noInit_function));
			} else if (nretString.equals("-10003")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_validation_faile));
			} else if (nretString.equals("-10004")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_serial_number_null));
			} else if (nretString.equals("-10005")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_disconnected_server));
			} else if (nretString.equals("-10006")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_obtain_activation_code));
			} else if (nretString.equals("-10007")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_noexist_serial_number));
			} else if (nretString.equals("-10008")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_serial_number_used));
			} else if (nretString.equals("-10009")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_unable_create_authfile));
			} else if (nretString.equals("-10010")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_check_activation_code));
			} else if (nretString.equals("-10011")) {
				resultEditText
						.setText(getString(R.string.recognize_result) + nRet
								+ "\n"
								+ getString(R.string.failed_other_errors));
			} else if (nretString.equals("-10012")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n" + getString(R.string.failed_not_active));
			} else if (nretString.equals("-10015")) {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n"
						+ getString(R.string.failed_check_failure));
			} else {
				resultEditText.setText(getString(R.string.recognize_result)
						+ nRet + "\n");
			}
		} else {
			String result = "";
			String[] resultString;
			String timeString = "";
			String boolString = "";
			boolString = fieldvalue[0];
			if (boolString != null && !boolString.equals("")) {
				resultString = boolString.split(";");
				int lenght = resultString.length;
				if (lenght == 1) {
					// result = "";
					if (fieldvalue[11] != null && !fieldvalue[11].equals("")) {
						int time = Integer.parseInt(fieldvalue[11]);
						time = time / 1000;
						timeString = "" + time;
					} else {
						timeString = "null";
					}

					if (null != fieldname) {
						result += getString(fieldname[0]) + ":" + fieldvalue[0]
								+ ";\n";
						result += getString(fieldname[1]) + ":" + fieldvalue[1]
								+ ";\n";
						result += getString(R.string.recognize_time) + ":"
								+ timeString + "ms" + ";\n";
					}
				} else {
					String itemString = "";
					// result = "";
					for (int i = 0; i < lenght; i++) {// for (int i = 0; i <
														// lenght; i++) {
														// //将0换成lenght-1
														// 强制输出一个识别结果
						itemString = fieldvalue[0];
						resultString = itemString.split(";");
						result += getString(fieldname[0]) + ":"
								+ resultString[i] + ";\n";
						itemString = fieldvalue[1];
						resultString = itemString.split(";");
						result += getString(fieldname[1]) + ":"
								+ resultString[i] + ";\n";
						itemString = fieldvalue[11];
						resultString = itemString.split(";");
						if (resultString[i] != null
								&& !resultString[i].equals("")) {
							int time = Integer.parseInt(resultString[i]);
							time = time / 1000;
							timeString = "" + time;
						} else {
							timeString = "null";
						}
						result += getString(R.string.recognize_time) + ":"
								+ timeString + "ms" + ";\n";
						result += "\n";
						result += "\n";
					}
				}
			} else {
				result += getString(fieldname[0]) + ":" + fieldvalue[0] + ";\n";
				result += getString(fieldname[1]) + ":" + fieldvalue[1] + ";\n";
				result += getString(R.string.recognize_time) + ":" + "null"
						+ ";\n";
			}
			resultEditText.setText(getString(R.string.recognize_result) + nRet
					+ "\n" + result);
		}
		nRet = -1;
		fieldvalue = null;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			Intent intent = new Intent(getApplicationContext(),
					MainActivity.class);
			startActivity(intent);
			ResultActivity.this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	 //hepx140812
	 @Override
	 protected void onDestroy() {

	 super.onDestroy();
		 if (bitmap != null) {
			 if (!bitmap.isRecycled()) {
				 bitmap.recycle();
			 }
			 bitmap = null;
		 }
	 }

}
