package com.nolanlawson.apptracker.helper;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;


public class SubtextHelper {
	
	private static final String DATE_FORMATTER_STRING = "0.00";
	
	public static String createSubtext(Context context, SortType sortType,
			AppHistoryEntry appHistoryEntry, boolean abbreviated) {
		
		//TODO: make this localizable
		switch (sortType) {
		case Recent:
			return getHumanReadableDateDiff(appHistoryEntry.getLastAccessed(), false);
		case MostUsed:
		case LeastUsed:
			int count = appHistoryEntry.getCount();
			
			if (abbreviated) {
				return Integer.toString(count);
			} else if (count == 1) {
				return count +" hit";
			} else {
				return count +" hits";
			}
		case TimeDecay:
			DecimalFormat decimalFormat = new DecimalFormat(DATE_FORMATTER_STRING);
			String formattedDecayScore = decimalFormat.format(appHistoryEntry.getDecayScore());
			return abbreviated ? formattedDecayScore : "Score: " + formattedDecayScore;
		case Alphabetic:
			return "";
		case RecentlyInstalled:
			return getHumanReadableDateDiff(appHistoryEntry.getInstallDate(), true);
		case RecentlyUpdated:
			// whether we say an app was updated "never" or "unknown" depends on
			// if we know when it was installed or not
			boolean unknownIfNull = appHistoryEntry.getInstallDate() == null 
					|| appHistoryEntry.getInstallDate().getTime() == 0L;
			return getHumanReadableDateDiff(appHistoryEntry.getUpdateDate(), unknownIfNull);
		}
		throw new IllegalArgumentException("cannot find sortType: " + sortType);
	}
	

	/**
	 * Returns some kind of human readable representation of a past date, e.g. "4 mins ago," "2 days ago"
	 * @param date
	 * @return
	 */
	public static String getHumanReadableDateDiff(Date pastDate, boolean unknownIfNull) {
		
		// TODO: localize
		if (pastDate == null || pastDate.getTime() == 0) {
			return unknownIfNull ? "Unknown" : "Never";
		}
		
		Date currentDate = new Date();
		
		long timeDiff = currentDate.getTime() - pastDate.getTime();
		
		if (timeDiff < TimeUnit.SECONDS.toMillis(60)) { // less than a minute ago
			return "<1 min ago";
		} else if (timeDiff < TimeUnit.SECONDS.toMillis(60 * 60)) { // less than an hour ago
			long mins = Math.round(TimeUnit.SECONDS.convert(timeDiff, TimeUnit.MILLISECONDS) / 60.0);
			if (mins == 1) {
				return "1 min ago";
			} else {
				return mins + " mins ago";
			}
		} else if (timeDiff < TimeUnit.SECONDS.toMillis(60 * 60 * 24)) { // less than a day ago
			long hours = Math.round(TimeUnit.SECONDS.convert(timeDiff, TimeUnit.MILLISECONDS) / (60.0 * 60));
			if (hours == 1) {
				return "1 hour ago";
			} else {
				return hours + " hours ago";
			}
		} else if (timeDiff < TimeUnit.SECONDS.toMillis(60 * 60 * 24 * 31)) { // less than 31 days ago
			long days = Math.round(TimeUnit.SECONDS.convert(timeDiff, TimeUnit.MILLISECONDS) / (60.0 * 60 * 24));
			if (days == 1) {
				return "1 day ago";
			} else {
				return days +" days ago";
			}
		} else if (timeDiff < TimeUnit.SECONDS.toMillis(60 * 60 * 24 * 365)){ // less than a year ago
			
			long months = Math.round(TimeUnit.SECONDS.convert(timeDiff, TimeUnit.MILLISECONDS) / (60.0 * 60 * 24 * 30));
			
			if (months == 1) {
				return "1 month ago";
			} else {
				return months + " months ago";
			}
		} else {
			return ">1 year ago";
		}
	}



}
