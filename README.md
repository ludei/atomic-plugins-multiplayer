Atomic Plugins for Multiplayer
===============================

This repo contains Multiplayer APIs designed using the [Atomic Plugins paradigm](#about-atomic-plugins). Integrate Social services in your app easily and take advantage of all the features provided: elegant API, flexible solution that works across multiple platforms, single API for different Social Services and more. 
 
Currently there are 2 multiplayer services implemented:

* GameCenter
* GooglePlay 

You can contribute and help to create more awesome plugins.

##About Atomic Plugins

Atomic Plugins provide an elegant and minimalist API and are designed with portability in mind from the beginning. Framework dependencies are avoided by design so the plugins can run on any platform and can be integrated with any app framework or game engine. 

#Provided APIs

  * [JavaScript API](#javascript-api)
  * [API Reference](#api-reference)
  * [Introduction](#introduction)
  * [Setup your project](#setup-your-project)
  * [Example](#example-1)

##JavaScript API:

###API Reference

See [API Documentation](http://ludei.github.io/cocoon-common/dist/doc/js/Cocoon.Multiplayer.html)

###Introduction 

Cocoon.Multiplayer class provides an easy to use Multiplayer API that can be used with different Multiplayer Services: GooglePlay games and GameCenter.

###Setup your project

Releases are deployed to Cordova Plugin Registry. You only have to install the desired plugins using Cordova CLI, CocoonJS CLI or Ludei's Cocoon.io Cloud Server.

    cordova plugin add com.ludei.multiplayer.ios.gamecenter;
    cordova plugin add com.ludei.multiplayer.android.googleplaygames;

The following JavaScript file is included automatically:

[`cocoon_multiplayer.js`](src/js/cocoon_multiplayer.js)

And, depending on the social service used, also: 

[`cocoon_multiplayer_gamecenter.js`](src/js/cocoon_multiplayer_gamecenter.js)
[`cocoon_multiplayer_googleplaygames.js`](src/js/cocoon_multiplayer_googleplaygames.js)

###Example

	var GameCenter = Cocoon.Social.GameCenter;
	var SocialGameCenter = gc.getSocialInterface();
	var MultiplayerGameCenter = gc.getMultiplayerInterface();
	  
	SocialGameCenter.login(function(loggedIn, error) {
		if(loggedIn){
			var request = new Cocoon.Multiplayer.MatchRequest(2,2);
			var handleMatch = function(match, error){
	  			console.log("Error: " + JSON.Stringify(error));
	   		}

	 		...

	   		MultiplayerGameCenter.findMatch(request, handleMatch);
	   		
	   		...
	    
	    }
	});

#License

Mozilla Public License, version 2.0

Copyright (c) 2015 Ludei 

See [`MPL 2.0 License`](LICENSE)

