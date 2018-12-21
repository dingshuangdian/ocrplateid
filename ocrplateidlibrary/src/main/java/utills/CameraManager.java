//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package utills;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.kernal.plateid.MemoryCameraActivity;
import com.kernal.plateid.RecogService;

import java.io.IOException;

public final class CameraManager {
    private final Context context;
    private final CameraConfigurationManager configManager;
    private Camera camera;
    private int mOrientation = -1;
    private CameraFragment fragment;
    public final MyPreviewCallback previewCallback;
    public CameraManager(Context context, View view) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context, view);
        this.previewCallback = new MyPreviewCallback(this.configManager);
    }

    /**
     *   开启相机
     * @param holder
     * @param cameraId
     * @throws IOException
     */
    public synchronized void openDriver(SurfaceHolder holder, int cameraId) throws IOException {
        Camera theCamera = this.camera;
        if(theCamera == null) {
            theCamera = Camera.open(cameraId);
            if(theCamera == null) {
                throw new IOException();
            }
            this.camera = theCamera;
        }
        theCamera.setPreviewDisplay(holder);
        this.configManager.initFromCameraParameters(theCamera);
        try {
            if(this.mOrientation != -1) {
                theCamera.setDisplayOrientation(this.mOrientation);
            } else {
                this.setCameraDisplayOrientation(this.context, cameraId, theCamera);
            }
            this.configManager.setDesiredCameraParameters(theCamera, cameraId);
            theCamera.setPreviewCallback(this.previewCallback);
            theCamera.startPreview();
        } catch (RuntimeException var9) {
            var9.printStackTrace();
        }

    }

    /**
     *  判断相机是否存在
     * @return
     */
    public synchronized boolean isOpen() {
        return this.camera != null;
    }

    /**
     * 关闭相机
     */
    public synchronized void closeDriver() {
        synchronized (this) {
            try {
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            } catch (Exception e) {
                Log.i("TAG", e.getMessage());
            }
        }
    }

    /**
     * 停止预览 （即终止相机回调函数）
     */
    public synchronized void stopPreview() {
        if(this.camera != null){
            this.camera.setPreviewCallback(null);
            this.camera.stopPreview();

        }
    }
    public void setTorch() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String flashMode = parameters.getFlashMode();
            if(flashMode==null){
                Toast.makeText(fragment.getActivity(),"不支持闪光灯",Toast.LENGTH_LONG).show();
            }else{
                if (flashMode
                        .equals(Camera.Parameters.FLASH_MODE_TORCH)) {

                    parameters
                            .setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    parameters
                            .setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    parameters.setExposureCompensation(-1);
                }
            }
            try {
                camera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 计算并设置相机取景角度
     * @param context
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(Context context, int cameraId, Camera camera) {

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager windowManager = (WindowManager)context.getSystemService("window");
        int rotation = windowManager.getDefaultDisplay().getRotation();
        short degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if(info.facing == 1) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
        this.mOrientation = result;
    }

    /**
     *  设置焦距
     * @param progress
     */
    void setFocallength(int progress ){

        if(this.camera != null){
            Camera.Parameters parameters = camera.getParameters();
            if(parameters.isZoomSupported()){
                parameters.setZoom(progress);
                camera.setParameters(parameters);
            }else{
                Toast.makeText(fragment.getActivity(),"不支持调焦",Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 计算单位焦距（因为seekbar分为100分，所以单位焦距为最大焦距除以100）
     * @return
     */
    double  getFocal(){
        if(this.camera != null){
            if(camera.getParameters().isZoomSupported()){
                return (double)camera.getParameters().getMaxZoom()/100;
            }else{
                Toast.makeText(fragment.getActivity(),"不支持调焦",Toast.LENGTH_LONG).show();
            }
        }
        return  0;
    }

    /**
     * 传递识别服务  对象参数  以及核心初始化结果参数
     * @param service
     * @param iInitPlateIDSDK
     */
    public void setData(RecogService.MyBinder service, int iInitPlateIDSDK){
        previewCallback.getData(service,iInitPlateIDSDK);
    }

    /**
     *  改变横竖屏状态 （旋转方向）
     * @param Left
     * @param Right
     * @param Top
     * @param Bottom
     */
    public void ChangeState(boolean Left,boolean Right,boolean Top,boolean Bottom){
        previewCallback.ChangeState(Left,Right,Top,Bottom);
    }

    /**
     * 传递fragment对象参数
     * @param fragment
     */
    public void setFragment(CameraFragment fragment){
        this.fragment = fragment;
        previewCallback.getFragment(fragment);
    }


    /**
     * 设置想要的预览分辨率
     * @param W
     * @param H
     */
    public void setPreviewSize(int W,int H){
        configManager.setPreSize(W,H);
    }

    /**
     * 获取真实使用的预览分辨率
     * @return
     */
    public Point getPreviewSize(){
        return configManager.getCameraResolution();
    }

    /**
     * 设置布尔值  判断是否执行相机回调函数中的代码（用于调整seekbar时，暂停识别）
     * @param issuspended
     */
    public void setRecogsuspended(boolean issuspended){
        previewCallback.setRecogsuspended(issuspended);
    }
}
