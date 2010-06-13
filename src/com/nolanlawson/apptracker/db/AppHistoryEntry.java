package com.nolanlawson.apptracker.db;

import java.util.Date;

public class AppHistoryEntry {

	private int id;
	private String packageName;
	private String process;
	private int count;
	private Date lastAccessed;
	
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
	
	public static AppHistoryEntry newAppHistoryEntry(
			int id, String packageName, String process, int count, Date lastAccessed) {
		
		AppHistoryEntry appHistoryEntry = new AppHistoryEntry();
		appHistoryEntry.setId(id);
		appHistoryEntry.setPackageName(packageName);
		appHistoryEntry.setProcess(process);
		appHistoryEntry.setCount(count);
		appHistoryEntry.setLastAccessed(lastAccessed);
		
		return appHistoryEntry;
		
		
	}
	@Override
	public String toString() {
		return "AppHistoryEntry [count=" + count + ", id=" + id
				+ ", lastAccessed=" + lastAccessed + ", packageName="
				+ packageName + ", process=" + process + "]";
	}
	
	
	
}
