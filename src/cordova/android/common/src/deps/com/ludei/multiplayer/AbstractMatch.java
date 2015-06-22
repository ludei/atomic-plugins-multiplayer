package com.ludei.multiplayer;


import java.util.ArrayList;

public abstract class AbstractMatch implements MultiplayerMatch {

    protected MultiplayerMatch.Delegate _matchDelegate;
    protected ArrayList<Runnable> _pendingNotifications = new ArrayList<Runnable>();
    protected boolean _started;


    public void setDelegate(MultiplayerMatch.Delegate delegate)
    {
        this._matchDelegate = delegate;
    }

    protected void notifyOnReceiveData(final byte[] data, final String fromPlayerID) {
        if (_started) {
            if (_matchDelegate != null) {
                _matchDelegate.onReceivedData(this, data, fromPlayerID);
            }
        }
        else {
            _pendingNotifications.add(new Runnable() {
                @Override
                public void run() {
                    AbstractMatch.this.notifyOnReceiveData(data, fromPlayerID);
                }
            });
        }
    }

    protected void notifyOnStateChange(String playerID, State state)
    {
        if (_matchDelegate != null) {
            _matchDelegate.onStateChange(this, playerID, state);
        }
    }

    protected void notifyOnPlayerError(String playerID, MultiplayerService.Error error)
    {
        if (_matchDelegate != null) {
            _matchDelegate.onPlayerConnectionError(this, playerID, error);
        }
    }

    protected void notifyOnMatchError(MultiplayerService.Error error)
    {
        if (_matchDelegate != null) {
            _matchDelegate.onMatchError(this, error);
        }
    }

    public void start()
    {
        if (!_started) {
            _started = true;
            for (Runnable runnable: _pendingNotifications) {
                runnable.run();
            }
            _pendingNotifications.clear();
        }
    }


}
