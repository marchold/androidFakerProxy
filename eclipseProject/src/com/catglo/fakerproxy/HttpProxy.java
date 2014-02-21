package com.catglo.fakerproxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class HttpProxy extends HashMap<String,String> {

	private static final long serialVersionUID = 1L;
	private static final int maxBufferSizeForProxy = 1024;
	private static final int MAX_HTTP_HEADER_SIZE = 25000;
	private static final int MAX_HTTP_HEADERS = 40;
	private static final boolean CONTINUE_SENDING_AFTER_REQUEST = true;
	private static final String LOG_TAG = "PROXY";
	
	
	private AssetManager assetManager;
	private int localhostRelayPort;
	private String apiServerHostName;
	private int apiServerPort;
	
	static final Pattern httpHeaderEndPattern            = Pattern.compile("\r\n\r\n");
	static final Pattern httpRequestFieldAndValuePattern = Pattern.compile("(.*?):(.*?)\r\n");
	static final Pattern statusLineEndpointPattern       = Pattern.compile("[GETPOSUDL]{3,6}\\s([\\w_\\-\\&\\?\\/\\\\\\:\\%\\(\\)\\$\\#\\!\\^\\*\\@\\.\\,<>]+)");
	
	interface ProxyStatusListener {
		public void statusChanged(String newStatus);
	}
	private ProxyStatusListener statusListener;
	public void setProxyStatusListener(ProxyStatusListener statusListener){
		this.statusListener = statusListener;
	}
	private void setStatus(String newStatus){
		if (statusListener!=null){
			statusListener.statusChanged(newStatus);
		}
	}
	
	ServerSocket localhostListeningSocket;
	
	interface ProxyLogListener {
		public void log(String newStatus);
	}
	private ProxyLogListener logger;
	public void setProxyLogListener(ProxyLogListener logger){
		this.logger = logger;
	}
	private void log(String log){
		if (logger!=null){
			logger.log(log);
		}
		Log.i("PROXY",log);
	}
	
	public boolean enableLogging=true;
	public boolean enableIntercept=true;
	private Thread thread;
	
	public void stop(){
		try {
			localhostListeningSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		thread.interrupt();
		start();
	}
	
	static int requestCounter = 0;
	
	public void start(){
		Runnable runnable = new Runnable(){public void run() {
			try {
				int httpHeaderPrebufferSize = MAX_HTTP_HEADER_SIZE;
				
				byte[] buffer = new byte[Math.max(maxBufferSizeForProxy,httpHeaderPrebufferSize)];		
				byte[] headerScratchBuffer = new byte[httpHeaderPrebufferSize];
				String headers[] = new String[MAX_HTTP_HEADERS];

				int port=localhostRelayPort;
				Socket socketApiHost;

				
				Matcher m;
				
				try {
					localhostListeningSocket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
				}
				catch (BindException e){
					setStatus("Failed to bind to port:"+port);
					log("Port in use");
					return;
				}
				port = localhostListeningSocket.getLocalPort();
				setStatus("port " + port + " obtained");
			
				int counter =0;
				do {
					counter++;
					setStatus("server listening");
					
					Socket localhostConnection = localhostListeningSocket.accept();
					if (localhostConnection == null) {
						continue;
					}
					setStatus("processing");
					
					
					InputStream localhostRelayInputStream = localhostConnection.getInputStream();
					OutputStream localhostRelayOutputStream = localhostConnection.getOutputStream();
		
					
					FileOutputStream apRequestLogger=null;  
				
					requestCounter++;
					
					String endpointFile = "Request"+requestCounter+".txt";
					Log.i("Count","Request count "+requestCounter);
				
					File sdCard = Environment.getExternalStorageDirectory();
					File file = new File(sdCard.getAbsolutePath(), endpointFile);
					file.delete();
					apRequestLogger = new FileOutputStream(file);  
					
					
					//Read in the HTTP header so we can determine if we need to handle the request ourselves
					int totalBytesRead=0;
					int bytesRead; 
					String myDebug;
				//	StringBuilder headerBuilderBuffer = new StringBuilder(httpHeaderPrebufferSize);
					
				//	while (totalBytesRead<httpHeaderPrebufferSize){
					    Thread.sleep(10);
						while (localhostRelayInputStream.available()>0){ 
							
							int bytesToRead = httpHeaderPrebufferSize-totalBytesRead;
							int avalableBytes = localhostRelayInputStream.available();
							if (bytesToRead > avalableBytes){
								bytesToRead = avalableBytes;
							}
							//get back the data
							bytesRead = localhostRelayInputStream.read(buffer,totalBytesRead,bytesToRead);
							totalBytesRead+=bytesRead;	
							myDebug = new String(buffer,0,totalBytesRead);
							//byte[] garb = new byte[10];
							//garb[0]= 'M';
							//garb[1]= '\n';
							//garb[2]= '\r';
							//apRequestLogger.write(garb,0,3);
							
							apRequestLogger.write(buffer,0,totalBytesRead);
							m = httpHeaderEndPattern.matcher(myDebug);
							
						//	if (m.find()){
						//		httpHeaderPrebufferSize=totalBytesRead;
						//		break;
						//	}
							Thread.sleep(10);
						}
			
				//	}
					
					
					
					//Parse the status line out of the header
					int statusLineBufferIndex;
					for (statusLineBufferIndex = 0; statusLineBufferIndex < httpHeaderPrebufferSize; statusLineBufferIndex++){
						headerScratchBuffer[statusLineBufferIndex] = buffer[statusLineBufferIndex];
						
						if (headerScratchBuffer[statusLineBufferIndex]==0x0A) {
							break;
						}
					}
					statusLineBufferIndex++;
					String statusLine = new String(headerScratchBuffer,0,statusLineBufferIndex);
					
					m = statusLineEndpointPattern.matcher(statusLine);
					String endpoint="error";
					if (m.find()){
						endpoint = m.group(1);
					}
					
					FileOutputStream apiDataLogger=null;  
			        
					if (enableIntercept && containsKey(endpoint)){
						log(counter+",Intercept:"+endpoint);
						
						String fileName = get(endpoint);
						
						InputStream fakeJsonInputStream=null;
						bytesRead=0;
						
						try {
							fakeJsonInputStream=assetManager.open(fileName);
						    
							while (fakeJsonInputStream.available()>0){
								bytesRead = fakeJsonInputStream.available();
								if (bytesRead>2048){
									bytesRead=2048;
								}
								
								fakeJsonInputStream.read(buffer, 0, bytesRead);
								localhostRelayOutputStream.write(buffer, 0, bytesRead);
							}
							
						} catch (IOException e) {
						    e.printStackTrace();
						    log("FAIL - Please reset proxy");
						    setStatus("CRASHED");
						} 
						continue;
					} 
					log(counter+",Forward:"+endpoint);
					
					
					//Get the HTTP headers in to an array
					int currentHeaderIndex = -1;
					int headerBufferIndex;
					do {
						currentHeaderIndex++;
						for (headerBufferIndex=0; statusLineBufferIndex < httpHeaderPrebufferSize; statusLineBufferIndex++,headerBufferIndex++){
							headerScratchBuffer[headerBufferIndex] = buffer[statusLineBufferIndex];
							if (headerScratchBuffer[headerBufferIndex]==0x0A) {
								break;
							} 
						}
						statusLineBufferIndex++;
						headerBufferIndex++;
						headers[currentHeaderIndex] = new String(headerScratchBuffer,0,headerBufferIndex);				  
					} while (headers[currentHeaderIndex].length()>2);
					 
					//Here we have a chance to intercept the http headers in case we need to tweak something
					for (int i = 0; i < currentHeaderIndex; i++){	
						//if (headers[i].contains("Host")){
						//	headers[i] = "Host: "+apiServerHostName+":"+apiServerPort+"\r\n";
						//}
						Log.i("faker","["+i+"] = "+headers[i]);
						headers[i] = headers[i].replaceAll("localhost\\:"+localhostRelayPort, apiServerHostName+":"+apiServerPort);

                        //if (headers[i].contains("Accept-Encoding")){
                        //    Log.i("MARC","replacing "+headers[i]);
                        //    headers[i] = "Accept-Encoding: text/plain \r\n";
                        //    Log.i("MARC","with "+headers[i]);
                        //}
                    }
					
					if (enableLogging){
						try {
							if (endpoint.equalsIgnoreCase(".") || endpoint.equalsIgnoreCase("/")){
								endpoint="ROOT";
							}
							endpointFile = endpoint.replace("/", "_").replace("?", ".");
							sdCard = Environment.getExternalStorageDirectory();
							file = new File(sdCard.getAbsolutePath(), endpointFile);
							file.delete();
							apiDataLogger = new FileOutputStream(file);   
							
							
						} catch (FileNotFoundException e) {
							Log.e("FAKER PROXY","failed to open log");
						}
					}
					
					int numberOfBytesInBufferAfterHeaderBytes = httpHeaderPrebufferSize-statusLineBufferIndex;
				
					//
					// At this point we are done reading the request and its time to forward it to the real API server
					//
					socketApiHost = new Socket(apiServerHostName,apiServerPort);
					InputStream apiServerInputStream = socketApiHost.getInputStream();
					OutputStream apiServerOutputStream = socketApiHost.getOutputStream();
					
					
					endpointFile = "Request-filtered"+requestCounter;
					sdCard = Environment.getExternalStorageDirectory();
					file = new File(sdCard.getAbsolutePath(), endpointFile);
					file.delete();
					FileOutputStream apFilteredRequestLogger = new FileOutputStream(file);  
					
					//Write the HTTP status line to the api server socket
					apiServerOutputStream.write(statusLine.getBytes());
					apFilteredRequestLogger.write(statusLine.getBytes());
					
					
					//Write the HTTP headers to the api server socket
					for (int i = 0; i < currentHeaderIndex; i++){
						apiServerOutputStream.write(headers[i].getBytes());
						apFilteredRequestLogger.write(headers[i].getBytes());
					}
					apiServerOutputStream.write("\r\n".getBytes());
					apFilteredRequestLogger.write("\n\r".getBytes());
					
					//Write the remaining bits of data we pulled in the header buffer
					if (numberOfBytesInBufferAfterHeaderBytes>0){
						apiServerOutputStream.write(buffer,statusLineBufferIndex,numberOfBytesInBufferAfterHeaderBytes);
						apFilteredRequestLogger.write(buffer,statusLineBufferIndex,numberOfBytesInBufferAfterHeaderBytes);
					}

                    //Just in case there was still data after we finished with the header buffer
					int len;
					if ((len = localhostRelayInputStream.available())>0){
						byte[] buf = new byte[len];
						localhostRelayInputStream.read(buf,0,len);
						apiServerOutputStream.write(buf,0,len);
						apRequestLogger.write(buf,0,len);
					}
					
					
					int zombiCount=0;
			        boolean keepGoing=true;
                    int totalApiBytesRead = 0;
					while (keepGoing && zombiCount<20)
					{
			        	//To make sure that the thread does not spin endlessly doing nothing but eating battery and cpu
			        	//we keep a counter to end the thread if its not doing anything 
			        	zombiCount++;
						while ((bytesRead = apiServerInputStream.available())>0 && keepGoing){ 
							
						
							zombiCount=0;
							
							//clip the bytes read to our max chunk size
							if (bytesRead>maxBufferSizeForProxy){
								bytesRead=maxBufferSizeForProxy;
							}
							apiServerInputStream.read(buffer, 0, bytesRead);
                            totalApiBytesRead += bytesRead;

							if (apiDataLogger!=null){
								apiDataLogger.write(buffer, 0, bytesRead);
							}
							localhostRelayOutputStream.write(buffer,0,bytesRead);
					
						}
						setStatus("spinning");
						Thread.sleep(20);

					}// End while 
					if (apiDataLogger!=null){
						apiDataLogger.close();
					}
					if (apRequestLogger!=null){
						apRequestLogger.close();
					}
				} while(Thread.interrupted()==false);
				
			} catch (UnknownHostException e) {
				Log.e(LOG_TAG, "Error initializing server", e);
				log("UnknownHostException");
				setStatus("CRASHED");
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error initializing server", e);
				log("IOException");
				setStatus("CRASHED");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}};
		thread = new Thread(runnable);
		thread.start();
	}
	
	

	
	/**
	 * HttpProxy will route through localhost and intercept packets to end points listed as its map keys. 
	 * To intercept an end point call .add(ENDPOINT,FILE_TO_SERVE). 
	 *  
	 * @param localhostRelayPort   The port which the app will connect to the proxy over 
	 * @param apiServerHostName    The host name of the real API server
	 * @param apiServerPort        The port to talk to the real API server on
	 */
	public HttpProxy(final AssetManager assetManager
					,final int    localhostRelayPort
					,final String apiServerHostName
					,final int    apiServerPort
			        ) 
	{
		this.assetManager=assetManager;
		this.localhostRelayPort=localhostRelayPort;
		this.apiServerHostName=apiServerHostName;
		this.apiServerPort=apiServerPort;
	}
	
	
	
}
