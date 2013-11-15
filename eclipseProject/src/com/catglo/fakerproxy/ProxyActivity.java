package com.catglo.fakerproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.HttpRequest;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class ProxyActivity extends Activity {

	protected static final String LOG_TAG = "PROXY";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Thread proxyThread = new Thread(new Runnable(){private ServerSocket socket;
		private int port=8080;
		private Socket server;

		public void run() {
			try {
				socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
				//socket.setSoTimeout(5000);
				port = socket.getLocalPort();
				Log.d(LOG_TAG, "port " + port + " obtained");
				
				server = new Socket("http://",80);
				InputStream serverInputStream = server.getInputStream();
				OutputStream serverOutputStream = server.getOutputStream();
				
				do {
					Socket client = socket.accept();
					if (client == null) {
						continue;
					}
					Log.d(LOG_TAG, "client connected");
					//HttpRequest request = readRequest(client);
					//processRequest(request, client);
				}while(1==1);
				
				
			} catch (UnknownHostException e) {
				Log.e(LOG_TAG, "Error initializing server", e);
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error initializing server", e);
			}
			
			
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
