package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

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
		// TODO Auto-generated method stub
		super.onDisabled(context);
		log.d("onDisabled()");
	}

	@Override
	public void onEnabled(Context context) {
		// TODO Auto-generated method stub
		super.onEnabled(context);
		log.d("onEnabled()");
		
		startBackgroundServiceIfNotAlreadyRunning(context);
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		super.onReceive(context, intent);
		log.d("onReceive()");
		startBackgroundServiceIfNotAlreadyRunning(context);
		
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// TODO Auto-generated method stub
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		log.d("onUpdate()");
		startBackgroundServiceIfNotAlreadyRunning(context);
		

	}
	
	private static synchronized void startBackgroundServiceIfNotAlreadyRunning(Context context) {
		if (!ServiceUtil.checkIfAppTrackerServiceIsRunning(context)) {
			
			Intent intent = new Intent(context, AppTrackerService.class);
			context.startService(intent);
		}
	}

	
	
}
