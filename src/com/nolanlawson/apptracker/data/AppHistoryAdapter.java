package com.nolanlawson.apptracker.data;

import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nolanlawson.apptracker.R;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.SubtextHelper;
import com.nolanlawson.apptracker.util.DrawableUtil;
import com.nolanlawson.apptracker.util.Pair;

/**
 * Display some app history adapters inside of the main ListView of the main activity
 * @author nolan
 *
 */
public class AppHistoryAdapter extends ArrayAdapter<Pair<AppHistoryEntry,PackageInfo>> {

	private List<Pair<AppHistoryEntry,PackageInfo>> items;
	private int resourceId;
	private SortType sortType;
	
	public AppHistoryAdapter(Context context, int resourceId, List<Pair<AppHistoryEntry,PackageInfo>> items, SortType sortType) {
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
		
		Pair<AppHistoryEntry,PackageInfo> pair = items.get(position);
		
		AppHistoryEntry entry = pair.getFirst();
		PackageInfo packageInfo = pair.getSecond();
		
		PackageManager packageManager = context.getPackageManager();
		loadIconInBackground(context, wrapper, packageInfo, packageManager);
		loadTitleInBackground(context, wrapper, packageInfo, packageManager);
		
		wrapper.description.setText(SubtextHelper.createSubtext(context, sortType, entry));
		
		return convertView;
		
	}
	
	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}
	
	private void loadTitleInBackground(final Context context, final ViewWrapper wrapper,
			final PackageInfo packageInfo, final PackageManager packageManager) {
		
		wrapper.title.setVisibility(View.INVISIBLE);
		
		AsyncTask<Void, Void, CharSequence> task = new AsyncTask<Void, Void, CharSequence>(){

			@Override
			protected CharSequence doInBackground(Void... params) {
				return packageInfo.applicationInfo.loadLabel(packageManager);
			}
			
			@Override
			protected void onPostExecute(CharSequence result) {
				super.onPostExecute(result);
				wrapper.title.setText(result);
				wrapper.title.setVisibility(View.VISIBLE);
			}	
		
		};
			
		task.execute((Void)null);

	}

	/**
	 * Do this in the background to avoid jankiness.
	 * @param context
	 * @param wrapper
	 * @param packageInfo
	 * @param packageManager
	 */
	private void loadIconInBackground(final Context context, final ViewWrapper wrapper,
			final PackageInfo packageInfo, final PackageManager packageManager) {
		
		wrapper.icon.setVisibility(View.INVISIBLE);
		
		AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>(){

			@Override
			protected Bitmap doInBackground(Void... params) {
				Drawable iconDrawable = packageInfo.applicationInfo.loadIcon(packageManager);
				Bitmap iconBitmap = DrawableUtil.convertIconToBitmap(context, iconDrawable);
				
				return iconBitmap;
			}
			
			@Override
			protected void onPostExecute(Bitmap result) {
				super.onPostExecute(result);
				wrapper.icon.setImageBitmap(result);
				wrapper.icon.setVisibility(View.VISIBLE);
			}	
		
		};
			
		task.execute((Void)null);
	}

	private static class ViewWrapper {
		
		ImageView icon;
		TextView title, description;
		
	}
		
}
