package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.ActivityInfoHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.helper.ResourceIdHelper;
import com.nolanlawson.apptracker.helper.SubtextHelper;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.StopWatch;
import com.nolanlawson.apptracker.util.UtilLogger;

public class WidgetUpdater {
	
	
	public static final int APPS_PER_PAGE = 4;
	
	public static final String NEW_PAGE_NUMBER = "newPageNumber";
	private static final String URI_SCHEME = "app_tracker_widget";
	
	
	private static UtilLogger log = new UtilLogger(WidgetUpdater.class);
	
	/**
	 * update only the app widgets associated with the given id
	 * @param context
	 * @param dbHelper
	 * @param appWidgetId
	 */
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		log.d("updating widget for appWidgetId: " + appWidgetId);
		
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
		if (updateViews != null) {
			manager.updateAppWidget(appWidgetId, updateViews);
		}
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
		log.d("updating widget for all app widget ids: %s", appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			
			// perform this check because there is a bug in Android where, if you
			// exit from the configuration screen, Android will never forget that
			// appWidgetId and it will remain as a "stale" appWidgetId
			boolean exists = PreferenceHelper.checkIfAppExists(context, appWidgetId);
			
			if (!exists) {
				log.d("skipping stale appWidgetId %d", appWidgetId);
				continue;
			}
			
			RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
			if (updateViews == null) { // nothing to see yet
				continue;
			}
			manager.updateAppWidget(appWidgetId, updateViews);
		}
	}
	
	
	private static RemoteViews buildUpdate(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		StopWatch stopWatch1 = new StopWatch("buildUpdate()");
		
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tracker_widget);
		
		
		int pageNumber = PreferenceHelper.getCurrentPageNumber(context, appWidgetId);
		String sortTypeAsString = PreferenceHelper.getSortTypePreference(context, appWidgetId);
		SortType sortType = SortType.findByName(context, sortTypeAsString);
		
		PackageManager packageManager = context.getPackageManager();
		
		StopWatch stopWatch2 = new StopWatch("getActivityInfos()");
		
		List<Pair<AppHistoryEntry,ActivityInfo>> activityInfos = ActivityInfoHelper.getActivityInfos(
				context, dbHelper, packageManager, pageNumber, APPS_PER_PAGE, sortType, false);
		
		stopWatch2.log(log);
		
		if (activityInfos.isEmpty()) {
			// nothing to show for now; just return
			stopWatch1.log(log);
			return null;
		}
		
		StopWatch stopWatch4 = new StopWatch("the forloop");
		
		List<CharSequence> labels = new ArrayList<CharSequence>();
		
		for (int i = 0; i < APPS_PER_PAGE; i++) {
			
			if (i < activityInfos.size()) {
			
				Pair<AppHistoryEntry,ActivityInfo> pair = activityInfos.get(i);
				AppHistoryEntry appHistoryEntry = pair.getFirst();
				ActivityInfo activityInfo = pair.getSecond();
				
				String label = ActivityInfoHelper.loadLabelFromAppHistoryEntry(context, appHistoryEntry, 
						activityInfo, packageManager);
				
				Bitmap iconBitmap = ActivityInfoHelper.loadIconFromAppHistoryEntry(context, appHistoryEntry, 
						activityInfo, packageManager);
				
				
				String subtextText = SubtextHelper.createSubtext(context, sortType, appHistoryEntry, false);
				
				
				updateViews.setTextViewText(ResourceIdHelper.getAppTitleId(i), label);
				updateViews.setTextViewText(ResourceIdHelper.getAppDescriptionId(i), subtextText);
				updateViews.setImageViewBitmap(ResourceIdHelper.getAppIconId(i), iconBitmap);
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.VISIBLE);
				
				
				
				Intent intent = appHistoryEntry.toIntent();

                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                updateViews.setOnClickPendingIntent(ResourceIdHelper.getRelativeLayoutId(i), pendingIntent);
                
                labels.add(label);
                
			} else {
				// no entry; just hide the icon and text
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.INVISIBLE);
			}
		}
		
		stopWatch4.log(log);
		
		log.d("Labels are: %s", labels);
		
		StopWatch stopWatch3 = new StopWatch("a bunch of set() functions");
		
		setAppTitleVisibility(context, appWidgetId, updateViews);
		setSubtextVisibility(context, appWidgetId, updateViews, sortType);
		setBackgroundVisibility(context, appWidgetId, updateViews);
		
		
		setBackAndForwardButtons(context, appWidgetId, updateViews, pageNumber, dbHelper, sortType);
		
		stopWatch3.log(log);
		
		stopWatch1.log(log);
		
		return updateViews;
		
	}
	private static void setBackgroundVisibility(Context context,
			int appWidgetId, RemoteViews updateViews) {
		
		boolean showBackground = PreferenceHelper.getShowBackgroundPreference(context, appWidgetId);

		updateViews.setViewVisibility(R.id.widget_background, showBackground ? View.VISIBLE : View.INVISIBLE);

		
	}

	private static void setBackAndForwardButtons(Context context,
			int appWidgetId, RemoteViews updateViews, int pageNumber, AppHistoryDbHelper dbHelper,
			SortType sortType) {
		
		
		boolean lockPage = PreferenceHelper.getLockPagePreference(context, appWidgetId);
		
		if (lockPage) {
		
			int goneOrInvisible = chooseGoneOrInvisible(context, appWidgetId);
			updateViews.setViewVisibility(R.id.back_button, goneOrInvisible);
			updateViews.setViewVisibility(R.id.forward_button, goneOrInvisible);
		} else {
		
			// if no more app results, disable forward button
			
			boolean noMoreAppResults;
			synchronized (AppHistoryDbHelper.class) {
				noMoreAppResults = dbHelper.findCountOfInstalledAppHistoryEntries(sortType, APPS_PER_PAGE, (pageNumber + 1) * APPS_PER_PAGE, false) == 0;
			}
			updateViews.setViewVisibility(R.id.forward_button, noMoreAppResults ? View.INVISIBLE : View.VISIBLE);
	
			log.d("page number is: %d", pageNumber);
			updateViews.setOnClickPendingIntent(R.id.forward_button, 
					getPendingIntentForForwardOrBackButton(context, true, pageNumber + 1, appWidgetId));
			
			// if no previous app results, disable back button
			updateViews.setViewVisibility(R.id.back_button, pageNumber == 0 ? View.INVISIBLE : View.VISIBLE);

			updateViews.setOnClickPendingIntent(R.id.back_button, 
					getPendingIntentForForwardOrBackButton(context, false, pageNumber - 1, appWidgetId));
		}
		
	}

	private static void setSubtextVisibility(Context context,
			int appWidgetId, RemoteViews updateViews, SortType sortType) {
		
		boolean hideSubtext = PreferenceHelper.getHideSubtextPreference(context, appWidgetId, sortType);
		
		int subTextVisibility = hideSubtext ? chooseGoneOrInvisible(context, appWidgetId) : View.VISIBLE;
		
		updateViews.setViewVisibility(R.id.app_description_1, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_2, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_3, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_4, subTextVisibility);
		
	}

	private static void setAppTitleVisibility(Context context, int appWidgetId,
			RemoteViews updateViews) {
		boolean hideAppTitle = PreferenceHelper.getHideAppTitlePreference(context, appWidgetId);
		
		int subTextVisibility = hideAppTitle ? chooseGoneOrInvisible(context, appWidgetId) : View.VISIBLE;
		
		updateViews.setViewVisibility(R.id.app_title_1, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_2, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_3, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_4, subTextVisibility);
		
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
	
	private static int chooseGoneOrInvisible(Context context, int appWidgetId) {
		return PreferenceHelper.getStretchToFillPreference(context, appWidgetId) ? View.GONE : View.INVISIBLE;
	}

}
