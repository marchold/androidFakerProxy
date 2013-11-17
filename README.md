Android Faker Proxy
===================

An Http Proxy for android that can send dummy json for specified endpoints.

The problem this app is trying to solve is this: Sometimes when writing an android app you are waiting on the server
team to make a change to a REST api. With this proxy you can put a responce for some API endpoints that is staticly 
defined and pass the rest of the calls to the real server. This way you can fake some API calls that do not exist yet.
Its also useful for debugging error conditions by making a page return a 404 when in reality its working fine.

The way it works is you set the host address and port of the server you want to tweak the results from. Then you pont your
app to localhost:8082 and the proxy takes packets sent to/from localhost:8082 and passes them to the real server. The proxy
class is derrived from a Map<string,string>. The key is the endpoint the value is a filename of a file in the proxy's 
assets folder. When those encpoints are accessed the file is returned and no server interaction takes place.

For example:

        final HttpProxy proxy = new HttpProxy(getAssets()
				                            ,8083
				                            ,"google.com"
				                            ,80);
		
		proxy.put("/api", "fake.json");
		
		proxy.start();
		
Now in a browser if you hit http://localhost:8083/ you will get google's home page. 
If you put in http://localhost:8083/api you will get the contense of a file fake.json that you put in the assets folder 

You need to include the HTTP headers in the file that is in the assets folder.
