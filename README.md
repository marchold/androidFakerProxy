Android Faker Proxy
===================

An Http Proxy for android that can send dummy json for specified endpoints.

The problem this app is trying to solve is this: Sometimes when writing an android app you are waiting on the server
team to make a change to a REST api. With this proxy you can put a responce for some API endpoints that is staticly 
defined and pass the rest of the calls to the real server. This way you can fake some API calls that do not exist yet. 

The way it works is you set the host address and port of the server you want to tweak the results from. Then you pont your
app to localhost:8082 and the proxy takes packets sent to/from localhost:8082 and passes them to the real server. There is
a Map<string,string> that is passed in to the constructor. The key is the endpoint the value is a filename of a file in 
the proxy's assets folder. When those encpoints are accessed the file is returned and no server interaction takes place.
