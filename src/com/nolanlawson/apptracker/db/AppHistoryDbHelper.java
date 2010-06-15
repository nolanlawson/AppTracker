package com.nolanlawson.apptracker.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.nolanlawson.apptracker.util.UtilLogger;

public class AppHistoryDbHelper extends SQLiteOpenHelper {
	
	//logger
	private static UtilLogger log = new UtilLogger(AppHistoryDbHelper.class);
	
	//TODO: make this configurable
	private static final String[] appsToIgnore = {"com.android.launcher", // launcher
		                                          "com.android.launcher2", // launcher2
	                                              "com.nolanlawson.apptracker", // apptracker itself
	                                              "com.android.contacts", // contacts OR phone
	                                              "com.android.phone", // phone
	                                              "com.android.browser", // browser
	                                              "com.android.mms"}; // messaging
	
	// schema constants
	
	private static final String DB_NAME = "app_history.db";
	private static final int DB_VERSION = 1;
	
	// table constants
	private static final String TABLE_NAME = "AppHistoryEntries";
	
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_PACKAGE = "package";
	private static final String COLUMN_PROCESS = "process";
	private static final String COLUMN_COUNT = "count";
	private static final String COLUMN_LAST_ACCESS = "lastAccess";
	
	private static final String[] COLUMNS = 
			{COLUMN_ID, COLUMN_PACKAGE, COLUMN_PROCESS, COLUMN_COUNT, COLUMN_LAST_ACCESS};
	
	// constructors
	public AppHistoryDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	// methods
	
	public List<AppHistoryEntry> getMostRecentAppHistoryEntries(int limit, int offset) {
		
		String appsToIgnoreString = "(\"" + TextUtils.join("\",\"", appsToIgnore) + "\")";
		
		String sql = "select " + TextUtils.join(",", COLUMNS)
				+ " from " + TABLE_NAME
				+ " where " + COLUMN_PACKAGE +" not in " + appsToIgnoreString
				+ " order by " + COLUMN_LAST_ACCESS
				+ " desc limit " + limit + " offset " + offset;
		
		Cursor cursor = getWritableDatabase().rawQuery(sql, null);
		
		List<AppHistoryEntry> result = fromCursor(cursor);
		
		cursor.close();
		
		return result;
		
	}
	
	/**
	 * Increment the count of the specified package and process
	 * and update its timestamp to be the most recent, or insert if it
	 * doesn't exist
	 */
	public void incrementAndUpdate(String packageName, String process) {
		
		if (findByPackageAndProcess(packageName, process) == null) {
			// create new
			log.d("inserting new app history: %s, %s", packageName, process);
			insertNewAppHistoryEntry(packageName, process);
		}
		log.d("updating/incrementing app history: %s, %s", packageName, process);
		
		long currentTime = System.currentTimeMillis();
		
		String sql = "update " + TABLE_NAME
			+ " set " + COLUMN_COUNT + " = " + COLUMN_COUNT + "+1, "
			+ COLUMN_LAST_ACCESS + " = " + currentTime
			+ " where " + COLUMN_PACKAGE + "=? "
			+ " and " + COLUMN_PROCESS + "=?";
		String[] bindArgs = {packageName,process};
		
		getWritableDatabase().execSQL(sql, bindArgs);
	
	}
	
	public void insertNewAppHistoryEntry(String packageName, String process) {
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(COLUMN_PACKAGE, packageName);
		contentValues.put(COLUMN_PROCESS, process);
		contentValues.put(COLUMN_COUNT, 0);
		contentValues.put(COLUMN_LAST_ACCESS, 0);
		
		getWritableDatabase().insert(TABLE_NAME, null, contentValues);
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
	
	private List<AppHistoryEntry> fromCursor(Cursor cursor) {
		
		List<AppHistoryEntry> result = new ArrayList<AppHistoryEntry>();
		
		while (cursor.moveToNext()) {
			AppHistoryEntry appHistoryEntry = AppHistoryEntry.newAppHistoryEntry(
					cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), new Date(cursor.getLong(4)));
			result.add(appHistoryEntry);
		}
		
		return result;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String sql = "create table if not exists " + TABLE_NAME
		+ " (" +
		COLUMN_ID + " integer not null primary key autoincrement, " +
		COLUMN_PACKAGE + " text not null, " +
		COLUMN_PROCESS + " text not null, " +
		COLUMN_COUNT + " int not null, " +
		COLUMN_LAST_ACCESS + " int not null" +
		");";
		
		db.execSQL(sql);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


	}

}
