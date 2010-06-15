package com.nolanlawson.apptracker;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.helper.ResourceIdHelper;
import com.nolanlawson.apptracker.util.DatetimeUtil;
import com.nolanlawson.apptracker.util.UtilLogger;

public class WidgetUpdater {
	
	
	public static final int APPS_PER_PAGE = 4;
	
	public static final String NEW_PAGE_NUMBER = "newPageNumber";
	private static final String URI_SCHEME = "app_tracker_widget";
	
	private static final String DATE_FORMATTER_STRING = "0.00";
	private static UtilLogger log = new UtilLogger(WidgetUpdater.class);
	
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
			if (updateViews == null) { // nothing to see yet
				continue;
			}
			manager.updateAppWidget(appWidgetId, updateViews);
		}
	}
	
	
	private static RemoteViews buildUpdate(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tracker_widget);
		
		int pageNumber = PreferenceHelper.getCurrentPageNumber(context, appWidgetId);
		String sortTypeAsString = PreferenceHelper.getSortTypePreference(context, appWidgetId);
		SortType sortType = SortType.findByName(context, sortTypeAsString);
		
		List<AppHistoryEntry> appHistories = 
			dbHelper.findAppHistoryEntries(sortType, APPS_PER_PAGE, pageNumber * APPS_PER_PAGE);
		
		log.d("Received the following appHistories: %s", appHistories);
		
		if (appHistories.isEmpty()) {
			log.d("No app history entries yet; canceling update");
			return null;
		}
		
		PackageManager packageManager = context.getPackageManager();
		
		List<CharSequence> labels = new ArrayList<CharSequence>();
		
		for (int i = 0; i < APPS_PER_PAGE; i++) {
			
			if (i < appHistories.size()) {
			
				AppHistoryEntry appHistoryEntry = appHistories.get(i);
				
				PackageInfo packageInfo = getPackageInfo(context, appHistoryEntry);
				
				CharSequence label = packageInfo.applicationInfo.loadLabel(packageManager);
				Bitmap iconBitmap = ((BitmapDrawable)packageInfo.applicationInfo.loadIcon(packageManager)).getBitmap();
				String subtextText = createSubtext(context, sortType, appHistoryEntry);
				
				updateViews.setTextViewText(ResourceIdHelper.getAppTitleId(i), label);
				updateViews.setTextViewText(ResourceIdHelper.getAppDescriptionId(i), subtextText);
				updateViews.setImageViewBitmap(ResourceIdHelper.getAppIconId(i), iconBitmap);
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.VISIBLE);
				
				Intent intent = new Intent();
				intent.setClassName(appHistoryEntry.getPackageName(), appHistoryEntry.getPackageName() + "." + appHistoryEntry.getProcess());
				intent.setAction(Intent.ACTION_MAIN);

                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                updateViews.setOnClickPendingIntent(ResourceIdHelper.getRelativeLayoutId(i), pendingIntent);
                
                labels.add(label);
                
			} else {
				// no entry; just hide the icon and text
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.INVISIBLE);
			}
		}
		
		log.d("Labels are: %s", labels);
		
		setSubtextVisibility(context, appWidgetId, updateViews);
		
		setBackAndForwardButtons(context, appWidgetId, updateViews, pageNumber, dbHelper, sortType);
		return updateViews;
		
	}

	private static String createSubtext(Context context, SortType sortType,
			AppHistoryEntry appHistoryEntry) {
		
		//TODO: make this localizable
		switch (sortType) {
		case Recent:
			return DatetimeUtil.getHumanReadableDateDiff(appHistoryEntry.getLastAccessed());
		case MostUsed:
			int count = appHistoryEntry.getCount();
			
			if (count == 1) {
				return count +" hit";
			} else {
				return count +" hits";
			}
		case TimeDecay:
			DecimalFormat decimalFormat = new DecimalFormat(DATE_FORMATTER_STRING);
			String formattedDecayScore = decimalFormat.format(appHistoryEntry.getDecayScore());
			return "Score: " + formattedDecayScore;
		default:
			throw new IllegalArgumentException("cannot find sortType: " + sortType);
		}
	}

	private static void setBackAndForwardButtons(Context context,
			int appWidgetId, RemoteViews updateViews, int pageNumber, AppHistoryDbHelper dbHelper,
			SortType sortType) {
		
		
		boolean lockPage = PreferenceHelper.getLockPagePreference(context, appWidgetId);
		
		if (lockPage) {
		
			updateViews.setViewVisibility(R.id.back_button, View.INVISIBLE);
			updateViews.setViewVisibility(R.id.forward_button, View.INVISIBLE);
		} else {
		
			// if no more app results, disable forward button
			// TODO: optimize this
			boolean noMoreAppResults = dbHelper.findAppHistoryEntries(sortType, APPS_PER_PAGE, (pageNumber + 1) * APPS_PER_PAGE).isEmpty();
	
			updateViews.setViewVisibility(R.id.forward_button, noMoreAppResults ? View.INVISIBLE : View.VISIBLE);
	
			log.d("forward button page number: %d", pageNumber + 1);
			updateViews.setOnClickPendingIntent(R.id.forward_button, 
					getPendingIntentForForwardOrBackButton(context, true, pageNumber + 1, appWidgetId));
			
			// if no previous app results, disable back button
			updateViews.setViewVisibility(R.id.back_button, pageNumber == 0 ? View.INVISIBLE : View.VISIBLE);
			
			log.d("back button page number: %d", pageNumber - 1);
			updateViews.setOnClickPendingIntent(R.id.back_button, 
					getPendingIntentForForwardOrBackButton(context, false, pageNumber - 1, appWidgetId));
		}
		
	}

	private static void setSubtextVisibility(Context context,
			int appWidgetId, RemoteViews updateViews) {
		
		boolean hideSubtext = PreferenceHelper.getHideSubtextPreference(context, appWidgetId);
		
		int subTextVisibility = hideSubtext ? View.INVISIBLE : View.VISIBLE;
		
		updateViews.setViewVisibility(R.id.app_description_1, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_2, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_3, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_4, subTextVisibility);
		
	}

	private static PendingIntent getPendingIntentForForwardOrBackButton(
			Context context, boolean forward, int newPageNumber, int appWidgetId) {
		
		
		Intent intent = new Intent();
		intent.setAction(forward ? AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_FORWARD
				: AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_BACK);

		intent.putExtra(NEW_PAGE_NUMBER, newPageNumber);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
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
