package com.ludei.multiplayer.cordova;

import com.ludei.multiplayer.googleplaygames.GPGMultiplayerService;

public class GPGMultiplayerPlugin extends MultiplayerPlugin  {

    protected void pluginInitialize() {

        _service = new GPGMultiplayerService(cordova.getActivity());
        super.pluginInitialize();

    }

}