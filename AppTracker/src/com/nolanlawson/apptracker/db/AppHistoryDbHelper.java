package com.nolanlawson.apptracker.db;

import java.util.ArrayList;
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
	private static final int DB_VERSION = 3;
	
	// table constants
	public static final String APP_HISTORY_TABLE_NAME = "AppHistoryEntries";
	private static final String INSTALL_INFO_TABLE_NAME = "PackageInstallInfos";
	
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_PACKAGE = "package";
	private static final String COLUMN_PROCESS = "process";
	private static final String COLUMN_INSTALLED = "installed";
	private static final String COLUMN_EXCLUDED = "excluded";
	private static final String COLUMN_COUNT = "count";
	private static final String COLUMN_LAST_ACCESS = "lastAccess";
	private static final String COLUMN_DECAY_SCORE = "decayScore";
	private static final String COLUMN_LAST_UPDATE = "lastUpdate";
	private static final String COLUMN_LABEL = "label";
	private static final String COLUMN_ICON_BLOB = "iconBlob";
	
	// columns on the second table only
	private static final String COLUMN_INSTALL_DATE = "installDate";
	private static final String COLUMN_UPDATE_DATE = "updateDate";
	
	private static final String[] COLUMNS = 
			{"t1." + COLUMN_ID, "t1." + COLUMN_PACKAGE, COLUMN_PROCESS, COLUMN_INSTALLED, COLUMN_EXCLUDED, 
			 COLUMN_COUNT, COLUMN_LAST_ACCESS, COLUMN_DECAY_SCORE, COLUMN_LAST_UPDATE,
			 COLUMN_LABEL, COLUMN_ICON_BLOB, COLUMN_INSTALL_DATE, COLUMN_UPDATE_DATE};
	
	private static final String[] SUMMARY_COLUMNS =
		{"t1." + COLUMN_ID, COLUMN_INSTALLED, COLUMN_EXCLUDED, 
		 COLUMN_COUNT, COLUMN_LAST_ACCESS, COLUMN_DECAY_SCORE, COLUMN_LAST_UPDATE, 
		 COLUMN_INSTALL_DATE, COLUMN_UPDATE_DATE};
	
	private Context context;
	
	// constructors
	public AppHistoryDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}
	// overrides
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String sql = "create table if not exists " + APP_HISTORY_TABLE_NAME
		+ " (" +
		COLUMN_ID + " integer not null primary key autoincrement, " +
		COLUMN_PACKAGE + " text not null, " +
		COLUMN_PROCESS + " text not null, " +
		COLUMN_INSTALLED + " int not null, " +
		COLUMN_EXCLUDED +" int not null, " + 
		COLUMN_COUNT + " int not null, " +
		COLUMN_LAST_ACCESS + " int not null, " +
		COLUMN_DECAY_SCORE + " double not null, " +
		COLUMN_LAST_UPDATE + " int not null, " +
		COLUMN_LABEL + " text, " +
		COLUMN_ICON_BLOB + " blob " +
		");";
		
		db.execSQL(sql);
		createVersionThreeIndices(db);
		createInstallInfoTable(db);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if (oldVersion == 1) {
			db.execSQL("alter table " + APP_HISTORY_TABLE_NAME + 
					" add column " + COLUMN_LABEL + " text ;");
			db.execSQL("alter table " + APP_HISTORY_TABLE_NAME + 
					" add column " + COLUMN_ICON_BLOB + " blob ;");
		}
		
		if (oldVersion <= 2) {
			createVersionThreeIndices(db);
			createInstallInfoTable(db);
		}
	}
	
	private void createVersionThreeIndices(SQLiteDatabase db) {
		
		db.execSQL(String.format(
				"CREATE INDEX IF NOT EXISTS index_package_process on %s (%s,%s);",
				APP_HISTORY_TABLE_NAME,
				COLUMN_PACKAGE,
				COLUMN_PROCESS));
		
		db.execSQL(String.format(
				"CREATE INDEX IF NOT EXISTS index_installed_excluded on %s (%s,%s);",
				APP_HISTORY_TABLE_NAME,
				COLUMN_INSTALLED,
				COLUMN_EXCLUDED));
		
		db.execSQL(String.format(
				"CREATE INDEX IF NOT EXISTS index_decayscore on %s (%s);",
				APP_HISTORY_TABLE_NAME,
				COLUMN_DECAY_SCORE));		
	}
	
	private void createInstallInfoTable(SQLiteDatabase db) {
		String sql = "create table if not exists " + INSTALL_INFO_TABLE_NAME
		+ " (" +
		COLUMN_ID + " integer not null primary key autoincrement, " +
		COLUMN_PACKAGE + " text not null, " +
		COLUMN_INSTALL_DATE + " int, " +
		COLUMN_UPDATE_DATE + " int " +
		");";
		
		db.execSQL(sql);
		
		db.execSQL(String.format(
				"CREATE INDEX IF NOT EXISTS index_install_info_package on %s (%s);",
				INSTALL_INFO_TABLE_NAME,
				COLUMN_PACKAGE));
		
	}
	
	public void deleteAll() {
		
		getWritableDatabase().execSQL("delete from " + APP_HISTORY_TABLE_NAME);
		getWritableDatabase().execSQL("delete from " + INSTALL_INFO_TABLE_NAME);
	}

	// methods
	
	/**
	 * Count number of installed and non-excluded apps
	 */
	public int findCountOfInstalledAppHistoryEntries() {
		
		String whereClause = createObligatoryWhereClause(false);
		
		Cursor cursor = getWritableDatabase().query(APP_HISTORY_TABLE_NAME, new String[]{"count(*)"}, whereClause, 
				null, null, null, null);
		
		cursor.moveToFirst();
		int result = cursor.getInt(0);
		cursor.close();
		
		return result;
		
		
	}
	public List<AppHistoryEntry> findInstalledAppHistoryEntriesWithNullLabels() {
		
		String whereClause = createObligatoryWhereClause(true);
		
		String sql = "select " + TextUtils.join(",", COLUMNS)
				+ " from " 
				+ joinedTables()
				+ " where " + whereClause
				+ " and " + COLUMN_LABEL + " is null ";
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	
	public List<AppHistoryEntry> findInstalledAppHistoryEntries(SortType sortType, int limit, int offset,
			boolean showExcludedApps) {
		
		String orderByClause = createOrderByClause(sortType);
		String whereClause = createObligatoryWhereClause(showExcludedApps);
		
		String sql = "select " + TextUtils.join(",", COLUMNS)
				+ " from " 
				+ joinedTables()
				+ " where " + whereClause
				+ orderByClause
				+ " limit " + limit + " offset " + offset;
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	
	private String joinedTables() {
		return APP_HISTORY_TABLE_NAME +" t1 left join " + INSTALL_INFO_TABLE_NAME + " t2 " 
		+ " on t1." + COLUMN_PACKAGE + " = t2." + COLUMN_PACKAGE;
	}

	public int findCountOfInstalledAppHistoryEntries(SortType sortType, int limit, int offset,
			boolean showExcludedApps) {
		
		String orderByClause = createOrderByClause(sortType);
		String whereClause = createObligatoryWhereClause(showExcludedApps);
		
		String sql = "select t1." + COLUMN_ID
				+ " from " 
				+ joinedTables()
				+ " where " + whereClause
				+ orderByClause
				+ " limit " + limit + " offset " + offset;
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		int result = cursor.getCount();
		
		cursor.close();
		
		return result;
		
	}

	/**
	 * Adds a package/process combo with no count and a last used date of 0 if it doesn't exist
	 * If it does exist, updates the entry to show that it's been reinstalled.
	 * @param packageName
	 * @param process
	 */
	public void addEmptyPackageAndProcessIfNotExists(String packageName, String process) {
		
		AppHistoryEntry existingEntry = findByPackageAndProcess(packageName, process);
		
		if (existingEntry == null) {
			insertNewAppHistoryEntry(packageName, process, System.currentTimeMillis(), true);
		} else {
			setInstalled(existingEntry.getId(), true);
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
			insertNewAppHistoryEntry(packageName, process, currentTime, false);
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
		
		sql = String.format(sql, APP_HISTORY_TABLE_NAME, 
				COLUMN_COUNT, COLUMN_COUNT, 
				COLUMN_LAST_ACCESS, currentTime,
				COLUMN_DECAY_SCORE, COLUMN_DECAY_SCORE,	
				COLUMN_INSTALLED,
				COLUMN_PACKAGE, COLUMN_PROCESS);
		
		
		String[] bindArgs = {packageName,process};
		
		getWritableDatabase().execSQL(sql, bindArgs);
	
	}
		
	public AppHistoryEntry findByPackageAndProcess(String packageName, String process) {
		
		String selection = "t1." + COLUMN_PACKAGE + "=? and " + COLUMN_PROCESS+"=?";
		
		String[] bindArgs = {packageName, process};
		
		Cursor cursor = getWritableDatabase().query(joinedTables(), COLUMNS, selection, bindArgs, null, null, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result.isEmpty() ? null : result.get(0);
		
		
	}

	public void setInstalled(int id, boolean bool) {
		
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(COLUMN_INSTALLED, bool);
		
		String whereClause = COLUMN_ID + "=" + id;
		
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, null);
		
	}
	
	public void setExcluded(int id, boolean bool) {
		
		ContentValues contentValues = new ContentValues();
		
		contentValues.put(COLUMN_EXCLUDED, bool);
		
		String whereClause = COLUMN_ID + "=" + id;
		
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, null);
		
	}
	
	private String createObligatoryWhereClause(boolean showExcludedApps) {
		
		StringBuilder stringBuilder = new StringBuilder(" ");
		
		stringBuilder.append(COLUMN_INSTALLED).append(" = 1 ");
		
		if (!showExcludedApps) {
			stringBuilder.append(" and ").append(COLUMN_EXCLUDED).append(" = 0 ");
		}
		
		return stringBuilder.toString();
	}
	
	public List<AppHistoryEntry> findAllAppHistoryEntries() {
		
		Cursor cursor = getWritableDatabase().query(APP_HISTORY_TABLE_NAME, COLUMNS, null, null, null, null, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	
	public List<AppHistoryEntrySummary> findAllAppHistoryEntrySummariesWithDecayScoreGreaterThan(
			double greaterThan) {
		
		String whereClause = COLUMN_DECAY_SCORE + " > " + greaterThan;
		
		Cursor cursor = getWritableDatabase().query(joinedTables(), SUMMARY_COLUMNS, whereClause, null, null, null, null);
		
		List<AppHistoryEntrySummary> result = fromCursorToSummaries(cursor);
		
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
		case LeastUsed:
			stringBuilder.append(COLUMN_COUNT).append(" ");
			break;
		case RecentlyInstalled:
			stringBuilder.append(COLUMN_INSTALL_DATE).append(" desc ");
			break;
		case RecentlyUpdated:
			stringBuilder.append(COLUMN_UPDATE_DATE).append(" desc ");
			break;
		case Alphabetic:
			stringBuilder.append(COLUMN_LABEL).append(" ");
			break;
			
		}
		return stringBuilder.toString();
	}
	
	private List<AppHistoryEntry> fromCursor(Cursor cursor) {
		
		List<AppHistoryEntry> result = new ArrayList<AppHistoryEntry>(cursor.getCount());
		
		while (cursor.moveToNext()) {
			AppHistoryEntry appHistoryEntry = AppHistoryEntry.newAppHistoryEntry(
					cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3) == 1,
					cursor.getInt(4) == 1, cursor.getInt(5), new Date(cursor.getLong(6)), cursor.getDouble(7),
					cursor.getLong(8), cursor.getString(9), cursor.getBlob(10), 
					cursor.getString(11) == null ? null : new Date(cursor.getLong(11)),
					cursor.getString(12) == null ? null : new Date(cursor.getLong(12))		
							);
			result.add(appHistoryEntry);
		}
		
		return result;
	}

	private List<AppHistoryEntrySummary> fromCursorToSummaries(Cursor cursor) {
		
		List<AppHistoryEntrySummary> result = new ArrayList<AppHistoryEntrySummary>(cursor.getCount());
		
		while (cursor.moveToNext()) {
			
			AppHistoryEntrySummary appHistoryEntrySummary = AppHistoryEntrySummary.newAppHistoryEntrySummary(
					cursor.getInt(0), cursor.getInt(1) == 1,
					cursor.getInt(2) == 1, cursor.getInt(3), 
					new Date(cursor.getLong(4)), cursor.getDouble(5),
					cursor.getLong(6),
					cursor.getString(7) == null ? null : new Date(cursor.getLong(7)),
					cursor.getString(8) == null ? null : new Date(cursor.getLong(8))		
							);
			result.add(appHistoryEntrySummary);
		}
		
		return result;
	}


	
	
	public void updateInstallDate(String packageName, long timestamp) {
		
		boolean databaseUpdated = updatePackageRelatedDate(packageName, COLUMN_INSTALL_DATE, timestamp, false);
		
		if (!databaseUpdated) {
			// consider this an update event rather than an install event
			updatePackageRelatedDate(packageName, COLUMN_UPDATE_DATE, timestamp, true);
		}
		
	}
	
	public void updateUpdateDate(String packageName, long timestamp) {
		
		updatePackageRelatedDate(packageName, COLUMN_UPDATE_DATE, timestamp, true);
		
	}
	
	/**
	 * return true if something in the database was changed
	 * @param packageName
	 * @param column
	 * @param timestamp
	 * @param installEvent
	 * @return
	 */
	private boolean updatePackageRelatedDate(String packageName, String column, long timestamp, boolean installEvent) {
		
		Date oldInstallDate = findDateByPackageName(packageName, column);
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(column, timestamp);
		
		if (oldInstallDate == null) { 
			// not added to database yet
			contentValues.put(COLUMN_PACKAGE, packageName);
			getWritableDatabase().insert(INSTALL_INFO_TABLE_NAME, null, contentValues);

			
			return true;
		} else if (installEvent) {
			
			// the Android market uninstalls and then reinstalls apps, so to detect that
			// we have to be careful not to overwrite as "new install" when it's really an update
			// of an existing app that we've already seen before
			
			String whereClause = COLUMN_PACKAGE + "=?";
			String[] whereArgs = new String[]{packageName};
			
			getWritableDatabase().update(INSTALL_INFO_TABLE_NAME, contentValues, whereClause, whereArgs);
			
			return true;
		} else {
			return false;
		}
	}
	
	private Date findDateByPackageName(String packageName, String column) {
		
		String[] selection = new String[]{column};
		String whereClause = COLUMN_PACKAGE + "=?";
		String[] whereArgs = new String[]{packageName};
		
		Cursor cursor = getWritableDatabase().query(INSTALL_INFO_TABLE_NAME, selection, whereClause, whereArgs,
				null, null, null);
		
		Date result = null;
		
		if (cursor.moveToFirst()) {
			result = new Date(cursor.getLong(0));
		}
		
		cursor.close();
		
		return result;
		
	}
	
	public void updateDecayScore(AppHistoryEntrySummary appHistoryEntry, long currentTime) {
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
			getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, null);
		} else {
			log.d("old score is lower than new score; not updating");
		}
		
	}
	
	public void setIconBlob(int appHistoryEntryId, byte[] iconBlob) {
		
		String whereClause = COLUMN_ID +"=" + appHistoryEntryId;
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_ICON_BLOB, iconBlob);
		
		
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, null);
	}
	
	public void setLabel(int appHistoryEntryId, String label) {
		
		String whereClause = COLUMN_ID +"=" + appHistoryEntryId;
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_LABEL, label);
		
		
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, null);
	}

	public void clearIconAndLabel(String packageName) {
		
		String whereClause = COLUMN_PACKAGE +"=?";
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_LABEL, (String)null);
		contentValues.put(COLUMN_ICON_BLOB, (byte[])null);
		
		String[] whereArgs = new String[] { packageName };
		
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, whereClause, whereArgs);		
	}
	
	public void clearAllIcons() {
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_ICON_BLOB, (byte[])null);
	
		getWritableDatabase().update(APP_HISTORY_TABLE_NAME, contentValues, null, null);
		
	}
	
	
	private void insertNewAppHistoryEntry(String packageName, String process, long currentTime, boolean empty) {
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_PACKAGE, packageName);
		contentValues.put(COLUMN_PROCESS, process);
		contentValues.put(COLUMN_INSTALLED, 1);
		contentValues.put(COLUMN_EXCLUDED, 0);
		contentValues.put(COLUMN_COUNT, empty ? 0 : 1);
		contentValues.put(COLUMN_LAST_ACCESS, empty ? 0 : currentTime);
		contentValues.put(COLUMN_DECAY_SCORE, empty ? 0.0 : 1);
		contentValues.put(COLUMN_LAST_UPDATE, currentTime);
				
		getWritableDatabase().insert(APP_HISTORY_TABLE_NAME, null, contentValues);
	}

	/**
	 * Have to do this when an app is uninstalled so we can detect that it's being RE-installed,
	 * because the stupid Android Market uninstalls apps and then installs them, so you can't
	 * detect a RE-install event.
	 * @param packageName
	 */
	public void addEmptyPackageStubIfNotExists(String packageName) {

		Date existingDate = findDateByPackageName(packageName, COLUMN_INSTALL_DATE);
		
		if (existingDate == null) {
			
			ContentValues contentValues = new ContentValues();
			
			contentValues.put(COLUMN_PACKAGE, packageName);
			
			getWritableDatabase().insert(INSTALL_INFO_TABLE_NAME, null, contentValues);
		
		}
		
	}



}
