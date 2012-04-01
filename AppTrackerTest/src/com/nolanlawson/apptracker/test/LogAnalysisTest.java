package com.nolanlawson.apptracker.test;

import android.test.ActivityInstrumentationTestCase2;

import com.nolanlawson.apptracker.AppTrackerActivity;
import com.nolanlawson.apptracker.data.AnalyzedLogLine;
import com.nolanlawson.apptracker.helper.LogAnalyzer;

public class LogAnalysisTest extends ActivityInstrumentationTestCase2<AppTrackerActivity> {

	public LogAnalysisTest() {
		super("com.nolanlawson.apptracker", AppTrackerActivity.class);
	}
	
	public void testCupcake() {
		testActivityLine("I/ActivityManager(  583): Starting activity: Intent { action=android.intent.action.MAIN flags=0x10000000 " +
				"comp={com.android.mms/com.android.mms.ui.ConversationList} }",
				"com.android.mms",
				"com.android.mms.ui.ConversationList");
	}
	
	public void testDonut() {
		testActivityLine("I/ActivityManager(   52): Starting activity: Intent { " +
				"act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 " +
				"cmp=com.android.browser/.BrowserActivity }", 
				"com.android.browser", 
				".BrowserActivity");
	}
	
	public void testEclair() {
		testActivityLine("I/ActivityManager(  118): Starting activity: " +
				"Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 " +
				"cmp=com.android.contacts/.DialtactsContactsEntryActivity bnds=[83,338][157,417] }",
				"com.android.contacts",
				".DialtactsContactsEntryActivity");
	}
	
	public void testGingerbread() {
		testActivityLine("I/ActivityManager(  175): Starting: Intent { act=android.intent.action.MAIN " +
				"cat=[android.intent.category.LAUNCHER] " +
				"flg=0x10200000 cmp=com.google.android.gm/.ConversationListActivityGmail } " +
				"from pid 1271",
				"com.google.android.gm",
				".ConversationListActivityGmail");
	}
	
	public void testHoneycomb() {
		testActivityLine("I/ActivityManager(   79): Starting: Intent { act=android.intent.action.MAIN " +
				"cat=[android.intent.category.LAUNCHER] flg=0x10200000 " +
				"cmp=com.android.email/.activity.Welcome } from pid 147", 
				"com.android.email", ".activity.Welcome");
	}
	
	public void testIcs() {
		testActivityLine("I/ActivityManager(   89): START {" +
				"act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] " +
				"flg=0x10200000 cmp=com.android.mms/.ui.ConversationList bnds=[288,706][384,802]} from pid 194",
				"com.android.mms",
				".ui.ConversationList");
	}
	
	public void testHomeLine() {
		AnalyzedLogLine line = LogAnalyzer.analyzeLogLine(
				"I/ActivityManager(  175): Starting: Intent { " +
				"act=android.intent.action.MAIN cat=[android.intent.category.HOME] " +
				"flg=0x10200000 cmp=com.android.launcher/.Launcher } from pid 175");
		assertTrue(line.isStartHomeActivity());
	}
	
	public void testInnocuousLine() {
		AnalyzedLogLine line = LogAnalyzer.analyzeLogLine("I/ActivityManager(  175): " +
				"No longer want com.google.android.apps.docs (pid 6038): hidden #16");
		assertNull(line);
	}
	

	public void testSamsungLogLine() {
		testActivityLine("03-14 17:33:34.830 W/ActivityManager(2690): " +
				"Trying to launch com.facelock4appspro/com.facedklib.FaceRecActivity",
				"com.facelock4appspro",
				"com.facedklib.FaceRecActivity");
	}

	private void testActivityLine(String line, String packageName, String processName) {
		AnalyzedLogLine logLine = LogAnalyzer.analyzeLogLine(line);
		assertEquals(packageName, logLine.getPackageName());
		assertEquals(processName, logLine.getProcessName());
		assertFalse(logLine.isStartHomeActivity());
	}

}
