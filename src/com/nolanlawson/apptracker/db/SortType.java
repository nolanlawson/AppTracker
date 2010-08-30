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
}
