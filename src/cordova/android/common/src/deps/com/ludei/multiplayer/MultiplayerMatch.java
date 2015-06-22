package com.ludei.multiplayer;



public interface MultiplayerMatch {

    enum Connection {
        RELIABLE , //TCP
        UNRELIABLE
    }

    enum State {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED
    }


    void setDelegate(MultiplayerMatch.Delegate delegate);
    /**
     Returns all the identifiers of the players currently in the match.
     @return An array with the players identifiers
     */
    String[] getPlayerIDs();
    /**
     Gets the current player info
     */
    Player getLocalPlayer();

    /**
     Gets the current player ID
     */
    String getLocalPlayerID();
    /**
     Requests additional player info of the players currently in the match.
     */
    void requestPlayersInfo(PlayerRequestCompletion completion);
    /**
     Returns the expected player number for the match to be considered complete and ready to start.
     @return The expected player count.
     */
    int expectedPlayerCount();

    /**
     Asynchronously send data to one or more players. Returns YES if delivery started, NO if unable to start sending and error will be set.
     @param data The data to be sent.
     @param playerIDs Array with the player ids to send the data to.
     @param mode The data mode.
     @return null error if delivery started, error if unable to start sending.
     */
    MultiplayerService.Error sendData(byte[] data, String[] playerIDs, Connection mode);

    /**
     Asynchronously send data to one or more players. Returns YES if delivery started, NO if unable to start sending and error will be set.
     @param data The data to be sent.
     @param mode The data mode.
     @return null error if delivery started, error if unable to start sending.
     */
    MultiplayerService.Error sendDataToAllPlayers(byte[] data, Connection mode);

    /**
     Disconnect the match. This will show all other players in the match that the local player has disconnected. This should be called before releasing the match instance.
     */
    void disconnect();
    /**
     Start processing received messages. The user must call this method when the game is ready to process messages. Messages received before being prepared are stored and processed later.
     */
    void start();

    interface Delegate {
        void onReceivedData(MultiplayerMatch match, byte[] data,  String fromPlayer);
        void onStateChange(MultiplayerMatch match, String playerID, State state);
        void onPlayerConnectionError(MultiplayerMatch match, String playerID, MultiplayerService.Error error);
        void onMatchError(MultiplayerMatch match, MultiplayerService.Error error);
    }

    interface PlayerRequestCompletion {
        void onComplete(Player[] players, MultiplayerService.Error error);
    }
}
