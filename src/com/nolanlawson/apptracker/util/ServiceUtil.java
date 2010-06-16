package com.nolanlawson.apptracker.util;

import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

public class ServiceUtil {

	private static UtilLogger log = new UtilLogger(ServiceUtil.class);
	
	public static boolean checkIfAppTrackerServiceIsRunning(Context context) {
		
		return checkIfServiceIsRunning(context, "com.nolanlawson.apptracker.AppTrackerService");
	}

	public static boolean checkIfUpdateAppStatsServiceIsRunning(Context context) {
		return checkIfServiceIsRunning(context, "com.nolanlawson.apptracker.UpdateAppStatsService");
	}
	
	private static boolean checkIfServiceIsRunning(Context context, String serviceName) {
		
		ComponentName componentName = new ComponentName("com.nolanlawson.apptracker", serviceName);
		
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		List<ActivityManager.RunningServiceInfo> procList = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (procList != null) {

			for (ActivityManager.RunningServiceInfo appProcInfo : procList) {
				if (appProcInfo != null && componentName.equals(appProcInfo.service)) {
					log.d("%s is already running", serviceName);
					return true;
				}
			}
		}
		log.d("%s is not running", serviceName);
		return false;	
	}
}
