package com.nolanlawson.apptracker;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;


public class AppTrackerWidgetConfiguration extends PreferenceActivity implements OnClickListener {

	private static UtilLogger log = new UtilLogger(AppTrackerWidgetConfiguration.class);
	
	private int appWidgetId;
	private AppHistoryDbHelper dbHelper;
	private Button okButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.preference_list_content_with_button);
		
		addPreferencesFromResource(R.xml.tracker_widget_config);
		
		dbHelper = new AppHistoryDbHelper(getApplicationContext());
		
		Bundle extras = getIntent().getExtras();
		
		appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		okButton = (Button) findViewById(R.id.config_ok_button);
		
		okButton.setOnClickListener(this);
		
		initializePreferences();
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		dbHelper.close();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			completeConfig();
			return true;
		}

		return (super.onKeyDown(keyCode, event));
	}
	
	private void completeConfig() {
		saveConfigurations();
		setResult();
		finish();
	}
	
	private void initializePreferences() {
		
		int numAppHistories = dbHelper.findCountOfInstalledAppHistoryEntries();
		
		log.d("num app histories: %d", numAppHistories);
		
		// possible pages of results to show
		int numPages = (numAppHistories / WidgetUpdater.APPS_PER_PAGE) 
			+ (numAppHistories % WidgetUpdater.APPS_PER_PAGE == 0 ? 0 : 1);
		
		CharSequence[] pageNumbers = new CharSequence[numPages];
		
		for (int i = 0; i < numPages; i++) {
			pageNumbers[i] = Integer.toString(i + 1);
		}
		ListPreference pageNumberPreference = (ListPreference) findPreference(R.string.page_number_preference);
		
		pageNumberPreference.setEntries(pageNumbers);
		pageNumberPreference.setEntryValues(pageNumbers);
		
		
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
		
		ListPreference pageNumberPreference = (ListPreference) findPreference(R.string.page_number_preference);
		
		int pageNumber = Integer.parseInt(pageNumberPreference.getEntry().toString()) - 1;
		
		PreferenceHelper.setCurrentPageNumber(getApplicationContext(), pageNumber, appWidgetId);
		
		CheckBoxPreference lockPagePreference = (CheckBoxPreference) findPreference(R.string.lock_page_preference);
		boolean lockPage = lockPagePreference.isChecked();
		PreferenceHelper.setLockPagePreference(getApplicationContext(), lockPage, appWidgetId);
		
		CheckBoxPreference hideSubtextPreference = (CheckBoxPreference) findPreference(R.string.hide_subtext_preference);
		boolean hideSubtext = hideSubtextPreference.isChecked();
		PreferenceHelper.setHideSubtextPreference(getApplicationContext(), hideSubtext, appWidgetId);

	}


	private Preference findPreference(int stringResId) {
		return findPreference(getResources().getString(stringResId));
	}

	@Override
	public void onClick(View v) {
		// ok button clicked
		completeConfig();
		
	}
		
}
