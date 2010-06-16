package com.nolanlawson.apptracker;

import java.util.regex.Pattern;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

import android.app.IntentService;
import android.content.Intent;

/**
 * Update the decay scores of each app history.
 * @author nolan
 *
 */
public class UpdateAppStatsService extends IntentService {
	private static UtilLogger log = new UtilLogger(UpdateAppStatsService.class);

	private AppHistoryDbHelper dbHelper;


	public UpdateAppStatsService() {
		super("UpdateAppStatsService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();

		dbHelper = new AppHistoryDbHelper(getApplicationContext());
		
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		dbHelper.close();
	}
	
	protected void onHandleIntent(Intent intent) {
		
		AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
		synchronized (AppHistoryDbHelper.class) {
			dbHelper.updateAllDecayScores();
		}
		
		WidgetUpdater.updateWidget(getApplicationContext(), dbHelper);
		dbHelper.close();
	}
}
