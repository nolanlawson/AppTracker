package com.nolanlawson.apptracker;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private static UtilLogger log = new UtilLogger(SettingsActivity.class);
	
	private EditTextPreference decayConstantPreference;
	private Preference appsToExcludePreference, resetDataPreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		initializePreferences();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		saveConfigurations();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void initializePreferences() {
		
		decayConstantPreference = (EditTextPreference) findPreference(R.string.time_decay_constant_preference);
		decayConstantPreference.setOnPreferenceChangeListener(this);
		
		appsToExcludePreference = findPreference(R.string.apps_to_exclude_preference);
		
		resetDataPreference = findPreference(R.string.reset_data_preference);
		
		for (Preference preference : new Preference[]{appsToExcludePreference, resetDataPreference}) {
			preference.setOnPreferenceClickListener(this);
		}
		
	}
	

	private void saveConfigurations() {

		log.d("Saving configurations...");
		
	}


	private Preference findPreference(int stringResId) {
		return findPreference(getResources().getString(stringResId));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {


		try {
			int valueAsInt = Integer.parseInt((String)newValue);
			if (valueAsInt < 1 || valueAsInt > 100) {
				throw new RuntimeException("value must be between 1 and 100");
			}
			PreferenceHelper.setDecayConstantPreference(getApplicationContext(), valueAsInt);
		} catch (Exception ex) {
			log.e(ex, "Couldn't parse number or bad number: %s", newValue);
			Toast.makeText(getApplicationContext(), R.string.bad_decay_constant_toast, Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		
		if (preference.getKey().equals(getResources().getString(R.string.apps_to_exclude_preference))) {
			
			Intent excludeIntent = new Intent(this, AppTrackerActivity.class);
			excludeIntent.setAction(AppTrackerActivity.ACTION_EXCLUDE_APPS);
			startActivity(excludeIntent);
			
		} else { // reset preference
			doResetDialog();
		}
		
		return true;
	}

	private void doResetDialog() {
		Builder builder = new Builder(this);
		
		builder.setTitle(R.string.reset_data_title);
		builder.setCancelable(true);
		builder.setMessage(R.string.delete_all1);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Builder builder = new Builder(SettingsActivity.this);
				
				builder.setTitle(R.string.reset_data_title);
				builder.setCancelable(true);
				builder.setMessage(R.string.delete_all2);
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ok, delete everything
						AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
						
						try {
							synchronized (AppHistoryDbHelper.class) {
								dbHelper.deleteAll();
							}
						} finally {
							dbHelper.close();
						}
						
						Toast.makeText(getApplicationContext(), R.string.data_deleted, Toast.LENGTH_LONG).show();
						
					}
				});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.show();
				
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		
		builder.show();
		
	}
}
