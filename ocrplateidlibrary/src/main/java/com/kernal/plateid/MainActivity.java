package com.kernal.plateid;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import utills.CheckPermission;
import utills.Utils;

public class MainActivity extends Activity implements OnClickListener{
	public static final String PATH = Environment.getExternalStorageDirectory()	.toString();
    public static final String TAG = "TestPlateServiceDemo";
    String cls;
    String pic;
    int imageformat = 1;
    boolean bGetVersion = false;
    String sn;
    String authfile;
    int bVertFlip = 0;
    String userdata;
    private Button setButton;
    int butsetId;
    int authButtonId;
    int recogButtonId;
    private EditText editText;

    
    private Button buttonActivation,buttonSelectPic,buttonExit;
    
    private int ReturnAuthority = -1;
    String[] fieldvalue = new String[14];
//    int nRet = -1;
//    int iInitPlateIDSDK = -1;
//    String returnGetVersion = "";
    public AuthService.MyBinder authBinder;
//    public RecogService.MyBinder recogBinder;
//    public Integer lock = 0;
    public String sdDir;
//    public Intent recogIntent;
//    int[] fieldname = { R.string.plate_number, R.string.plate_color, R.string.plate_color_code,
//            R.string.plate_type_code,R.string.plate_reliability,R.string.plate_brightness_reviews,
//            R.string.plate_move_orientation, R.string.plate_leftupper_pointX,R.string.plate_leftupper_pointY, 
//            R.string.plate_rightdown_pointX, R.string.plate_rightdown_pointY, R.string.plate_elapsed_time,
//            R.string.plate_light, R.string.plate_car_color};
    
//    private Button sysCameraButton;
    private Button CameraButton,customCameraButton;
//    private EditText codeEditText;
//    private TextView showofflinetext;

//    private  Boolean takePictureBoolean = false;
//    private final int RESULT_CODE = 1;
    private final int SYSTEM_RESULT_CODE = 2;
    private final int SELECT_RESULT_CODE = 3;
//    private LinearLayout layout ;
    private Button videoReg;
    private  int index=0;
    private boolean isTouch = false;
    //授权验证服务绑定后的操作与start识别服务
    public ServiceConnection authConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            authBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            authBinder = (AuthService.MyBinder) service;
            Toast.makeText(getApplicationContext(), R.string.auth_check_service_bind_success, Toast.LENGTH_SHORT).show();
            try {           	         
            	PlateAuthParameter pap = new PlateAuthParameter();
            	pap.sn = sn;
            	pap.authFile = authfile;
            	pap.devCode = Devcode.DEVCODE;
            	ReturnAuthority = authBinder.getAuth(pap);
                if (ReturnAuthority != 0) {
                	Toast.makeText(getApplicationContext(),getString(R.string.license_verification_failed)+":"+ReturnAuthority,Toast.LENGTH_LONG).show();
                }else{
                	Toast.makeText(getApplicationContext(),R.string.license_verification_success,Toast.LENGTH_LONG).show();
                }
            }catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.failed_check_failure, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }finally{
                if (authBinder != null) {
                    unbindService(authConn);
                }
            }
        }
            	// sn：采用序列号方式激活时设置此参数，否则写""
            	// authfile：采用激活文件方式激活时设置此参数，否则写""
            	// 以上俩个参数都不为""时按序列号方式激活；当sn和authfile为""时会在根目录下找激活文件xxxxxxxxxxxxxxx_cp.txt   
//            	pap.server =  "http://192.168.0.36:8080";
//            	authfile=PATH+"/AndroidWT/auth/authfile.db";//文件激活方式路径
            	
            	
//            	pap.dataFile = datefile;
//            	pap.dataFile = PATH+"/AndroidWT/dataauth/wtdate.lsc";
            	
//            	pap.versionfile = PATH+"/AndroidWT/wtversion.lsc";
                
    };
    static final String[] PERMISSION = new String[] {permission.CAMERA,
		permission.WRITE_EXTERNAL_STORAGE,// 写入权限
		permission.READ_EXTERNAL_STORAGE, // 读取权限
		 permission.READ_PHONE_STATE,
		permission.VIBRATE, permission.INTERNET,
		permission.FLASHLIGHT };

    @Override
    protected void onCreate(Bundle savedInstanceState) {   
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.test_plate_activity);
        PlateProjectTool.addActivityList(MainActivity.this);
        findViews();
        getCameraInformation();
        String sdStatus = Environment.getExternalStorageState();  
        if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
        	sdDir = Environment.getExternalStorageDirectory().toString()+"/kernalimage";
        }  
		File dir = new File(sdDir);
		if (!dir.exists()) {
			dir.mkdir();
		}
//        setButton.setOnClickListener(this);
        CameraButton.setOnClickListener(this);
        customCameraButton.setOnClickListener(this);
        buttonActivation.setOnClickListener(this);
        buttonSelectPic.setOnClickListener(this);
        buttonExit.setOnClickListener(this);
        videoReg.setOnClickListener(this);
        
    }
    
    
    //获取设备下面的硬件信息，基本的拍照分辨率，预览分辨率
    private void getCameraInformation(){
    	if(readIntPreferences("PlateService","picWidth") == 0 || readIntPreferences("PlateService","picHeight") == 0
    			|| readIntPreferences("PlateService","preWidth") == 0 || readIntPreferences("PlateService","preHeight") == 0
    			|| readIntPreferences("PlateService","preMaxWidth") == 0 || readIntPreferences("PlateService","preMaxHeight") == 0){
    		
            Camera camera = null;
            int pre_Max_Width = 640;
            int pre_Max_Height = 480;
            final int Max_Width = 2048;
            final int Max_Height = 1536;
        	boolean isCatchPicture = false;
        	int picWidth = 2048;
        	int picHeight = 1536;
        	int preWidth = 320;
        	int preHeight = 240;
    		try {
    			camera = Camera.open();
    			if (camera != null) {
    				Camera.Parameters parameters = camera.getParameters();
    				List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
    				Camera.Size size;
    			    int second_Pre_Width = 0,second_Pre_Height = 0;
    				int length = previewSizes.size();
    				if (length == 1) {
    					size = previewSizes.get(0);
    					pre_Max_Width = size.width;
    					pre_Max_Height = size.height;
    				}else {
    					for (int i = 0; i < length; i++) {
    						size = previewSizes.get(i);
    						if (size.width <= Max_Width && size.height <= Max_Height) {
    							second_Pre_Width = size.width;
    							second_Pre_Height = size.height;
    							if (pre_Max_Width < second_Pre_Width) {
    								pre_Max_Width = second_Pre_Width;
    								pre_Max_Height = second_Pre_Height;
    							}
    						}
    					}
    				}
    				
                    for(int i=0;i<previewSizes.size();i++){
                        if(previewSizes.get(i).width == 640 && previewSizes.get(i).height == 480){
                        	preWidth = 640;
                        	preHeight = 480;
                            break;
                        }
                        if(previewSizes.get(i).width == 320 && previewSizes.get(i).height == 240) {
                        	preWidth = 320;
                        	preHeight = 240;
                        }
                    }
                    if(preWidth == 0 || preHeight == 0){
                		if(previewSizes.size() == 1){
                			preWidth = previewSizes.get(0).width;
                			preHeight = previewSizes.get(0).height;
                		}else{
                			preWidth = previewSizes.get(previewSizes.size()/2).width;
                			preHeight = previewSizes.get(previewSizes.size()/2).height;
                		}
                    }
    				
            		List<Camera.Size> PictureSizes = parameters.getSupportedPictureSizes();
            		for(int i=0;i<PictureSizes.size();i++){
            			if(PictureSizes.get(i).width == 2048 && PictureSizes.get(i).height == 1536){
            				if(isCatchPicture == true) {
            					break;
            				}
            				isCatchPicture = true;
            				picWidth = 2048;
            				picHeight = 1536;
            			}
            			if(PictureSizes.get(i).width == 1600 && PictureSizes.get(i).height == 1200){
            				isCatchPicture = true;
            				picWidth = 1600;
            				picHeight = 1200;
            			}
            			if(PictureSizes.get(i).width == 1280 && PictureSizes.get(i).height == 960) {
            				isCatchPicture = true;
            				picWidth = 1280;
            				picHeight = 960;
            				break;
            			}
            		}
    			}
    			
    			writeIntPreferences("PlateService","picWidth",picWidth);	
    			writeIntPreferences("PlateService","picHeight",picHeight);
    			writeIntPreferences("PlateService","preWidth",preWidth);
    			writeIntPreferences("PlateService","preHeight",preHeight);
    			writeIntPreferences("PlateService","preMaxWidth",pre_Max_Width);
    			writeIntPreferences("PlateService","preMaxHeight",pre_Max_Height);
    		} catch (Exception e) {
    			
    		} finally {
    			if (camera != null) {
    				try {
    					camera.release();
    					camera = null;
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}

    }
    

    
    private void findViews() {
//        setButton = (Button) findViewById(R.id.butset);
        CameraButton = (Button) findViewById(R.id.buttoncamera);
        customCameraButton = (Button) findViewById(R.id.customcamerabutton);
        buttonActivation = (Button) findViewById(R.id.butactivation);
        buttonSelectPic = (Button) findViewById(R.id.butselectpic);
        buttonExit = (Button) findViewById(R.id.butclose);
        videoReg= (Button) findViewById(R.id.videoReg);
    }

    protected int readIntPreferences(String perferencesName, String key) {
    	SharedPreferences preferences = getSharedPreferences(perferencesName, MODE_PRIVATE);
	    int result = preferences.getInt(key, 0);
	    return result;
    }
    protected void writeIntPreferences(String perferencesName, String key, int value) {
        SharedPreferences preferences = getSharedPreferences(perferencesName, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    @Override
    public void onClick(View v) {
		//拍照识别入口
		if(getResources()
				.getIdentifier("buttoncamera", "id", this.getPackageName())==v.getId()){
			Intent cameraintent = new Intent(MainActivity.this,MemoryCameraActivity.class);
        	if (Build.VERSION.SDK_INT >= 23) {
				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
				if (checkPermission.permissionSet(PERMISSION)) {
				PermissionActivity.startActivityForResult(MainActivity.this,0,"false",  PERMISSION);
				} else {
					cameraintent.putExtra("camera", false);
		        	startActivity(cameraintent);
				}
			} else {
	        	cameraintent.putExtra("camera", false);
	        	startActivity(cameraintent);
			}
			finish();
		}else if(getResources()
				.getIdentifier("videoReg", "id", this.getPackageName())==v.getId()){
			//视频识别入口
			CreatDialog();
		}else if(getResources()
				.getIdentifier("butclose", "id", this.getPackageName())==v.getId()){
			//返回
			for (int i = 0; i < PlateProjectTool.platelist.size(); i++) {
				if (!PlateProjectTool.platelist.get(i).isFinishing()) {
					PlateProjectTool.platelist.get(i).finish();
				}
			}
		}else if(getResources()
				.getIdentifier("butactivation", "id", this.getPackageName())==v.getId()){
			//激活程序按钮
			if (Build.VERSION.SDK_INT >= 23) {
    				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
    				if (checkPermission.permissionSet(PERMISSION)) {
    					PermissionActivity.startActivityForResult(MainActivity.this,0,"AuthService",  PERMISSION);
    				}
    					CreatViewtoAuthservice();
    			}else{
    				CreatViewtoAuthservice();
    			}
		}else if(getResources()
				.getIdentifier("butselectpic", "id", this.getPackageName())==v.getId()){
			//选择识别入口
			Intent selectIntent = new Intent(
					Intent.ACTION_PICK,
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Intent wrapperIntent = Intent.createChooser(selectIntent,"请选择一张图片");
        	if (Build.VERSION.SDK_INT >= 23) {
				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
				if (checkPermission.permissionSet(PERMISSION)) {
					PermissionActivity.startActivityForResult(MainActivity.this,0,"choice",  PERMISSION);
				} else {
					try {
						startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
					} catch (Exception e) {
						Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				try {
					startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
				} catch (Exception e) {
					Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
				}
			}

		}

//        switch (v.getId()) {
//        case R.id.butset:
//            Intent intent = new Intent(MainActivity.this, PlateIDCfg.class);
//            startActivity(intent);
//            break;
        //进入拍照识别界面
//        case R.id.buttoncamera:// 2015-7-31   CPY   Memory Recognition
//
//        	Intent cameraintent = new Intent(MainActivity.this,MemoryCameraActivity.class);
//        	if (Build.VERSION.SDK_INT >= 23) {
//				CheckPermission checkPermission = new CheckPermission(this);
//				if (checkPermission.permissionSet(PERMISSION)) {
//					PermissionActivity.startActivityForResult(this,0,"false",  PERMISSION);
//				} else {
//					cameraintent.putExtra("camera", false);
//		        	startActivity(cameraintent);
//				}
//			} else {
//	        	cameraintent.putExtra("camera", false);
//	        	startActivity(cameraintent);
//			}
//        	cameraintent.putExtra("camera", false);
//        	startActivity(cameraintent);
//            break;
            //进入视频识别界面
//        case R.id.videoReg:
//
//        		CreatDialog();
//
//            break;
//            //激活程序点击事件
//        case R.id.butactivation:
//
//        		if (Build.VERSION.SDK_INT >= 23) {
//    				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
//    				if (checkPermission.permissionSet(PERMISSION)) {
//    					PermissionActivity.startActivityForResult(MainActivity.this,0,"AuthService",  PERMISSION);
//    				}
//    					CreatViewtoAuthservice();
//    			}else{
//    				CreatViewtoAuthservice();
//    			}
//
//
//            break;
//            //进入选择识别界面
//        case R.id.butselectpic:
//        	Intent selectIntent = new Intent(Intent.ACTION_GET_CONTENT);
//        	Intent wrapperIntent = Intent.createChooser(selectIntent,"Select Picture");
//        	if (Build.VERSION.SDK_INT >= 23) {
//				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
//				if (checkPermission.permissionSet(PERMISSION)) {
//					PermissionActivity.startActivityForResult(MainActivity.this,0,"choice",  PERMISSION);
//				} else {
//					selectIntent.addCategory(Intent.CATEGORY_OPENABLE);
//					selectIntent.setType("image/*");
//					startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
//				}
//			} else {
//				selectIntent.addCategory(Intent.CATEGORY_OPENABLE);
//				selectIntent.setType("image/*");
//				startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
//			}
//
////			selectIntent.addCategory(Intent.CATEGORY_OPENABLE);
////			selectIntent.setType("image/*");
////			startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
//            break;
//            //退出按钮点击事件
//        case R.id.butclose:
//        	for (int i = 0; i < PlateProjectTool.platelist.size(); i++) {
//				if (!PlateProjectTool.platelist.get(i).isFinishing()) {
//					PlateProjectTool.platelist.get(i).finish();
//				}
//			}
//            break;
//
//        case R.id.customcamerabutton:
//        	if (readIntPreferences("PlateService","picWidth") != 0 && readIntPreferences("PlateService","picHeight") != 0
//        			&& readIntPreferences("PlateService","preWidth") != 0 && readIntPreferences("PlateService","preHeight") != 0
//        					&& readIntPreferences("PlateService","preMaxWidth") != 0 && readIntPreferences("PlateService","preMaxHeight") != 0) {
//                Intent camera_intent = new Intent(MainActivity.this, CameraActivity.class);
//                startActivity(camera_intent);
//
//			}else {
//	            Intent sysCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//	            String sdStatus = Environment.getExternalStorageState();
//	            String sdDirString = PATH+"/kernalimage";
//				long datetime = System.currentTimeMillis();
//				pic =  sdDirString +"/plateid"+datetime+".jpg";
//	            sysCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(pic)));
//	            startActivityForResult(sysCameraIntent,SYSTEM_RESULT_CODE);
//	            sysCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//	            sysCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(pic)));
//	            startActivityForResult(sysCameraIntent,SYSTEM_RESULT_CODE);
//			}
//
//            break;
//        }
//
    }
    /** 
	* @Title: CreatDDialog 
	* @Description: TODO(这里用一句话描述这个方法的作用) 创建选择对话框，选择识别模式,只在进入视频识别时进行选择，拍照识别默认快速识别模式
	* @return void    返回类型
	* @throws 
	*/
	private void CreatDialog() {
		// TODO Auto-generated method stub
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("请选择识别模式");
        final String[] model = {"精准模式", "快速模式"};
     
        //    设置一个单项选择下拉框
        /**
         * 第一个参数指定我们要显示的一组下拉单选框的数据集合
         * 第二个参数代表索引，指定默认哪一个单选框被勾选上，0表示默认'精准模式' 会被勾选上   1 表示快速模式
         * 第三个参数给每一个单选项绑定一个监听器
         */
        builder.setSingleChoiceItems(model, index, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                index = which;
               isTouch = true;
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	isTouch = false;
            	Intent video_intent = new Intent();
            	video_intent.putExtra("camera", true);          
            	if(index==1){
            		RecogService.recogModel = false;//  false：快速模式             		
            	}else{
            		RecogService.recogModel = true;//true  精准模式
            	}
            	video_intent = new Intent(MainActivity.this,MemoryCameraActivity.class);
            	if (Build.VERSION.SDK_INT >= 23) {
    				CheckPermission checkPermission = new CheckPermission(MainActivity.this);
    				if (checkPermission.permissionSet(PERMISSION)) {
    					PermissionActivity.startActivityForResult(MainActivity.this,0,"true",  PERMISSION);

    				} else {
    					video_intent.setClass(getApplicationContext(), MemoryCameraActivity.class);
						video_intent.putExtra("camera", true);
    	            	startActivity(video_intent);
						finish();
    				}
    			} else {				
    				video_intent.setClass(getApplicationContext(), MemoryCameraActivity.class);
					video_intent.putExtra("camera", true);
                	startActivity(video_intent);
					finish();
    			}
//            	video_intent.setClass(getApplicationContext(), MemoryCameraActivity.class);
//            	startActivity(video_intent);           	
                dialog.dismiss();
              
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	if(isTouch){
            		if(index==0){
            			index = 1;
            		}else{
            			index =0;
            		}
            		isTouch = false;
            	}
            	  dialog.dismiss();
            }
        });
        builder.show();
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (SYSTEM_RESULT_CODE == requestCode && resultCode == Activity.RESULT_OK) {			
            Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
            intent.putExtra("recogImagePath", pic);
            startActivity(intent);
			finish();
		}
		if (requestCode == SELECT_RESULT_CODE && resultCode == Activity.RESULT_OK) {
			String picPathString = null;
			Uri uri = data.getData();
			picPathString = Utils.getPath(getApplicationContext(), uri);
			if(picPathString != null && !picPathString.equals("") && !picPathString.equals(" ") && !picPathString.equals("null")){
				File file = new File(picPathString);
				Intent intentResult = new Intent(getApplicationContext(),ResultActivity.class);
				if(!file.exists()||file.isDirectory()){
					Toast.makeText(this, "请选择一张正确的图片", Toast.LENGTH_SHORT).show();
				}else{
					intentResult.putExtra("recogImagePath", picPathString);
				}
				startActivity(intentResult);
				finish();
			}

		}

	}
	
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
        	for (int i = 0; i < PlateProjectTool.platelist.size(); i++) {
				if (!PlateProjectTool.platelist.get(i).isFinishing()) {
					PlateProjectTool.platelist.get(i).finish();
				}
			}
        }
        return super.onKeyDown(keyCode, event);
    }
   public void CreatViewtoAuthservice(){
   		editText = new EditText(getApplicationContext());
	   	editText.setTextColor(Color.BLACK);
			new  AlertDialog.Builder(MainActivity.this)
			.setTitle(R.string.dialog_title)
			.setIcon(android.R.drawable.ic_dialog_info)                  
			.setView(editText)
			.setPositiveButton(R.string.license_verification, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
		        	sn = editText.getText().toString().toUpperCase();
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
					}					
		            Intent authIntent = new Intent(MainActivity.this, AuthService.class);
		            bindService(authIntent, authConn, Service.BIND_AUTO_CREATE);
		            dialog.dismiss();
					
				}
			})
			.setNegativeButton(R.string.offline_activation ,  new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
					}
					
					String sdDir = null;
			        boolean sdCardExist = Environment.getExternalStorageState().equals(
			                Environment.MEDIA_MOUNTED);
			        if (sdCardExist) {
			        	String PATH = Environment.getExternalStorageDirectory().toString() + "/AndroidWT";
			        	File file = new File(PATH);
						if (!file.exists()) {
							file.mkdir();
						}
			        	sdDir = PATH+"/wt.dev";
			            String deviceId;
			            String androId;
			            TelephonyManager telephonyManager;
			            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			            StringBuilder sb = new StringBuilder();
			            sb.append(telephonyManager.getDeviceId());
			            deviceId = sb.toString();

			            StringBuilder sb1 = new StringBuilder();
			            sb1.append(Settings.Secure.getString(getContentResolver(),
			                    Settings.Secure.ANDROID_ID));
			            androId = sb1.toString();
						File newFile = new File(sdDir);
						String idString = deviceId+";"+androId;
						try {
							newFile.delete();
							newFile.createNewFile();
							FileOutputStream fos = new FileOutputStream(newFile);
							StringBuffer sBuffer = new StringBuffer();
							sBuffer.append(idString);
							fos.write(sBuffer.toString().getBytes());
							fos.close();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}	
					
			        	}
			        dialog.dismiss();
			        new  AlertDialog.Builder(MainActivity.this)    
			        .setTitle(R.string.dialog_alert)  
			        .setMessage(R.string.dialog_message_one)  
			        .setPositiveButton(R.string.confirm ,  new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							
						}
					} )   
			        .show();   

					}
				})
			.show(); 
   	
   }
}
