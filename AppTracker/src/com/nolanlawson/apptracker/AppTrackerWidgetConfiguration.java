package com.nolanlawson.apptracker;

import java.util.Arrays;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.FreemiumHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.ArrayUtil;
import com.nolanlawson.apptracker.util.UtilLogger;


public class AppTrackerWidgetConfiguration extends PreferenceActivity implements OnClickListener, OnPreferenceChangeListener {

	private static UtilLogger log = new UtilLogger(AppTrackerWidgetConfiguration.class);
	 
	private int appWidgetId;
	private AppHistoryDbHelper dbHelper;
	private Button okButton;
	private ProgressBar progressBar;
	
	private CheckBoxPreference lockPagePreference, hideSubtextPreference, hideAppTitlePreference, 
			stretchToFillPreference;
	private ListPreference sortTypePreference, pageNumberPreference;
	private View freeVersionTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.preference_list_content_with_button);
		
		addPreferencesFromResource(R.xml.tracker_widget_config);
		
		dbHelper = new AppHistoryDbHelper(getApplicationContext());
		
		Bundle extras = getIntent().getExtras();
		
		appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
		okButton = (Button) findViewById(R.id.config_ok_button);
		progressBar = (ProgressBar) findViewById(R.id.config_progress_bar);
		
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
		
		// do in background to avoid jankiness
		
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void, Void, Void>(){
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				okButton.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
			}
			@Override
			protected Void doInBackground(Void... params) {
				saveConfigurations();
				sendOutBroadcast();
				return null;

			}
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				setResult();
				finish();	
				okButton.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
			}			
		};
		
		task.execute((Void)null);
	}
	
	private void initializePreferences() {
		
		sortTypePreference = (ListPreference) findPreference(R.string.sort_type_preference);
		sortTypePreference.setSummary(String.format(getText(R.string.sort_type_summary).toString(),sortTypePreference.getEntry()));
		sortTypePreference.setOnPreferenceChangeListener(this);
		
		// TODO: should we enable alphabetic sortings, even if it doesn't list every installed app??
		sortTypePreference.setEntries(ArrayUtil.copyOf(
				sortTypePreference.getEntries(), sortTypePreference.getEntries().length - 1));
		sortTypePreference.setEntryValues(ArrayUtil.copyOf(
				sortTypePreference.getEntryValues(), sortTypePreference.getEntryValues().length - 1));
		
		
		
		int numAppHistories;
		synchronized (AppHistoryDbHelper.class) {
			numAppHistories = dbHelper.findCountOfInstalledAppHistoryEntries();
		}
		
		log.d("num app histories: %d", numAppHistories);
		
		// possible pages of results to show
		int numPages = (numAppHistories / WidgetUpdater.APPS_PER_PAGE) 
			+ (numAppHistories % WidgetUpdater.APPS_PER_PAGE == 0 ? 0 : 1);
		
		CharSequence[] pageNumbers = new CharSequence[numPages];
		
		for (int i = 0; i < numPages; i++) {
			pageNumbers[i] = Integer.toString(i + 1);
		}
		pageNumberPreference = (ListPreference) findPreference(R.string.page_number_preference);
		
		pageNumberPreference.setEntries(pageNumbers);
		pageNumberPreference.setEntryValues(pageNumbers);
		
		
		lockPagePreference = (CheckBoxPreference) findPreference(R.string.lock_page_preference);
		
		lockPagePreference.setOnPreferenceChangeListener(this);
		
		hideSubtextPreference = (CheckBoxPreference) findPreference(R.string.hide_subtext_preference);
		
		hideSubtextPreference.setOnPreferenceChangeListener(this);
		
		hideAppTitlePreference = (CheckBoxPreference) findPreference(R.string.hide_app_title_preference);
		
		hideAppTitlePreference.setOnPreferenceChangeListener(this);
		
		//showBackgroundPreference = (CheckBoxPreference) findPreference(R.string.show_background_preference);
		
		stretchToFillPreference = (CheckBoxPreference) findPreference(R.string.stretch_to_fill_preference);
		
		freeVersionTextView = findViewById(R.id.free_version_notification_view);
		
		// most options are disabled in the free version
		if (FreemiumHelper.isAppTrackerPremiumInstalled(getApplicationContext())) {
			freeVersionTextView.setVisibility(View.GONE);
		} else {
			freeVersionTextView.setVisibility(View.VISIBLE);
			stretchToFillPreference.setChecked(true);
			stretchToFillPreference.setEnabled(false);
			hideAppTitlePreference.setEnabled(false);
			hideSubtextPreference.setEnabled(false);
			lockPagePreference.setChecked(true);
			lockPagePreference.setEnabled(false);
		}
	}
	
	private void setResult() {

		Intent result = new Intent();

		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, result);
	
	}
	
	private void sendOutBroadcast() {

		AppWidgetManager mgr = AppWidgetManager.getInstance(this);
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.tracker_widget);

		mgr.updateAppWidget(appWidgetId, views);
		
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
		
		
		
		CharSequence sortType = sortTypePreference.getValue();
		
		PreferenceHelper.setSortTypePreference(getApplicationContext(), sortType.toString(), appWidgetId);
		
		log.d("pageNumberpref: " + pageNumberPreference);
		log.d("pagenumberprefentry: " + pageNumberPreference.getEntry());
		
		CharSequence pageNumberEntry = pageNumberPreference.getEntry();
		
		int pageNumber = pageNumberEntry != null ? (Integer.parseInt(pageNumberEntry.toString()) - 1) : 0;
		
		PreferenceHelper.setCurrentPageNumber(getApplicationContext(), pageNumber, appWidgetId);
		
		boolean lockPage = lockPagePreference.isChecked();
		PreferenceHelper.setLockPagePreference(getApplicationContext(), lockPage, appWidgetId);
		
		
		boolean hideSubtext = hideSubtextPreference.isChecked();
		PreferenceHelper.setHideSubtextPreference(getApplicationContext(), hideSubtext, appWidgetId);
		
		boolean hideAppTitle = hideAppTitlePreference.isChecked();
		PreferenceHelper.setHideAppTitlePreference(getApplicationContext(), hideAppTitle, appWidgetId);
		
		//boolean showBackground = showBackgroundPreference.isChecked();
		//PreferenceHelper.setShowBackgroundPreference(getApplicationContext(), showBackground, appWidgetId);
		
		boolean stretchToFill = stretchToFillPreference.isChecked();
		PreferenceHelper.setStretchToFillPreference(getApplicationContext(), stretchToFill, appWidgetId);
				

	}


	private Preference findPreference(int stringResId) {
		return findPreference(getResources().getString(stringResId));
	}

	@Override
	public void onClick(View v) {
		// ok button clicked
		completeConfig();
		
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		if (preference.getKey().equals(getText(R.string.sort_type_preference))) {
			updateSortTypePreference(preference, newValue);
			
		} else {
			updateNonSortTypePreference(preference, newValue);
		}
		
		return true;
	}

	private void updateSortTypePreference(Preference preference, Object newValue) {
		
		// if the sorting is alphabetic, then the subtext has to be disabled no matter what, because
		// there is no subtext
		
		if (newValue.equals(getText(R.string.sort_type_alphabetic))) {
			hideSubtextPreference.setChecked(true);
			hideSubtextPreference.setSummary(R.string.hide_subtext_disabled_for_alphabetic);
			
			if (FreemiumHelper.isAppTrackerPremiumInstalled(getApplicationContext())) {
				hideSubtextPreference.setEnabled(false);
			}
			
		} else {
			
			if (sortTypePreference.getValue().equals(getText(R.string.sort_type_alphabetic))) {
				// reset if we're switching back from sort type alphabetic
				hideSubtextPreference.setChecked(false);
				hideSubtextPreference.setSummary(R.string.hide_subtext_summary);
			}
			
			if (FreemiumHelper.isAppTrackerPremiumInstalled(getApplicationContext())) {
		
				hideSubtextPreference.setEnabled(true);
			}
		}
		
		stretchToFillPreference.setEnabled(
				FreemiumHelper.isAppTrackerPremiumInstalled(getApplicationContext()) 
				&& (hideAppTitlePreference.isChecked() 
				|| lockPagePreference.isChecked()
				|| hideSubtextPreference.isChecked()));
		
		// show the printable sort type rather than the internal one
		CharSequence[] entries = sortTypePreference.getEntries();
		CharSequence[] entryValues = sortTypePreference.getEntryValues();
		CharSequence newValueAsEntry = entries[Arrays.asList(entryValues).indexOf(newValue)];
		
		sortTypePreference.setSummary(String.format(getText(R.string.sort_type_summary).toString(),newValueAsEntry));
		
	}

	private void updateNonSortTypePreference(Preference preference,
			Object newValue) {

		
		
		String lockPagePreferenceKey = getResources().getString(R.string.lock_page_preference);
		String hideAppTitlePreferenceKey = getResources().getString(R.string.hide_app_title_preference);
		String hideSubtextPreferenceKey = getResources().getString(R.string.hide_subtext_preference);
		
		if (preference.getKey().equals(lockPagePreferenceKey)) {
			// enable or disable the page number preference depending on whether it's locked
			pageNumberPreference.setEnabled((Boolean)newValue);
		}
		
		boolean enableStretchToFill = (Boolean)newValue;
		
		// if it's being set to true, then we know we want to enable stretch to fill
		// otherwise, we have to check each one individually,
		// because we want to enable or disable the stretch to fill depending on whether or not ANY element
		// is "hidden"
		if (!(Boolean)newValue) {
			if (preference.getKey().equals(lockPagePreferenceKey)) {
				enableStretchToFill = hideAppTitlePreference.isChecked() || hideSubtextPreference.isChecked();
			} else if (preference.getKey().equals(hideAppTitlePreferenceKey)) {
				enableStretchToFill = lockPagePreference.isChecked() || hideSubtextPreference.isChecked();
			} else if (preference.getKey().equals(hideSubtextPreferenceKey)) {
				enableStretchToFill = hideAppTitlePreference.isChecked() || lockPagePreference.isChecked();
			}
		}
		
		stretchToFillPreference.setEnabled(enableStretchToFill);
		
	}
		
}
