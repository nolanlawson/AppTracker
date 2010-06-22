package com.nolanlawson.apptracker;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.nolanlawson.apptracker.db.AppHistoryDbHelper;
import com.nolanlawson.apptracker.db.AppHistoryEntry;
import com.nolanlawson.apptracker.db.SortType;
import com.nolanlawson.apptracker.helper.PackageInfoHelper;
import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.helper.ResourceIdHelper;
import com.nolanlawson.apptracker.helper.SubtextHelper;
import com.nolanlawson.apptracker.util.DrawableUtil;
import com.nolanlawson.apptracker.util.Pair;
import com.nolanlawson.apptracker.util.UtilLogger;

public class WidgetUpdater {
	
	
	public static final int APPS_PER_PAGE = 4;
	
	public static final String NEW_PAGE_NUMBER = "newPageNumber";
	private static final String URI_SCHEME = "app_tracker_widget";
	
	
	private static UtilLogger log = new UtilLogger(WidgetUpdater.class);
	
	/**
	 * update only the app widgets associated with the given id
	 * @param context
	 * @param dbHelper
	 * @param appWidgetId
	 */
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		log.d("updating widget for appWidgetId: " + appWidgetId);
		
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
		if (updateViews != null) {
			manager.updateAppWidget(appWidgetId, updateViews);
		}
	}
	
	/**
	 * Update all app tracker app widgets
	 * @param context
	 * @param dbHelper
	 */
	public static void updateWidget(Context context, AppHistoryDbHelper dbHelper) {

		log.d("updating widget for all app widget ids");
		
		ComponentName widget = new ComponentName(context, AppTrackerWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = manager.getAppWidgetIds(widget);
		for (int appWidgetId : appWidgetIds) {
			RemoteViews updateViews = buildUpdate(context, dbHelper, appWidgetId);
			if (updateViews == null) { // nothing to see yet
				continue;
			}
			manager.updateAppWidget(appWidgetId, updateViews);
		}
	}
	
	
	private static RemoteViews buildUpdate(Context context, AppHistoryDbHelper dbHelper, int appWidgetId) {
		
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tracker_widget);
		
		
		int pageNumber = PreferenceHelper.getCurrentPageNumber(context, appWidgetId);
		String sortTypeAsString = PreferenceHelper.getSortTypePreference(context, appWidgetId);
		SortType sortType = SortType.findByName(context, sortTypeAsString);
		
		PackageManager packageManager = context.getPackageManager();
		
		List<Pair<AppHistoryEntry,PackageInfo>> packageInfos = PackageInfoHelper.getPackageInfos(
				context, dbHelper, packageManager, pageNumber, APPS_PER_PAGE, sortType);
		
		if (packageInfos.isEmpty()) {
			// nothing to show for now; just return
			return null;
		}
		
		List<CharSequence> labels = new ArrayList<CharSequence>();
		
		for (int i = 0; i < APPS_PER_PAGE; i++) {
			
			if (i < packageInfos.size()) {
			
				Pair<AppHistoryEntry,PackageInfo> pair = packageInfos.get(i);
				AppHistoryEntry appHistoryEntry = pair.getFirst();
				PackageInfo packageInfo = pair.getSecond();
				ComponentName componentName = appHistoryEntry.toComponentName();
				
				CharSequence label = packageInfo.applicationInfo.loadLabel(packageManager);
				
				Drawable iconDrawable = packageInfo.applicationInfo.loadIcon(packageManager);
				Bitmap iconBitmap = DrawableUtil.convertIconToBitmap(context, iconDrawable);
				
				
				
				String subtextText = SubtextHelper.createSubtext(context, sortType, appHistoryEntry);
				
				updateViews.setTextViewText(ResourceIdHelper.getAppTitleId(i), label);
				updateViews.setTextViewText(ResourceIdHelper.getAppDescriptionId(i), subtextText);
				updateViews.setImageViewBitmap(ResourceIdHelper.getAppIconId(i), iconBitmap);
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.VISIBLE);
				
				Intent intent = appHistoryEntry.toIntent();

                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                updateViews.setOnClickPendingIntent(ResourceIdHelper.getRelativeLayoutId(i), pendingIntent);
                
                labels.add(label);
                
			} else {
				// no entry; just hide the icon and text
				updateViews.setViewVisibility(ResourceIdHelper.getRelativeLayoutId(i), View.INVISIBLE);
			}
		}
		
		log.d("Labels are: %s", labels);
		
		setAppTitleVisibility(context, appWidgetId, updateViews);
		setSubtextVisibility(context, appWidgetId, updateViews);
		setBackgroundVisibility(context, appWidgetId, updateViews);
		
		
		setBackAndForwardButtons(context, appWidgetId, updateViews, pageNumber, dbHelper, sortType);
		return updateViews;
		
	}
	private static void setBackgroundVisibility(Context context,
			int appWidgetId, RemoteViews updateViews) {
		
		boolean showBackground = PreferenceHelper.getShowBackgroundPreference(context, appWidgetId);

		updateViews.setViewVisibility(R.id.widget_background, showBackground ? View.VISIBLE : View.INVISIBLE);

		
	}

	private static void setBackAndForwardButtons(Context context,
			int appWidgetId, RemoteViews updateViews, int pageNumber, AppHistoryDbHelper dbHelper,
			SortType sortType) {
		
		
		boolean lockPage = PreferenceHelper.getLockPagePreference(context, appWidgetId);
		
		if (lockPage) {
		
			int goneOrInvisible = chooseGoneOrInvisible(context, appWidgetId);
			updateViews.setViewVisibility(R.id.back_button, goneOrInvisible);
			updateViews.setViewVisibility(R.id.forward_button, goneOrInvisible);
		} else {
		
			// if no more app results, disable forward button
			// TODO: optimize this
			boolean noMoreAppResults = dbHelper.findInstalledAppHistoryEntries(sortType, APPS_PER_PAGE, (pageNumber + 1) * APPS_PER_PAGE).isEmpty();
	
			updateViews.setViewVisibility(R.id.forward_button, noMoreAppResults ? View.INVISIBLE : View.VISIBLE);
	
			log.d("forward button page number: %d", pageNumber + 1);
			updateViews.setOnClickPendingIntent(R.id.forward_button, 
					getPendingIntentForForwardOrBackButton(context, true, pageNumber + 1, appWidgetId));
			
			// if no previous app results, disable back button
			updateViews.setViewVisibility(R.id.back_button, pageNumber == 0 ? View.INVISIBLE : View.VISIBLE);
			
			log.d("back button page number: %d", pageNumber - 1);
			updateViews.setOnClickPendingIntent(R.id.back_button, 
					getPendingIntentForForwardOrBackButton(context, false, pageNumber - 1, appWidgetId));
		}
		
	}

	private static void setSubtextVisibility(Context context,
			int appWidgetId, RemoteViews updateViews) {
		
		boolean hideSubtext = PreferenceHelper.getHideSubtextPreference(context, appWidgetId);
		
		int subTextVisibility = hideSubtext ? chooseGoneOrInvisible(context, appWidgetId) : View.VISIBLE;
		
		updateViews.setViewVisibility(R.id.app_description_1, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_2, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_3, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_description_4, subTextVisibility);
		
	}

	private static void setAppTitleVisibility(Context context, int appWidgetId,
			RemoteViews updateViews) {
		boolean hideAppTitle = PreferenceHelper.getHideAppTitlePreference(context, appWidgetId);
		
		int subTextVisibility = hideAppTitle ? chooseGoneOrInvisible(context, appWidgetId) : View.VISIBLE;
		
		updateViews.setViewVisibility(R.id.app_title_1, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_2, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_3, subTextVisibility);
		updateViews.setViewVisibility(R.id.app_title_4, subTextVisibility);
		
	}

	
	private static PendingIntent getPendingIntentForForwardOrBackButton(
			Context context, boolean forward, int newPageNumber, int appWidgetId) {
		
		
		Intent intent = new Intent();
		intent.setAction(forward ? AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_FORWARD
				: AppTrackerWidgetProvider.ACTION_UPDATE_PAGE_BACK);

		intent.putExtra(NEW_PAGE_NUMBER, newPageNumber);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// gotta make this unique for this appwidgetid - otherwise, the PendingIntents conflict
		// it seems to be a quasi-bug in Android
		Uri data = Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://widget/id/#"+forward + newPageNumber), String.valueOf(appWidgetId));
        intent.setData(data);
        
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0 /* no requestCode */, intent, PendingIntent.FLAG_ONE_SHOT);
		
		return pendingIntent;
		
	}
	
	private static int chooseGoneOrInvisible(Context context, int appWidgetId) {
		return PreferenceHelper.getStretchToFillPreference(context, appWidgetId) ? View.GONE : View.INVISIBLE;
	}

}
