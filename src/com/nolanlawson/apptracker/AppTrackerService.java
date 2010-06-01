package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.nolanlawson.apptracker.util.UtilLogger;

import android.app.IntentService;
import android.content.Intent;

/**
 * Reads logs.  Named "AppTrackerService" in order to obfuscate, so the user won't get freaked out if they see
 * e.g. "LogReaderService" running on their phone.
 * @author nolan
 *
 */
public class AppTrackerService extends IntentService {

	    private static UtilLogger log = new UtilLogger(AppTrackerService.class);

	    public AppTrackerService() {
	        super("AppTrackerService");
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
					
					if (line.contains("Starting activity")) {
						log.d("log is %s", line);
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

