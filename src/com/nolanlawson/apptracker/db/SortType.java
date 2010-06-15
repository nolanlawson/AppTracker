package com.nolanlawson.apptracker.db;

import android.content.Context;

import com.nolanlawson.apptracker.R;

public enum SortType {

	
	Recent,
	MostUsed,
	TimeDecay;

	public static SortType findByName(Context context, String name) {
		
		if (name.equals(context.getResources().getString(R.string.sort_type_recent))) {
			return Recent;
		} else if (name.equals(context.getResources().getString(R.string.sort_type_most_used))) {
			return MostUsed;
		} else if (name.equals(context.getResources().getString(R.string.sort_type_time_decay))) {
			return TimeDecay;
		}
		throw new IllegalArgumentException("Can't find SortType to match: " + name);
	}
}
