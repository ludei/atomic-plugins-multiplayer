package com.ludei.multiplayer.cordova;
import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.ludei.multiplayer.MatchRequest;
import com.ludei.multiplayer.MultiplayerMatch;
import com.ludei.multiplayer.MultiplayerService;
import com.ludei.multiplayer.Player;

public class MultiplayerPlugin extends CordovaPlugin implements MultiplayerService.Delegate, MultiplayerMatch.Delegate {

	protected MultiplayerService _service;
	private CallbackContext _serviceListener;
	private CallbackContext _matchListener;
    private HashMap<String, MultiplayerMatch> _matches = new HashMap<String, MultiplayerMatch>();
    private int _matchIndex = 0;
	
	protected void pluginInitialize() {
        this._service.setDelegate(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        this._service.onActivityResult(requestCode, resultCode, intent);

    }


	@Override
	public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

		try
		{
			Method method = this.getClass().getMethod(action, CordovaArgs.class, CallbackContext.class);
			method.invoke(this, args, callbackContext);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@SuppressWarnings("unused")
	public void setServiceListener(CordovaArgs args, CallbackContext ctx) throws JSONException {
		_serviceListener = ctx;
	}

	@SuppressWarnings("unused")
	public void setMatchListener(CordovaArgs args, CallbackContext ctx) throws JSONException {
		_matchListener = ctx;
	}

    private MultiplayerService.MatchCompletion createMatchCompletion(final CallbackContext ctx) {
        return new MultiplayerService.MatchCompletion() {
            @Override
            public void onComplete(MultiplayerMatch match, MultiplayerService.Error error) {
                if (match != null) {
                    String key = storeMatch(match);
                    ctx.success(matchToJson(match, key));
                }
                else if (error != null) {
                    ctx.error(errorToJson(error));
                }
                else {
                    ctx.error((String)null);
                }

            }
        };
    }

    //Multiplayer Service API

    @SuppressWarnings("unused")
    public void findMatch(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        MatchRequest request = parseMatchRequest(args.optJSONObject(0));
        cordova.setActivityResultCallback(this);
        _service.findMatch(request, this.createMatchCompletion(ctx));
    }

    @SuppressWarnings("unused")
    public void findAutoMatch(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        MatchRequest request = parseMatchRequest(args.optJSONObject(0));
        _service.findAutoMatch(request, this.createMatchCompletion(ctx));
    }

    @SuppressWarnings("unused")
    public void cancelAutoMatch(CordovaArgs args, final CallbackContext ctx) {
        _service.cancelAutoMatch();
        ctx.success();
    }

    @SuppressWarnings("unused")
    public void addPlayersToMatch(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }

        MatchRequest request = parseMatchRequest(args.getJSONObject(1));
        _service.addPlayersToMatch(match, request, new MultiplayerService.Completion() {
            @Override
            public void onComplete(MultiplayerService.Error error) {
                if (error != null) {
                    ctx.error(errorToJson(error));
                } else {
                    ctx.success((String) null);
                }
            }
        });
    }

    //Match API
    @SuppressWarnings("unused")
    public void start(CordovaArgs args, final CallbackContext ctx)  {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }
        match.start();
        ctx.success();
    }

    @SuppressWarnings("unused")
    public void sendDataToAllPlayers(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }

        String msg = args.getString(1);
        byte[] data;
        try
        {
            data = msg.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            ctx.error(errorToJson(new MultiplayerService.Error(0, e.getLocalizedMessage())));
            return;
        }

        int value = args.optInt(2);
        MultiplayerMatch.Connection mode = value == 0 ? MultiplayerMatch.Connection.RELIABLE : MultiplayerMatch.Connection.UNRELIABLE;

        MultiplayerService.Error error = match.sendDataToAllPlayers(data, mode);
        if (error == null) {
            ctx.success();
        }
        else {
            ctx.error(errorToJson(error));
        }
    }

    @SuppressWarnings("unused")
    public void sendData(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }

        String msg = args.getString(1);
        byte[] data;
        try
        {
            data = msg.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            ctx.error(errorToJson(new MultiplayerService.Error(0, e.getLocalizedMessage())));
            return;
        }

        JSONArray array = args.getJSONArray(1);

        String[] playerIDs = new String[array.length()];
        for (int i = 0; i < array.length(); ++i) {
            playerIDs[i] = array.get(i).toString();
        }

        int value = args.optInt(3);
        MultiplayerMatch.Connection mode = value == 0 ? MultiplayerMatch.Connection.RELIABLE : MultiplayerMatch.Connection.UNRELIABLE;


        MultiplayerService.Error error = match.sendData(data, playerIDs, mode);
        if (error == null) {
            ctx.success();
        }
        else {
            ctx.error(errorToJson(error));
        }
    }

    @SuppressWarnings("unused")
    public void disconnect(CordovaArgs args, final CallbackContext ctx)  {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }
        match.disconnect();
        match.setDelegate(null);
        String key = keyFromMatch(match);
        if (key != null) {
            _matches.remove(key);
        }
        ctx.success();
    }

    @SuppressWarnings("unused")
    public void requestPlayersInfo(CordovaArgs args, final CallbackContext ctx)  {
        final MultiplayerMatch match = matchFromArgs(args);
        if (match == null) {
            ctx.error(errorToJson(new MultiplayerService.Error(0, "Match not found")));
            return;
        }
        match.requestPlayersInfo(new MultiplayerMatch.PlayerRequestCompletion() {
            @Override
            public void onComplete(Player[] players, MultiplayerService.Error error) {
                if (error != null) {
                    ctx.error(errorToJson(error));
                } else {
                    JSONArray array = new JSONArray();
                    if (players != null) {
                        for (Player player : players) {
                            array.put(playerToJson(player));
                        }
                    }
                    ctx.success(array);
                }
            }
        });
    }


    //MultiplayerService Delegate
    @Override
    public void onInvitationReceive(MultiplayerService sender) {
        cordova.setActivityResultCallback(this);
        if (_serviceListener != null) {
            JSONArray array = new JSONArray();
            array.put("invitationReceived");
            PluginResult result = new PluginResult(Status.OK, array);
            result.setKeepCallback(true);
            _serviceListener.sendPluginResult(result);
        }

    }

    @Override
    public void onInvitationLoad(MultiplayerService service, MultiplayerMatch match, MultiplayerService.Error error) {
        if (_serviceListener != null) {
            JSONArray array = new JSONArray();
            array.put("invitationLoaded");
            Status st = Status.OK;
            if (match != null) {
                String key = storeMatch(match);
                array.put(matchToJson(match, key));
            }
            else if (error != null) {
                st = Status.ERROR;
                array.put(errorToJson(error));
            }
            else {
                st = Status.ERROR;
            }

            PluginResult result = new PluginResult(st, array);
            result.setKeepCallback(true);
            _serviceListener.sendPluginResult(result);
        }
    }

    //MultiplayerMatchDelegate

    @Override
    public void onReceivedData(MultiplayerMatch match, byte[] data, String fromPlayer) {

        try
        {
            String msg = new String(data, "UTF-8");
            this.notifyMatchEvent(match, new Object[]{"dataReceived", msg, fromPlayer});
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onStateChange(MultiplayerMatch match, String playerID, MultiplayerMatch.State state) {

        int st = 0;
        if (state == MultiplayerMatch.State.CONNECTED) {
            st = 1;
        }
        else if (state == MultiplayerMatch.State.DISCONNECTED) {
            st = 2;
        }
        String key = keyFromMatch(match);
        this.notifyMatchEvent(match, new Object[]{"stateChanged", playerID, st, matchToJson(match, key)});
    }

    @Override
    public void onPlayerConnectionError(MultiplayerMatch match, String playerID, MultiplayerService.Error error) {
        this.notifyMatchEvent(match, new Object[]{"connectionWithPlayerFailed", playerID, errorToJson(error)});
    }

    @Override
    public void onMatchError(MultiplayerMatch match, MultiplayerService.Error error) {
        this.notifyMatchEvent(match, new Object[]{"failed", errorToJson(error)});
    }


    //Utilities

    private void notifyMatchEvent(MultiplayerMatch match, Object[] args)
    {
        if (_matchListener != null) {
            String key = keyFromMatch(match);
            if (key != null) {
                JSONArray array = new JSONArray();
                array.put(key);
                for (Object value: args) {
                    array.put(value);
                }
                PluginResult result = new PluginResult(Status.OK, array);
                result.setKeepCallback(true);
                _matchListener.sendPluginResult(result);
            }

        }
    }

    private MultiplayerMatch matchFromArgs(CordovaArgs args) {
        String key = args.optString(0);
        if (key != null) {
            return _matches.get(key);
        }
        return null;
    }

    private String storeMatch(MultiplayerMatch match)
    {
        match.setDelegate(this);
        _matchIndex++;
        String key = String.valueOf(_matchIndex);
        _matches.put(key, match);
        return key;
    }

    private String keyFromMatch(MultiplayerMatch match)
    {
        for (String key: _matches.keySet()) {
            if (_matches.get(key) == match) {
                return key;
            }
        }
        return null;
    }

    private JSONObject errorToJson(MultiplayerService.Error error)  {
        if (error == null) {
            return  null;
        }
        JSONObject result = new JSONObject();
        try {
            result.put("message", error.message);
            result.put("code", error.code);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return  result;
    }

    private JSONObject matchToJson(MultiplayerMatch match, String key) {

        JSONObject result = new JSONObject();
        try
        {
            result.put("key", key);
            result.put("expectedPlayerCount", match.expectedPlayerCount());
            JSONArray array = new JSONArray();
            for (String pid: match.getPlayerIDs()) {
                array.put(pid);
            }
            result.put("playerIDs", array);
            result.put("localPlayerID", match.getLocalPlayerID());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return result;
    }

    private JSONObject playerToJson(Player player) {
        JSONObject result = new JSONObject();
        try
        {
            result.put("playerID", player.playerID);
            result.put("alias", player.playerAlias);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return result;
    }

    private MatchRequest parseMatchRequest(JSONObject dic) throws JSONException{
        MatchRequest request = new MatchRequest();
        if (dic != null) {
            request.minPlayers = dic.optInt("minPlayers", 2);
            request.maxPlayers = dic.optInt("maxPlayers", request.minPlayers);
            request.playerGroup = dic.optInt("playerGroup", 0);
            request.playerAttributes = dic.optInt("playerAttributes", 0);

            JSONArray array = dic.optJSONArray("playersToInvite");
            if (array != null) {
                String[] invitees = new String[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    invitees[i] = array.get(i).toString();
                }
                request.playersToInvite = invitees;
            }
        }

        return request;
    }
}