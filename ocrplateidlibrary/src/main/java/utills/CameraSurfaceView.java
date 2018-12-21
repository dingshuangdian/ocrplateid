package utills;
import android.content.Context;
import android.view.SurfaceView;
public class CameraSurfaceView extends SurfaceView {
    private static final String TAG = "zhangzhipeng";
    private final boolean isPortrait;
    private  int sWidth = 480;
    private  int sHeight = 640;
    private Context context;
    public CameraSurfaceView(Context context, boolean isPortrait,int W,int H) {
        super(context);
        this.context = context;
        this.isPortrait = isPortrait;
        sWidth = W;
        sHeight = H;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int desiredWidth;
        int desiredHeight;
        if(this.isPortrait) {
            desiredWidth = sHeight;
            desiredHeight = sWidth;
        } else {
            desiredWidth = sWidth;
            desiredHeight =sHeight;
        }
        //想要的尺寸比例
        float radio = (float)desiredWidth / (float)desiredHeight;
        LogUtil.I(TAG, "获取显示区域参数比例 radio:" + radio);
        //获取模式  mode共有三种情况，取值分别为MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY, MeasureSpec.AT_MOST。

       // MeasureSpec.EXACTLY是精确尺寸，当我们将控件的layout_width或layout_height指定为具体数值时如andorid:layout_width="50dip"，或者为FILL_PARENT是，都是控件大小已经确定的情况，都是精确尺寸。

        //MeasureSpec.AT_MOST是最大尺寸，当控件的layout_width或layout_height指定为WRAP_CONTENT时，控件大小一般随着控件的子空间或内容进行变化，此时控件尺寸只要不超过父控件允许的最大尺寸即可。因此，此时的mode是AT_MOST，size给出了父控件允许的最大尺寸。

        //MeasureSpec.UNSPECIFIED是未指定尺寸，这种情况不多，一般都是父控件是AdapterView，通过measure方法传入的模式。
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        //获取尺寸
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        boolean layout_width = false;
        boolean layout_height = false;
        int layout_width1;
        //MeasureSpec.EXACTLY是精确尺寸
        if(widthMode == 1073741824) {
            layout_width1 = widthSize;
            //MeasureSpec.AT_MOST是最大尺寸
        } else if(widthMode == -2147483648) {
            layout_width1 = Math.min(desiredWidth, widthSize);
            //MeasureSpec.UNSPECIFIED是未指定尺寸
        } else {
            layout_width1 = desiredWidth;
        }

        int layout_height1;
        if(heightMode == 1073741824) {
            layout_height1 = heightSize;
        } else if(heightMode == -2147483648) {
            layout_height1 = Math.min(desiredHeight, heightSize);
        } else {
            layout_height1 = desiredHeight;
        }

//        LogUtil.I(TAG, "CSV选择宽度:" + layout_width1 + "  设定高度:" + layout_height1);
        float layout_radio = (float)layout_width1 / (float)layout_height1;
        if(layout_radio > radio) {
            layout_height1 = (int)((float)layout_width1 / radio);
        } else {
            layout_width1 = (int)((float)layout_height1 * radio);
        }

        this.setMeasuredDimension(layout_width1, layout_height1);
    }
}
