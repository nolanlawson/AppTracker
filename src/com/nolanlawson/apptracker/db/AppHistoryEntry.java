package com.nolanlawson.apptracker.db;

import java.util.Date;

import android.content.ComponentName;

public class AppHistoryEntry {

	private int id;
	private String packageName;
	private String process;
	private boolean installed;
	private int count;
	private Date lastAccessed;
	private double decayScore;
	private long lastUpdate;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getProcess() {
		return process;
	}
	public void setProcess(String process) {
		this.process = process;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public Date getLastAccessed() {
		return lastAccessed;
	}
	public void setLastAccessed(Date lastAccessed) {
		this.lastAccessed = lastAccessed;
	}
	
	public double getDecayScore() {
		return decayScore;
	}
	public void setDecayScore(double decayScore) {
		this.decayScore = decayScore;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	
	public boolean isInstalled() {
		return installed;
	}
	public void setInstalled(boolean installed) {
		this.installed = installed;
	}
	
	public static AppHistoryEntry newAppHistoryEntry(
			int id, String packageName, String process, boolean installed, int count, 
			Date lastAccessed, double decayScore, long lastUpdate) {
		
		AppHistoryEntry appHistoryEntry = new AppHistoryEntry();
		appHistoryEntry.setId(id);
		appHistoryEntry.setPackageName(packageName);
		appHistoryEntry.setProcess(process);
		appHistoryEntry.setInstalled(installed);
		appHistoryEntry.setCount(count);
		appHistoryEntry.setLastAccessed(lastAccessed);
		appHistoryEntry.setDecayScore(decayScore);
		appHistoryEntry.setLastUpdate(lastUpdate);
		
		return appHistoryEntry;
		
		
	}
	@Override
	public String toString() {
		return "AppHistoryEntry [count=" + count + ", decayScore=" + decayScore
				+ ", id=" + id + ", installed=" + installed + ", lastAccessed="
				+ lastAccessed + ", lastUpdate=" + lastUpdate
				+ ", packageName=" + packageName + ", process=" + process + "]";
	}
	
	public ComponentName toComponentName() {
		String fullProcessName;

		if (process.startsWith(".")) {
			// beginning period is the most common case - this means that the process's path is
			// simply appended to the package name
			fullProcessName = packageName + process;
		} else {
			// strange case where the full path is specified (e.g. the Maps app)
			fullProcessName = process;
		}
		return new ComponentName(packageName, fullProcessName);
	}

	
}
