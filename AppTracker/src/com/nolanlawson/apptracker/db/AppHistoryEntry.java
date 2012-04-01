package com.nolanlawson.apptracker.db;

import java.util.Arrays;
import java.util.Date;

import android.content.ComponentName;
import android.content.Intent;

public class AppHistoryEntry {

	private int id;
	private String packageName;
	private String process;
	private boolean installed;
	private boolean excluded;
	private int count;
	private Date lastAccessed;
	private double decayScore;
	private long lastUpdate;
	private String label;
	private byte[] iconBlob;
	private Date installDate;
	private Date updateDate;
	
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
	
	
	public boolean isExcluded() {
		return excluded;
	}
	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
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
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public byte[] getIconBlob() {
		return iconBlob;
	}
	public void setIconBlob(byte[] iconBlob) {
		this.iconBlob = iconBlob;
	}
	
	public Date getInstallDate() {
		return installDate;
	}
	public void setInstallDate(Date installDate) {
		this.installDate = installDate;
	}
	
	
	public Date getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
	public static AppHistoryEntry newAppHistoryEntry(
			int id, String packageName, String process, boolean installed, boolean excluded, int count, 
			Date lastAccessed, double decayScore, long lastUpdate, String label, byte[] iconBlob,
			Date installDate, Date updateDate) {
		
		AppHistoryEntry appHistoryEntry = new AppHistoryEntry();
		appHistoryEntry.setId(id);
		appHistoryEntry.setPackageName(packageName);
		appHistoryEntry.setProcess(process);
		appHistoryEntry.setInstalled(installed);
		appHistoryEntry.setExcluded(excluded);
		appHistoryEntry.setCount(count);
		appHistoryEntry.setLastAccessed(lastAccessed);
		appHistoryEntry.setDecayScore(decayScore);
		appHistoryEntry.setLastUpdate(lastUpdate);
		appHistoryEntry.setLabel(label);
		appHistoryEntry.setIconBlob(iconBlob);
		appHistoryEntry.setInstallDate(installDate);
		appHistoryEntry.setUpdateDate(updateDate);
		
		return appHistoryEntry;
		
		
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AppHistoryEntry other = (AppHistoryEntry) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "AppHistoryEntry [count=" + count + ", decayScore=" + decayScore
				+ ", excluded=" + excluded + ", id=" + id + ", installed="
				+ installed + ", label=" + label + ", lastAccessed="
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
	
	public Intent toIntent() {
		
		Intent intent = new Intent();
		intent.setComponent(toComponentName());
		intent.setAction(Intent.ACTION_MAIN);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		return intent;
	}

	
}
