package com.nolanlawson.apptracker.data;

public class AnalyzedLogLine {
	private boolean isStartHomeActivity;
	private String packageName;
	private String processName;
	
	public AnalyzedLogLine(boolean isStartHomeActivity,
			String packageName, String processName) {
		this.isStartHomeActivity = isStartHomeActivity;
		this.packageName = packageName;
		this.processName = processName;
	}
	
	public boolean isStartHomeActivity() {
		return isStartHomeActivity;
	}
	public String getPackageName() {
		return packageName;
	}
	public String getProcessName() {
		return processName;
	}
}
