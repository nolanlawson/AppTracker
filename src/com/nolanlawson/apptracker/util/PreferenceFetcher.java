package com.nolanlawson.apptracker.util;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.nolanlawson.apptracker.R;


public class PreferenceFetcher {

	public static int getCurrentPageNumber(Context context, int appWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		int result = prefs.getInt("current_page_number_" + appWidgetId, 0);
		
		return result;
	}
	
	public static void setCurrentPageNumber(Context context, int pageNumber, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putInt("current_page_number_" + appWidgetId, pageNumber);
		
		editor.commit();
	}
	
}
