package com.ludei.multiplayer.googleplaygames;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer.ReliableMessageSentCallback;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.games.Player;
import com.ludei.googleplaygames.GPUtils;
import com.ludei.multiplayer.AbstractMatch;
import com.ludei.multiplayer.MultiplayerService;
import com.ludei.multiplayer.LDPlayer;

import java.util.ArrayList;
import java.util.List;

public class GPGMultiplayerMatch extends AbstractMatch implements RoomUpdateListener, RoomStatusUpdateListener, RealTimeMessageReceivedListener, ReliableMessageSentCallback
{

    public interface GPGMatchRoomListener {
        void roomCreated(GPGMultiplayerMatch match, MultiplayerService.Error error);
    }

    private GoogleApiClient client;
    private boolean showWaitingUI;
    private Room room;
    private GPGMatchRoomListener listener;
    private int minPlayers = 2;
    private String myID = "";
    private boolean disconnected = false;
    private ProgressDialog loadingRoomDialog;
    private boolean waitingActivityFinished = false;
    private Activity activity;

    GPGMultiplayerMatch(GoogleApiClient client, Activity activity, boolean showWaitingUI) {
        this.client = client;
        this.showWaitingUI = showWaitingUI;
        this.activity = activity;

        if (showWaitingUI) {
            loadingRoomDialog = ProgressDialog.show(activity, "", "Loading...");
            loadingRoomDialog.setCancelable(false);
        }
    }

    //MultiplayerMatch Interface

    @Override
    public String[] getPlayerIDs() {
        if (room == null)
            return new String[0];

        ArrayList<Participant> players = room.getParticipants();
        String[] result = new String[players.size()];
        for (int i = 0; i < players.size(); ++i) {
            result[i] = players.get(i).getParticipantId();
        }
        return result;
    }

    @Override
    public com.ludei.multiplayer.LDPlayer getLocalPlayer() {
        com.ludei.multiplayer.LDPlayer result = new com.ludei.multiplayer.LDPlayer();
        result.playerID = myID;
        Player person = Games.Players.getCurrentPlayer(client);
        if (person != null){
            result.playerAlias = person.getDisplayName();
        }
        result.avatarURL = "";
        return result;
    }

    @Override
    public String getLocalPlayerID() {
        return myID;
    }

    @Override
    public void requestPlayersInfo(PlayerRequestCompletion completion) {
        if (room == null) {
            completion.onComplete(null, new MultiplayerService.Error(1, "Null room"));
        }

        ArrayList<Participant> players = room.getParticipants();

        com.ludei.multiplayer.LDPlayer[] result = new com.ludei.multiplayer.LDPlayer[players.size()];
        for (int i = 0; i< players.size(); ++i) {
            com.ludei.multiplayer.LDPlayer player = new com.ludei.multiplayer.LDPlayer();
            player.playerID = players.get(i).getParticipantId();
            player.playerAlias = players.get(i).getDisplayName();
            player.avatarURL = players.get(i).getHiResImageUrl();
            result[i] = player;
        }

        completion.onComplete(result, null);
    }

    @Override
    public int expectedPlayerCount() {
        if (room == null)
            return 2;
        int connectedPlayers = 0;
        for (Participant p: room.getParticipants()) {
            if (p.isConnectedToRoom()) {
                connectedPlayers++;
            }
        }

        return Math.max(minPlayers - connectedPlayers,0);
    }

    @Override
    public MultiplayerService.Error sendData(byte[] data, String[] playerIDs, Connection mode) {
        if (room == null || disconnected) {
            return new MultiplayerService.Error(1, "Null room");
        }

        for (Participant p: room.getParticipants()) {
            String pid = p.getParticipantId();
            if (!pid.equals(myID) && p.isConnectedToRoom() && GPUtils.contains(playerIDs, pid)) {
                if (mode == Connection.RELIABLE) {
                    Games.RealTimeMultiplayer.sendReliableMessage(client, new RealTimeMultiplayer.ReliableMessageSentCallback() {
                        @Override
                        public void onRealTimeMessageSent(int i, int i2, String s) {

                        }
                    }, data, room.getRoomId(), pid);
                }
                else {
                    Games.RealTimeMultiplayer.sendUnreliableMessage(client, data, room.getRoomId(), pid);
                }
            }
        }

        if (GPUtils.contains(playerIDs, myID)) {
            this.notifyOnReceiveData(data, myID);
        }

        return null;
    }

    @Override
    public MultiplayerService.Error sendDataToAllPlayers(byte[] data, Connection mode) {
        if (room == null || disconnected) {
            return new MultiplayerService.Error(1, "Null room");
        }

        if (mode == Connection.RELIABLE) {
            for (Participant p: room.getParticipants()) {
                if (!p.getParticipantId().equals(myID) && p.isConnectedToRoom()) {
                    Games.RealTimeMultiplayer.sendReliableMessage(client, new RealTimeMultiplayer.ReliableMessageSentCallback() {
                        @Override
                        public void onRealTimeMessageSent(int i, int i2, String s) {

                        }
                    }, data, room.getRoomId(), p.getParticipantId());
                }
            }
        }
        else {
            Games.RealTimeMultiplayer.sendUnreliableMessageToOthers(client, data, room.getRoomId());
        }

                //send to localPlayer too
        this.notifyOnReceiveData(data, myID);

        return null;
    }

    @Override
    public void disconnect() {
        if (!disconnected) {
            disconnected = true;
            this._matchDelegate = null;
            if (room != null) {
                Games.RealTimeMultiplayer.leave(client, this, this.getRoomId());
            }
        }
    }

    public void setListener(GPGMatchRoomListener listener) {
        this.listener = listener;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public String getRoomId() {
        return room != null ? room.getRoomId() : null;
    }


    public void waitingUIDismissed(int response) {

        if (response != Activity.RESULT_OK && !waitingActivityFinished) {
            //Possible causes

            //Response == Activity.RESULT_CANCELED
            // Waiting room was dismissed with the back button. The meaning of this
            // action is up to the game. You may choose to leave the room and cancel the
            // match, or do something else like minimize the waiting room and
            // continue to connect in the background. We take the simple approach and just leave the room

            //Response = GamesActivityResultCodes.RESULT_LEFT_ROOM
            // player wants to leave the room.
            notifyOnMatchError(new MultiplayerService.Error(1, "User has left the room"));
            this.disconnect();
        }
    }

    public void cancelAutoMatch() {

        String roomId = this.getRoomId();
        if (roomId != null) {
            Games.RealTimeMultiplayer.leave(client, this, this.getRoomId());
            room = null;
        }
    }


    @Override
    public void onRealTimeMessageSent(int i, int i1, java.lang.String s) {


    }

    void updateRoom(Room room) {
        if (room != null)
            this.room = room;
    }

    //RoomUpdateListener

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (loadingRoomDialog != null) {
            loadingRoomDialog.dismiss();
            loadingRoomDialog = null;
        }
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            if (listener != null) {
                listener.roomCreated(this, new MultiplayerService.Error(statusCode, GPGMultiplayerMatch.codeToString(statusCode)));
            }
            return;
        }
        updateRoom(room);

        if (showWaitingUI) {
            Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(client, room, minPlayers);
            waitingActivityFinished = false;
            activity.startActivityForResult(intent, GPGMultiplayerService.REQUEST_WAITING_UI);
        }

        if (listener != null) {
            listener.roomCreated(this,null);
        }

    }
    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        this.onRoomCreated(statusCode,room);
    }

    @Override
    public void onLeftRoom(int statusCode, String s) {

    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        updateRoom(room);
        if (showWaitingUI) {
            waitingActivityFinished = true;
            activity.finishActivity(GPGMultiplayerService.REQUEST_WAITING_UI);
        }
        //All players connected!
    }


    //RealTimeMessageReceivedListener
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        if (disconnected)
            return;

        final String playerID = realTimeMessage.getSenderParticipantId();
        final byte[] data = realTimeMessage.getMessageData();
        notifyOnReceiveData(data, playerID);
    }


    //RoomStatusUpdateListener
    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onPeerDeclined(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> strings) {
        updateRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        updateRoom(room);
        myID = room.getParticipantId(Games.Players.getCurrentPlayer(client).getPlayerId());
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        updateRoom(room);
        if (disconnected)
            return;
        notifyOnMatchError(new MultiplayerService.Error(1, "Disconnected from room"));
    }

    @Override
    public void onPeersConnected(Room room, final List<String> strings) {
        updateRoom(room);
        if (disconnected)
            return;
        for (String playerID : strings) {
            notifyOnStateChange(playerID, State.CONNECTED);
        }
    }

    @Override
    public void onPeersDisconnected(Room room, final List<String> strings) {
        updateRoom(room);
        if (disconnected)
            return;
        for (String playerID : strings) {
            notifyOnStateChange(playerID, State.DISCONNECTED);
        }
    }

    @Override
    public void onP2PConnected(String s) {
        updateRoom(room);

    }

    @Override
    public void onP2PDisconnected(String s) {
        updateRoom(room);

    }

    public static String codeToString(int errorCode) {
        switch (errorCode) {
            case GamesStatusCodes.STATUS_ACHIEVEMENT_NOT_INCREMENTAL: return "The call to increment achievement failed since the achievement is not an incremental achievement.";
            case GamesStatusCodes.STATUS_ACHIEVEMENT_UNKNOWN: return "Could not find the achievement, so the operation to update the achievement failed.";
            case GamesStatusCodes.STATUS_ACHIEVEMENT_UNLOCKED: return "The incremental achievement was also unlocked when the call was made to increment the achievement.";
            case GamesStatusCodes.STATUS_ACHIEVEMENT_UNLOCK_FAILURE: return "An incremental achievement cannot be unlocked directly, so the call to unlock achievement failed.";
            case GamesStatusCodes.STATUS_APP_MISCONFIGURED: return "The developer has misconfigured their application in some way.";
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED: return "The GoogleApiClient is in an inconsistent state and must reconnect to the service to resolve the issue.";
            case GamesStatusCodes.STATUS_GAME_NOT_FOUND: return "The specified game ID was not recognized by the server.";
            case GamesStatusCodes.STATUS_INTERNAL_ERROR: return "An unspecified error occurred; no more specific information is available.";
            case GamesStatusCodes.STATUS_INTERRUPTED: return "Was interrupted while waiting for the result.";
            case GamesStatusCodes.STATUS_INVALID_REAL_TIME_ROOM_ID: return "Constant indicating that the real-time room ID provided to the operation was not valid, or does not correspond to the currently active real-time room.";
            case GamesStatusCodes.STATUS_LICENSE_CHECK_FAILED: return "The game is not licensed to the user.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED: return "The specified match has already had a rematch created.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH: return "The match is not currently active.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_INVALID_MATCH_RESULTS: return "The match results provided in this API call are invalid.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_INVALID_MATCH_STATE: return "The match is not in the correct state to perform the specified action.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_INVALID_PARTICIPANT_STATE: return "One or more participants in this match are not in valid states.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED: return "The specified match has already been modified locally.";
            case GamesStatusCodes.STATUS_MATCH_ERROR_OUT_OF_DATE_VERSION: return "The match data is out of date.";
            case GamesStatusCodes.STATUS_MATCH_NOT_FOUND: return "The specified match cannot be found.";
            case GamesStatusCodes.STATUS_MILESTONE_CLAIMED_PREVIOUSLY: return "This quest milestone was previously claimed (on this device or another).";
            case GamesStatusCodes.STATUS_MILESTONE_CLAIM_FAILED: return "This quest milestone is not available for claiming.";
            case GamesStatusCodes.STATUS_MULTIPLAYER_DISABLED: return "This game does not support multiplayer.";
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_CREATION_NOT_ALLOWED: return "The user is not allowed to create a new multiplayer game at this time.";
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_INVALID_MULTIPLAYER_TYPE: return "The match is not the right type to perform this action on.";
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_INVALID_OPERATION: return "This multiplayer operation is not valid, and the server rejected it.";
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER: return "The user attempted to invite another user who was not authorized to see the game.";
            case GamesStatusCodes.STATUS_NETWORK_ERROR_NO_DATA: return "A network error occurred while attempting to retrieve fresh data, and no data was available locally.";
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED: return "A network error occurred while attempting to modify data, but the data was successfully modified locally and will be updated on the network the next time the device is able to sync.";
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED: return "A network error occurred while attempting to perform an operation that requires network access.";
            case GamesStatusCodes.STATUS_NETWORK_ERROR_STALE_DATA: return "A network error occurred while attempting to retrieve fresh data, but some locally cached data was available.";
            case GamesStatusCodes.STATUS_OK: return "The operation was successful.";
            case GamesStatusCodes.STATUS_OPERATION_IN_FLIGHT: return "Trying to start a join/create operation while another is already in flight.";
            case GamesStatusCodes.STATUS_PARTICIPANT_NOT_CONNECTED: return "Constant indicating that the ID of the participant provided by the user is not currently connected to the client in the real-time room.";
            case GamesStatusCodes.STATUS_QUEST_NOT_STARTED: return "This quest is not available yet and cannot be accepted.";
            case GamesStatusCodes.STATUS_QUEST_NO_LONGER_AVAILABLE: return "This quest has expired or the developer has removed, and cannot be accepted.";
            case GamesStatusCodes.STATUS_REAL_TIME_CONNECTION_FAILED: return "Failed to initialize the network connection for a real-time room.";
            case GamesStatusCodes.STATUS_REAL_TIME_INACTIVE_ROOM: return "The room is not currently active.";
            case GamesStatusCodes.STATUS_REAL_TIME_MESSAGE_SEND_FAILED: return "Failed to send message to the peer participant for a real-time room.";
            case GamesStatusCodes.STATUS_REAL_TIME_ROOM_NOT_JOINED: return "Failed to send message to the peer participant for a real-time room, since the user has not joined the room.";
            case GamesStatusCodes.STATUS_REQUEST_TOO_MANY_RECIPIENTS: return "Sending request failed due to too many recipients.";
            case GamesStatusCodes.STATUS_REQUEST_UPDATE_PARTIAL_SUCCESS: return "Some of the batched network operations succeeded.";
            case GamesStatusCodes.STATUS_REQUEST_UPDATE_TOTAL_FAILURE: return "All of the request update operations attempted failed.";
            case GamesStatusCodes.STATUS_SNAPSHOT_COMMIT_FAILED: return "The attempt to commit the snapshot change failed.";
            case GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT: return "A conflict was detected for the snapshot.";
            case GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT_MISSING: return "The conflict that was being resolved doesn't exist.";
            case GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE: return "An error occurred while attempting to open the contents of the snapshot.";
            case GamesStatusCodes.STATUS_SNAPSHOT_CREATION_FAILED: return "The attempt to create a snapshot failed.";
            case GamesStatusCodes.STATUS_SNAPSHOT_FOLDER_UNAVAILABLE: return "The root folder for snapshots could not be found or created.";
            case GamesStatusCodes.STATUS_SNAPSHOT_NOT_FOUND: return "The specified snapshot does not exist on the server.";
            case GamesStatusCodes.STATUS_TIMEOUT: return "Timeout";
            default:
                return "Unknown error code " + errorCode;
        }
    }
}
