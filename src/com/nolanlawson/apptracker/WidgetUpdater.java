package com.nolanlawson.apptracker;

import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.util.DatetimeUtil;
import com.nolanlawson.apptracker.util.ResourceIdFetcher;
import com.nolanlawson.apptracker.util.UtilLogger;

public class WidgetUpdater {
	
	private static UtilLogger log = new UtilLogger(WidgetUpdater.class);
	
	public static final int APPS_PER_PAGE = 4;
	
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper) {

		ComponentName widget = new ComponentName(context, AppTrackerWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		RemoteViews updateViews = buildUpdate(context, dbHelper);
		manager.updateAppWidget(widget, updateViews);
	}
	
	private static RemoteViews buildUpdate(Context context, AppHistoryDbHelper dbHelper) {
		
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tracker_widget);
		
		List<AppHistoryEntry> appHistories = 
			dbHelper.getMostRecentAppHistoryEntries(APPS_PER_PAGE, 0);
		
		log.d("Received the following appHistories: %s", appHistories);
		
		PackageManager packageManager = context.getPackageManager();
		
		for (int i = 0; i < APPS_PER_PAGE; i++) {
			
			if (i < appHistories.size()) {
			
				AppHistoryEntry appHistoryEntry = appHistories.get(i);
				
				PackageInfo packageInfo = getPackageInfo(context, appHistoryEntry);
				
				CharSequence label = packageInfo.applicationInfo.loadLabel(packageManager);
				Bitmap iconBitmap = ((BitmapDrawable)packageInfo.applicationInfo.loadIcon(packageManager)).getBitmap();
				String dateDiff = DatetimeUtil.getHumanReadableDateDiff(appHistoryEntry.getLastAccessed());
				
				log.d("label is %s", label);
				
				updateViews.setTextViewText(ResourceIdFetcher.getAppTitleId(i), label);
				updateViews.setTextViewText(ResourceIdFetcher.getAppDescriptionId(i), dateDiff);
				updateViews.setImageViewBitmap(ResourceIdFetcher.getAppIconId(i), iconBitmap);
				updateViews.setViewVisibility(ResourceIdFetcher.getRelativeLayoutId(i), View.VISIBLE);
				
				Intent intent = new Intent();
				intent.setClassName(appHistoryEntry.getPackageName(), appHistoryEntry.getPackageName() + "." + appHistoryEntry.getProcess());
				intent.setAction(Intent.ACTION_MAIN);

                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                updateViews.setOnClickPendingIntent(ResourceIdFetcher.getRelativeLayoutId(i), pendingIntent);
                
			} else {
				// no entry; just hide the icon and text
				updateViews.setViewVisibility(ResourceIdFetcher.getRelativeLayoutId(i), View.INVISIBLE);
			}
		}
		
		return updateViews;
		
	}

	private static PackageInfo getPackageInfo(Context context, AppHistoryEntry appHistoryEntry) {
		PackageManager packageManager = context.getPackageManager();
		PackageInfo packageInfo = null;
		try {
			packageInfo = packageManager.getPackageInfo(appHistoryEntry.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			log.e(e, "package no longer installed: %s", appHistoryEntry);
		}
		
		return packageInfo;
	}
}
