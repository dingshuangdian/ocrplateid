package utills;
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.Time;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

import com.kernal.plateid.MemoryCameraActivity;
import com.kernal.plateid.PlateCfgParameter;
import com.kernal.plateid.RecogService;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import view.ViewfinderView;


@SuppressLint({"NewApi"})
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {
    private static final String TAG = CameraFragment.class.getSimpleName();
    private CameraManager cameraManager;
    private boolean hasSurface;
    private SurfaceView surfaceView;
    public int deviceIdx;
    private int iInitPlateIDSDK=-1;
    public RecogService.MyBinder recogBinder;
    private int imageformat = 6;// NV21 -->6
    private int bVertFlip = 0;
    private int bDwordAligned = 1;
    private boolean ToastShow = true;
    private ViewfinderView myview;
	private int width, height;
	private FrameLayout frameLayout;
	private MemoryCameraActivity myCameraActivity;
	private  Bitmap bitmap;
    //预览分辨率
    private int preW =1920,preH =1080;
    public ServiceConnection recogConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            recogConn = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recogBinder = (RecogService.MyBinder) service;
            iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();
            if (iInitPlateIDSDK != 0) {
                Toast.makeText(getActivity(),"错误码："+iInitPlateIDSDK,Toast.LENGTH_LONG).show();
            }
            PlateCfgParameter cfgparameter = new PlateCfgParameter();
            cfgparameter.armpolice = 4;
            cfgparameter.armpolice2 = 16;
            cfgparameter.embassy = 12;
            cfgparameter.individual = 0;
            // cfgparameter.nContrast = 9;
            cfgparameter.nOCR_Th = 0;
            cfgparameter.nPlateLocate_Th = 5;
            cfgparameter.onlylocation = 15;
            cfgparameter.tworowyellow = 2;
            cfgparameter.tworowarmy = 6;
            cfgparameter.szProvince = "";
            cfgparameter.onlytworowyellow = 11;
            cfgparameter.tractor = 8;
            cfgparameter.bIsNight = 1;
            //新能源车牌开启
            cfgparameter.newEnergy  = 24;
            //领事馆车牌开启;
            cfgparameter.consulate = 22;
            cfgparameter.Infactory = 18;//厂内车牌是否开启     18:是  19不是
            cfgparameter.civilAviation  = 20;//民航车牌是否开启  20是 21 不是

            recogBinder.setRecogArgu(cfgparameter, imageformat, bVertFlip,
                    bDwordAligned);
            cameraManager.setData(recogBinder,iInitPlateIDSDK);
        }
    };
    public CameraFragment() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.cameraManager = new CameraManager(this.getActivity().getApplication(), this.getView());
        cameraManager.setFragment(this);
        cameraManager.setPreviewSize(preW,preH);
        Intent authIntent = new Intent(getActivity(),
                RecogService.class);
        getActivity().bindService(authIntent, recogConn, Service.BIND_AUTO_CREATE);
        myCameraActivity=(MemoryCameraActivity) getActivity();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View codeView = this.createViewFromCode(inflater, container, savedInstance);
        Window window = this.getActivity().getWindow();
        window.addFlags(128);


        return codeView;
    }
    //添加View
    @SuppressLint({"NewApi"})
    private View createViewFromCode(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        Display display = this.getActivity().getWindowManager().getDefaultDisplay();
        Boolean isPortrait = Boolean.valueOf(display.getWidth() < display.getHeight());
        //添加 surfaceView
        this.surfaceView = new CameraSurfaceView(this.getActivity(), isPortrait.booleanValue(),preW,preH);
         frameLayout = new FrameLayout(this.getActivity());
        LayoutParams frameLayoutpm = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        frameLayout.setLayoutParams(frameLayoutpm);
        frameLayout.addView(this.surfaceView, frameLayoutpm);
        //添加 扫描框
        Point srcPixs=Utils.getScreenSize(getActivity());
        width=srcPixs.x;
        height=srcPixs.y;
        myview = new ViewfinderView(getActivity(),width,height,true);
        frameLayoutpm= new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        frameLayout.setLayoutParams(frameLayoutpm);
        frameLayout.addView( myview, frameLayoutpm );
        return frameLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    //开启相机
    private boolean initCamera(SurfaceHolder surfaceHolder) {
        if(surfaceHolder == null) {
            throw new IllegalStateException("没有SurfaceHolder");
        }
    else {
            try {
                this.cameraManager.openDriver(surfaceHolder, this.deviceIdx);
            } catch (IOException var3) {
                LogUtil.E(TAG, var3.getMessage());
                return false;
            } catch (RuntimeException var4) {
                LogUtil.E(TAG, "camera init fail");
                return false;
            } catch (Exception var5) {
                LogUtil.E(TAG, "camera init fail");
                return false;
            }

            //预览分辨率
            return true;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        SurfaceHolder surfaceHolder = this.surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(3);

    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!this.hasSurface) {
            this.hasSurface = true;
            boolean isOpen = this.initCamera(holder);
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.hasSurface = false;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void onDestroy() {
        cameraManager.previewCallback.stopOcrThread();
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        this.cameraManager.closeDriver();
        if (recogBinder != null) {
            getActivity().unbindService(recogConn);
            recogBinder = null;

        }
        super.onDestroy();
    }


    /**
     * 改变状态  即根据重力感应获取的当前旋转状态，以便计算识别区域
     * @param Left
     * @param Right
     * @param Top
     * @param Bottom
     */
    public void ChangeState(boolean Left, boolean Right, boolean Top, boolean Bottom,boolean isPortrait){
        cameraManager.ChangeState(Left,Right,Top,Bottom);
    	frameLayout.removeView(myview);
		myview = new ViewfinderView(getActivity(),width,height,isPortrait);
		frameLayout.addView(myview);
    }

    /**
     *   当识别结束  向Activity传递结果
     * @param result
     * @param Currentdata
     */
    public void getRecogResult(String[] result,byte[] Currentdata){
        preH = cameraManager.getPreviewSize().y;
        preW = cameraManager.getPreviewSize().x;
        //String path;
        if(Currentdata==null){
            myCameraActivity.SendfeedbackData(recogBinder.getRecogData());
        }else{
            myCameraActivity.SendfeedbackData(Currentdata);
        }
        if (result[0] != null && !result[0].equals("")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inPurgeable = true;
                options.inInputShareable = true;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                YuvImage yuvimage;
                if(Currentdata==null){
                    if (myCameraActivity.Rotate_top || myCameraActivity.Rotate_bottom) {
                        yuvimage = new YuvImage(recogBinder.getRecogData(),
                                ImageFormat.NV21,  preH, preW,
                                null);
                        yuvimage.compressToJpeg(new Rect(0, 0,
                                preH, preW), 100, baos);
                    } else if (myCameraActivity.Rotate_left || myCameraActivity.Rotate_right) {
                        yuvimage = new YuvImage(recogBinder.getRecogData(),
                                ImageFormat.NV21, preW, preH,
                                null);
                        yuvimage.compressToJpeg(new Rect(0, 0,
                                preW, preH), 100, baos);
                    }
                }else{
                    yuvimage = new YuvImage(Currentdata,
                            ImageFormat.NV21, preW, preH,
                            null);
                    yuvimage.compressToJpeg(new Rect(0, 0,
                            preW, preH), 100, baos);
                }

                bitmap = BitmapFactory.decodeByteArray(
                        baos.toByteArray(), 0, baos.size(),
                        options);
                //path = Utils.savePicture(bitmap);
                if(bitmap!=null&&!bitmap.isRecycled()){
                    bitmap.recycle();
                    bitmap=null;
                }

        }else {
                // 未检测到车牌时执行下列代码
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inPurgeable = true;
                options.inInputShareable = true;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                YuvImage yuvimage = new YuvImage(Currentdata, ImageFormat.NV21, preW, preH, null);
                yuvimage.compressToJpeg(new Rect(0, 0, preW, preH), 100, baos);
                bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size(), options);
                Matrix matrix = new Matrix();
                matrix.reset();
                if (myCameraActivity.Rotate_left) {
                    matrix.setRotate(0);
                } else if (myCameraActivity.Rotate_top) {
                    matrix.setRotate(90);
                } else if (myCameraActivity.Rotate_right) {
                    matrix.setRotate(180);
                } else if (myCameraActivity.Rotate_bottom) {
                    matrix.setRotate(270);
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix,
                        true);
                //path = Utils.savePicture(bitmap);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    bitmap = null;
                }


        }
        /**
         * 将结果返回到activity
         */
        myCameraActivity.getResult(result);

    }

    /**
     * 返回按钮触发事件
     */
    public  void backLastActivtiy() {
        // TODO Auto-generated method stub
        this.cameraManager.previewCallback.issuspended=true;
        //CameraFragment.this.getActivity().finish();
    }

    /**
     * 判断是都点击了拍照按钮  默认false  点击拍照按钮修改为true
     * @param modle
     */
    public void setRecogModle(boolean modle,boolean isStartRecog){
        cameraManager.previewCallback.isTakePicRecog=modle;
        cameraManager.previewCallback.isStartRecog=isStartRecog;
    }

    /**
     *  错误码返回
     * @param nRet
     */
    public void getnRet(int nRet){
            if(ToastShow){
                Toast.makeText(getActivity(),"错误码："+nRet+"\n"+"请查阅开发手册！",Toast.LENGTH_LONG).show();
                ToastShow = false;
            }

    }

    /**
     * 根据seekbar位置 设置焦距
     * @param progress
     */
    public void setFocallength(int progress){
       cameraManager.setFocallength(progress);
    }

    /**
     * seekbar 单位长度对应的焦距值
     * @return
     */
    public double  getFocal(){

        return (double)cameraManager.getFocal();
    }


    /**
     *  调整seekbar时  判断是否暂停执行识别函数  点击时暂停，调整完毕解除
     * @param issuspended
     */
    public void setRecogsuspended(boolean issuspended){
        cameraManager.setRecogsuspended(issuspended);
    }

    /**
     * 控制闪光灯
     */
    public void setFlash(){
        cameraManager.setTorch();
    }
    public void setROIArea(int left,int top,int right,int bottom){
        myCameraActivity.areas[0]=left;
        myCameraActivity.areas[1]=top;
        myCameraActivity.areas[2]=right;
        myCameraActivity.areas[3]=bottom;
    }

}
