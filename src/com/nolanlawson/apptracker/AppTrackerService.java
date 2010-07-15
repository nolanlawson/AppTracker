package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.util.FlagUtil;
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
	
	private static UtilLogger log = new UtilLogger(AppTrackerService.class);

	private static Pattern launcherPattern = Pattern
			.compile("\\bco?mp=\\{?([^/]++)/([^ \t}]+)");
	
	private static Pattern flagPattern = Pattern.compile("\\bfl(?:g|ags)=0x(\\d+)\\b");

	private boolean kill = false;
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			log.d("Screen waking up; updating widgets");
			

			AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
			try {
				WidgetUpdater.updateWidget(context, dbHelper);
			} finally {
				dbHelper.close();
			}
			
		}
	};


	public AppTrackerService() {
		super("AppTrackerService");
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		log.d("onCreate()");
		
		// update all widgets when the screen wakes up again - that's the case where
		// the user unlocks their screen and sees the home screen, so we need
		// instant updates
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
	}
	
	



	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
		unregisterReceiver(receiver);
		// always restart the service if killed
		restartAppTrackerService();
		kill = true;
	}
	
	@Override
	public void onLowMemory() {
		log.d("onLowMemory()");
		super.onLowMemory();
		// just to be safe, attempt to restart app tracker service 60 seconds after low memory
		// conditions are detected
		restartAppTrackerService();
	}


	protected void onHandleIntent(Intent intent) {
		
		log.d("Starting up AppTrackerService now with intent: %s", intent);

		Process logcatProcess = null;
		BufferedReader reader = null;
		
		try {
			// logcat -d AndroidRuntime:E ActivityManager:V *:S
			logcatProcess = Runtime.getRuntime().exec(
					new String[] { "logcat",
							"AndroidRuntime:E ActivityManager:V *:S" });

			reader = new BufferedReader(new InputStreamReader(logcatProcess
					.getInputStream()));

			String line;
			
			while ((line = reader.readLine()) != null) {
								
				if (kill) {
					log.d("manually killed AppTrackerService");
					break;
				}
				if (line.contains("Starting activity") 
						&& line.contains("=android.intent.action.MAIN")
						&& !line.contains("(has extras)")) { // if it has extras, we can't call it (e.g. com.android.phone)
					log.d("log is %s", line);
					

					AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
					try {					
						if (!line.contains("android.intent.category.HOME")) { // ignore home apps
		
							Matcher flagMatcher = flagPattern.matcher(line);
							
							if (flagMatcher.find()) {
								String flagsAsString = flagMatcher.group(1);
								int flags = Integer.parseInt(flagsAsString, 16);
								
								log.d("flags are: 0x%s",flagsAsString);
								
								// intents have to be "new tasks" and they have to have been launched by the user 
								// (not like e.g. the incoming call screen)
								if (FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NEW_TASK)
										&& !FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NO_USER_ACTION)) {
									
									Matcher launcherMatcher = launcherPattern.matcher(line);
		
									if (launcherMatcher.find()) {
										String packageName = launcherMatcher.group(1);
										String process = launcherMatcher.group(2);
										
										log.d("package name is: " + packageName);
										log.d("process name is: " + process);
										synchronized (AppHistoryDbHelper.class) {
											dbHelper.incrementAndUpdate(packageName, process);
										}
										WidgetUpdater.updateWidget(this, dbHelper);
									}				
								}
								
							}
						} else { // home activity
							// update the widget if it's the home activity, 
							// so that the widgets stay up-to-date when the home screen is invoked
							WidgetUpdater.updateWidget(this, dbHelper);							
						}

					} finally {
						dbHelper.close();
						dbHelper = null;
					}
				}
			}

		}

		catch (IOException e) {
			log.e(e, "unexpected exception");
		}

		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}

			log.d("AppTrackerService died for some reason");

		}
	}


	private void restartAppTrackerService() {
				
		log.d("Attempting to restart appTrackerService because it was killed.");
		
        Intent restartServiceIntent = new Intent();
        restartServiceIntent.setAction(AppTrackerWidgetProvider.ACTION_RESTART_SERVICE);
        
        // have to make this unique for God knows what reason
        restartServiceIntent.setData(Uri.withAppendedPath(Uri.parse(AppTrackerWidgetProvider.URI_SCHEME + "://widget/restart/"), 
        		Long.toHexString(new Random().nextLong())));
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                    0 /* no requestCode */, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        long timeToExecute = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60); // start 60 seconds from now
        
        alarms.set(AlarmManager.RTC, timeToExecute, pendingIntent);
        
        log.d("AppTrackerService will restart at %s", new Date(timeToExecute));
        
	}
	
}
