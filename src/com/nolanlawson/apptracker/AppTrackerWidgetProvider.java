package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.ServiceUtil;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerWidgetProvider extends AppWidgetProvider {

	private static UtilLogger log = new UtilLogger(
			AppTrackerWidgetProvider.class);

	public static String ACTION_UPDATE_PAGE_FORWARD = "com.nolanlawson.apptracker.action.PAGE_UPDATE_FORWARD";
	public static String ACTION_UPDATE_PAGE_BACK = "com.nolanlawson.apptracker.action.PAGE_UPDATE_BACK";
	public static final String URI_SCHEME = "app_tracker_widget";


	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {

		super.onDeleted(context, appWidgetIds);
		
		log.d("deleting appWidgetIds: %s", appWidgetIds);
		
		for (int appWidgetId : appWidgetIds) {
			PreferenceHelper.deletePreferences(context, appWidgetId);
		}
		
		
		log.d("onDeleted()");
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		log.d("onDisabled()");
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		log.d("onEnabled()");

		startBackgroundServiceIfNotAlreadyRunning(context);

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		log.d("onReceive()");
		startBackgroundServiceIfNotAlreadyRunning(context);
		

		
		// did the user click the button to update the widget?
		if (ACTION_UPDATE_PAGE_FORWARD.equals(intent.getAction())
				|| ACTION_UPDATE_PAGE_BACK.equals(intent.getAction())) {
			
			int newPageNumber = intent.getIntExtra(WidgetUpdater.NEW_PAGE_NUMBER, 0);
			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			
			if (appWidgetId != -1) {
			
				PreferenceHelper.setCurrentPageNumber(context, newPageNumber, appWidgetId);
				
				log.d("moving to new page for appWidgetId %d; pageNumber is now %d", appWidgetId, newPageNumber);
				
				updateWidget(context, appWidgetId);
			
			}
			
		}
		
		

	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		log.d("onUpdate() for appWidgetIds %s", appWidgetIds);
		startBackgroundServiceIfNotAlreadyRunning(context);

		doPeriodicUpdate(context);

	}

	private static synchronized void startBackgroundServiceIfNotAlreadyRunning(
			Context context) {
		if (!ServiceUtil.checkIfAppTrackerServiceIsRunning(context)) {

			Intent intent = new Intent(context, AppTrackerService.class);
			context.startService(intent);
		}
	}

	private static void updateWidget(final Context context, final int appWidgetId) {


		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		WidgetUpdater.updateWidget(context, dbHelper, appWidgetId);
		dbHelper.close();


	}
	
	/*
	 * update all app widget ids in the background
	 */
	private static void doPeriodicUpdate(final Context context) {
		
		if (!ServiceUtil.checkIfUpdateAppStatsServiceIsRunning(context)) {

			Intent intent = new Intent(context, UpdateAppStatsService.class);
			context.startService(intent);
		}		
		
	}

}
