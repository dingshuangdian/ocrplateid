
package com.kernal.plateid;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.Window;
import android.widget.Toast;

import java.io.File;

import utills.CheckPermission;
import utills.Utils;

/**
 * Created by LaiYingtang on 2016/5/18. 用户权限获取页面，权限处理
 */
public class PermissionActivity extends Activity {
	// 首先声明权限授权
	public static final int PERMISSION_GRANTED = 0;// 标识权限授权
	public static final int PERMISSION_DENIEG = -1;// 权限不足，权限被拒绝的时候
	private static final int PERMISSION_REQUEST_CODE = 0;// 系统授权管理页面时的结果参数
	private static final String EXTRA_PERMISSION = "com.wintone.permissiondemo";// 权限参数
	private static final String PACKAGE_URL_SCHEME = "package:";// 权限方案
	private final int SELECT_RESULT_CODE = 3;
	private CheckPermission checkPermission;// 检测权限类的权限检测器
	public static boolean isrequestCheck;// 判断是否需要系统权限检测。防止和系统提示框重叠
	private static String typeMark;
	private Intent activityIntent;
	// 启动当前权限页面的公开接口
	public static void startActivityForResult(Activity activity,int requestCode,String iscamera,
											  String... permission) {
		typeMark=iscamera;
		Intent intent = new Intent(activity, PermissionActivity.class);
		intent.putExtra(EXTRA_PERMISSION, permission);
		ActivityCompat.startActivityForResult(activity, intent, requestCode,
				null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.permission_layout);
		if (getIntent() == null || !getIntent().hasExtra(EXTRA_PERMISSION))// 如果参数不等于配置的权限参数时
		{
			throw new RuntimeException(
					"当前Activity需要使用静态的StartActivityForResult方法启动");// 异常提示
		}

		checkPermission = new CheckPermission(this);
		isrequestCheck = true;// 改变检测状态


	}

	// 检测完之后请求用户授权
	@Override
	protected void onResume() {
		super.onResume();
		if (isrequestCheck) {
			String[] permission = getPermissions();
			if (checkPermission.permissionSet(permission)) {
				requestPermissions(permission); // 去请求权限
			} else {
//				System.out.println("第一次");
//				allPermissionGranted();// 获取全部权限
			}
		} else {
			isrequestCheck = true;
		}
	}

	// 获取全部权限
	private void allPermissionGranted() {
		setResult(PERMISSION_GRANTED);

		if(typeMark.equals("false")){
			activityIntent = new Intent(this, MemoryCameraActivity.class);
			activityIntent.putExtra("camera", false);
			startActivity(activityIntent);
			finish();
		}else if(typeMark.equals("true")){
			activityIntent = new Intent(this, MemoryCameraActivity.class);

			activityIntent.putExtra("camera", true);
			startActivity(activityIntent);
			finish();
		}else if(typeMark.equals("AuthService")){
			finish();
		}else if(typeMark.equals("choice")){
			Intent selectIntent = new Intent(
					Intent.ACTION_PICK,
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			Intent wrapperIntent = Intent.createChooser(selectIntent,"请选择一张图片");
			try {
				startActivityForResult(wrapperIntent, SELECT_RESULT_CODE);
			} catch (Exception e) {
				Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
			}
		}

	}

	// 请求权限去兼容版本
	private void requestPermissions(String... permission) {

		PermissionActivity.this.requestPermissions(permission,
				PERMISSION_REQUEST_CODE);
	}

	// 返回传递过来的权限参数
	private String[] getPermissions() {
		return getIntent().getStringArrayExtra(EXTRA_PERMISSION);
	}

	/**
	 * 用于权限管理 如果全部授权的话，则直接通过进入 如果权限拒绝，缺失权限时，则使用dialog提示
	 *
	 * @param requestCode
	 *            请求代码
	 * @param permissions
	 *            权限参数
	 * @param grantResults
	 *            结果
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (PERMISSION_REQUEST_CODE == requestCode
				&& hasAllPermissionGranted(grantResults)) // 判断请求码与请求结果是否一致
		{
			isrequestCheck = true;// 需要检测权限，直接进入，否则提示对话框进行设置
			allPermissionGranted(); // 进入

		} else { // 提示对话框设置

			isrequestCheck = false;
//			 showMissingPermissionDialog();//dialog
			if(typeMark.equals("AuthService")){
				Toast.makeText(this, "您禁止了此权限！无法进行授权操作！请选择允许", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(this, "您禁止了此权限！请选择允许", Toast.LENGTH_SHORT).show();
			}

//			finish();
		}

	}

	 //显示对话框提示用户缺少权限
//	 private void showMissingPermissionDialog() {
//	 AlertDialog.Builder builder = new
//	 AlertDialog.Builder(PermissionActivity.this);
//	 builder.setTitle(R.string.help);//提示帮助
//	 builder.setMessage(R.string.string_help_text);
//
//	 //如果是拒绝授权，则退出应用
//	 //退出
//	 builder.setNegativeButton(R.string.quit, new
//	 DialogInterface.OnClickListener() {
//	 @Override
//	 public void onClick(DialogInterface dialog, int which) {
//	 setResult(PERMISSION_DENIEG);//权限不足
//	 finish();
//	 }
//	 });
//	 //打开设置，让用户选择打开权限
//	 builder.setPositiveButton(R.string.settings, new
//	 DialogInterface.OnClickListener() {
//	 @Override
//	 public void onClick(DialogInterface dialog, int which) {
//	 startAppSettings();//打开设置
//	 }
//	 });
//	 builder.setCancelable(false);
//	 builder.show();
//	 }

	// 获取全部权限
	private boolean hasAllPermissionGranted(int[] grantResults) {
		for (int grantResult : grantResults) {

			if (grantResult == PackageManager.PERMISSION_DENIED) {
				return false;
			}
		}
		return true;
	}

	// 打开系统应用设置(ACTION_APPLICATION_DETAILS_SETTINGS:系统设置权限)
	private void startAppSettings() {
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
		startActivity(intent);
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
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
		}else{
			finish();
		}

	}
}
