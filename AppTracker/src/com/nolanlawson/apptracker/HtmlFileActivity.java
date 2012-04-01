package com.nolanlawson.apptracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nolanlawson.apptracker.helper.FreemiumHelper;
import com.nolanlawson.apptracker.util.UtilLogger;

public class HtmlFileActivity extends Activity implements OnClickListener {
	
	public static final String ACTION_ABOUT = "com.nolanlawson.apptracker.action.ABOUT";
	public static final String ACTION_USER_GUIDE = "com.nolanlawson.apptracker.action.USER_GUIDE";
	
	private static UtilLogger log = new UtilLogger(HtmlFileActivity.class);
	
	private Button okButton;
	private WebView aboutWebView;
	private ProgressBar progressBar;
	private ImageView iconImageView;
	private Handler handler = new Handler();
	
	private boolean aboutActivity = false;
	
	
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		
		Intent intent = getIntent();
		
		if (ACTION_ABOUT.equals(intent.getAction())) {
			aboutActivity = true;
			setTitle(R.string.about_app_tracker);
		} else {
			setTitle(R.string.user_guide);
		}
		
		setContentView(R.layout.html_file_view);
		

		iconImageView = (ImageView) findViewById(R.id.holmes_icon_image);
		
		
		
		okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(this);
				
		aboutWebView = (WebView) findViewById(R.id.aboutTextWebView);

		aboutWebView.setVisibility(View.GONE);
		iconImageView.setVisibility(View.GONE);
		
		progressBar = (ProgressBar) findViewById(R.id.aboutProgressBar);

		aboutWebView.setBackgroundColor(0);
		
		aboutWebView.setWebViewClient(new AboutWebClient());
		
		initializeWebView();				
	}
	
	
	public void initializeWebView() {
		
		StringBuilder htmlData = new StringBuilder();
		
		if (aboutActivity) {
		
			if (FreemiumHelper.isAppTrackerPremiumInstalled(getApplicationContext())) {
				htmlData.append(loadTextFile(R.raw.about_body_1_premium));
			} else {
				htmlData.append(loadTextFile(R.raw.about_body_1_free));
			}
			
			htmlData.append(loadTextFile(R.raw.about_body_2));
		
			
		} else { // user guide 
			
			htmlData = new StringBuilder(loadTextFile(R.raw.user_guide));
		}
					
		aboutWebView.loadData(htmlData.toString(), "text/html", "utf-8");
	}


	private String loadTextFile(int resourceId) {
		
		InputStream is = getResources().openRawResource(resourceId);
		
		BufferedReader buff = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		
		try {
			while (buff.ready()) {
				sb.append(buff.readLine()).append("\n");
			}
		} catch (IOException e) {
			log.e(e, "This error should not happen");
		}
		
		return sb.toString();
		
	}
	
	private void loadExternalUrl(String url) {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW"); 
		intent.setData(Uri.parse(url));
		
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		finish();
	}
	
	private class AboutWebClient extends WebViewClient {

		

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, final String url) {
			
			// XXX hack to make the webview go to an external url if the hyperlink is 
			// in my own HTML file - otherwise it says "Page not available" because I'm not calling
			// loadDataWithBaseURL.  But if I call loadDataWithBaseUrl using a fake URL, then
			// the links within the page itself don't work!!  Arggggh!!!
			
			if (url.startsWith("http") || url.startsWith("mailto") || url.startsWith("market")) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						loadExternalUrl(url);
					}
				});
				return true;
			}		
			return false;
		}


		@Override
		public void onPageFinished(WebView view, String url) {
			// dismiss the loading bar when the page has finished loading
			handler.post(new Runnable(){

				@Override
				public void run() {
					progressBar.setVisibility(View.GONE);
					aboutWebView.setVisibility(View.VISIBLE);
					iconImageView.setVisibility(aboutActivity ? View.VISIBLE : View.GONE);
					
					
				}});
			super.onPageFinished(view, url);
		}
	}
}
