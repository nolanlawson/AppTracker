package com.nolanlawson.apptracker.util;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtil {

	private static UtilLogger log = new UtilLogger(ServiceUtil.class);
	
	public static boolean checkIfAppTrackerServiceIsRunning(Context context) {
		
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		
		List<ActivityManager.RunningServiceInfo> procList = activityManager
				.getRunningServices(Integer.MAX_VALUE);
		
		if (procList != null && procList.size() > 0) {

			for (ActivityManager.RunningServiceInfo appProcInfo : procList) {
				if (appProcInfo != null) {
					//log.d("process is: " + appProcInfo.process);
					if (appProcInfo.process.equals("com.nolanlawson.apptracker")) {
						log.d("intentservice is already running");
						return true;
					}
				}
			}
		}
		log.d("intentservice is not running");
		return false;
	}
}
