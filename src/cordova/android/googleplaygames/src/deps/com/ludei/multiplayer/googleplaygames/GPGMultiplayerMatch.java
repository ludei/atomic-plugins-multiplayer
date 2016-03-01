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
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.ludei.googleplaygames.GPUtils;
import com.ludei.multiplayer.AbstractMatch;
import com.ludei.multiplayer.MultiplayerService;
import com.ludei.multiplayer.Player;

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
    public com.ludei.multiplayer.Player getLocalPlayer() {
        com.ludei.multiplayer.Player result = new com.ludei.multiplayer.Player();
        result.playerID = myID;
        Person person = Plus.PeopleApi.getCurrentPerson(client);
        if (person != null){
            result.playerAlias = person.getNickname();
            if (result.playerAlias == null || result.playerAlias.length() == 0) {
                result.playerAlias = person.getDisplayName();
            }
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

        Player[] result = new Player[players.size()];
        for (int i = 0; i< players.size(); ++i) {
            Player player = new Player();
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
        if (room == null) {
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
        if (room == null) {
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
        disconnected = true;
        this._matchDelegate = null;
        if (room != null) {
            Games.RealTimeMultiplayer.leave(client, this, this.getRoomId());
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

        if (response == Activity.RESULT_OK || waitingActivityFinished) {
            // start the game when the activity result is ok or when we have willfully dismissed the activity on room creation
            return;
        }

        boolean leave = false;

        if (response == Activity.RESULT_CANCELED) {
            // Waiting room was dismissed with the back button. The meaning of this
            // action is up to the game. You may choose to leave the room and cancel the
            // match, or do something else like minimize the waiting room and
            // continue to connect in the background.

            // we take the simple approach and just leave the room:
            leave = true;
        }
        else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
            // player wants to leave the room.
            leave = true;
        }

        if (leave) {
            String roomId = this.getRoomId();
            if (roomId != null) {
                Games.RealTimeMultiplayer.leave(client, this, this.getRoomId());
            }

            notifyOnMatchError(new MultiplayerService.Error(1, "User has left the room"));
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
                listener.roomCreated(this, new MultiplayerService.Error(statusCode, GPUtils.errorCodeToString(statusCode)));
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
        myID = room.getParticipantId(Plus.PeopleApi.getCurrentPerson(client).getId());
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
}
