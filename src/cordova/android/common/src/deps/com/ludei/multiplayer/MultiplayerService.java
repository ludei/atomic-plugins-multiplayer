package com.ludei.multiplayer;


import android.content.Intent;

import java.util.HashMap;

public interface MultiplayerService
{
    void setDelegate(MultiplayerService.Delegate delegate);
    /**
     Presents a system View for the matchmaking and creates a new Match
     */
    void findMatch(MatchRequest request, MatchCompletion completion);
    /**
     Sends an automatch request to join the authenticated user to a match waiting for players to start.
     Only one automatch request can be active at the same time.
     If an automatch request is requested while a previous request is still active, it will be ignored.
     */
    void findAutoMatch(MatchRequest request, MatchCompletion completion);
    /**
     Cancels the ongoing automatch request.
     If there are no ongoing request it will be ignored.
     */
    void cancelAutoMatch();
    /**
     Automatically adds players to an ongoing match owned by the user.
     */
    void addPlayersToMatch(MultiplayerMatch match, MatchRequest request, Completion completion);
    /**
     Get the current match reference.
     @return Current match reference.
     */
    MultiplayerMatch getCurrentMatch();

    /**
     * Android onActivityResult handler
     */
    boolean onActivityResult(int requestCode, int resultCode, Intent data);


    interface Delegate {
        void onInvitationReceive(MultiplayerService sender);
        void onInvitationLoad(MultiplayerService service, MultiplayerMatch match, Error error);
    }

    interface MatchCompletion {
        void onComplete(MultiplayerMatch match, Error error);
    }

    interface Completion{
        void onComplete(Error error);
    }

    class Error {
        public String message;
        public int code;

        public Error(int code, String msg) {
            this.code = code;
            this.message = msg;
        }
    }
}