package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.content.Intent;

import com.nolanlawson.apptracker.util.UtilLogger;

/**
 * Reads logs.  Named "AppTrackerService" in order to obfuscate, so the user won't get freaked out if they see
 * e.g. "LogReaderService" running on their phone.
 * @author nolan
 *
 */
public class AppTrackerService extends IntentService {

	    private static UtilLogger log = new UtilLogger(AppTrackerService.class);

	    private static Pattern launcherPattern = Pattern.compile("\\bcmp=(.+?)/\\.(.+?) ");
	    
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
						
						Matcher matcher = launcherPattern.matcher(line);
						
						if (matcher.find()) {
							log.d("package name is: " + matcher.group(1));
							log.d("process name is: " + matcher.group(2));
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

