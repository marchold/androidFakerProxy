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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ProxyActivity extends Activity {

	public static final String API_HOST = "oep04-d.samsmk.com";
	public static final int API_PORT = 80;
	public static final int MAX_DEVICE_ON_SCREEN_HISTORY_LIST=30;
	
	private TextView statusText;
	private ViewGroup historyLayout;
	private ToggleButton enableLoggingToggle;
	private ToggleButton enablInterceptToggle;
	private EditText localPortField;
	private SharedPreferences prefs;
	private HttpProxy proxy;
	private Editor editor;
	private Button restartProxyButton;

	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		localPortField = (EditText)findViewById(R.id.localPort);
		statusText = (TextView)findViewById(R.id.statusText);
		historyLayout = (ViewGroup)findViewById(R.id.historyLayout);
		enableLoggingToggle = (ToggleButton)findViewById(R.id.enableLoggingToggle);
		enablInterceptToggle = (ToggleButton)findViewById(R.id.enablInterceptToggle);
		restartProxyButton = (Button)findViewById(R.id.restartProxyButton);
		restartProxyButton.setOnClickListener(new OnClickListener(){public void onClick(View arg0) {
			reStartProxy();
		}});
		startProxy();
	}
	

	void reStartProxy() {
		if (proxy != null) proxy.stop();
		startProxy();
	}
	
	void startProxy() {
		
		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		editor = prefs.edit();
		String port = prefs.getString("localport", "8081");
		localPortField.setText(port);
		localPortField.addTextChangedListener(new TextWatcher(){
			public void afterTextChanged(Editable arg0) {
				editor.putString("localport", arg0.toString());
				editor.commit();
			}
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
		});

		proxy = new HttpProxy(getAssets(),Integer.valueOf(port)
				                            ,API_HOST
				                            ,API_PORT);

		proxy.put("/services/api/tap/rewards", "rewards.txt");
		proxy.put("/services/api/tap/events", "allevents.txt");
		proxy.put("/services/customers/me/profile?fields","profile.txt");
		proxy.put("/services/api/tap/location/1?deviceId", "taptowin.txt");
		
		
		proxy.setProxyStatusListener(new ProxyStatusListener(){public void statusChanged(final String newStatus) {
			runOnUiThread(new Runnable(){public void run() {
				statusText.setText(newStatus);		
			}});
		}});
		
		proxy.setProxyLogListener(new ProxyLogListener(){public void log(final String log) {
			runOnUiThread(new Runnable(){public void run() {
				if (historyLayout.getChildCount()>MAX_DEVICE_ON_SCREEN_HISTORY_LIST){
					historyLayout.removeViewAt(historyLayout.getChildCount()-1);
				}
				TextView t = new TextView(ProxyActivity.this);
				t.setText(log);
				historyLayout.addView(t, 0);
			}});
		}});
		
		enableLoggingToggle.setChecked(true);
		enableLoggingToggle.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
			proxy.enableLogging = isChecked;
		}});
		
		
		enablInterceptToggle.setChecked(true);
		enablInterceptToggle.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
			proxy.enableIntercept = isChecked;
		}});
		
		
		proxy.start();
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
