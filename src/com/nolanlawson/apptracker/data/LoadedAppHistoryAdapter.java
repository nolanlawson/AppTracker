package com.nolanlawson.apptracker.data;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nolanlawson.apptracker.R;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.SubtextHelper;

public class LoadedAppHistoryAdapter extends
		ArrayAdapter<LoadedAppHistoryEntry> {

	private List<LoadedAppHistoryEntry> items;
	private int resourceId;
	private SortType sortType;
	
	public LoadedAppHistoryAdapter(Context context, int resourceId, 
			List<LoadedAppHistoryEntry> items, SortType sortType) {
		super(context, resourceId, items);
		
		this.items = items;
		this.resourceId = resourceId;
		this.sortType = sortType;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		Context context = parent.getContext();
		ViewWrapper wrapper;
		if (convertView == null) {
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = li.inflate(resourceId, parent, false);
			
			wrapper = new ViewWrapper();
			wrapper.icon = (ImageView) convertView.findViewById(R.id.app_history_list_icon);
			wrapper.title = (TextView) convertView.findViewById(R.id.app_history_list_title);
			wrapper.description = (TextView) convertView.findViewById(R.id.app_history_list_description);
			convertView.setTag(wrapper);
		} else {
			wrapper = (ViewWrapper) convertView.getTag();
		}
		
		LoadedAppHistoryEntry entry = items.get(position);
		
		wrapper.icon.setImageBitmap(entry.getIconBitmap());
		wrapper.title.setText(entry.getTitle());
		wrapper.description.setText(SubtextHelper.createSubtext(context, sortType, entry.getAppHistoryEntry()));
		
		return convertView;
		
	}
	
	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}
	
	private static class ViewWrapper {
		
		ImageView icon;
		TextView title, description;
		
	}

}
