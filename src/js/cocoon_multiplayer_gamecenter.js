(function() {

    if (window.cordova && typeof require !== 'undefined') {
        require('cocoon-plugin-multiplayer-common.Multiplayer'); //force dependency load
    }
    var Cocoon = window.Cocoon;
	/**
	 * This namespace provides an abstraction API for the Game Center Multiplayer Service.
	 * @namespace Cocoon.Multiplayer.GameCenter
	 * @example
	 * var GameCenter = Cocoon.Social.GameCenter;
	 * var SocialGameCenter = gc.getSocialInterface();
	 * var MultiplayerGameCenter = gc.getMultiplayerInterface();
	 *
	 * SocialGameCenter.login(function(loggedIn, error) {
	 * 	if(loggedIn){
	 * 		var request = new Cocoon.Multiplayer.MatchRequest(2,2);
	 * 		var handleMatch = function(match, error){
	 *
	 * 		}
	 * 		...
	 * 		MultiplayerGameCenter.findMatch(request, handleMatch);
	 * 		...
	 * 	}
	 * });
	 */
	Cocoon.define("Cocoon.Multiplayer", function(extension) {

        extension.GameCenter = new Cocoon.Multiplayer.MultiplayerService('LDMultiplayerGameCenterPlugin');
		/**
		 * Presents a system View for the matchmaking and creates a new Match.
		 * @function findMatch
		 * @memberOf Cocoon.Multiplayer.GameCenter
		 * @param {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
		 * @param {Function} callback The callback function. It receives the following parameters:
		 * - {@link Cocoon.Multiplayer.Match}.
		 * - Error.
		 */

		/**
		 * Sends an automatch request to join the authenticated user to a match. It doesn't present a system view while waiting to other players.
		 * @function findAutoMatch
		 * @memberOf Cocoon.Multiplayer.GameCenter
		 * @param  {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
		 * @param {Function} callback The callback function. It receives the following parameters:
		 * - {@link Cocoon.Multiplayer.Match}.
		 * - Error.
		 */

		/**
		 * Cancels the ongoing automatch request.
		 * @function cancelAutoMatch
		 * @memberOf Cocoon.Multiplayer.GameCenter
		 */

		/**
		 * Automatically adds players to an ongoing match owned by the user.
		 * @function addPlayersToMatch
		 * @memberOf Cocoon.Multiplayer.GameCenter
		 * @param {Cocoon.Multiplayer.MatchRequest} matchRequest The parameters for the match.
		 * @param {Cocoon.Multiplayer.Match} matchRequest The match where new players will be added.
		 * @param {Function} callback The callback function. Response parameters: error.
		 */

		/**
		 * Get the current match reference.
		 * @function getMatch
		 * @memberOf Cocoon.Multiplayer.GameCenter
		 * @return {Cocoon.Multiplayer.Match} The current match reference.
		 */
		return extension;
	});

})();
