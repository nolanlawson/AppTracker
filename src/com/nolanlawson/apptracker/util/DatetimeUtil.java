package com.nolanlawson.apptracker.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DatetimeUtil {

	/**
	 * Returns some kind of human readable representation of a past date, e.g. "4 mins ago," "2 days ago"
	 * @param date
	 * @return
	 */
	public static String getHumanReadableDateDiff(Date pastDate) {
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
			long hours = Math.round(TimeUnit.SECONDS.convert(timeDiff, TimeUnit.MILLISECONDS) / 3600.0);
			if (hours == 1) {
				return "1 hour ago";
			} else {
				return hours + " hours ago";
			}
		} else { // more than one day
			return ">1 day ago";
		}
	}
	
}
