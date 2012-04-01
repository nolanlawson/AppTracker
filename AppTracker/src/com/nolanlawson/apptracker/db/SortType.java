package com.nolanlawson.apptracker.db;

import java.util.Arrays;

import android.content.Context;

import com.nolanlawson.apptracker.R;

public enum SortType {

	
	Recent,
	MostUsed,
	LeastUsed,
	RecentlyInstalled,
	RecentlyUpdated,
	TimeDecay,
	Alphabetic
	;

	public static SortType findByName(Context context, String name) {
		
		String[] nameValues = context.getResources().getStringArray(R.array.sort_type_list);
		
		int index = Arrays.asList(nameValues).indexOf(name);
		
		if (index != -1) {
			return values()[index];
		}
		
		throw new IllegalArgumentException("Can't find SortType to match: " + name);
	}
	
	public static int getDrawableIcon(SortType sortType) {
		
		switch (sortType) {
			case Alphabetic:
				return R.drawable.ic_menu_sort_alphabetically;
			case LeastUsed:
				return R.drawable.ic_menu_star_off;
			case MostUsed:
				return R.drawable.ic_menu_star;
			case Recent:
				return R.drawable.ic_menu_clock;
			case RecentlyInstalled:
				return R.drawable.ic_menu_add;
			case RecentlyUpdated:
				return R.drawable.ic_menu_refresh;
			case TimeDecay:
				return R.drawable.ic_menu_recent_history;
		}
		
		throw new IllegalArgumentException("Can't find sortType: " + sortType);
	}
}
