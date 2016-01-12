(function(){

/*
* @private
* @namespace Cocoon.Multiplayer.LoopbackService
* This namespace provides an abstraction API for the Loopback Multiplayer Service.
* @namespace Cocoon.Multiplayer.Loopback
* @example
* var loopback = new Cocoon.Multiplayer.LoopbackService();
* var request = new Cocoon.Multiplayer.MatchRequest(2,2);
* var handleMatch = function(match, error){
*
* }
* ...
* loopback.findMatch(request, handleMatch);
* ...
*/
Cocoon.define("Cocoon.Multiplayer" , function(extension){

    var loopbackServices = [];
    var indexCounter = 0;
    var matchServices = [];
    var matchCounter = 0;

    extension.LoopbackService = function() {
        loopbackServices.push(this);
        this.playerID = "" + indexCounter;
        indexCounter++;
        this.signal = new Cocoon.Signal();
        this.on = this.signal.expose();
    };

    extension.LoopbackService.prototype =  {

        findMatch : function(request, callback)  {

            this.findMatchCallback = callback;

            //checks if the service is already added to the match list
            var exists = false;
            for (var i = 0; i< matchServices.length; ++i) {
                if (matchServices[i] === this) {
                    exists = true; break;
                }
            }
            if (!exists)
                matchServices.push(this);

            //Create the match is all required players are ready
            //TODO: check more conditions (playerGroup, playerAttributes) to complete a match
            if (matchServices.length >= request.minPlayers) {
                var playerIDs = [];
                //create playerIDs array
                for (i = 0; i< matchServices.length; ++i) {
                    playerIDs.push(matchServices[i].getPlayerID());
                }

                //notify the found match to each manager
                for (i = 0; i< matchServices.length; ++i) {
                    var match = new LoopbackMatch(matchServices[i]);
                    match.playerIDs = playerIDs.slice();
                    matchServices[i].currentMatch = match;
                    matchServices[i].findMatchCallback(match, null);
                }
                //clear pending list
                matchServices = [];
            }


        },
        findAutoMatch : function(matchRequest, callback) {
            this.findMatch(matchRequest,callback);
        },
        cancelAutoMatch : function() {

        },

        addPlayersToMatch : function(matchRequest, match, callback) {
            callback({message:"Not implemmented"});
        },
        getPlayerID : function() {
            return this.playerID;
        },

        getMatch : function() {
            return this.currentMatch;
        }
    };

    var LoopbackMatch = function(service) {
        matchCounter++;
        this.started = false;
        this.disconnected = false;
        this.pendingData = [];
        this.service = service;
        this.signal = new Cocoon.Signal();
        this.on = this.signal.expose();
    };

    LoopbackMatch.prototype = {

        start : function() {
            var me = this;
            setTimeout(function() {
                me.started = true;
                for (var i = 0; i < me.pendingData.length; ++i) {
                    me.signal.emit('match', 'dataReceived', [me, me.pendingData[i].data, me.pendingData[i].player]);
                }
                me.pendingData = [];

            },0);

        },
        sendDataToAllPlayers : function(data, sendMode) {
            this.sendData(data, this.playerIDs, sendMode);
        },

        sendData : function(data, playerIDs,  sendMode) {
            var me = this;
            setTimeout(function() {
                for (var i = 0; i< loopbackServices.length; ++i) {
                    var destService = null;
                    for (var j = 0; j < playerIDs.length; ++j) {
                        if (playerIDs[j] === loopbackServices[i].getPlayerID()) {
                            destService = loopbackServices[i];
                        }
                    }
                    if (destService) {
                        destService.getMatch().notifyDataReceived(data,me.service.getPlayerID());
                    }
                }
            },0);
        },

        disconnect : function() {
            this.disconnected = true;
            for (var i = 0; i < this.playerIDs.length; ++i) {
                var p = this.playerIDs[i];
                for (var j = 0; j < loopbackServices.length; ++j) {
                    if (loopbackServices[j].getPlayerID() === p) {
                        var match = loopbackServices[i].getMatch();
                        if (!match.disconnected) {
                            match.signal.emit('match', 'stateChanged', [match, this.service.getPlayerID(), Cocoon.Multiplayer.ConnectionState.DISCONNECTED]);
                        }
                    }
                }
            }
        },

        requestPlayersInfo : function(callback) {
            var me = this;
            setTimeout(function() {
                var playersInfo = [];
                for (var i = 0; i < me.playerIDs.length; ++i) {
                    playersInfo[i] = {userID: me.playerIDs[i], userName: "Player" + me.playerIDs[i]};
                }
                callback(playersInfo);
            },1);
        },

        getExpectedPlayerCount : function() {
            return 0;
        },
        getPlayerIDs : function() {
            return this.playerIDs;
        },
        getLocalPlayerID: function() {
            return this.service.playerID;
        },
        notifyDataReceived: function(data, fromPlayer) {
            if (this.disconnected) {
                return;
            }
            if (!this.started) {
                this.pendingData.push({data:data, player:fromPlayer});
            }
            else {
                this.signal.emit('match', 'dataReceived', [this, data,fromPlayer]);
            }
        }

    };

    return extension;
});

})();