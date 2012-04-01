package com.nolanlawson.apptracker.db;

import java.util.Date;

public class AppHistoryEntrySummary {

	private int id;
	private boolean installed;
	private boolean excluded;
	private int count;
	private Date lastAccessed;
	private double decayScore;
	private long lastUpdate;
	private Date installDate;
	private Date updateDate;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
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
	public static AppHistoryEntrySummary newAppHistoryEntrySummary(int id, boolean installed, boolean excluded,
			int count, Date lastAccessed, double decayScore, long lastUpdate, Date installDate, Date updateDate) {
		
		AppHistoryEntrySummary result = new AppHistoryEntrySummary();
		result.id = id;
		result.installed = installed;
		result.excluded = excluded;
		result.count = count;
		result.lastAccessed = lastAccessed;
		result.decayScore = decayScore;
		result.lastUpdate = lastUpdate;
		result.installDate = installDate;
		result.updateDate = updateDate;
		
		return result;
	}	
	
	
}
