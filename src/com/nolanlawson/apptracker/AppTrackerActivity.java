package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
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
import android.widget.ListView;

import com.nolanlawson.apptracker.data.LoadedAppHistoryAdapter;
import com.nolanlawson.apptracker.data.LoadedAppHistoryEntry;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.PackageInfoHelper;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppTrackerActivity extends ListActivity implements OnClickListener, OnTouchListener {
    
	private static UtilLogger log = new UtilLogger(AppTrackerActivity.class);
	
	private Button recentButton, mostUsedButton, timeDecayButton;
	private Button[] buttons;
	
	private LoadedAppHistoryAdapter adapter;
	private SortType sortType = SortType.Recent;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setUpWidgets(false);

		adapter = new LoadedAppHistoryAdapter(
				this, R.layout.app_history_item, new ArrayList<LoadedAppHistoryEntry>(), sortType);

    }

    @Override
    protected void onResume() {
    	super.onResume();
		
        setUpList();
    }
    


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// just redraw the widgets while doing as little work as possible
		setContentView(R.layout.main);
		setUpWidgets(true);
		
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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		LoadedAppHistoryEntry appHistoryEntry = adapter.getItem(position);
		
		Intent intent = new Intent();
		intent.setComponent(appHistoryEntry.getAppHistoryEntry().toComponentName());
		intent.setAction(Intent.ACTION_MAIN);
		
		startActivity(intent);
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
		
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	private void setUpWidgets(boolean listAlreadyLoaded) {
		
		recentButton = (Button) findViewById(R.id.recent_button);
		mostUsedButton = (Button) findViewById(R.id.most_used_button);
		timeDecayButton = (Button) findViewById(R.id.time_decay_button);
		buttons = new Button[]{recentButton, mostUsedButton, timeDecayButton};
		
		for (Button button : buttons) {
			button.setOnClickListener(this);
			button.setOnTouchListener(this);
			button.setEnabled(listAlreadyLoaded);
		}
	}
    
	private void setUpList() {

		adapter.clear();
		
		// set up the list in the background with a "loading" progress because it's slooooow
		
		final Context context = getApplicationContext();
		
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		final View progressView = layoutInflater.inflate(R.layout.progress_footer, null);
		
		getListView().addFooterView(progressView);
		
		setListAdapter(adapter);
		
		
		
		AsyncTask<Void, LoadedAppHistoryEntry, Void> task = 
			new AsyncTask<Void, LoadedAppHistoryEntry, Void>(){

			
			
			@Override
			protected Void doInBackground(Void... params) {
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
				
				try {
				
					PackageManager packageManager = getPackageManager();
					
					List<Pair<AppHistoryEntry, PackageInfo>> pairs = 
						PackageInfoHelper.getPackageInfos(context, dbHelper, packageManager,0, Integer.MAX_VALUE, sortType);
					
					List<LoadedAppHistoryEntry> entryList = new ArrayList<LoadedAppHistoryEntry>();
					
					
					for (int i = 0; i < pairs.size(); i++) {
						
						Pair<AppHistoryEntry, PackageInfo> pair = pairs.get(i);
						LoadedAppHistoryEntry loadedEntry = LoadedAppHistoryEntry.fromAppHistoryEntry(
								pair.getFirst(), pair.getSecond(), packageManager, getApplicationContext());
						entryList.add(loadedEntry);
						
						// batch the updates for a smoother-looking UI
						if (entryList.size() == 2 || i == pairs.size() - 1) {
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
				adapter.notifyDataSetChanged();
				
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				
				for (Button button : buttons) {
					button.setEnabled(true);
				}
				setButtonAsPressed(recentButton);
				getListView().removeFooterView(progressView);
				
			}
		
		};
		
		task.execute((Void)null);
		
	}
	
	@Override
	public void onClick(View v) {

		
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
				
		// have to do onTouch/ActionUp instead of onClick because
		// onClick will override the "pressed" setting
		if (event.getAction() == MotionEvent.ACTION_UP) {
			
			log.d("action up!");
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
			adapter.sort(LoadedAppHistoryEntry.orderBy(sortType));
			adapter.notifyDataSetInvalidated();
			
			return true;
		}
		
		return false;
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