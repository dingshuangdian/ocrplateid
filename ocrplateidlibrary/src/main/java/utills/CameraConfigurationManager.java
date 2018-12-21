package utills;//
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@TargetApi(9)
final class CameraConfigurationManager {
    private static final String TAG = "zhangzhipeng";
    private final Context context;
    private final View view;
    private Point screenResolution;
    public Point cameraResolution;
    private int prewidth,preheight;
    CameraConfigurationManager(Context context, View view) {
        this.context = context;
        this.view = view;
    }
    Point getCameraResolution() {
        return this.cameraResolution;
    }
    /**
     * 设置想要的预览分辨率
     * @param W
     * @param H
     */
    public void setPreSize(int W,int H){
        prewidth = W;
        preheight = H;
    }

    /**
     * 初始化相机参数
     * @param camera
     */
    void initFromCameraParameters(Camera camera) {
        Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager)this.context.getSystemService("window");
        Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        this.screenResolution = new Point(width, height);
        this.cameraResolution = this.findBestPreviewSizeValue( parameters, this.screenResolution);
        LogUtil.E(TAG, "预览分辨率："+cameraResolution);
    }

    /**
     * 设置相机参数  对焦方式  以及预览分辨率
     * @param camera
     * @param
     * @param index
     */
    void setDesiredCameraParameters(Camera camera, int index) {
        Parameters parameters = camera.getParameters();
        if(parameters == null) {
            LogUtil.E(TAG, "setDesiredCameraParameters:Device error: no camera parameters are available. Proceeding without configuration.");
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
            String focusMode = null;
            focusMode = findSettableValue(parameters.getSupportedFocusModes(), new String[]{"continuous-picture", "continuous-video", "auto"});
            if(focusMode != null) {
                parameters.setFocusMode(focusMode);
            }
            parameters.setPreviewSize(this.cameraResolution.x, this.cameraResolution.y);
            camera.setParameters(parameters);
        }
    }


    /**
     *  寻找设置好的预览分辨率
     * @param parameters
     * @param screenResolution
     * @return
     */
    private Point findBestPreviewSizeValue( Parameters parameters, Point screenResolution) {
        //获取所有支持的预览分辨率组
        List<Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        Size supportedPreviewSize;
        if(rawSupportedSizes == null) {
            supportedPreviewSize = parameters.getPreviewSize();
            return new Point(supportedPreviewSize.height, supportedPreviewSize.width);
        } else {
            if(rawSupportedSizes.size()==1){
                return new Point(rawSupportedSizes.get(0).width, rawSupportedSizes.get(0).height);
            }else{
                Iterator var6 = rawSupportedSizes.iterator();
                do {
                    if(!var6.hasNext()) {
                        return this.getCloselyPreSize( parameters, prewidth,preheight);
                    }

                    supportedPreviewSize = (Size)var6.next();
//                    System.out.println("预览分辨率全部："+supportedPreviewSize.width+"   "+supportedPreviewSize.height);
                } while(supportedPreviewSize.width != prewidth || supportedPreviewSize.height != preheight);
            }
            return new Point(prewidth, preheight);
        }
    }

    /**
     *  若未找到设置的预览分辨率   则寻找最接近预览分辨率（优先寻找同比例   例如1920*1080与1280*720 同比例）
     * @param parameters
     * @param
     * @return
     */
    protected Point getCloselyPreSize( Parameters parameters,int width,int height) {
        int ReqTmpWidth = width;
        int ReqTmpHeight = height;
        int realWidth = 0, realHeight = 0;
        float reqRatio = (float) ReqTmpWidth / (float) ReqTmpHeight;
        float deltaRatioMin = 3.4028235E38F;
        List preSizeList = parameters.getSupportedPreviewSizes();
        Size retSize = null;
        Iterator var13 = preSizeList.iterator();
        Size defaultSize;
        while (var13.hasNext()) {
            defaultSize = (Size) var13.next();
            float realRatio = (float) defaultSize.width / (float) defaultSize.height;
            if (reqRatio == realRatio) {
                if(defaultSize.width<=1920){
                    if (realWidth <= defaultSize.width) {
                        realWidth = defaultSize.width;
                        realHeight = defaultSize.height;
                        LogUtil.E(TAG, "筛选参数：" + realWidth + "   " + realHeight);
                    }
                }

            }

        }
        if (realWidth == 0 || realHeight == 0) {
            while (var13.hasNext()) {
                defaultSize = (Size) var13.next();
                float curRatio = (float) defaultSize.width / (float) defaultSize.height;
                float deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize =defaultSize;
                    realWidth = defaultSize.width;
                    realHeight = defaultSize.height;
                }
            }
            if (retSize == null) {
                defaultSize = parameters.getPreviewSize();
                retSize = defaultSize;
                LogUtil.D(TAG, "没找到合适的尺寸，使用默认尺寸: " + defaultSize);
            }
        }
        return new Point(realWidth, realHeight);
    }

    private static String findSettableValue(Collection<String> supportedValues, String... desiredValues) {
        String result = null;
        if(supportedValues != null) {
            String[] var6 = desiredValues;
            int var5 = desiredValues.length;
            for(int i = 0; i < var5; ++i) {
                String desiredValue = var6[i];
                if(supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        return result;
    }
}
