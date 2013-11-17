androidFakerProxy
=================

An Http Proxy for android that can send dummy json for specified endpoints.

The problem this app is trying to solve is this: Sometimes when writing an android app you are waiting on the server
team to make a change to a REST api. With this proxy you can put a responce for some API endpoints that is staticly 
defined and pass the rest of the calls to the real server. This way you can fake some API calls that do not exist yet. 
