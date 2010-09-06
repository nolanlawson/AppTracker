package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.nolanlawson.apptracker.data.LoadedAppHistoryAdapter;
import com.nolanlawson.apptracker.data.LoadedAppHistoryEntry;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.ActivityInfoHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.helper.ServiceHelper;
import com.nolanlawson.apptracker.util.ArrayUtil;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerActivity extends Activity implements OnClickListener, OnItemClickListener {
    
	public static String ACTION_EXCLUDE_APPS = "com.nolanlawson.apptracker.action.EXCLUDE_APPS";
	
	private static final int LOAD_BATCH_SIZE = 4; // how many apps to put in the list at once
	
	private static UtilLogger log = new UtilLogger(AppTrackerActivity.class);
	
	private boolean listLoading = false;
	
	private LinearLayout buttonsLinearLayout, headerLinearLayout;
	private Button mainButton;
	private TextView headerDescriptionTextView;
	private ListView listView;
	
	private LoadedAppHistoryAdapter adapter;
	private SortType sortType = SortType.Recent;
	
	private boolean excludeAppsMode = false;
	
	private Handler handler = new Handler(Looper.getMainLooper());
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.d("onCreate()");
        
        Intent intent = getIntent();
    	
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_EXCLUDE_APPS)) {
        	excludeAppsMode = true;
        	setTitle(R.string.apps_to_exclude_title);
        }
        
        setContentView(R.layout.main);
        
        setUpWidgets(false);

        
		adapter = new LoadedAppHistoryAdapter(
				this, R.layout.app_history_item, new ArrayList<LoadedAppHistoryEntry>(), sortType, excludeAppsMode);
		

    }

	@Override
    protected void onResume() {
    	super.onResume();
    	log.d("onResume()");
    	
    	ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(getApplicationContext());
    	
		setUpList();
		
		if (excludeAppsMode) {
			// we're in "exclude apps" mode, so set this up appropriately
			setUpAsExcludeAppsMode();
		}
		
		
		showFirstRunDialog();

    }

	@Override
    protected void onPause() {
    	super.onPause();
    	log.d("onPause()");

    }    
    
    
    
    @Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		log.d("onWindowFocusChanged()");
		
	}

	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	log.d("onDestroy()");
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		log.d("onConfigurationChanged()");
		
		int indexToView = 0;
		if (listView.getCount() > 0) {
			indexToView = listView.getFirstVisiblePosition();
		}
		
		// just redraw the widgets while doing as little work as possible
		setContentView(R.layout.main);
		setUpWidgets(true);
		listView.setAdapter(adapter);
		
		// reset the list to the specified index
		if (listView.getCount() > indexToView) {
			listView.setSelection(indexToView);
		}
		
		if (excludeAppsMode) {
			// we're in "exclude apps" mode, so set this up appropriately
			setUpAsExcludeAppsMode();
		}
		
		changeButtonAndHeaderData();
	
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		
		final LoadedAppHistoryEntry appHistoryEntry = adapter.getItem(position);
		
		if (excludeAppsMode) {
			// just check the box if we're in exclude apps mode
			
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.app_history_list_check_box);
			checkBox.performClick();
			
		} else {
			
			// otherwise give a choice of options
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			final String[] choices = getResources().getStringArray(R.array.app_touch_choices);
			
			builder.setCancelable(true)
				.setSingleChoiceItems(R.array.app_touch_choices, -1, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (choices[which].equals(getApplicationContext().getText(R.string.choice_exclude))) {
							// exclude
							AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
							try {
								synchronized (AppHistoryDbHelper.class) {
									dbHelper.setExcluded(appHistoryEntry.getAppHistoryEntry().getId(), true);
								}
							} finally {
								dbHelper.close();
							}
							adapter.remove(appHistoryEntry);
						} else if (choices[which].equals(getApplicationContext().getText(R.string.choice_uninstall))) {
							// uninstall
							Uri packageURI = Uri.fromParts("package",appHistoryEntry.getAppHistoryEntry().getPackageName(), null);
							Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);

							startActivity(uninstallIntent);
						} else {
							// launch
							Intent intent = appHistoryEntry.getAppHistoryEntry().toIntent();
							startActivity(intent);
						}
						dialog.dismiss();
						
					}
				})
				.setTitle(appHistoryEntry.getTitle())
				.setIcon(new BitmapDrawable(appHistoryEntry.getIconBitmap()))
				.show();
			
		
		}
		

	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.menu_refresh:
	    	setUpList();
	    	break;
	    case R.id.menu_settings:
	    	Intent settingsIntent = new Intent(this, SettingsActivity.class);
	    	startActivity(settingsIntent);
	    	break;
	    case R.id.menu_user_guide:
	    	Intent userGuideIntent = new Intent(this, HtmlFileActivity.class);
	    	userGuideIntent.setAction(HtmlFileActivity.ACTION_USER_GUIDE);
	    	startActivity(userGuideIntent);
	    	break;
	    case R.id.menu_about:
	    	Intent aboutIntent = new Intent(this, HtmlFileActivity.class);
	    	aboutIntent.setAction(HtmlFileActivity.ACTION_ABOUT);
	    	startActivity(aboutIntent);
	    	break;
	    }
	    return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		
		// no options if exclude apps mode
		if (excludeAppsMode) {
			for (int i = 0; i < menu.size(); i++) {
				menu.getItem(i).setVisible(false);
			}
		}
		
		
		return super.onPrepareOptionsMenu(menu);
	}
    
    private void setUpAsExcludeAppsMode() {
    	
		buttonsLinearLayout.setVisibility(View.GONE);
		
			
	}
    
	private void setUpWidgets(boolean listAlreadyLoaded) {
		
		headerLinearLayout = (LinearLayout) findViewById(R.id.apps_header_layout);
		
		buttonsLinearLayout = (LinearLayout) findViewById(R.id.main_buttons_linear_layout);
		
		mainButton = (Button) findViewById(R.id.main_button);
		mainButton.setOnClickListener(this);
		
		headerDescriptionTextView = (TextView) headerLinearLayout.findViewById(R.id.app_history_list_description);
		
		listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(this);
		
		changeButtonAndHeaderData();
		
	}
	
	private void showFirstRunDialog() {



		boolean isFirstRun = PreferenceHelper.getFirstRunPreference(getApplicationContext());
		if (isFirstRun) {
			
			AlertDialog.Builder builder = new Builder(this);
			builder.setMessage(R.string.first_run_message);
			builder.setCancelable(false);
			builder.setIcon(R.drawable.holmes_icon);
			builder.setTitle(R.string.first_run_title);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							PreferenceHelper.setFirstRunPreference(getApplicationContext(), false);
						}
					});
			builder.create().show();
			
		}

	}	
    
	private void setUpList() {

		synchronized (AppTrackerActivity.class) {
			if (listLoading) {
				return; // somebody else already doing it
			} else {
				listLoading = true; // we're doing it
			}
		}
		
		adapter.clear();
		
		// set up the list in the background with a "loading" progress because it's slooooow
		
		final Context context = getApplicationContext();
		
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		final View progressView = layoutInflater.inflate(R.layout.progress_footer, null);
		
		listView.addFooterView(progressView, null, false);
		listView.setAdapter(adapter);
		
		
		
		AsyncTask<Void, LoadedAppHistoryEntry, Void> task = 
			new AsyncTask<Void, LoadedAppHistoryEntry, Void>(){

			
			
			@Override
			protected Void doInBackground(Void... params) {
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
				
				try {
				
					PackageManager packageManager = getPackageManager();
					
					List<Pair<AppHistoryEntry, ActivityInfo>> pairs = 
						ActivityInfoHelper.getActivityInfos(context, dbHelper, packageManager,0, Integer.MAX_VALUE, SortType.Recent, excludeAppsMode);
					
					List<LoadedAppHistoryEntry> entryList = new ArrayList<LoadedAppHistoryEntry>();
					
					
					for (int i = 0; i < pairs.size(); i++) {
						
						Pair<AppHistoryEntry, ActivityInfo> pair = pairs.get(i);
						LoadedAppHistoryEntry loadedEntry = LoadedAppHistoryEntry.fromAppHistoryEntry(
								pair.getFirst(), pair.getSecond(), packageManager, getApplicationContext());
						entryList.add(loadedEntry);
						
						// batch the updates for a smoother-looking UI
						if (entryList.size() == LOAD_BATCH_SIZE || i == pairs.size() - 1) {
							publishProgress(entryList.toArray(new LoadedAppHistoryEntry[entryList.size()]));
							entryList.clear();
						}
					}
					
					
					return null;
					
				} finally {
					dbHelper.close();
				}
			}
			
			@Override
			protected void onProgressUpdate(LoadedAppHistoryEntry... values) {
				super.onProgressUpdate(values);
				adapter.setNotifyOnChange(false);
				for (LoadedAppHistoryEntry entry : values) {
					adapter.add(entry);
				}

				adapter.sort(excludeAppsMode ? LoadedAppHistoryEntry.orderByLabel() : LoadedAppHistoryEntry.orderBy(sortType));
				adapter.notifyDataSetChanged();
				
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				
				listView.removeFooterView(progressView);
				synchronized (AppTrackerActivity.class) {
					listLoading = false;
				}
				
			}
		
		};
		
		task.execute((Void)null);
		
	}

	private void changeSortType(SortType newSortType) {
		
		sortType = newSortType;
		adapter.setSortType(sortType);
		adapter.sort(excludeAppsMode ? LoadedAppHistoryEntry.orderByLabel() : LoadedAppHistoryEntry.orderBy(sortType));
		adapter.notifyDataSetChanged();
		
		// reset the list to the beginning
		
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				if (listView.getCount() > 0) {
					listView.setSelection(0);
				}
				
			}
		});

		
		changeButtonAndHeaderData();
		
	}

	private void changeButtonAndHeaderData() {

		String[] printableSortTypes = getResources().getStringArray(R.array.sort_type_display_list);
		
		Drawable drawable = getResources().getDrawable(SortType.getDrawableIcon(sortType));
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		WindowManager windowManager = getWindowManager();
		boolean portrait = windowManager.getDefaultDisplay().getWidth() < windowManager.getDefaultDisplay().getHeight();
		if (portrait) {
			mainButton.setCompoundDrawables(drawable, null, null, null);
		} else { // landscape
			mainButton.setCompoundDrawables(null, drawable, null, null);
		}
		mainButton.setText(printableSortTypes[sortType.ordinal()]);
		
		if (excludeAppsMode) {
			headerDescriptionTextView.setText(R.string.app_exclude_text);
		} else {
			String[] descriptions = getResources().getStringArray(R.array.sort_type_descriptions);
			headerDescriptionTextView.setText(descriptions[sortType.ordinal()]);
		}
	}

	@Override
	public void onClick(View v) {

		String[] entries = getResources().getStringArray(R.array.sort_type_display_list);
		
		// disable Alphabetic for now
		entries = ArrayUtil.copyOf(entries, entries.length - 1);
		
		new AlertDialog.Builder(this)
				.setTitle(R.string.choose_sort_type)
				.setCancelable(true)
				.setSingleChoiceItems(entries, sortType.ordinal(), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						changeSortType(SortType.values()[which]);
						dialog.dismiss();
					}
				})
				.show();
		
	}
}
