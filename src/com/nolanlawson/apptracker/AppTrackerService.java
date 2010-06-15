package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import com.nolanlawson.apptracker.util.ResourceIdFetcher;
import com.nolanlawson.apptracker.util.UtilLogger;

/**
 * Reads logs. Named "AppTrackerService" in order to obfuscate, so the user
 * won't get freaked out if they see e.g. "LogReaderService" running on their
 * phone.
 * 
 * @author nolan
 * 
 */
public class AppTrackerService extends IntentService {

	private AppHistoryDbHelper dbHelper;

	
	private static UtilLogger log = new UtilLogger(AppTrackerService.class);

	private static Pattern launcherPattern = Pattern
			.compile("\\bcmp=(.+?)/\\.?(.+?)\\s");



	public AppTrackerService() {
		super("AppTrackerService");
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

		log.d("Starting up LogReader now");

		Process mLogcatProc = null;
		BufferedReader reader = null;
		
		try {
			// logcat -d AndroidRuntime:E ActivityManager:V *:S
			mLogcatProc = Runtime.getRuntime().exec(
					new String[] { "logcat",
							"AndroidRuntime:E ActivityManager:V *:S" });

			reader = new BufferedReader(new InputStreamReader(mLogcatProc
					.getInputStream()));

			String line;

			while ((line = reader.readLine()) != null) {

				if (line.contains("Starting activity") 
						&& line.contains("act=android.intent.action.MAIN")
						&& line.contains("flg=0x1")  // indicates starting up a new activity, i.e via launcher, AppTracker, or Market notification
						&& !line.contains("(has extras)")) { // if it has extras, we can't call it (e.g. com.android.phone)
					log.d("log is %s", line);

					Matcher matcher = launcherPattern.matcher(line);

					if (matcher.find()) {
						String packageName = matcher.group(1);
						String process = matcher.group(2);
						log.d("package name is: " + packageName);
						log.d("process name is: " + process);
						dbHelper.incrementAndUpdate(packageName, process);
						WidgetUpdater.updateWidget(this, dbHelper);
					}

				}
			}

		}

		catch (IOException e) {
			log.e(e, "unexpected");
			throw new RuntimeException(e);
		}

		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected");
				}
			}

			log.d("done reading logs");

		}
	}

}
