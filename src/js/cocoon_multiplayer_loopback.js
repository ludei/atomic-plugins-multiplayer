(function(){

/**
* This namespace provides an abstraction API for a local loopback service that emulates the bahaviour of a multiplayer service.
* This service can be very helpful during the development process to setup the login before integrating any other multiplayer service.
* @namespace Cocoon.Multiplayer.LoopbackService
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

        /**
         * Presents a system View for the matchmaking and creates a new Match.
         * @function findMatch
         * @memberOf Cocoon.Multiplayer.LoopbackService
         * @param {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
         * @param {Function} callback The callback function. It receives the following parameters:
         * - {@link Cocoon.Multiplayer.Match}.
         * - Error.
         */
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

        /**
         * Sends an automatch request to join the authenticated user to a match. It doesn't present a system view while waiting to other players.
         * @function findAutoMatch
         * @memberOf Cocoon.Multiplayer.LoopbackService
         * @param  {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
         * @param {Function} callback The callback function. It receives the following parameters:
         * - {@link Cocoon.Multiplayer.Match}.
         * - Error.
         */
        findAutoMatch : function(matchRequest, callback) {
            this.findMatch(matchRequest,callback);
        },

        /**
         * Cancels the ongoing automatch request.
         * @function cancelAutoMatch
         * @memberOf Cocoon.Multiplayer.LoopbackService
         */
        cancelAutoMatch : function() {

        },

        /**
         * Automatically adds players to an ongoing match owned by the user.
         * @function addPlayersToMatch
         * @memberOf Cocoon.Multiplayer.LoopbackService
         * @param {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
         * @param {Cocoon.Multiplayer.Match} matchRequest The match where new players will be added.
         * @param {Function} callback The callback function. Response parameters: error.
         */
        addPlayersToMatch : function(matchRequest, match, callback) {
            callback({message:"Not implemmented"});
        },

        /**
         * Gets the local playerID taking part in the match.
         * @function getPlayerID
         * @memberOf Cocoon.Multiplayer.Match
         * @return {string} the playerID attached to the match manager.
         */
        getPlayerID : function() {
            return this.playerID;
        },

        /**
         * Get the current match reference.
         * @function getMatch
         * @memberOf Cocoon.Multiplayer.LoopbackService
         * @return {Cocoon.Multiplayer.Match} The current match reference.
         */
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