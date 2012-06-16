package com.nolanlawson.apptracker;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntrySummary;
import com.nolanlawson.apptracker.util.UtilLogger;

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
		
		try {
			
			updateAllDecayScores(dbHelper);
			
			WidgetUpdater.updateWidget(getApplicationContext(), dbHelper);
		} finally {
			dbHelper.close();
		}
			
	}
	
	/**
	 * Go through each decay score and reduce them by a small amount given the current time
	 * and the last time we updated
	 */
	private void updateAllDecayScores(AppHistoryDbHelper dbHelper) {
		
		List<AppHistoryEntrySummary> appHistoryEntries;
		
		synchronized (AppHistoryDbHelper.class) {
			appHistoryEntries = dbHelper.findAllAppHistoryEntrySummariesWithDecayScoreGreaterThan(0.0);
		}

		log.d("Updating all decay scores for %d entries", appHistoryEntries.size());
		
		long currentTime = System.currentTimeMillis();

		try {
			for (AppHistoryEntrySummary appHistoryEntry : appHistoryEntries) {
				
				synchronized (AppHistoryDbHelper.class) {
					dbHelper.updateDecayScore(appHistoryEntry, currentTime);
				}
			}
			
		} catch (Exception ex) {
			log.e(ex, "Unexpected exception; unable to update all decay scores");
		}
		
		log.d("Took %d ms to update all decay scores", (System.currentTimeMillis() - currentTime));

	}
}
