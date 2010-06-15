package com.nolanlawson.apptracker.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nolanlawson.apptracker.R;


public class PreferenceHelper {

	public static int getCurrentPageNumber(Context context, int appWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		int result = prefs.getInt(getCurrentPagePreferenceName(context, appWidgetId), 0);
		
		return result;
	}
	
	public static void setCurrentPageNumber(Context context, int pageNumber, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putInt(getCurrentPagePreferenceName(context, appWidgetId), pageNumber);
		editor.commit();
	}
	
	/**
	 * delete a record of this appWidgetId's page number
	 * @param context
	 * @param appWidgetId
	 */
	public static void deletePreferences(Context context, int appWidgetId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.remove(getCurrentPagePreferenceName(context, appWidgetId));
		editor.remove(getHideSubtextPreferenceName(context, appWidgetId));
		editor.remove(getLockPagePreferenceName(context, appWidgetId));
		editor.commit();
	}
	
	public static boolean getHideSubtextPreference(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getHideSubtextPreferenceName(context, appWidgetId), false);
	}
	
	public static void setHideSubtextPreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getHideSubtextPreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}
	
	public static boolean getLockPagePreference(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getLockPagePreferenceName(context, appWidgetId), false);
	}
	
	public static void setLockPagePreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getLockPagePreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}	
	
	public static String getSortTypePreference(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getString(getSortTypePreference(context, appWidgetId), 
				context.getResources().getString(R.string.sort_type_recent));
	}
	
	public static void setSortTypePreference(Context context, String sortType, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putString(getLockPagePreferenceName(context, appWidgetId), sortType);
		editor.commit();
		
	}	
	
	private static String getCurrentPagePreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.page_number_preference, appWidgetId);
	}
	
	private static String getHideSubtextPreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.hide_subtext_preference, appWidgetId);
	}
	
	private static String getLockPagePreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.lock_page_preference, appWidgetId);
	}
	
	private static String getSortTypePreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.sort_type_preference, appWidgetId);
	}
	
	private static String concat(Context context, int resId, int appWidgetId) {
		return context.getResources().getString(resId) + "_" + appWidgetId;
	}
	
}
