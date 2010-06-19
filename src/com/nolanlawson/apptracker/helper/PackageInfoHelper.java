package com.nolanlawson.apptracker.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.nolanlawson.apptracker.WidgetUpdater;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class PackageInfoHelper {

	private static UtilLogger log = new UtilLogger(PackageInfoHelper.class);
	
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
	public static List<Pair<AppHistoryEntry,PackageInfo>> getPackageInfos(
			Context context, AppHistoryDbHelper dbHelper, 
			PackageManager packageManager, int pageNumber, int limit, SortType sortType) {
		
		List<Pair<AppHistoryEntry,PackageInfo>> packageInfos = new ArrayList<Pair<AppHistoryEntry,PackageInfo>>();
		
		List<AppHistoryEntry> appHistories;
		
		mainloop: while (true) {
		
			appHistories = dbHelper.findInstalledAppHistoryEntries(sortType, limit, 
					pageNumber * limit);
			
			for (AppHistoryEntry appHistory : appHistories) {
				
				PackageInfo packageInfo = getPackageInfo(context, packageManager, appHistory, dbHelper);
				
				if (packageInfo == null) { // uninstalled
					synchronized (AppHistoryDbHelper.class) {
						// update the database to reflect that the app is uninstalled
						dbHelper.setInstalled(appHistory.getId(), false);
					}
					packageInfos.clear();
					// try to select from the database again, while skipping the uninstalled one
					continue mainloop;
				}
				packageInfos.add(new Pair<AppHistoryEntry,PackageInfo>(appHistory, packageInfo));
			}
			break;
		}
		
		log.d("Received the following appHistories: %s", appHistories);
		
		if (appHistories.isEmpty()) {
			log.d("No app history entries yet; canceling update");
			return Collections.emptyList();
		}
		
		return packageInfos;
		
	}
	

	private static PackageInfo getPackageInfo(
			Context context, PackageManager packageManager, 
			AppHistoryEntry appHistoryEntry, AppHistoryDbHelper dbHelper) {

		PackageInfo packageInfo = null;
		try {
			packageInfo = packageManager.getPackageInfo(appHistoryEntry.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			log.e(e, "package no longer installed: %s", appHistoryEntry);
		}
		
		return packageInfo;
	}

}
