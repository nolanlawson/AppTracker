package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.nolanlawson.apptracker.data.LoadedAppHistoryAdapter;
import com.nolanlawson.apptracker.data.LoadedAppHistoryEntry;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.ActivityInfoHelper;
import com.nolanlawson.apptracker.helper.ServiceHelper;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerActivity extends ListActivity implements OnTouchListener, OnClickListener {
    
	public static String ACTION_EXCLUDE_APPS = "com.nolanlawson.apptracker.action.EXCLUDE_APPS";
	
	private static final int LOAD_BATCH_SIZE = 4; // how many apps to put in the list at once
	
	private static UtilLogger log = new UtilLogger(AppTrackerActivity.class);
	
	private boolean listLoading = false;
	
	private LinearLayout buttonsLinearLayout, appsToExcludeHeaderLinearLayout;
	private Button recentButton, mostUsedButton, timeDecayButton;
	private Button[] buttons;
	
	private LoadedAppHistoryAdapter adapter;
	private SortType sortType = SortType.Recent;
	
	private boolean excludeAppsMode = false;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.d("onCreate()");
        
        ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(getApplicationContext());

        Intent intent = getIntent();
    	
        if (intent != null && intent.getAction().equals(ACTION_EXCLUDE_APPS)) {
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
    	
		setUpList();
		
		setAppropriateButtonAsPressed();
		
		if (excludeAppsMode) {
			// we're in "exclude apps" mode, so set this up appropriately
			setUpAsExcludeAppsMode();
		}  	

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
		setAppropriateButtonAsPressed();
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
		
		// just redraw the widgets while doing as little work as possible
		setContentView(R.layout.main);
		setUpWidgets(true);
		
		setAppropriateButtonAsPressed();
		
		if (excludeAppsMode) {
			// we're in "exclude apps" mode, so set this up appropriately
			setUpAsExcludeAppsMode();
		}
	
		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		LoadedAppHistoryEntry appHistoryEntry = adapter.getItem(position);
		
		if (excludeAppsMode) {
			// just check the box if we're in exclude apps mode
			
			CheckBox checkBox = (CheckBox) v.findViewById(R.id.app_history_list_check_box);
			checkBox.performClick();
			
		} else {
			
			// otherwise launch it
			
			Intent intent = appHistoryEntry.getAppHistoryEntry().toIntent();
			
			startActivity(intent);			
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
	    	//TODO
	    	break;
	    case R.id.menu_about:
	    	//TODO
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
		appsToExcludeHeaderLinearLayout.setVisibility(View.VISIBLE);
		
			
	}
    
	private void setUpWidgets(boolean listAlreadyLoaded) {
		
		recentButton = (Button) findViewById(R.id.recent_button);
		mostUsedButton = (Button) findViewById(R.id.most_used_button);
		timeDecayButton = (Button) findViewById(R.id.time_decay_button);
		buttons = new Button[]{recentButton, mostUsedButton, timeDecayButton};
		buttonsLinearLayout = (LinearLayout) findViewById(R.id.buttons_layout);
		appsToExcludeHeaderLinearLayout = (LinearLayout) findViewById(R.id.apps_to_exclude_header_layout);
		
		for (Button button : buttons) {
			button.setOnTouchListener(this);
			button.setOnClickListener(this);
		}
	}
	
	private void setAppropriateButtonAsPressed() {
		switch (sortType) {
		case Recent:
			setButtonAsPressed(recentButton);
			break;
		case MostUsed:
			setButtonAsPressed(mostUsedButton);
			break;
		case TimeDecay:
			setButtonAsPressed(timeDecayButton);
			break;
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
		
		getListView().addFooterView(progressView, null, false);

		setListAdapter(adapter);
		
		
		
		AsyncTask<Void, LoadedAppHistoryEntry, Void> task = 
			new AsyncTask<Void, LoadedAppHistoryEntry, Void>(){

			
			
			@Override
			protected Void doInBackground(Void... params) {
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
				
				try {
				
					PackageManager packageManager = getPackageManager();
					
					List<Pair<AppHistoryEntry, ActivityInfo>> pairs = 
						ActivityInfoHelper.getActivityInfos(context, dbHelper, packageManager,0, Integer.MAX_VALUE, sortType, excludeAppsMode);
					
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
				
				getListView().removeFooterView(progressView);
				synchronized (AppTrackerActivity.class) {
					listLoading = false;
				}
				
			}
		
		};
		
		task.execute((Void)null);
		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
				
		// have to do onTouch/ActionUp instead of onClick because
		// onClick will override the "pressed" setting
		if (event.getAction() == MotionEvent.ACTION_UP) {
			
			log.d("action up!");
			clickButton(v);
			
			return true;
		}
		
		return false;
	}
	

	@Override
	public void onClick(View v) {
		clickButton(v);
	}
	private void clickButton(View v) {
		switch (v.getId()) {
		case R.id.recent_button:
			sortType = SortType.Recent;
			setButtonAsPressed(recentButton);
			break;
		case R.id.most_used_button:
			sortType = SortType.MostUsed;
			setButtonAsPressed(mostUsedButton);
			break;
		case R.id.time_decay_button:
			sortType = SortType.TimeDecay;
			setButtonAsPressed(timeDecayButton);
			break;
		}
		adapter.setSortType(sortType);
		adapter.sort(excludeAppsMode ? LoadedAppHistoryEntry.orderByLabel() : LoadedAppHistoryEntry.orderBy(sortType));
		adapter.notifyDataSetInvalidated();
		
	}

	
	private void setButtonAsPressed(final Button button) {
		
		for (Button otherButton : buttons) {
			if (otherButton.getId() != button.getId()) {
				otherButton.setPressed(false);
			}
		}
		button.setPressed(true);
		
	}

}