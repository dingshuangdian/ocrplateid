package com.kernal.plateid;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
//未使用
//自动采集界面，调用系统的预览回调方法，获取设备的重力感应装置，把获取的xyz坐标进行处理，当处于相对静止时，获取设备返回的数据，在进行保存识别；
public class PlateVideoCameraActivity extends Activity implements SurfaceHolder.Callback{
	private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/KernalImage/";
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private DisplayMetrics dm = new DisplayMetrics();;
    private int screen_height,screen_width;
    private SensorManager sManager = null;
    private SensorEventListener myListener = null;
    private float x,y,z;
    private final int UPTATE_Difference_TIME = 200;   
    private final float MoveDifference = 0.005f;  
    private final float MoveDifferencemin = 0.001f;  
    private final int ListMaxLen = 3;
    private long last_Time;
    private List<float[]> mlist;
    private ImageView backImageView;
    private Camera.AutoFocusCallback mAutoFocusCallback;
    private ImageButton text1,text2,text4;
    private int bitmapZoom = 1;
    private RelativeLayout.LayoutParams lParams; 
    private Boolean paidBool = true;
    private TextView textView,lockTextView;
    private ImageView lockImageView; 
    private Bitmap bitmap;
    private Boolean unlockBoolean = true;
    private Boolean recognitionBoolean = true;
    private Boolean noShakeBoolean = false;
    private Boolean autoFocusBoolean = false;
    private Boolean previewBoolean = false;
    private byte[] imageData;
    private int preMaxWidth;
    private int preMaxHeight;
    private String recogPicturePath;
    public RecogService.MyBinder recogBinder;
    private int imageformat = 1;
    private int bVertFlip = 0;
    private int bDwordAligned = 1;
    private boolean bGetVersion = false;
    private int nRet = -1;
    private String[] fieldvalue = new String[14];
    private int width = 420;
    private int height = 232;
    public Intent recogIntent;
    private float widthScale = (float) 1.0;
    private float heightScale = (float) 1.0;
    private int iInitPlateIDSDK;
    private ImageView video_inside_frame_left,video_inside_frame_right;
    private ImageView video_outside_frame_left,video_outside_frame_right;
    private RelativeLayout.LayoutParams inside_left_horizontal_params_max,inside_right_horizontal_params_max;
    private RelativeLayout.LayoutParams inside_left_horizontal_params_middle,inside_right_horizontal_params_middle;
    private RelativeLayout.LayoutParams inside_left_horizontal_params_min,inside_right_horizontal_params_min;
    private RelativeLayout.LayoutParams inside_left_vertical_params_max,inside_right_vertical_params_max;
    private RelativeLayout.LayoutParams inside_left_vertical_params_middle,inside_right_vertical_params_middle;
    private RelativeLayout.LayoutParams inside_left_vertical_params_min,inside_right_vertical_params_min;
    
    private RelativeLayout.LayoutParams outside_left_horizontal_params_max,outside_right_horizontal_params_max;
    private RelativeLayout.LayoutParams outside_left_horizontal_params_middle,outside_right_horizontal_params_middle;
    private RelativeLayout.LayoutParams outside_left_horizontal_params_min,outside_right_horizontal_params_min;
    private RelativeLayout.LayoutParams outside_left_vertical_params_max,outside_right_vertical_params_max;
    private RelativeLayout.LayoutParams outside_left_vertical_params_middle,outside_right_vertical_params_middle;
    private RelativeLayout.LayoutParams outside_left_vertical_params_min,outside_right_vertical_params_min;
    private int[] fieldname = { R.string.plate_number, R.string.plate_color, R.string.plate_color_code,
            R.string.plate_type_code,R.string.plate_reliability,R.string.plate_brightness_reviews,
            R.string.plate_move_orientation, R.string.plate_leftupper_pointX,R.string.plate_leftupper_pointY, 
            R.string.plate_rightdown_pointX, R.string.plate_rightdown_pointY, R.string.plate_elapsed_time,
            R.string.plate_light, R.string.plate_car_color};

    //绑定识别服务
    public ServiceConnection recogConn = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            recogConn = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            recogBinder = (RecogService.MyBinder) service;

        }
    };
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PlateProjectTool.addActivityList(PlateVideoCameraActivity.this);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.plate_video_camera);
        preMaxWidth = readIntPreferences("PlateService", "preMaxWidth");
        preMaxHeight = readIntPreferences("PlateService", "preMaxHeight");
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screen_width = Math.max(dm.widthPixels , dm.heightPixels);
        screen_height = Math.min(dm.widthPixels , dm.heightPixels);
       
	}
	
	
	
	protected void onStart() {
		super.onStart();
        recognitionBoolean = true;
        unlockBoolean = true;
        autoFocusBoolean = true;
        findview();
        startPlateRecogService();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(PlateVideoCameraActivity.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        initCamera();
        mlist = new ArrayList<float[]>();
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myListener = new SensorEventListener(){
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                
            }
            public void onSensorChanged(SensorEvent event) {
                long now_Time = System.currentTimeMillis();
                long time_Difference = now_Time - last_Time;
                if (time_Difference >= UPTATE_Difference_TIME) {
                    last_Time = now_Time;
                    x = event.values[SensorManager.DATA_X];
                    y = event.values[SensorManager.DATA_Y];
                    z = event.values[SensorManager.DATA_Z];
                    double move_Difference = getStableFloat(x, y, z);
                    if (move_Difference <= MoveDifference && move_Difference >= MoveDifferencemin) {
                        noShakeBoolean = true;
                        startAutoFocus();
                    }
                }
            }
        };
        sManager.registerListener(myListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);

	}
	
	private void startPlateRecogService(){
        recogIntent = new Intent(getApplicationContext(),
        		RecogService.class);
        bindService(recogIntent, recogConn, Service.BIND_AUTO_CREATE);
	}
	
	private void unBindPlateRecogService(){
        try {
            if (recogBinder != null) {
                unbindService(recogConn);
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	
	 //调用识别方法
	private void recogCarPlatePicture(){
		if (recogBinder != null) {
			iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();
		      if (iInitPlateIDSDK != 0) {
		    	  textView.setText("车牌识别初始化失败！返回错误代码：" + iInitPlateIDSDK);
		          unlockBoolean = false;
		          lockImageView.setBackgroundResource(R.drawable.lock);
		      } else {

		          recogBinder.setRecogArgu(recogPicturePath,imageformat, bGetVersion,
	                        bVertFlip, bDwordAligned);
		          nRet = recogBinder.getnRet();
		          fieldvalue = recogBinder.doRecogDetail(null,recogPicturePath, width, height,"");
		          nRet = recogBinder.getnRet();
		          if (nRet != 0) {
		              textView.setText("车牌识别识别失败！返回错误代码：" + nRet);
		          } else {
		              if (fieldvalue[1] == null || fieldvalue[1].equals("null")) {
		                  recognitionBoolean = true;
		                  autoFocusBoolean = true;
		            	  lockTextView.setText("");
		              } else {
		                  unlockBoolean = false;
		                  lockTextView.setVisibility(View.VISIBLE);
		                  lockTextView.setText(getResources().getString(R.string.clear));
		                  lockTextView.setTextColor(Color.WHITE);
		                  lockImageView.setBackgroundResource(R.drawable.lock);
		                  getResult(fieldvalue);
		                  recognitionBoolean = true;
		                  autoFocusBoolean = true;
		              }
		          }
		      }
		}

	}
	
    private void findview() {

    	int min_lenght = Math.min(screen_height, screen_width);
    	
        surfaceView = (SurfaceView) findViewById(R.id.camerasurfaceview);
        backImageView = (ImageView) findViewById(R.id.imagebackground);
        textView = (TextView) findViewById(R.id.resulttextview);
        textView.setText("");
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        setConfigurationChangedLayoutParam(dm.widthPixels,dm.heightPixels);

        
        video_inside_frame_left = (ImageView) findViewById(R.id.platefreevideoframeinsideleft);
        video_inside_frame_right = (ImageView) findViewById(R.id.platefreevideoframeinsideright);
        video_outside_frame_left = (ImageView) findViewById(R.id.platefreevideoframeoutsideleft);
        video_outside_frame_right = (ImageView) findViewById(R.id.platefreevideoframeoutsideright);

        
        int inside_width = 0;
        int inside_height = 0;
        int outside_width = 0;
        int outside_height = 0;
        int margin = 0;
        int screen_pic_width = 0;
        
        
        inside_width = (int)(screen_height*0.06);
        inside_height = (int)(screen_height*0.1);
        screen_pic_width = 80*screen_width/preMaxWidth;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        inside_left_horizontal_params_min = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_horizontal_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_horizontal_params_min.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_horizontal_params_min.leftMargin = margin;
        
        inside_right_horizontal_params_min = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_horizontal_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_horizontal_params_min.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_horizontal_params_min.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.075);
        outside_height = (int)(screen_height*0.125);
        screen_pic_width = 140*screen_width/preMaxWidth;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        outside_left_horizontal_params_min = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_horizontal_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_horizontal_params_min.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_horizontal_params_min.leftMargin = margin;
        
        outside_right_horizontal_params_min = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_horizontal_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_horizontal_params_min.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_horizontal_params_min.rightMargin = margin;
        
        
        inside_width = (int)(screen_height*0.075);
        inside_height = (int)(screen_height*0.125);
        screen_pic_width = 160*screen_width/preMaxWidth;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        inside_left_horizontal_params_middle = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_horizontal_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_horizontal_params_middle.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_horizontal_params_middle.leftMargin = margin;
        
        inside_right_horizontal_params_middle = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_horizontal_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_horizontal_params_middle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_horizontal_params_middle.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.09);
        outside_height = (int)(screen_height*0.15);
        screen_pic_width = 280*screen_width/preMaxWidth;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        outside_left_horizontal_params_middle = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_horizontal_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_horizontal_params_middle.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_horizontal_params_middle.leftMargin = margin;
        
        outside_right_horizontal_params_middle = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_horizontal_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_horizontal_params_middle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_horizontal_params_middle.rightMargin = margin;
        
        
        inside_width = (int)(screen_height*0.09);
        inside_height = (int)(screen_height*0.15);
        screen_pic_width = 320*screen_width/preMaxWidth;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        inside_left_horizontal_params_max = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_horizontal_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_horizontal_params_max.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_horizontal_params_max.leftMargin = margin;
        
        inside_right_horizontal_params_max = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_horizontal_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_horizontal_params_max.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_horizontal_params_max.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.105);
        outside_height = (int)(screen_height*0.175);
        screen_pic_width = 560*screen_width/preMaxWidth;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_width-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_width-(screen_pic_width+10))/2);
		}
        
        outside_left_horizontal_params_max = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_horizontal_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_horizontal_params_max.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_horizontal_params_max.leftMargin = margin;
        
        outside_right_horizontal_params_max = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_horizontal_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_horizontal_params_max.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_horizontal_params_max.rightMargin = margin;
        
        
        
        inside_width = (int)(screen_height*0.06);
        inside_height = (int)(screen_height*0.1);
        screen_pic_width = 80*screen_height/preMaxHeight;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        
        inside_left_vertical_params_min = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_vertical_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_vertical_params_min.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_vertical_params_min.leftMargin = margin;
        
        inside_right_vertical_params_min = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_vertical_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_vertical_params_min.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_vertical_params_min.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.075);
        outside_height = (int)(screen_height*0.125);
        screen_pic_width = 140*screen_height/preMaxHeight;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        
        outside_left_vertical_params_min = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_vertical_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_vertical_params_min.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_vertical_params_min.leftMargin = margin;
        
        outside_right_vertical_params_min = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_vertical_params_min.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_vertical_params_min.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_vertical_params_min.rightMargin = margin;
        
        
        inside_width = (int)(screen_height*0.075);
        inside_height = (int)(screen_height*0.125);
        screen_pic_width = 160*screen_height/preMaxHeight;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        
        inside_left_vertical_params_middle = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_vertical_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_vertical_params_middle.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_vertical_params_middle.leftMargin = margin;
        
        inside_right_vertical_params_middle = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_vertical_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_vertical_params_middle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_vertical_params_middle.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.09);
        outside_height = (int)(screen_height*0.15);
        screen_pic_width = 280*screen_height/preMaxHeight;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        
        outside_left_vertical_params_middle = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_vertical_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_vertical_params_middle.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_vertical_params_middle.leftMargin = margin;
        
        outside_right_vertical_params_middle = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_vertical_params_middle.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_vertical_params_middle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_vertical_params_middle.rightMargin = margin;
        
        
        inside_width = (int)(screen_height*0.09);
        inside_height = (int)(screen_height*0.15);
        screen_pic_width = 320*screen_height/preMaxHeight;
        if (inside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(inside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        
        inside_left_vertical_params_max = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_left_vertical_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_left_vertical_params_max.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        inside_left_vertical_params_max.leftMargin = margin;
        
        inside_right_vertical_params_max = new RelativeLayout.LayoutParams(inside_width, inside_height);
        inside_right_vertical_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        inside_right_vertical_params_max.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        inside_right_vertical_params_max.rightMargin = margin;
        
        outside_width = (int)(screen_height*0.105);
        outside_height = (int)(screen_height*0.175);
        screen_pic_width = 560*screen_height/preMaxHeight;
        if (outside_width > (screen_pic_width/2)) {
        	margin = (int) ((screen_height-(outside_width*2+20))/2);
		}else {
			margin = (int) ((screen_height-(screen_pic_width+10))/2);
		}
        if (margin <= 0) {
        	margin = 0;
		}

        outside_left_vertical_params_max = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_left_vertical_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_left_vertical_params_max.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        outside_left_vertical_params_max.leftMargin = margin;
        
        outside_right_vertical_params_max = new RelativeLayout.LayoutParams(outside_width, outside_height);
        outside_right_vertical_params_max.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        outside_right_vertical_params_max.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        outside_right_vertical_params_max.rightMargin = margin;
        
        text1 = (ImageButton) findViewById(R.id.platevideotext1);
        lParams = new RelativeLayout.LayoutParams((int)(min_lenght*0.075),(int)(min_lenght*0.075));
        lParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.LEFT_OF, R.id.platevideotext2);
        lParams.topMargin = 5;
        text1.setLayoutParams(lParams);
        text1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                bitmapZoom = 1;
                showTwoFrameLayout();
            }
        });
        
        text2 = (ImageButton) findViewById(R.id.platevideotext2);
        lParams = new RelativeLayout.LayoutParams((int)(min_lenght*0.075),(int)(min_lenght*0.075));
        lParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.LEFT_OF, R.id.platevideotext4);
        lParams.leftMargin = 15;
        lParams.rightMargin = 15;
        lParams.topMargin = 5;
        text2.setLayoutParams(lParams);
        text2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                bitmapZoom = 2;
                showTwoFrameLayout();
            }
        });
        
        text4 = (ImageButton) findViewById(R.id.platevideotext4);
        lParams = new RelativeLayout.LayoutParams((int)(min_lenght*0.075),(int)(min_lenght*0.075));
        lParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        lParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        lParams.topMargin = 5;
        lParams.rightMargin = 5;
        text4.setLayoutParams(lParams);
        text4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                bitmapZoom = 4;
                showTwoFrameLayout();
            }
        });
        showTwoFrameLayout();
        lockImageView = (ImageView) findViewById(R.id.unlockbutton);
        lockImageView.setBackgroundResource(R.drawable.unlock);
        lockImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!lockTextView.getText().toString().equals("清空")) {
                    unlockBoolean = false;
                    lockTextView.setText("清空");
                    lockTextView.setTextColor(Color.WHITE);
                    lockImageView.setBackgroundResource(R.drawable.lock);
                }else {
                    lockTextView.setText("");
                    textView.setText("");
                    unlockBoolean = true;
                    lockImageView.setBackgroundResource(R.drawable.unlock);
                }
            }
        });
        
        lockTextView = (TextView) findViewById(R.id.locktextview);
        lockTextView.setText("");
        lockTextView.setTextColor(Color.WHITE);
        lockTextView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!lockTextView.getText().toString().equals("清空")) {
                    lockTextView.setText("清空");
                    unlockBoolean = false;
                    lockImageView.setBackgroundResource(R.drawable.lock);
                }else {
                    lockTextView.setText("");
                    textView.setText("");
                    unlockBoolean = true;
                    lockImageView.setBackgroundResource(R.drawable.unlock);
                }
            }
        });
        
    }
    
    private void showTwoFrameLayout(){

        getWindowManager().getDefaultDisplay().getMetrics(dm);
        switch (bitmapZoom) {
			case 1:
				if (dm.widthPixels > dm.heightPixels) {
					video_inside_frame_left.setLayoutParams(inside_left_horizontal_params_min);
					video_inside_frame_right.setLayoutParams(inside_right_horizontal_params_min);
					video_outside_frame_left.setLayoutParams(outside_left_horizontal_params_min);
					video_outside_frame_right.setLayoutParams(outside_right_horizontal_params_min);
				}else {
					video_inside_frame_left.setLayoutParams(inside_left_vertical_params_min);
					video_inside_frame_right.setLayoutParams(inside_right_vertical_params_min);
					video_outside_frame_left.setLayoutParams(outside_left_vertical_params_min);
					video_outside_frame_right.setLayoutParams(outside_right_vertical_params_min);
				}
				break;
			case 2:
				if (dm.widthPixels > dm.heightPixels) {
					video_inside_frame_left.setLayoutParams(inside_left_horizontal_params_middle);
					video_inside_frame_right.setLayoutParams(inside_right_horizontal_params_middle);
					video_outside_frame_left.setLayoutParams(outside_left_horizontal_params_middle);
					video_outside_frame_right.setLayoutParams(outside_right_horizontal_params_middle);
				}else {
					video_inside_frame_left.setLayoutParams(inside_left_vertical_params_middle);
					video_inside_frame_right.setLayoutParams(inside_right_vertical_params_middle);
					video_outside_frame_left.setLayoutParams(outside_left_vertical_params_middle);
					video_outside_frame_right.setLayoutParams(outside_right_vertical_params_middle);
				}
				break;
			case 4:
				if (dm.widthPixels > dm.heightPixels) {
					video_inside_frame_left.setLayoutParams(inside_left_horizontal_params_max);
					video_inside_frame_right.setLayoutParams(inside_right_horizontal_params_max);
					video_outside_frame_left.setLayoutParams(outside_left_horizontal_params_max);
					video_outside_frame_right.setLayoutParams(outside_right_horizontal_params_max);
				}else {
					video_inside_frame_left.setLayoutParams(inside_left_vertical_params_max);
					video_inside_frame_right.setLayoutParams(inside_right_vertical_params_max);
					video_outside_frame_left.setLayoutParams(outside_left_vertical_params_max);
					video_outside_frame_right.setLayoutParams(outside_right_vertical_params_max);
				}
				break;
		}
        
    }
    

    //对识别结果进行处理
    private void getResult(String[] fieldvalue) {
        String result = "";
        String[] resultStrings;
        String timeString = "";
        if (fieldvalue[0] != null && !fieldvalue[0].equals("")) {
        	resultStrings = fieldvalue[0].split(";");
            int lenght = resultStrings.length;
            if (lenght > 3) {
            	lenght = 3;
			}
            if (lenght == 1) {
                if (fieldvalue[11] != null && !fieldvalue[11].equals("")) {
                    int time = Integer.parseInt(fieldvalue[11]);
                    time = time/1000;
                    timeString = ""+time;
                }else {
                    timeString = "null";
                }
                if (null != fieldname) {
                    result += getString(fieldname[0]) + ":" + fieldvalue[0] + ";\n";
                    result += getString(fieldname[1]) + ":" + fieldvalue[1] + ";\n";
                    result += getString(R.string.recognize_time) + ":" + timeString +"ms"+ ";\n";
                }
            }else {
                String itemString = "";
                for (int i = 0; i < lenght; i++) {
                    itemString = fieldvalue[0];
                    resultStrings = itemString.split(";");
                    result += getString(fieldname[0]) + ":" + resultStrings[i] + ";\n";
                    itemString = fieldvalue[1];
                    resultStrings = itemString.split(";");
                    result += getString(fieldname[1]) + ":" + resultStrings[i] + ";\n";
                    itemString = fieldvalue[11];
                    resultStrings = itemString.split(";");
                    if (resultStrings[i] != null && !resultStrings[i].equals("")) {
                        int time = Integer.parseInt(resultStrings[i]);
                        time = time/1000;
                        timeString = ""+time;
                    }else {
                        timeString = "null";
                    }
                    result += getString(R.string.recognize_time) + ":" + timeString +"ms"+ ";\n";
                    result +=  "\n";
                }
            }
        }else {
            result += getString(fieldname[0]) + ":" + fieldvalue[0] + ";\n";
            result += getString(fieldname[1]) + ":" + fieldvalue[1] + ";\n";
            result += getString(R.string.recognize_time) + ":" + "null" + ";\n";
        }
        textView.setText(getString(R.string.recognize_result) + "\n" + result);
        fieldvalue = null;
    }
    
    private class mPreviewCallback implements Camera.PreviewCallback{
        public void onPreviewFrame(byte[] data, Camera camera) {
        	imageData = data;
            previewBoolean = true;
        }
    }
    
    protected int readIntPreferences(String perferencesName, String key) {
    	SharedPreferences preferences = getSharedPreferences(perferencesName, MODE_PRIVATE);
	    int result = preferences.getInt(key, 0);
	    return result;
    }
    
    
    //通过传进来的xyz坐标，进行处理，来判断是否处于静止状态
    private float getStableFloat(float x,float y,float z){
        float move_Difference = 0.0f;
        float[] floatdata = {x,y,z};
        if(mlist.size() < ListMaxLen){
            mlist.add(floatdata);
        }else {
            mlist.remove(0);
            mlist.add(floatdata);
        }
        if(mlist.size() < ListMaxLen){
            return 0.1f;
        }
        float sumx = 0;
        float sumy = 0;
        float sumz = 0;
        int len = mlist.size();
        for (int i = 0; i < len; i++){
            float [] dd = (float [])mlist.get(i);
            sumx += dd[0];
            sumy += dd[1];
            sumz += dd[2];
        }
        float avgx = sumx/len;
        float avgy = sumy/len;
        float avgz = sumz/len;
        for (int i = 0; i < len; i++){
            float [] dd = (float [])mlist.get(i);
            move_Difference = (dd[0]-avgx)*(dd[0]-avgx)+(dd[1]-avgy)*(dd[1]-avgy)+(dd[2]-avgz)*(dd[2]-avgz);
        }
        return move_Difference;
    }
    
    //根据不同的状态，对bitmap进行基本操作
    private void getPreviewBitmap() {
        Camera.Parameters parameters = camera.getParameters();
        int format = parameters.getPreviewFormat();
         Matrix matrix = new Matrix(); 
        if (format == PixelFormat.YCbCr_420_SP || format == PixelFormat.YCbCr_422_I) {
            int w = parameters.getPreviewSize().width;
            int h = parameters.getPreviewSize().height;
            int[] i = new int[imageData.length];
            decodeYUV420SP(i, imageData, w, h);
            bitmap = Bitmap.createBitmap(i, w, h, Bitmap.Config.RGB_565);
        }
        if (format == PixelFormat.JPEG || format == PixelFormat.RGB_565) {
        	bitmap = BitmapFactory.decodeByteArray(imageData, 0,
            		imageData.length);
        }
        if (bitmapZoom == 2) {
             matrix.postScale(0.5f,0.5f);
             bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,false);
        }
        if (bitmapZoom == 4) {
             matrix.postScale(0.25f,0.25f);
             bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,false);
        }
        savePicture();
    }
    
    //根据不同的状态，对图片进行压缩，旋转，在最后保存到本地存储
    private void savePicture() {
        if (bitmap != null) {
            int uiRot = getWindowManager().getDefaultDisplay().getRotation();
            if (uiRot == 3) {
                if (paidBool) {
                     Matrix matrix = new Matrix();  
                     matrix.preRotate(90);  
                     Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
		                if (bitmap != rotate_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = rotate_bitmap;
						}
                     
		                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float)2048/bitmap.getWidth();
		                    heightScale = (float)1536/bitmap.getHeight();
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float)2048/bitmap.getWidth();
		                    heightScale = (float) 1.0;
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float) 1.0;
		                    heightScale = (float)1536/bitmap.getHeight();
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                
                }else {
                     Matrix matrix = new Matrix();  
                     matrix.preRotate(180);  
                     Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
		                if (bitmap != rotate_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = rotate_bitmap;
						}
		                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float)2048/bitmap.getWidth();
		                    heightScale = (float)1536/bitmap.getHeight();
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float)2048/bitmap.getWidth();
		                    heightScale = (float) 1.0;
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
		                	matrix = new Matrix();  
		                    widthScale = (float) 1.0;
		                    heightScale = (float)1536/bitmap.getHeight();
		                    matrix.postScale(widthScale,heightScale);
		                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
			                if (bitmap != cut_bitmap) {
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
									bitmap = null;
								}
								bitmap = cut_bitmap;
							}
		                }
		                
                }

            }
            else if (uiRot == 1 && paidBool) {
                Matrix matrix = new Matrix();  
                matrix.preRotate(270);  
                Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false); 
                if (bitmap != rotate_bitmap) {
					if (!bitmap.isRecycled()) {
						bitmap.recycle();
						bitmap = null;
					}
					bitmap = rotate_bitmap;
				}
                
                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
                	matrix = new Matrix();  
                    widthScale = (float)2048/bitmap.getWidth();
                    heightScale = (float)1536/bitmap.getHeight();
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
                	matrix = new Matrix();  
                    widthScale = (float)2048/bitmap.getWidth();
                    heightScale = (float) 1.0;
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
                	matrix = new Matrix();  
                    widthScale = (float) 1.0;
                    heightScale = (float)1536/bitmap.getHeight();
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
            }
            else if (uiRot == 2) {
                if (paidBool) {
                    Matrix matrix = new Matrix();  
                    matrix.preRotate(180);  
                    Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false); 
                    if (bitmap != rotate_bitmap) {
    					if (!bitmap.isRecycled()) {
    						bitmap.recycle();
    						bitmap = null;
    					}
    					bitmap = rotate_bitmap;
    				}
	                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float)2048/bitmap.getWidth();
	                    heightScale = (float)1536/bitmap.getHeight();
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
	                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float)2048/bitmap.getWidth();
	                    heightScale = (float) 1.0;
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
	                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float) 1.0;
	                    heightScale = (float)1536/bitmap.getHeight();
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
	                
                }else {
                    Matrix matrix = new Matrix();  
                    matrix.preRotate(270);  
                    Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false); 
                    if (bitmap != rotate_bitmap) {
    					if (!bitmap.isRecycled()) {
    						bitmap.recycle();
    						bitmap = null;
    					}
    					bitmap = rotate_bitmap;
    				}
                    
	                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float)2048/bitmap.getWidth();
	                    heightScale = (float)1536/bitmap.getHeight();
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
	                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float)2048/bitmap.getWidth();
	                    heightScale = (float) 1.0;
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
	                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
	                	matrix = new Matrix();  
	                    widthScale = (float) 1.0;
	                    heightScale = (float)1536/bitmap.getHeight();
	                    matrix.postScale(widthScale,heightScale);
	                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
		                if (bitmap != cut_bitmap) {
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
								bitmap = null;
							}
							bitmap = cut_bitmap;
						}
	                }
                }

            }
            else if (uiRot == 0 && paidBool == false) {
                Matrix matrix = new Matrix();  
                matrix.preRotate(90);  
                Bitmap rotate_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false); 
                if (bitmap != rotate_bitmap) {
					if (!bitmap.isRecycled()) {
						bitmap.recycle();
						bitmap = null;
					}
					bitmap = rotate_bitmap;
				}
                
                if (bitmap.getWidth() > 2048 && bitmap.getHeight() > 1536) {
                	matrix = new Matrix();  
                    widthScale = (float)2048/bitmap.getWidth();
                    heightScale = (float)1536/bitmap.getHeight();
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,1536, matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
                else if (bitmap.getWidth() > 2048 && bitmap.getHeight() <= 1536) {
                	matrix = new Matrix();  
                    widthScale = (float)2048/bitmap.getWidth();
                    heightScale = (float) 1.0;
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, 2048,bitmap.getHeight(), matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
                else if (bitmap.getWidth() <= 2048 && bitmap.getHeight() > 1536) {
                	matrix = new Matrix();  
                    widthScale = (float) 1.0;
                    heightScale = (float)1536/bitmap.getHeight();
                    matrix.postScale(widthScale,heightScale);
                	Bitmap cut_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),1536, matrix, true);
	                if (bitmap != cut_bitmap) {
						if (!bitmap.isRecycled()) {
							bitmap.recycle();
							bitmap = null;
						}
						bitmap = cut_bitmap;
					}
                }
            }

            File file = new File(PATH);
            if (!file.exists()) {
            	file.mkdirs();
            }
            recogPicturePath = PATH+"KernalPlateFree.jpg";
            file = new File(recogPicturePath);
            try {
            	if (file.exists()) {
                    file.delete();
				}
            	file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();

                if (bitmap != null) {
                    if (!bitmap.isRecycled()) {
                    	bitmap.recycle();
                    }
                	bitmap = null;
				}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void startAutoFocus(){
        if (autoFocusBoolean == false && noShakeBoolean && previewBoolean
                && recognitionBoolean && unlockBoolean) {
            
            if (unlockBoolean && recognitionBoolean) {

                try {
                    camera.autoFocus(mAutoFocusCallback);
                    getPreviewBitmap();
                    recognitionBoolean = false;
                    recogCarPlatePicture();
                } catch (Exception e) {
                }
            }
        }
         if(autoFocusBoolean){
             autoFocusBoolean = false;
             try {
                 if (camera != null) {
                     camera.cancelAutoFocus();
                }
            } catch (Exception e) {
               
            }
         }
     }
    
    //把nv21数组改变成 rgb格式
    public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height){
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++)
        {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) 
            {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) 
                {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }
    

	
	
    
    private void initCamera(){
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
        if (camera != null) {
            try {
            	Camera.Parameters parameters = camera.getParameters();
                parameters.setPictureFormat(PixelFormat.JPEG);
                parameters.setPreviewSize(preMaxWidth, preMaxHeight);
                camera.setParameters(parameters);
                camera.setPreviewCallback(new mPreviewCallback());
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
            	
            }
        }
    }

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
        try {
            camera.setPreviewDisplay(holder);
            Toast.makeText(getApplicationContext(),"开始自动识别!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {

        }
	}


	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        try {
        	Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(PixelFormat.JPEG);
            parameters.setPreviewSize(preMaxWidth, preMaxHeight);
            camera.setParameters(parameters);
            camera.setPreviewCallback(new mPreviewCallback());
            camera.setPreviewDisplay(surfaceHolder);
            int uiRot = getWindowManager().getDefaultDisplay().getRotation();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
         switch (uiRot) {
            case 0:
            	if (dm.widthPixels > dm.heightPixels) {
            		paidBool = true;
            		camera.setDisplayOrientation(0);
            	}else {
            		paidBool = false;
            		camera.setDisplayOrientation(90);
            	}
            break;
            
            case 1:
            	if (dm.heightPixels > dm.widthPixels) {
            		paidBool = true;
            		camera.setDisplayOrientation(270);
            	}else {
            		paidBool = false;
            		camera.setDisplayOrientation(0);
            	}
            break;
            
            case 2:
            	if (dm.widthPixels > dm.heightPixels) {
            		paidBool = true;
            		camera.setDisplayOrientation(180);
            	}else {
            		paidBool = false;
            		camera.setDisplayOrientation(270);
            	}
            break;
            
            case 3:
            	if (dm.heightPixels > dm.widthPixels) {
            		paidBool = true;
            		camera.setDisplayOrientation(90);
            	}else {
            		paidBool = false;
            		camera.setDisplayOrientation(180);
            	}
            break;
        }
         camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    //当旋转时，设备界面根据不同的位置进行相应的布局
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

         int uiRot = getWindowManager().getDefaultDisplay().getRotation();
         getWindowManager().getDefaultDisplay().getMetrics(dm);
         setConfigurationChangedLayoutParam(dm.widthPixels,dm.heightPixels);
         showTwoFrameLayout();
         switch (uiRot) {
            case 0:
            	if (camera!=null) {
            		camera.stopPreview();
            	}
            	if (dm.widthPixels >  dm.heightPixels) {
            		camera.setDisplayOrientation(0);
            	}else {
            		camera.setDisplayOrientation(90);
            	}
            break;
            
            case 1:
            	if (camera!=null) {
            		camera.stopPreview();
            	}
            	if ( dm.heightPixels > dm.widthPixels) {
            		camera.setDisplayOrientation(270);
            	}else {
            		camera.setDisplayOrientation(0);
            	}
            break;
            
            case 2:
            	if (camera!=null) {
            		camera.stopPreview();
            	}
            	if (dm.widthPixels >  dm.heightPixels) {
            		camera.setDisplayOrientation(180);
            	}
            	else {
            		camera.setDisplayOrientation(270);
            	}
            break;
            
            case 3:
            	if (camera!=null) {
            		camera.stopPreview();
            	}
            	if ( dm.heightPixels > dm.widthPixels) {
            		camera.setDisplayOrientation(90);
            	}else {
            		camera.setDisplayOrientation(180);
            	}
            break;
         }
         camera.startPreview();

    }
    
	
	
	private void setConfigurationChangedLayoutParam(int width,int height){
        if (width > height) {
        	//heng
            backImageView.setBackgroundColor(Color.BLACK);
            lParams = new RelativeLayout.LayoutParams((int)(screen_width/2),(int)(screen_height*0.3));
            lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
            backImageView.setLayoutParams(lParams);
            
            textView.setMovementMethod(new ScrollingMovementMethod());
            lParams = new RelativeLayout.LayoutParams((int)(screen_width/2),(int)(screen_height*0.3)-50);
            lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.imagebackground);
            lParams.leftMargin = 10;
            textView.setLayoutParams(lParams);
            textView.setTextColor(Color.WHITE);
            
		}else {
			//shu
            backImageView.setBackgroundColor(Color.BLACK);
            lParams = new RelativeLayout.LayoutParams((int)(screen_width/2),(int)(screen_height*0.35));
            lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
            backImageView.setLayoutParams(lParams);
            
            textView.setMovementMethod(new ScrollingMovementMethod());
            lParams = new RelativeLayout.LayoutParams((int)(screen_width/2),(int)(screen_height*0.35)-50);
            lParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
            lParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.imagebackground);
            lParams.leftMargin = 10;
            textView.setLayoutParams(lParams);
            textView.setTextColor(Color.WHITE);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
            recognitionBoolean = false;
            unlockBoolean = false;
            autoFocusBoolean = false;
            try {
                if(camera!=null){
                    camera.cancelAutoFocus();
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }
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
            if (myListener != null) {
                sManager.unregisterListener(myListener);
                sManager = null;
                myListener = null;
            }
            unBindPlateRecogService();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PlateVideoCameraActivity.this.finish();
            startActivity(intent);

        }
        return super.onKeyDown(keyCode, event);
    }
    

    protected void onDestroy() {
    	super.onDestroy();
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
            	bitmap.recycle();
            }
        	bitmap = null;
		}
        try {

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
        } catch (Exception e) {
        	e.printStackTrace();
        }

    }
    
    
    @Override
    protected void onStop() {
        super.onStop();
        recognitionBoolean = false;
        unlockBoolean = false;
        autoFocusBoolean = false;
        if (camera != null) {
            camera.cancelAutoFocus();
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }

        if (myListener != null) {
            sManager.unregisterListener(myListener);
        }
        sManager = null;
        myListener = null;
    }

}
