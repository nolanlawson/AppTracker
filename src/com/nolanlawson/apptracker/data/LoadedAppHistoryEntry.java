package com.nolanlawson.apptracker.data;

import java.util.Comparator;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.util.DrawableUtil;

/**
 * representation of an AppHistoryEntry whose data has already been loaded (e.g. icon and label)
 * @author nolan
 *
 */
public class LoadedAppHistoryEntry {

	private AppHistoryEntry appHistoryEntry;
	private Bitmap iconBitmap;
	private CharSequence title;
	
	public AppHistoryEntry getAppHistoryEntry() {
		return appHistoryEntry;
	}



	public void setAppHistoryEntry(AppHistoryEntry appHistoryEntry) {
		this.appHistoryEntry = appHistoryEntry;
	}



	public Bitmap getIconBitmap() {
		return iconBitmap;
	}



	public void setIconBitmap(Bitmap iconBitmap) {
		this.iconBitmap = iconBitmap;
	}



	public CharSequence getTitle() {
		return title;
	}



	public void setTitle(CharSequence title) {
		this.title = title;
	}


	public static LoadedAppHistoryEntry fromAppHistoryEntry(
			AppHistoryEntry appHistoryEntry, PackageInfo packageInfo, PackageManager packageManager,
			Context context) {
		
		LoadedAppHistoryEntry loadedAppHistoryEntry = new LoadedAppHistoryEntry();
		loadedAppHistoryEntry.setAppHistoryEntry(appHistoryEntry);
		
		loadedAppHistoryEntry.setTitle(packageInfo.applicationInfo.loadLabel(packageManager));
		
		Drawable iconDrawable = packageInfo.applicationInfo.loadIcon(packageManager);
		Bitmap iconBitmap = DrawableUtil.convertIconToBitmap(context, iconDrawable);
		
		loadedAppHistoryEntry.setIconBitmap(iconBitmap);
		
		return loadedAppHistoryEntry;
	}
	
	public static Comparator<LoadedAppHistoryEntry> orderBy(final SortType sortType) {
		
		switch (sortType) {
		case Recent:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return object2.getAppHistoryEntry().getLastAccessed()
					.compareTo(object1.getAppHistoryEntry().getLastAccessed());
				}
			};
		case MostUsed:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return object2.getAppHistoryEntry().getCount() -
							object1.getAppHistoryEntry().getCount();
				}
			};
		case TimeDecay:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return Double.compare(object2.getAppHistoryEntry().getDecayScore(),
							object1.getAppHistoryEntry().getDecayScore());
				}
			};			
		}
		throw new IllegalArgumentException("this should never be " +
				"reached unless we don't know this sortType: " + sortType);
		
	}
	
	
}
