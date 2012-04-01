package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.helper.ServiceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerWidgetProvider extends AppWidgetProvider {

	private static UtilLogger log = new UtilLogger(
			AppTrackerWidgetProvider.class);

	public static String ACTION_UPDATE_PAGE_FORWARD = "com.nolanlawson.apptracker.action.PAGE_UPDATE_FORWARD";
	public static String ACTION_UPDATE_PAGE_BACK = "com.nolanlawson.apptracker.action.PAGE_UPDATE_BACK";
	public static String ACTION_RESTART_SERVICE = "com.nolanlawson.apptracker.action.RESTART_SERVICE";
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

		ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(context);

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		log.d("onReceive(); intent is: %s",intent);
		ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(context);
		
		// did the user click the button to update the widget?
		if (ACTION_UPDATE_PAGE_FORWARD.equals(intent.getAction())
				|| ACTION_UPDATE_PAGE_BACK.equals(intent.getAction())) {

			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			
			int newPageNumber = intent.getIntExtra(WidgetUpdater.NEW_PAGE_NUMBER, 0);

			PreferenceHelper.setCurrentPageNumber(context, newPageNumber, appWidgetId);
			
			log.d("moving to new page for appWidgetId %d; pageNumber is now %d", appWidgetId, newPageNumber);
			
			updateWidget(context, appWidgetId);
			
		} else if (ACTION_RESTART_SERVICE.equals(intent.getAction())) {
			log.d("Simply restarted the service, because it was killed");
		} else if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())
				|| Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
				|| Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
			
			log.d("package change event: %s", intent);
			
			if (intent.getData() != null) {
				
				String packageName = intent.getData().getEncodedSchemeSpecificPart();
				
				clearIconAndLabel(context, packageName);
				
				if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
					// have to add a dummy entry in case it gets updated - stupid
					// android market removes and then installs apps so you can't tell
					// that they're being RE-installed
					packageRemoveEvent(context, packageName);
				} else if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
					packageInstallEvent(context, packageName);
				} else if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
					packageReplaceEvent(context, packageName);
				}
			}
			
			updateWidget(context);
			
		}
		
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		log.d("onUpdate() for appWidgetIds %s", appWidgetIds);
		log.d("appWidgetIds are %s", appWidgetIds);
		ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(context);

		doPeriodicUpdate(context);

		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);		
		WidgetUpdater.updateWidget(context, dbHelper);
		dbHelper.close();

	}

	private void packageRemoveEvent(Context context, String packageName) {
		log.d("package removed: %s", packageName);
		
		// package has been removed
		
		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		synchronized (AppHistoryDbHelper.class) {
			dbHelper.addEmptyPackageStubIfNotExists(packageName);
		}
		
		dbHelper.close();			
	}

	
	private void packageReplaceEvent(Context context, String packageName) {

		log.d("package reinstalled: %s", packageName);
		
		// package has been reinstalled
		
		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		synchronized (AppHistoryDbHelper.class) {
			dbHelper.updateUpdateDate(packageName, System.currentTimeMillis());
		}
		
		dbHelper.close();	
		
		
		updateActivityLog(context, packageName);
	}

	private void packageInstallEvent(Context context, String packageName) {
		
		log.d("new package installed: %s", packageName);
		
		// new package installed!  make a note of this date
	
		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		synchronized (AppHistoryDbHelper.class) {
			dbHelper.updateInstallDate(packageName, System.currentTimeMillis());
		}
		
		dbHelper.close();
		
		updateActivityLog(context, packageName);
		
	}

	private void updateActivityLog(Context context, String packageName) {
		PackageManager packageManager = context.getPackageManager();
		
		Intent launchIntent = null;
		try {
			launchIntent = packageManager.getLaunchIntentForPackage(packageName);
		} catch (Exception ex) {
			log.w(ex, "package name not found: %s", packageName);
		}
		
		log.d("launchIntent is %s", launchIntent);
		
		if (launchIntent == null) {
			log.w("launchIntent is null - maybe this package doesn't have one?: package %s", packageName);
			return;
		}
		
		ComponentName componentName = launchIntent.getComponent();
		
		log.d("componentName is '%s' / '%s'", componentName.getPackageName(), componentName.getShortClassName());
		
		AppHistoryDbHelper dbHelper =  new AppHistoryDbHelper(context);
		
		synchronized (AppHistoryDbHelper.class) {
			
			dbHelper.addEmptyPackageAndProcessIfNotExists(componentName.getPackageName(), componentName.getShortClassName());
		}
		
		dbHelper.close();
	}

	private void clearIconAndLabel(Context context, String packageName) {
		log.d("Package was changed; need to clear labels and icon for: %s", packageName);
		
		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		try {
			
			synchronized (AppHistoryDbHelper.class) {
				dbHelper.clearIconAndLabel(packageName);
			}
			
		} finally {
			dbHelper.close();
		}
		
	}
	
	private static void updateWidget(final Context context) {


		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
		
		WidgetUpdater.updateWidget(context, dbHelper);
		dbHelper.close();


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
		
		if (!ServiceHelper.checkIfUpdateAppStatsServiceIsRunning(context)) {

			Intent intent = new Intent(context, UpdateAppStatsService.class);
			context.startService(intent);
		}	
		
	}

}
