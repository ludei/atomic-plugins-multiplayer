package com.ludei.multiplayer.googleplaygames;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.ludei.googleplaygames.GPGService;
import com.ludei.multiplayer.MatchRequest;
import com.ludei.multiplayer.MultiplayerMatch;
import com.ludei.multiplayer.MultiplayerService;

import java.util.ArrayList;


public class GPGMultiplayerService implements MultiplayerService, OnInvitationReceivedListener {


    public final static int REQUEST_FIND_MATCH = 0x000000000001714;
    public final static int REQUEST_WAITING_UI = 0x000000000001715;
    public final static int REQUEST_INVITATION_BOX = 0x000000000001716;

    private MatchRequest findMatchRequest;
    private MultiplayerService.MatchCompletion findMatchCallback;
    private GPGMultiplayerMatch currentMatch;
    private MultiplayerService.Delegate delegate;
    private Activity activity;


    public GPGMultiplayerService(Activity activity)
    {
        this.activity = activity;
    }

    public void init()
    {
        GoogleApiClient client = getGamesClient();
        //Check invitations whenever the user succeeds logging into Google Play Games
        if (client != null && client.isConnected()) {
            checkInvitations();
        }
        GPGService.onConnectedCallback = new Runnable() {
            public void run() {
                checkInvitations();
            }
        };

    }


    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {

        boolean managed = false;
        if (requestCode == REQUEST_FIND_MATCH) {
            managed = true;
            if (resultCode != Activity.RESULT_OK) {

                //User signed out from the multiplayer settings in the upper right corner. Notify the GPG Service!
                if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED && GPGService.currentInstance() != null) {
                    GPGService.currentInstance().logout(null);
                }
                //user cancelled
                if (findMatchCallback != null) {
                    findMatchCallback.onComplete(null, null);
                    findMatchCallback = null;
                }
                return true;
            }

            // get the invitee list
            Bundle extras = data.getExtras();
            ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
            findMatchRequest.playersToInvite = invitees != null ? invitees.toArray(new String[invitees.size()]) : null;
            createMatch(findMatchRequest, true, new GPGMultiplayerMatch.GPGMatchRoomListener() {
                public void roomCreated(final GPGMultiplayerMatch match, final MultiplayerService.Error error) {

                    if (findMatchCallback != null) {
                        findMatchCallback.onComplete(error == null ? match : null, error);
                        findMatchCallback = null;
                    }
                }
            });
        }
        else if (requestCode == REQUEST_WAITING_UI) {
            managed = true;
            if (currentMatch != null) {
                currentMatch.waitingUIDismissed(resultCode);
            }
        }
        else if (requestCode == REQUEST_INVITATION_BOX) {
            managed = true;
            if (resultCode != Activity.RESULT_OK) {
                // canceled
                return true;
            }
            // get the selected invitation
            Bundle extras = data.getExtras();
            Invitation invitation = extras.getParcelable(Multiplayer.EXTRA_INVITATION);
            createMatchFromInvitation(invitation);
        }
        return managed;
    }

    private void checkInvitations() {
        GoogleApiClient client = getGamesClient();
        if (client == null)
            return;

        //In-game invitation listener
        Games.Invitations.unregisterInvitationListener(client);
        Games.Invitations.registerInvitationListener(client, this);

        //Invitation received on connection callback
        if (GPGService.multiplayerInvitation != null) {
            createMatchFromInvitation(GPGService.multiplayerInvitation);
            GPGService.multiplayerInvitation = null;
        }
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {

        //TODO: show a popup instead of the Invitation Box
        Intent intent = Games.Invitations.getInvitationInboxIntent(getGamesClient());
        activity.startActivityForResult(intent, REQUEST_INVITATION_BOX);
    }

    //@Override
    public void onInvitationRemoved(java.lang.String s) {

    }

    private boolean isReady() {
        GoogleApiClient client = GPGService.getGoogleAPIClient();
        return client != null && client.isConnected();
    }

    private GoogleApiClient getGamesClient() {
        return GPGService.getGoogleAPIClient();
    }


    private GPGMultiplayerMatch createMatch(MatchRequest request, boolean showUI, GPGMultiplayerMatch.GPGMatchRoomListener listener)
    {
        GoogleApiClient client = getGamesClient();
        GPGMultiplayerMatch match = new GPGMultiplayerMatch(client, activity, showUI);
        currentMatch = match;
        match.setListener(listener);
        int minPlayers = request.minPlayers - 1;
        int maxPlayers = request.maxPlayers - 1;

        match.setMinPlayers(request.minPlayers);
        RoomConfig.Builder builder = RoomConfig.builder(match)
                .setMessageReceivedListener(match)
                .setRoomStatusUpdateListener(match);

        if (request.playersToInvite != null && request.playersToInvite.length > 0) {
            builder.addPlayersToInvite(request.playersToInvite);
            minPlayers = Math.max(minPlayers - request.playersToInvite.length, 0);
            maxPlayers = Math.max(minPlayers - request.playersToInvite.length, 0);
        }

        if (minPlayers > 0 || maxPlayers > 0) {
            long mask =  (((long)request.playerAttributes) << 32) | (request.playerGroup & 0xffffffffL);
            builder.setAutoMatchCriteria(RoomConfig.createAutoMatchCriteria(minPlayers,maxPlayers,mask));
        }

        RoomConfig roomConfig = builder.build();
        Games.RealTimeMultiplayer.create(client, roomConfig);
        return match;
    }

    public void createMatchFromInvitation(Invitation invitation) {

        GoogleApiClient client = getGamesClient();
        GPGMultiplayerMatch match = new GPGMultiplayerMatch(client, activity, true);
        match.setListener(new GPGMultiplayerMatch.GPGMatchRoomListener() {
            @Override
            public void roomCreated(final GPGMultiplayerMatch match, final MultiplayerService.Error error) {
                if (delegate != null) {
                    delegate.onInvitationLoad(GPGMultiplayerService.this, error == null ? match : null, error);
                }
            }
        });
        RoomConfig.Builder builder = RoomConfig.builder(match)
                .setMessageReceivedListener(match)
                .setRoomStatusUpdateListener(match);
        builder.setInvitationIdToAccept(invitation.getInvitationId());
        RoomConfig roomConfig = builder.build();
        Games.RealTimeMultiplayer.join(client, roomConfig);

        if (delegate != null) {
            delegate.onInvitationReceive(this);
        }
    }

    private MultiplayerService.Error createLoginError() {
        return new MultiplayerService.Error(1, "User is not logged into Google Play Games");
    }

    //MultiplayerService Interface

    @Override
    public void setDelegate(MultiplayerService.Delegate delegate)
    {
        this.delegate = delegate;
    }


    @Override
    public void findMatch(MatchRequest request, MatchCompletion completion)
    {
        if (!isReady()) {
            if (completion != null) {
                completion.onComplete(null, createLoginError());
            }
            return;
        }

        this.findMatchCallback = completion;
        GoogleApiClient client = getGamesClient();
        findMatchRequest = request;
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(client,findMatchRequest.minPlayers - 1, findMatchRequest.maxPlayers - 1);
        activity.startActivityForResult(intent, REQUEST_FIND_MATCH);
    }

    @Override
    public void findAutoMatch(MatchRequest request, final MatchCompletion completion)
    {
        if (!isReady()) {
            if (completion != null) {
                completion.onComplete(null, createLoginError());
            }
            return;
        }

        createMatch(request, false, new GPGMultiplayerMatch.GPGMatchRoomListener() {
            @Override
            public void roomCreated(final GPGMultiplayerMatch match, final MultiplayerService.Error error) {
                if (completion != null) {
                    completion.onComplete(error == null ? match: null, error);
                }

            }
        });
    }

    @Override
    public void cancelAutoMatch() {
        if (currentMatch != null)
            currentMatch.cancelAutoMatch();
    }

    @Override
    public void addPlayersToMatch(MultiplayerMatch match,  MatchRequest request, Completion completion) {
        //TODO
        completion.onComplete(new Error(1, "Not implemented"));
    }

    @Override
    public MultiplayerMatch getCurrentMatch() {
        return currentMatch;
    }
}
