package com.nolanlawson.apptracker.data;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.nolanlawson.apptracker.R;
import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.SubtextHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

public class LoadedAppHistoryAdapter extends
		ArrayAdapter<LoadedAppHistoryEntry> {

	private static UtilLogger log = new UtilLogger(LoadedAppHistoryAdapter.class);
	
	private List<LoadedAppHistoryEntry> items;
	private int resourceId;
	private SortType sortType;
	private boolean excludeAppsMode;
	
	public LoadedAppHistoryAdapter(Context context, int resourceId, 
			List<LoadedAppHistoryEntry> items, SortType sortType, boolean excludeAppsMode) {
		super(context, resourceId, items);
		
		this.items = items;
		this.resourceId = resourceId;
		this.sortType = sortType;
		this.excludeAppsMode = excludeAppsMode;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		LoadedAppHistoryEntry entry = items.get(position);
		
		Context context = parent.getContext();
		ViewWrapper wrapper;
		if (convertView == null) {
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(resourceId, parent, false);
			
			wrapper = new ViewWrapper();
			wrapper.icon = (ImageView) convertView.findViewById(R.id.app_history_list_icon);
			wrapper.title = (TextView) convertView.findViewById(R.id.app_history_list_title);
			wrapper.description = (TextView) convertView.findViewById(R.id.app_history_list_description);
			wrapper.subtext = (TextView) convertView.findViewById(R.id.app_history_list_subtext);
			wrapper.enabledCheckBox = (CheckBox) convertView.findViewById(R.id.app_history_list_check_box);
			
			convertView.setTag(wrapper);
		} else {
			wrapper = (ViewWrapper) convertView.getTag();
		}
		
		
		
		wrapper.icon.setImageBitmap(entry.getIconBitmap());
		wrapper.title.setText(entry.getTitle());
		wrapper.description.setText(SubtextHelper.createSubtext(context, sortType, entry.getAppHistoryEntry(), true));
		wrapper.subtext.setText(entry.getAppHistoryEntry().getPackageName());
		
		// do it silently
		wrapper.enabledCheckBox.setOnCheckedChangeListener(null);
		wrapper.enabledCheckBox.setChecked(entry.getAppHistoryEntry().isExcluded());
		wrapper.enabledCheckBox.setOnCheckedChangeListener(new CheckBoxListener(context, entry));
		
		wrapper.description.setVisibility(excludeAppsMode ? View.GONE : View.VISIBLE);
		wrapper.enabledCheckBox.setVisibility(excludeAppsMode ? View.VISIBLE : View.GONE);
		wrapper.subtext.setVisibility(excludeAppsMode ? View.VISIBLE : View.GONE);
		
		return convertView;
		
	}

	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}
	
	
	public void setExcludeAppsMode(boolean excludeAppsMode) {
		this.excludeAppsMode = excludeAppsMode;
	}

	private static class ViewWrapper {
		
		ImageView icon;
		TextView title, description, subtext;
		CheckBox enabledCheckBox;
		
	}
	
	private static class CheckBoxListener implements OnCheckedChangeListener {

		Context context;
		LoadedAppHistoryEntry entry;
		
		CheckBoxListener(Context context, LoadedAppHistoryEntry entry) {
			this.context = context;
			this.entry = entry;
			
		}
		
		@Override
		public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
			
			// synchronize to avoid race conditions if the user clicks the button wildly
			synchronized (CheckBoxListener.class) {	
				
			log.d("onCheckedChanged(): %s", isChecked);
	
				
				final Context finalContext = context;
				
				// update the excluded field in the background to avoid jankiness
				AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
	
	
					@Override
					protected Void doInBackground(Void... params) {
						
	
						
						entry.getAppHistoryEntry().setExcluded(isChecked);
						
						AppHistoryDbHelper dbHelper = new AppHistoryDbHelper(finalContext);
						
						try {
							synchronized (AppHistoryDbHelper.class) {
								dbHelper.setExcluded(entry.getAppHistoryEntry().getId(), isChecked);
							}
						} finally {
							dbHelper.close();
						}
						
						
						return null;
					}
					
				};
				task.execute((Void)null);
			}
			
		}
	}

}
