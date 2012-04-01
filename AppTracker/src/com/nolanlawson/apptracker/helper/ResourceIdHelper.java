package com.nolanlawson.apptracker.helper;

import com.nolanlawson.apptracker.R;

public class ResourceIdHelper {

	public static int getAppTitleId(int i) {
		switch (i) {
		case 0:
			return R.id.app_title_1;
		case 1:
			return R.id.app_title_2;
		case 2:
			return R.id.app_title_3;
		case 3:
			return R.id.app_title_4;
		}
		throw new IllegalArgumentException("index invalid: " + i);
	}
	
	public static int getAppDescriptionId(int i) {
		switch (i) {
		case 0:
			return R.id.app_description_1;
		case 1:
			return R.id.app_description_2;
		case 2:
			return R.id.app_description_3;
		case 3:
			return R.id.app_description_4;
		}
		throw new IllegalArgumentException("index invalid: " + i);
	}	
	
	public static int getAppIconId(int i) {
		switch (i) {
		case 0:
			return R.id.app_icon_1;
		case 1:
			return R.id.app_icon_2;
		case 2:
			return R.id.app_icon_3;
		case 3:
			return R.id.app_icon_4;
		}
		throw new IllegalArgumentException("index invalid: " + i);
	}	
	public static int getRelativeLayoutId(int i) {
		switch (i) {
		case 0:
			return R.id.app_item_1;
		case 1:
			return R.id.app_item_2;
		case 2:
			return R.id.app_item_3;
		case 3:
			return R.id.app_item_4;
		}
		throw new IllegalArgumentException("index invalid: " + i);
	}	
}
