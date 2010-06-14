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
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.util.DatetimeUtil;
import com.nolanlawson.apptracker.util.PreferenceFetcher;
import com.nolanlawson.apptracker.util.ResourceIdFetcher;
import com.nolanlawson.apptracker.util.UtilLogger;

public class WidgetUpdater {
	
	private static UtilLogger log = new UtilLogger(WidgetUpdater.class);
	
	public static final int APPS_PER_PAGE = 4;
	
	public static final String NEW_PAGE_NUMBER = "newPageNumber";
	public static final String APP_WIDGET_ID = "appWidgetId";
	private static final String URI_SCHEME = "app_tracker_widget";
	
	/**
	 * update only the app widgets associated with the given id
	 * @param context
	 * @param dbHelper
	 * @param appWidgetId
	 */
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
		manager.updateAppWidget(appWidgetId, updateViews);
	}
	
	/**
	 * Update all app tracker app widgets
	 * @param context
	 * @param dbHelper
	 */
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper) {

		ComponentName widget = new ComponentName(context, AppTrackerWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = manager.getAppWidgetIds(widget);
		for (int appWidgetId : appWidgetIds) {
			RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
			manager.updateAppWidget(appWidgetId, updateViews);
		}
	}
	
	
	private static RemoteViews buildUpdate(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tracker_widget);
		
		int pageNumber = PreferenceFetcher.getCurrentPageNumber(context, appWidgetId);
		
		List<AppHistoryEntry> appHistories = 
			dbHelper.getMostRecentAppHistoryEntries(APPS_PER_PAGE, pageNumber * APPS_PER_PAGE);
		
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
		
		
		
		// if no more app results, disable forward button
		// TODO: optimize this
		boolean noMoreAppResults = dbHelper.getMostRecentAppHistoryEntries(APPS_PER_PAGE, (pageNumber + 1) * APPS_PER_PAGE).isEmpty();

		updateViews.setViewVisibility(R.id.forward_button, noMoreAppResults ? View.INVISIBLE : View.VISIBLE);

		log.d("forward button page number: %d", pageNumber + 1);
		updateViews.setOnClickPendingIntent(R.id.forward_button, 
				getPendingIntentForForwardOrBackButton(context, true, pageNumber + 1, appWidgetId));
		
		
		// if no previous app results, disable back button
		updateViews.setViewVisibility(R.id.back_button, pageNumber == 0 ? View.INVISIBLE : View.VISIBLE);
		
		log.d("back button page number: %d", pageNumber - 1);
		updateViews.setOnClickPendingIntent(R.id.back_button, 
				getPendingIntentForForwardOrBackButton(context, false, pageNumber - 1, appWidgetId));
			
		
		
		return updateViews;
		
	}

	private static PendingIntent getPendingIntentForForwardOrBackButton(
			Context context, boolean forward, int newPageNumber, int appWidgetId) {
		
		
		Intent intent = new Intent();
		intent.setAction(forward ? AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_FORWARD
				: AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_BACK);

		intent.putExtra(NEW_PAGE_NUMBER, newPageNumber);
		intent.putExtra(APP_WIDGET_ID, appWidgetId);
		// gotta make this unique for this appwidgetid - otherwise, the PendingIntents conflict
		// it seems to be a quasi-bug in Android
		Uri data = Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://widget/id/#"+forward + newPageNumber), String.valueOf(appWidgetId));
        intent.setData(data);
        
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0 /* no requestCode */, intent, PendingIntent.FLAG_ONE_SHOT);
		
		return pendingIntent;
		
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
