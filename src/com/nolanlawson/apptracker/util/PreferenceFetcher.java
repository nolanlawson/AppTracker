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
		
		int result = prefs.getInt(getPreferenceName(appWidgetId), 0);
		
		return result;
	}
	
	public static void setCurrentPageNumber(Context context, int pageNumber, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putInt(getPreferenceName(appWidgetId), pageNumber);
		editor.commit();
	}
	
	/**
	 * delete a record of this appWidgetId's page number
	 * @param context
	 * @param appWidgetId
	 */
	public static void deleteCurrentPageNumber(Context context, int appWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.remove(getPreferenceName(appWidgetId));
		editor.commit();		
	}
	
	private static String getPreferenceName(int appWidgetId) {
		return "current_page_number_" + appWidgetId;
	}
	
}
