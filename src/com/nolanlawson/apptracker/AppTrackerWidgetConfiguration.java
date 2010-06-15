package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;


public class AppTrackerWidgetConfiguration extends PreferenceActivity {

	private static UtilLogger log = new UtilLogger(AppTrackerWidgetConfiguration.class);
	
	private int appWidgetId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.config);
		
		Bundle extras = getIntent().getExtras();
		
		appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			saveConfigurations();
			setResult();
			finish();
			return true;
		}

		return (super.onKeyDown(keyCode, event));
	}
	
	private void setResult() {

		AppWidgetManager mgr = AppWidgetManager.getInstance(this);
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.tracker_widget);

		mgr.updateAppWidget(appWidgetId, views);

		Intent result = new Intent();

		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, result);
		
        Intent widgetUpdate = new Intent();
        widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
        
        // have to make this unique for God knows what reason, otherwise you get a really
        // funny and strange bug where the PowerControl layout gets grafted onto the wdiget
        // for a split second before it loads
        widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(AppTrackerWidgetProvider.URI_SCHEME + "://widget/id/"), String.valueOf(appWidgetId)));
        
        sendBroadcast(widgetUpdate);
	
	}

	private void saveConfigurations() {

		log.d("Saving configurations...");
		
		ListPreference sortTypePreference = (ListPreference) findPreference(R.string.sort_type_preference);
		CharSequence sortType = sortTypePreference.getEntry();
		
		PreferenceHelper.setSortTypePreference(getApplicationContext(), sortType.toString(), appWidgetId);
		
		EditTextPreference pageNumberPreference = (EditTextPreference) findPreference(R.string.page_number_preference);
		
		int pageNumber = 0;
		try {
			pageNumber = Integer.parseInt(pageNumberPreference.getText()) - 1;
			
			if (pageNumber < 0) {
				showInvalidPageNumberToast();
				pageNumber = 0;
			}
		} catch (NumberFormatException ignore) {
			showInvalidPageNumberToast();
		}
		
		PreferenceHelper.setCurrentPageNumber(getApplicationContext(), pageNumber, appWidgetId);
		
		CheckBoxPreference lockPagePreference = (CheckBoxPreference) findPreference(R.string.lock_page_preference);
		boolean lockPage = lockPagePreference.isChecked();
		PreferenceHelper.setLockPagePreference(getApplicationContext(), lockPage, appWidgetId);
		
		CheckBoxPreference hideSubtextPreference = (CheckBoxPreference) findPreference(R.string.hide_subtext_preference);
		boolean hideSubtext = hideSubtextPreference.isChecked();
		PreferenceHelper.setHideSubtextPreference(getApplicationContext(), hideSubtext, appWidgetId);

	}
	
	private void showInvalidPageNumberToast() {
		Toast.makeText(getApplicationContext(), R.string.invalid_page_number, Toast.LENGTH_LONG).show();
		
	}

	private Preference findPreference(int stringResId) {
		return findPreference(getResources().getString(stringResId));
	}
		
}
