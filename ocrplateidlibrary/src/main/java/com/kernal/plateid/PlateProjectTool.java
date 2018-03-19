package com.kernal.plateid;

import android.app.Activity;
import android.hardware.Camera;

import java.util.ArrayList;

public class PlateProjectTool {
	public static Camera mCamera;
	public static ArrayList<Activity> platelist = new ArrayList<Activity>();
	public static void addActivityList(Activity activity){
		if (platelist == null) {
			platelist = new ArrayList<Activity>();
		}
		platelist.add(activity);
	}

}
