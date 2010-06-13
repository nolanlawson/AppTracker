package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.util.ServiceUtil;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerWidgetProvider extends AppWidgetProvider {

	private static UtilLogger log = new UtilLogger(AppTrackerWidgetProvider.class);
	
	
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		
		super.onDeleted(context, appWidgetIds);
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
		
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		log.d("onUpdate()");
		startBackgroundServiceIfNotAlreadyRunning(context);
		
		updateWidget(context);
		

	}
	
	private static synchronized void startBackgroundServiceIfNotAlreadyRunning(Context context) {
		if (!ServiceUtil.checkIfAppTrackerServiceIsRunning(context)) {
			
			Intent intent = new Intent(context, AppTrackerService.class);
			context.startService(intent);
		}
	}
	
	private static void updateWidget(final Context context) {
		AsyncTask<Void,Void,Void> updateTask = new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(context);
				WidgetUpdater.updateWidget(context, dbHelper);
				dbHelper.close();
				
				return null;
			}};
			
		updateTask.execute();
	}
	
}
