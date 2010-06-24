package com.nolanlawson.apptracker.helper;

import android.content.Context;

public class FreemiumHelper {

	public static boolean isAppTrackerPremiumInstalled(Context context) {
		return context.getPackageManager().checkSignatures(
				context.getPackageName(), "com.nolanlawson.apptracker.license") >= 0;
	}
}
