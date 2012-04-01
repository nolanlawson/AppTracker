package com.nolanlawson.apptracker.helper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.StopWatch;
import com.nolanlawson.apptracker.util.UtilLogger;

public class ActivityInfoHelper {

	private static UtilLogger log = new UtilLogger(ActivityInfoHelper.class);

	private static int sIconSize = -1;
	
	public static String loadLabelFromAppHistoryEntry(Context context, AppHistoryEntry appHistoryEntry, 
			ActivityInfo activityInfo, PackageManager packageManager) {
		
		//StopWatch stopWatch = new StopWatch("loadLabelFromAppHistoryEntry()");
		
		String label;
		
		if (appHistoryEntry.getLabel() == null) {
			// not loaded yet; load now
			
			CharSequence labelAsCharSequence = activityInfo.loadLabel(packageManager);
			label = labelAsCharSequence.toString();
			
			AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
			
			try {
				
				synchronized (AppHistoryDbHelper.class) {
					dbHelper.setLabel(appHistoryEntry.getId(), label);
				}
				
			} finally {
				dbHelper.close();
			}			
			
		} else {
			// already loaded
			label = appHistoryEntry.getLabel();
		}
		
		//stopWatch.log(log);
		
		return label;
		
	}
	
	public static Bitmap loadIconFromAppHistoryEntry(Context context, AppHistoryEntry appHistoryEntry, 
			ActivityInfo activityInfo, PackageManager packageManager) {
		
		//StopWatch stopWatch = new StopWatch("loadIconFromAppHistoryEntry()");
		
		Bitmap iconBitmap;
		
		if (appHistoryEntry.getIconBlob() == null) {
		
			// icon has not been loaded into db yet; load now
			Drawable iconDrawable = activityInfo.loadIcon(packageManager);
			iconBitmap = convertIconToBitmap(context, iconDrawable);	
			
			// only cache if the user wants us to cache the icon
			if (PreferenceHelper.getEnableIconCachingPreference(context)) {
			
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
				
				byte[] bytes = out.toByteArray();
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
				
				try {
					
					synchronized (AppHistoryDbHelper.class) {
						dbHelper.setIconBlob(appHistoryEntry.getId(), bytes);
					}
					
				} finally {
					dbHelper.close();
				}
			}
			
		} else {
			// icon already loaded; just get it from the database
            byte[] blob = appHistoryEntry.getIconBlob();
            iconBitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
		
		}
		
		//stopWatch.log(log);
		
		return iconBitmap;
		
	}

	
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
		
		if (sortType == SortType.Alphabetic) {
			// have to be sure to load all the labels
			loadLabelsInAdvance(context, dbHelper, packageManager);
		}
		
		
		List<Pair<AppHistoryEntry,ActivityInfo>> activityInfos = new ArrayList<Pair<AppHistoryEntry,ActivityInfo>>();
		
		List<AppHistoryEntry> appHistories;
		
		mainloop: while (true) {
			
			StopWatch stopWatch = new StopWatch("findInstalledAppHistoryEntries()");
			
			synchronized (AppHistoryDbHelper.class) {
				appHistories = dbHelper.findInstalledAppHistoryEntries(sortType, limit, 
						pageNumber * limit, showExcludedApps);
			}
			
			stopWatch.log(log);
			
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
	

	private static void loadLabelsInAdvance(Context context,
			AppHistoryDbHelper dbHelper, PackageManager packageManager) {

		log.d("loading labels in advance");
		
		// if the sort type is alphabetic, then we have to make sure that all the 
		// labels are loaded in the database first
		
		
		List<AppHistoryEntry> labellessEntries = null;
		
		synchronized (AppHistoryDbHelper.class) {
			
			labellessEntries = dbHelper.findInstalledAppHistoryEntriesWithNullLabels();
		}
		
		log.d("found %d labelless app history entries", labellessEntries.size());
		
		for (AppHistoryEntry appHistoryEntry : labellessEntries) {
			ActivityInfo activityInfo = getActivityInfo(packageManager, appHistoryEntry);
			loadLabelFromAppHistoryEntry(context, appHistoryEntry, activityInfo, packageManager);
		}
		
	}

	private static ActivityInfo getActivityInfo(
			PackageManager packageManager, 
			AppHistoryEntry appHistoryEntry) {
		
		ActivityInfo activityInfo = null;
		ComponentName componentName = appHistoryEntry.toComponentName();
		
		try {
			
			activityInfo = packageManager.getActivityInfo(componentName, 0);
		} catch (NameNotFoundException e) {
			log.e(e, "package no longer installed: %s and %s", appHistoryEntry, componentName);
		}
		
		return activityInfo;
	}
	
	private static Bitmap convertIconToBitmap(Context context, Drawable drawable) {
		
		if (sIconSize == -1) {
			sIconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
		}
		
		return toBitmap(drawable, sIconSize, sIconSize);
	}
	
	private static Bitmap toBitmap(Drawable drawable, int width, int height) {
		
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		drawable.setBounds(new Rect(0,0,width,height));
		drawable.draw(c);

		return bmp;
	}
}
