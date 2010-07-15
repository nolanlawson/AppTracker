package com.nolanlawson.apptracker.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.nolanlawson.apptracker.helper.PreferenceHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

public class AppHistoryDbHelper extends SQLiteOpenHelper {
	
	//logger
	private static UtilLogger log = new UtilLogger(AppHistoryDbHelper.class);
	
	// schema constants
	
	private static final String DB_NAME = "app_history.db";
	private static final int DB_VERSION = 1;
	
	// table constants
	private static final String TABLE_NAME = "AppHistoryEntries";
	
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_PACKAGE = "package";
	private static final String COLUMN_PROCESS = "process";
	private static final String COLUMN_INSTALLED = "installed";
	private static final String COLUMN_EXCLUDED = "excluded";
	private static final String COLUMN_COUNT = "count";
	private static final String COLUMN_LAST_ACCESS = "lastAccess";
	private static final String COLUMN_DECAY_SCORE = "decayScore";
	private static final String COLUMN_LAST_UPDATE = "lastUpdate";
	
	private static final String[] COLUMNS = 
			{COLUMN_ID, COLUMN_PACKAGE, COLUMN_PROCESS, COLUMN_INSTALLED, COLUMN_EXCLUDED, 
			 COLUMN_COUNT, COLUMN_LAST_ACCESS, COLUMN_DECAY_SCORE, COLUMN_LAST_UPDATE};
	
	private Context context;
	
	// constructors
	public AppHistoryDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}
	// overrides
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String sql = "create table if not exists " + TABLE_NAME
		+ " (" +
		COLUMN_ID + " integer not null primary key autoincrement, " +
		COLUMN_PACKAGE + " text not null, " +
		COLUMN_PROCESS + " text not null, " +
		COLUMN_INSTALLED + " int not null, " +
		COLUMN_EXCLUDED +" int not null, " + 
		COLUMN_COUNT + " int not null, " +
		COLUMN_LAST_ACCESS + " int not null, " +
		COLUMN_DECAY_SCORE + " double not null, " +
		COLUMN_LAST_UPDATE + " int not null" +
		");";
		
		db.execSQL(sql);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


	}
	
	public void deleteAll() {
		
		getWritableDatabase().execSQL("delete from " + TABLE_NAME);
	}

	// methods
	
	/**
	 * Count number of installed and non-excluded apps
	 */
	public int findCountOfInstalledAppHistoryEntries() {
		
		String whereClause = createObligatoryWhereClause(false);
		
		Cursor cursor = getWritableDatabase().query(TABLE_NAME, new String[]{"count(*)"}, whereClause, 
				null, null, null, null);
		
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		
		return result;
		
		
	}
	
	public List<AppHistoryEntry> findInstalledAppHistoryEntries(SortType sortType, int limit, int offset,
			boolean showExcludedApps) {
		
		String orderByClause = createOrderByClause(sortType);
		String whereClause = createObligatoryWhereClause(showExcludedApps);
		
		String sql = "select " + TextUtils.join(",", COLUMNS)
				+ " from " + TABLE_NAME
				+ " where " + whereClause
				+ orderByClause
				+ " limit " + limit + " offset " + offset;
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	
	public int findCountOfInstalledAppHistoryEntries(SortType sortType, int limit, int offset,
			boolean showExcludedApps) {
		
		String orderByClause = createOrderByClause(sortType);
		String whereClause = createObligatoryWhereClause(showExcludedApps);
		
		String sql = "select " + COLUMN_ID
				+ " from " + TABLE_NAME
				+ " where " + whereClause
				+ orderByClause
				+ " limit " + limit + " offset " + offset;
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		int result = cursor.getCount();
		
		cursor.close();
		
		return result;
		
	}

	/**
	 * Go through each decay score and reduce them by a small amount given the current time
	 * and the last time we updated
	 */
	public void updateAllDecayScores() {
		
		List<AppHistoryEntry> appHistoryEntries = findAllAppHistoryEntries();
		
		log.d("Updating all decay scores for %d entries", appHistoryEntries.size());
		
		long currentTime = System.currentTimeMillis();
		
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			for (AppHistoryEntry appHistoryEntry : appHistoryEntries) {
				updateDecayScore(db, appHistoryEntry, currentTime);
				
			}
			db.setTransactionSuccessful();
		} catch (Exception ex) {
			log.e(ex, "Unexpected exception; unable to update all decay scores");
		} finally {
			db.endTransaction();
		}

	}

	/**
	 * Increment the count of the specified package and process
	 * and update its timestamp to be the most recent, or insert if it
	 * doesn't exist
	 */
	public void incrementAndUpdate(String packageName, String process) {
		
		long currentTime = System.currentTimeMillis();
		
		AppHistoryEntry existingEntry = findByPackageAndProcess(packageName, process);
		
		if (existingEntry == null) {
			// create new
			log.d("inserting new app history: %s, %s", packageName, process);
			insertNewAppHistoryEntry(packageName, process, currentTime);
			return;
		}
		
		log.d("updating/incrementing app history: %s, %s", packageName, process);
		
		String sql = "update %s "
			+ " set %s = %s + 1, " // count
			+ "%s = %d, " // timestamp
			+ "%s = %s + 1, "// decay score
			+ "%s = 1 " // installed, just in case the app was re-installed
			+ " where %s = ? "
			+ " and %s = ?";
		
		sql = String.format(sql, TABLE_NAME, 
				COLUMN_COUNT, COLUMN_COUNT, 
				COLUMN_LAST_ACCESS, currentTime,
				COLUMN_DECAY_SCORE, COLUMN_DECAY_SCORE,	
				COLUMN_INSTALLED,
				COLUMN_PACKAGE, COLUMN_PROCESS);
		
		
		String[] bindArgs = {packageName,process};
		
		getWritableDatabase().execSQL(sql, bindArgs);
	
	}
		
	public AppHistoryEntry findByPackageAndProcess(String packageName, String process) {
		
		ContentValues contentValues = new ContentValues();
		
		String selection = COLUMN_PACKAGE + "=? and " + COLUMN_PROCESS+"=?";
		
		contentValues.put(COLUMN_PACKAGE, packageName);
		contentValues.put(COLUMN_PROCESS, process);
		
		String[] bindArgs = {packageName, process};
		
		Cursor cursor = getWritableDatabase().query(TABLE_NAME, COLUMNS, selection, bindArgs, null, null, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result.isEmpty() ? null : result.get(0);
		
		
	}

	public void setInstalled(int id, boolean bool) {
		
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(COLUMN_INSTALLED, bool);
		
		String whereClause = COLUMN_ID + "=" + id;
		
		getWritableDatabase().update(TABLE_NAME, contentValues, whereClause, null);
		
	}
	
	public void setExcluded(int id, boolean bool) {
		
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(COLUMN_EXCLUDED, bool);
		
		String whereClause = COLUMN_ID + "=" + id;
		
		getWritableDatabase().update(TABLE_NAME, contentValues, whereClause, null);
		
	}
	
	private String createObligatoryWhereClause(boolean showExcludedApps) {
		
		StringBuilder stringBuilder = new StringBuilder(" ");
		
		stringBuilder.append(COLUMN_INSTALLED).append(" = 1 ");
		
		if (!showExcludedApps) {
			stringBuilder.append(" and ").append(COLUMN_EXCLUDED).append(" = 0 ");
		}
		
		return stringBuilder.toString();
	}
	
	private List<AppHistoryEntry> findAllAppHistoryEntries() {
		
		Cursor cursor = getWritableDatabase().query(TABLE_NAME, COLUMNS, null, null, null, null, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	private String createOrderByClause(SortType sortType) {
		StringBuilder stringBuilder = new StringBuilder(" order by ");
		switch (sortType) {
		case Recent:
			stringBuilder.append(COLUMN_LAST_ACCESS).append(" desc ");
			break;
		case MostUsed:
			stringBuilder.append(COLUMN_COUNT).append(" desc ");
			break;
		case TimeDecay:
			stringBuilder.append(COLUMN_DECAY_SCORE).append(" desc ");
			break;
		}
		return stringBuilder.toString();
	}
	
	private List<AppHistoryEntry> fromCursor(Cursor cursor) {
		
		List<AppHistoryEntry> result = new ArrayList<AppHistoryEntry>();
		
		while (cursor.moveToNext()) {
			AppHistoryEntry appHistoryEntry = AppHistoryEntry.newAppHistoryEntry(
					cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) == 1,
					cursor.getInt(4) == 1, cursor.getInt(5), new Date(cursor.getLong(6)), cursor.getDouble(7),
					cursor.getLong(8));
			result.add(appHistoryEntry);
		}
		
		return result;
	}

	private void updateDecayScore(SQLiteDatabase db, AppHistoryEntry appHistoryEntry, long currentTime) {
		// existing entry; update decay score
		long lastUpdate = appHistoryEntry.getLastUpdate();
		double lastScore = appHistoryEntry.getDecayScore();
		
		int decayConstantInDays = PreferenceHelper.getDecayConstantPreference(context);
		long decayConstantInMillis = TimeUnit.SECONDS.toMillis(60 * 60 * 24 * decayConstantInDays);
		
		double newDecayScore = (lastScore * Math.exp((1.0 * currentTime - lastUpdate) / -decayConstantInMillis));
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_DECAY_SCORE, newDecayScore);
		contentValues.put(COLUMN_LAST_UPDATE, currentTime);
		
		String whereClause = COLUMN_ID + "=" + appHistoryEntry.getId();
		
		log.d("updating decay score for appHistoryEntryId: %d; oldScore is: %g, newScore is: %g", 
				appHistoryEntry.getId(), lastScore, newDecayScore);
		
		if (newDecayScore < lastScore) {
			db.update(TABLE_NAME, contentValues, whereClause, null);
		} else {
			log.d("old score is lower than new score; not updating");
		}
		
	}

	private void insertNewAppHistoryEntry(String packageName, String process, long currentTime) {
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_PACKAGE, packageName);
		contentValues.put(COLUMN_PROCESS, process);
		contentValues.put(COLUMN_INSTALLED, 1);
		contentValues.put(COLUMN_EXCLUDED, 0);
		contentValues.put(COLUMN_COUNT, 1);
		contentValues.put(COLUMN_LAST_ACCESS, currentTime);
		contentValues.put(COLUMN_DECAY_SCORE, 1);
		contentValues.put(COLUMN_LAST_UPDATE, currentTime);
		

		
		getWritableDatabase().insert(TABLE_NAME, null, contentValues);
	}



}
