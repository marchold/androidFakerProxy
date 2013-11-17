package com.catglo.fakerproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.catglo.fakerproxy.HttpProxy.ProxyLogListener;
import com.catglo.fakerproxy.HttpProxy.ProxyStatusListener;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.TextView;

public class ProxyActivity extends Activity {
	
	public static final int LOCALHOST_RELAY_PORT=8081;
	public static final String API_HOST = "oep06-d.samsmk.com";
	public static final int API_PORT = 80;
	public static final int MAX_DEVICE_ON_SCREEN_HISTORY_LIST=30;
	
	private TextView statusText;
	private ViewGroup historyLayout;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		HashMap<String,String> fakeApis = new HashMap<String,String>();
		
//		fakeApis.put("/api/tap/rewards", "rewards.json");
//		fakeApis.put("/api/tap/rewards?redeemed=true", "rewards.json");
//		fakeApis.put("/api/tap/rewards?redeemed=false", "rewards.json");		
	//	fakeApis.put("/services/customers/me/profile?fields","rewards.json");
		
		HttpProxy proxyThread = new HttpProxy(getAssets()
				                            ,LOCALHOST_RELAY_PORT
				                            ,API_HOST
				                            ,API_PORT
				                            ,fakeApis);
		
		statusText = (TextView)findViewById(R.id.statusText);
		proxyThread.setProxyStatusListener(new ProxyStatusListener(){public void statusChanged(String newStatus) {
			statusText.setText(newStatus);
		}});
		
		historyLayout = (ViewGroup)findViewById(R.id.historyLayout);
		proxyThread.setProxyLogListener(new ProxyLogListener(){public void log(String log) {
			if (historyLayout.getChildCount()>MAX_DEVICE_ON_SCREEN_HISTORY_LIST){
				historyLayout.removeViewAt(historyLayout.getChildCount()-1);
			}
			TextView t = new TextView(ProxyActivity.this);
			t.setText(log);
			historyLayout.addView(t, 0);
		}});
		
		proxyThread.start();
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
