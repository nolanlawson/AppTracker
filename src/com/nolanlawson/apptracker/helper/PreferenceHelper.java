package com.nolanlawson.apptracker.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nolanlawson.apptracker.R;
import com.nolanlawson.apptracker.db.SortType;


public class PreferenceHelper {

	public static final int DEFAULT_TIME_DECAY_CONSTANT = 7;
	
	public static int getCurrentPageNumber(Context context, int appWidgetId) {
		
		if (!FreemiumHelper.isAppTrackerPremiumInstalled(context)) {
			return 0;
		}
		
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
		editor.remove(getHideAppTitlePreferenceName(context, appWidgetId));
		editor.remove(getStretchToFillPreferenceName(context, appWidgetId));
		editor.remove(getSortTypePreferenceName(context, appWidgetId));
		editor.commit();
	}
	
	public static boolean getHideSubtextPreference(Context context, int appWidgetId, SortType sortType) {
		
		if (!FreemiumHelper.isAppTrackerPremiumInstalled(context)) {
			return sortType == SortType.Alphabetic ? true : false;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getHideSubtextPreferenceName(context, appWidgetId), false);
	}
	
	public static void setHideSubtextPreference(Context context, boolean bool, int appWidgetId) {

		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getHideSubtextPreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}
	
	public static boolean getHideAppTitlePreference(Context context, int appWidgetId) {
		
		
		if (!FreemiumHelper.isAppTrackerPremiumInstalled(context)) {
			return false;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getHideAppTitlePreferenceName(context, appWidgetId), false);
	}
	
	public static void setHideAppTitlePreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getHideAppTitlePreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}
	
	
	public static boolean getLockPagePreference(Context context, int appWidgetId) {
		
		if (!FreemiumHelper.isAppTrackerPremiumInstalled(context)) {
			return true;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getLockPagePreferenceName(context, appWidgetId), false);
	}
	
	public static void setLockPagePreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getLockPagePreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}	
	
	public static boolean getShowBackgroundPreference(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getShowBackgroundPreferenceName(context, appWidgetId), false);
	}
	
	public static void setShowBackgroundPreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getShowBackgroundPreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}	
	
	public static boolean getStretchToFillPreference(Context context, int appWidgetId) {
		
		if (!FreemiumHelper.isAppTrackerPremiumInstalled(context)) {
			return true;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getBoolean(getStretchToFillPreferenceName(context, appWidgetId), false);
	}
	
	public static void setStretchToFillPreference(Context context, boolean bool, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putBoolean(getStretchToFillPreferenceName(context, appWidgetId), bool);
		editor.commit();
		
	}		
	
	public static String getSortTypePreference(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.getString(getSortTypePreferenceName(context, appWidgetId), 
				context.getResources().getString(R.string.sort_type_recently_used));
	}
	
	public static void setSortTypePreference(Context context, String sortType, int appWidgetId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putString(getSortTypePreferenceName(context, appWidgetId), sortType);
		editor.commit();
		
	}	
	
	public static int getDecayConstantPreference(Context context) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		int result =  prefs.getInt(context.getResources().getString(R.string.time_decay_constant_preference), 
				DEFAULT_TIME_DECAY_CONSTANT);
		
		// can't return zero or we'll get a zero division error (i.e. infinity)
		if (result > 0 && result <= 100) {
			return result;
		}
		return DEFAULT_TIME_DECAY_CONSTANT;
	}
	
	public static void setDecayConstantPreference(Context context, int decayConstant) {
		
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		editor.putInt(context.getResources().getString(R.string.time_decay_constant_preference), decayConstant);
		editor.commit();
		
	}		
	public static boolean getEnableIconCachingPreference(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean result =  prefs.getBoolean(context.getResources().getString(R.string.enable_icon_caching_preference), 
				true);
		
		return result;
	}	
	
	public static void setEnableIconCachingPreference(Context context, boolean bool) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getResources().getString(R.string.enable_icon_caching_preference), bool);
		editor.commit();
	}
	
	public static boolean getShowNotificationPreference(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean result =  prefs.getBoolean(context.getResources().getString(R.string.show_notification_preference), 
				true);
		
		return result;
	}	
	
	public static void setShowNotificationPreference(Context context, boolean bool) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getResources().getString(R.string.show_notification_preference), bool);
		editor.commit();
	}
	
	public static boolean getFirstRunPreference(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean result =  prefs.getBoolean(context.getResources().getString(R.string.first_run_preference), 
				true);
		
		return result;
	}	
	
	public static void setFirstRunPreference(Context context, boolean bool) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getResources().getString(R.string.first_run_preference), bool);
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
	
	private static String getHideAppTitlePreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.hide_app_title_preference, appWidgetId);
	}
	private static String getShowBackgroundPreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.show_background_preference, appWidgetId);
	}
	private static String getStretchToFillPreferenceName(Context context, int appWidgetId) {
		return concat(context, R.string.stretch_to_fill_preference, appWidgetId);
	}
	
	private static String concat(Context context, int resId, int appWidgetId) {
		return context.getResources().getString(resId) + "_" + appWidgetId;
	}

	public static boolean checkIfAppExists(Context context, int appWidgetId) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return prefs.contains(getSortTypePreferenceName(context, appWidgetId));
		
	}
	
}
