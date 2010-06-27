package com.nolanlawson.apptracker.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class ActivityInfoHelper {

	private static UtilLogger log = new UtilLogger(ActivityInfoHelper.class);
	
	/**
	 * Get the package infos for each app history entry fetched from the db, and check
	 * to see if they were uninstalled or not.
	 * @param context
	 * @param appWidgetId
	 * @param dbHelper
	 * @param packageManager
	 * @param pageNumber
	 * @param sortType
	 * @return
	 */
	public static List<Pair<AppHistoryEntry,ActivityInfo>> getActivityInfos(
			Context context, AppHistoryDbHelper dbHelper, 
			PackageManager packageManager, int pageNumber, int limit, SortType sortType,
			boolean showExcludedApps) {
		
		log.d("getActivityInfos(), pageNumber: %d, limit: %d, sortType: %s, showExcludedApps: %s", 
				pageNumber, limit, sortType, showExcludedApps);
		
		List<Pair<AppHistoryEntry,ActivityInfo>> activityInfos = new ArrayList<Pair<AppHistoryEntry,ActivityInfo>>();
		
		List<AppHistoryEntry> appHistories;
		
		mainloop: while (true) {
		
			appHistories = dbHelper.findInstalledAppHistoryEntries(sortType, limit, 
					pageNumber * limit, showExcludedApps);
			
			for (AppHistoryEntry appHistory : appHistories) {
				
				ActivityInfo activityInfo = getActivityInfo(packageManager, appHistory);
				
				if (activityInfo == null) { // uninstalled
					synchronized (AppHistoryDbHelper.class) {
						// update the database to reflect that the app is uninstalled
						dbHelper.setInstalled(appHistory.getId(), false);
					}
					activityInfos.clear();
					// try to select from the database again, while skipping the uninstalled one
					continue mainloop;
				}
				activityInfos.add(new Pair<AppHistoryEntry,ActivityInfo>(appHistory, activityInfo));
			}
			break;
		}
		
		log.d("Received %d appHistories", appHistories.size());
		
		if (appHistories.isEmpty()) {
			log.d("No app history entries yet; canceling update");
			return Collections.emptyList();
		}
		
		return activityInfos;
		
	}
	

	private static ActivityInfo getActivityInfo(
			PackageManager packageManager, 
			AppHistoryEntry appHistoryEntry) {
		ActivityInfo activityInfo = null;
		try {
			activityInfo = packageManager.getActivityInfo(appHistoryEntry.toComponentName(), 0);
		} catch (NameNotFoundException e) {
			log.e(e, "package no longer installed: %s", appHistoryEntry);
		}
		
		return activityInfo;
	}

}
