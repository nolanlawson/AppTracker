package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.nolanlawson.apptracker.data.AnalyzedLogLine;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.LogAnalyzer;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

/**
 * Reads logs. Named "AppTrackerService" in order to obfuscate, so the user
 * won't get freaked out if they see e.g. "LogReaderService" running on their
 * phone.
 * 
 * 
 * @author nolan
 * 
 */
public class AppTrackerService extends IntentService {

	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};
	
	private static UtilLogger log = new UtilLogger(AppTrackerService.class);
	
	private boolean kill = false;

	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	
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

		// update all widgets when the screen wakes up again - that's the case
		// where
		// the user unlocks their screen and sees the home screen, so we need
		// instant updates
		log.d("hello0");
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

		log.d("hello1");
		
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		log.d("hello2");
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			log.d(e,"running on older platform; couldn't find startForeground method");
			mStartForeground = mStopForeground = null;
		}

	}


	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
		unregisterReceiver(receiver);
		// always restart the service if killed
		restartAppTrackerService();
		kill = true;
		
		if (PreferenceHelper.getShowNotificationPreference(getApplicationContext())) {
			// Make sure our notification is gone.
			stopForegroundCompat(R.string.notification_title);
		}

	}
	
	@Override
	public void onLowMemory() {
		log.d("onLowMemory()");
		super.onLowMemory();
		// just to be safe, attempt to restart app tracker service 60 seconds after low memory
		// conditions are detected
		restartAppTrackerService();
	}
    // This is the old onStart method that will be called on the pre-2.0
    // platform.
    @Override
    public void onStart(Intent intent, int startId) {
    	log.d("onStart()");
    	super.onStart(intent, startId);
        handleCommand(intent);
    }
/* couldn't get this to work
    public int onStartCommand(Intent intent, int flags, int startId) {
    	log.d("onStartCommand()");
    	try {
    		Method superMethod = getClass().getSuperclass().getMethod("onStartCommand", Intent.class, int.class, int.class);
    		superMethod.se
    		superMethod.invoke(super, intent, flags, startId);
    	} catch (Exception e) {
    		log.e(e, "couldn't invoke super method", mStartForegroundArgs);
    	}
    	
    	super.onStartCommand(intent, flags, startId);
    	
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }*/

	private void handleCommand(Intent intent) {
        
        CharSequence tickerText = getText(R.string.notification_ticker);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.service_notification_1, tickerText,
                System.currentTimeMillis());
        

        Intent appTrackerActivityIntent = new Intent(this, AppTrackerActivity.class);
        appTrackerActivityIntent.setAction(Intent.ACTION_MAIN);
        appTrackerActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        		appTrackerActivityIntent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                       getText(R.string.notification_subtext), contentIntent);

        if (PreferenceHelper.getShowNotificationPreference(getApplicationContext())) {
        	startForegroundCompat(R.string.notification_title, notification);
        }
        
        //handleIntent(intent);

		
	}


	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	private void startForegroundCompat(int id, Notification notification) {
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        try {
	            mStartForeground.invoke(this, mStartForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke startForeground");
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke startForeground");
	        }
	        return;
	    }

	    // Fall back on the old API.
	    setForeground(true);
	    mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	private void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(this, mStopForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke stopForeground");
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke stopForeground");
	        }
	        return;
	    }

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    mNM.cancel(id);
	    setForeground(false);
	}

	protected void onHandleIntent(Intent intent) {
		log.d("onHandleIntent()");
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		
		log.d("Starting up AppTrackerService now with intent: %s", intent);

		Process logcatProcess = null;
		BufferedReader reader = null;
		AppHistoryDbHelper dbHelper = null;
		
		try {
			dbHelper = new AppHistoryDbHelper(this);
			int numLines = getNumberOfExistingLogLines();
			
			log.d("number of existing lines in logcat log is %d", numLines);
			
			int currentLine = 0;
			
			// filter logcat only for ActivityManager messages of Info or higher
			logcatProcess = Runtime.getRuntime().exec(
					new String[] { "logcat",
							"ActivityManager:I", "*:S" });

			reader = new BufferedReader(new InputStreamReader(logcatProcess
					.getInputStream()));
			
			String line;
			
			while ((line = reader.readLine()) != null) {
								
				if (kill) {
					log.d("manually killed AppTrackerService");
					break;
				}
				if (++currentLine <= numLines) {
					//log.d("skipping line %d", currentLine);
					continue;
				}
				
				AnalyzedLogLine analyzedLogLine = LogAnalyzer.analyzeLogLine(line);
				
				if (analyzedLogLine == null) {
					continue; // not an ActivityManager line
				} else if (analyzedLogLine.isStartHomeActivity()) {
					// update the widget if it's the home activity, 
					// so that the widgets stay up-to-date when the home screen is invoked
					WidgetUpdater.updateWidget(this, dbHelper);	
				} else { // valid log line
				
					log.d("package name is: " + analyzedLogLine.getPackageName());
					log.d("process name is: " + analyzedLogLine.getProcessName());
					synchronized (AppHistoryDbHelper.class) {
						dbHelper.incrementAndUpdate(analyzedLogLine.getPackageName(), analyzedLogLine.getProcessName());
					}
					WidgetUpdater.updateWidget(this, dbHelper);
				}
			}

		} catch (IOException e) {
			log.e(e, "unexpected exception");
		} finally {
			if (dbHelper != null) {
				dbHelper.close();
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}
			
			if (logcatProcess != null) {
				logcatProcess.destroy();
			}

			log.i("AppTrackerService died");
		}
	}

	private int getNumberOfExistingLogLines() throws IOException {
		
		// figure out how many lines are already in the logcat log
		// to do this, just use the -d (for "dump") command in logcat
		
		Process logcatProcess = Runtime.getRuntime().exec(
				new String[] { "logcat",
						"-d", "ActivityManager:I", "*:S" });

		BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess
				.getInputStream()));
		try {
			int lines = 0;
			
			while (reader.readLine() != null) {
				lines++;
			}
			
			reader.close();
			logcatProcess.destroy();
			
			return lines;
		} finally {
			
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}
			
			if (logcatProcess != null) {
				logcatProcess.destroy();
			}
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
        
        log.i("AppTrackerService will restart at %s", new Date(timeToExecute));
        
	}
}
