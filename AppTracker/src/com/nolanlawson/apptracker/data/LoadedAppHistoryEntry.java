package com.nolanlawson.apptracker.data;

import java.util.Comparator;
import java.util.Date;

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

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((appHistoryEntry == null) ? 0 : appHistoryEntry.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LoadedAppHistoryEntry other = (LoadedAppHistoryEntry) obj;
		if (appHistoryEntry == null) {
			if (other.appHistoryEntry != null)
				return false;
		} else if (!appHistoryEntry.equals(other.appHistoryEntry))
			return false;
		return true;
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
		
		case Alphabetic:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return object1.getTitle().toString().toLowerCase().compareTo(object2.getTitle().toString().toLowerCase());
				}
			};			
		case LeastUsed:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return object1.getAppHistoryEntry().getCount() - object2.getAppHistoryEntry().getCount();
				}
			};		
		case RecentlyInstalled:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return compareDates(object2.getAppHistoryEntry().getInstallDate(), object1.getAppHistoryEntry().getInstallDate());
				}
			};			
		case RecentlyUpdated:
			return new Comparator<LoadedAppHistoryEntry>() {

				@Override
				public int compare(LoadedAppHistoryEntry object1,
						LoadedAppHistoryEntry object2) {
					return compareDates(object2.getAppHistoryEntry().getUpdateDate(), object1.getAppHistoryEntry().getUpdateDate());
				}
			};			
		}
		throw new IllegalArgumentException("this should never be " +
				"reached unless we don't know this sortType: " + sortType);
		
	}
	
	private static int compareDates(Date first, Date second) {
		
		long firstTime = first != null ? first.getTime() : 0;
		long secondTime = second != null ? second.getTime() : 0;
		
		return new Long(firstTime).compareTo(new Long(secondTime));
	}
	
	
}
