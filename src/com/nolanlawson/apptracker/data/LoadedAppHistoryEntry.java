package com.nolanlawson.apptracker.data;

import java.util.Comparator;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.ActivityInfoHelper;

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
			AppHistoryEntry appHistoryEntry, ActivityInfo activityInfo, PackageManager packageManager,
			Context context) {
		
		LoadedAppHistoryEntry loadedAppHistoryEntry = new LoadedAppHistoryEntry();
		loadedAppHistoryEntry.setAppHistoryEntry(appHistoryEntry);
		
		String label = ActivityInfoHelper.loadLabelFromAppHistoryEntry(context, appHistoryEntry, 
				activityInfo, packageManager);
		
		loadedAppHistoryEntry.setTitle(label);
		
		Bitmap iconBitmap = ActivityInfoHelper.loadIconFromAppHistoryEntry(context, appHistoryEntry, 
				activityInfo, packageManager);
		
		loadedAppHistoryEntry.setIconBitmap(iconBitmap);
		
		return loadedAppHistoryEntry;
	}
	
	public static Comparator<LoadedAppHistoryEntry> orderByLabel() {
		return new Comparator<LoadedAppHistoryEntry>() {

			@Override
			public int compare(LoadedAppHistoryEntry object1,
					LoadedAppHistoryEntry object2) {
				return object1.getTitle().toString().toLowerCase().compareTo(object2.getTitle().toString().toLowerCase());
			}
		};
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
