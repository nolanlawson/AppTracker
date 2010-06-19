package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.nolanlawson.apptracker.data.LoadedAppHistoryAdapter;
import com.nolanlawson.apptracker.data.LoadedAppHistoryEntry;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.PackageInfoHelper;
import com.nolanlawson.apptracker.util.Pair;

public class AppTrackerActivity extends ListActivity implements OnClickListener {
    
	private Button recentButton, mostUsedButton, timeDecayButton;
	private Button[] buttons;
	
	private LoadedAppHistoryAdapter adapter;
	private SortType sortType = SortType.Recent;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setUpWidgets(false);
        
        setUpList();
    }

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}    
    


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// just redraw the widgets while doing as little work as possible
		setContentView(R.layout.main);
		setUpWidgets(true);
		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		super.onListItemClick(l, v, position, id);
	}





	private void setUpWidgets(boolean listAlreadyLoaded) {
		
		recentButton = (Button) findViewById(R.id.recent_button);
		mostUsedButton = (Button) findViewById(R.id.most_used_button);
		timeDecayButton = (Button) findViewById(R.id.time_decay_button);
		buttons = new Button[]{recentButton, mostUsedButton, timeDecayButton};
		
		for (Button button : buttons) {
			button.setOnClickListener(this);
			button.setEnabled(listAlreadyLoaded);
		}
	}
    
	private void setUpList() {

		// set up the list in the background with a "loading" progress because it's slooooow
		
		final Context context = getApplicationContext();
		
		AsyncTask<Void, Void, List<LoadedAppHistoryEntry>> task = new AsyncTask<Void, Void, List<LoadedAppHistoryEntry>>(){

			@Override
			protected List<LoadedAppHistoryEntry> doInBackground(Void... params) {
				
				AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(getApplicationContext());
				
				try {
				
					PackageManager packageManager = getPackageManager();
					
					List<Pair<AppHistoryEntry, PackageInfo>> pairs = 
						PackageInfoHelper.getPackageInfos(context, dbHelper, packageManager,0, Integer.MAX_VALUE, sortType);
					
					List<LoadedAppHistoryEntry> loadedEntries = new ArrayList<LoadedAppHistoryEntry>();
					
					for (Pair<AppHistoryEntry, PackageInfo> pair : pairs) {
						LoadedAppHistoryEntry loadedEntry = LoadedAppHistoryEntry.fromAppHistoryEntry(
								pair.getFirst(), pair.getSecond(), packageManager, getApplicationContext());
						loadedEntries.add(loadedEntry);
					}
					
					return loadedEntries;
					
				} finally {
					dbHelper.close();
				}
			}

			
			
			@Override
			protected void onProgressUpdate(Void... values) {
				// TODO Auto-generated method stub
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(List<LoadedAppHistoryEntry> result) {
				super.onPostExecute(result);
				adapter = new LoadedAppHistoryAdapter(context, R.layout.app_history_item, result, sortType);
				
				setListAdapter(adapter);
				
				for (Button button : buttons) {
					button.setEnabled(true);
				}
			}
		
		};
		
		task.execute((Void)null);
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.recent_button:
			sortType = sortType.Recent;
			break;
		case R.id.most_used_button:
			sortType = sortType.MostUsed;
			break;
		case R.id.time_decay_button:
			sortType = SortType.TimeDecay;
			break;
		}
		adapter.sort(LoadedAppHistoryEntry.orderBy(sortType));
		
	}
}