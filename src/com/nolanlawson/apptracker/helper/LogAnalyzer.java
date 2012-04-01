package com.nolanlawson.apptracker.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;

import com.nolanlawson.apptracker.data.AnalyzedLogLine;
import com.nolanlawson.apptracker.util.FlagUtil;
import com.nolanlawson.apptracker.util.UtilLogger;

public class LogAnalyzer {
	
	private static UtilLogger log = new UtilLogger(LogAnalyzer.class);

	private static Pattern activityManagerPattern = Pattern.compile("Activity ?Manager");
	private static Pattern startPattern = Pattern.compile("Starting activity|Starting: Intent|START");
	
	private static Pattern launcherPattern = Pattern
			.compile("\\bco?mp=\\{?([^/]++)/([^ \t}]+)");
	
	// seems to be some kind of unconventional Samsung log pattern
	private static Pattern tryingToLaunchPattern = Pattern
			.compile("Trying to launch ([^/]++)/([^ \t}]+)");
	
	private static Pattern flagPattern = Pattern.compile("\\bfl(?:g|ags)=0x(\\d+)\\b");
	
	
	public static AnalyzedLogLine analyzeLogLine(String line) {
		
		if (!lineIsActivityManagerStart(line)) {
			return null;
		}
		
		if(startPattern.matcher(line).find()
				&& line.contains("=android.intent.action.MAIN") 
				&& !line.contains("(has extras)")) {// if it has extras, we can't call it (e.g. com.android.phone)
			
			if (line.contains("android.intent.category.HOME")) { // ignore home apps
				return new AnalyzedLogLine(true, null, null);
			}
			
			// must contain the proper flags
			if (!lineContainsIncompatibleFlags(line)) {
				
				Matcher launcherMatcher = launcherPattern.matcher(line);
				
				if (launcherMatcher.find()) {
					return new AnalyzedLogLine(false, launcherMatcher.group(1), launcherMatcher.group(2));
				}
			}
		}
		
		Matcher tryingToLaunchMatcher = tryingToLaunchPattern.matcher(line);
		
		if (tryingToLaunchMatcher.find()) {
			// unconventional Samsung log line
			return new AnalyzedLogLine(false, tryingToLaunchMatcher.group(1), tryingToLaunchMatcher.group(2));
		}
		
		return null;
	}


	private static boolean lineContainsIncompatibleFlags(String line) {
		Matcher flagMatcher = flagPattern.matcher(line);
		
		if (flagMatcher.find()) {
			String flagsAsString = flagMatcher.group(1);
			int flags = Integer.parseInt(flagsAsString, 16);
			
			log.d("flags are: 0x%s",flagsAsString);
			
			// intents have to be "new tasks" and they have to have been launched by the user 
			// (not like e.g. the incoming call screen)
			return !FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NEW_TASK)
					|| FlagUtil.hasFlag(flags, Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		}
		return false;
	}


	private static boolean lineIsActivityManagerStart(String line) {
		return activityManagerPattern.matcher(line).find();
	}
}
