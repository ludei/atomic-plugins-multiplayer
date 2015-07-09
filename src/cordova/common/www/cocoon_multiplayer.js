!function(){!window.Cocoon&&window.cordova&&"undefined"!=typeof require&&require("cocoon-plugin-common.Cocoon");var t=window.Cocoon;t.define("Cocoon.Multiplayer",function(e){return e.MultiplayerService=function(e){this.serviceName=e,this.serviceSignal=new t.Signal,this.on=this.serviceSignal.expose(),this.matches={},this.currentMatch=null,document.addEventListener("deviceready",function(){this.init()}.bind(this))},e.MultiplayerService.prototype={init:function(){var e=this;t.exec(e.serviceName,"setServiceListener",[],function(t){var n=t[0];if("invitationReceived"===n)e.serviceSignal.emit("invitation","received");else if("invitationLoaded"===n){var i=t[1];e.serviceSignal.emit("invitation","loaded",[e._createMatch(i)])}},function(t,n){e.serviceSignal.emit("invitation","loaded",[null,n])}),t.exec(e.serviceName,"setMatchListener",[],function(t){var n=t[0],i=t[1],a=e.matches[n];if(a){var c;if("dataReceived"===i){var s=t[2];c=t[3],a.signal.emit("match",i,[a,s,c])}else if("stateChanged"===i){c=t[2];var r=t[3];a.matchData=t[4],a.signal.emit("match",i,[a,c,r])}else a.signal.emit("match",i,[a,t[2],t[3]])}})},_createMatch:function(e){var n=new t.Multiplayer.Match(this.serviceName,e);return this.matches[e.key]=n,this.currentMatch=n,n},findMatch:function(e,n){var i=this;n=n||function(){},t.exec(this.serviceName,"findMatch",[e],function(t){n(i._createMatch(t))},function(t){n(null,t)})},findAutoMatch:function(e,n){var i=this;n=n||function(){},t.exec(this.serviceName,"findAutoMatch",[e],function(t){n(i._createMatch(t))},function(t){n(null,t)})},cancelAutoMatch:function(){t.exec(this.serviceName,"cancelAutoMatch")},addPlayersToMatch:function(e,n,i){i=i||function(){},t.exec(this.serviceName,"addPlayersToMatch",[n.key,e],function(){i(null)},function(t){i(t)})},getMatch:function(){return this.currentMatch}},e.Match=function(e,n){this.serviceName=e,this.matchData=n,this.signal=new t.Signal,this.on=this.signal.expose(),this.on=this.signal.expose()},e.Match.prototype={start:function(){t.exec(this.serviceName,"start",[this.matchData.key])},sendDataToAllPlayers:function(e,n){t.exec(this.serviceName,"sendDataToAllPlayers",[this.matchData.key,e,n])},sendData:function(e,n,i){t.exec(this.serviceName,"sendData",[this.matchData.key,e,n,i])},disconnect:function(){t.exec(this.serviceName,"disconnect",[this.matchData.key])},requestPlayersInfo:function(e){t.exec(this.serviceName,"requestPlayersInfo",[this.matchData.key],function(n){for(var i=[],a=0;a<n.length;++a){var c=n[a];i.push(new t.Multiplayer.PlayerInfo(c.playerID,c.alias))}e(i,null)},function(t){e(null,t)})},getExpectedPlayerCount:function(){return this.matchData.expectedPlayerCount},getPlayerIDs:function(){return this.matchData.playerIDs},getLocalPlayerID:function(){return this.matchData.localPlayerID}},e.SendDataMode={RELIABLE:0,UNRELIABLE:1},e.ConnectionState={UNKNOWN:0,CONNECTED:1,DISCONNECTED:2},e.PlayerInfo=function(t,e){this.userID=t,this.userName=e},e.MatchRequest=function(t,e,n,i,a){return this.minPlayers=t||2,this.maxPlayers=e||this.minPlayers,this.playersToInvite=n,this.playerGroup=i,this.playerAttributes=a,this},e})}(),function(){Cocoon.define("Cocoon.Multiplayer",function(t){var e=[],n=0,i=[],a=0;t.LoopbackService=function(){e.push(this),this.playerID=""+n,n++,this.signal=new Cocoon.Signal,this.on=this.signal.expose()},t.LoopbackService.prototype={findMatch:function(t,e){this.findMatchCallback=e;for(var n=!1,a=0;a<i.length;++a)if(i[a]===this){n=!0;break}if(n||i.push(this),i.length>=t.minPlayers){var s=[];for(a=0;a<i.length;++a)s.push(i[a].getPlayerID());for(a=0;a<i.length;++a){var r=new c(i[a]);r.playerIDs=s.slice(),i[a].currentMatch=r,i[a].findMatchCallback(r,null)}i=[]}},findAutoMatch:function(t,e){this.findMatch(t,e)},cancelAutoMatch:function(){},addPlayersToMatch:function(t,e,n){n({message:"Not implemmented"})},getPlayerID:function(){return this.playerID},getMatch:function(){return this.currentMatch}};var c=function(t){a++,this.started=!1,this.disconnected=!1,this.pendingData=[],this.service=t,this.signal=new Cocoon.Signal,this.on=this.signal.expose()};return c.prototype={start:function(){var t=this;setTimeout(function(){t.started=!0;for(var e=0;e<t.pendingData.length;++e)t.signal.emit("match","dataReceived",[t,t.pendingData[e].data,t.pendingData[e].player]);t.pendingData=[]},0)},sendDataToAllPlayers:function(t,e){this.sendData(t,this.playerIDs,e)},sendData:function(t,n,i){var a=this;setTimeout(function(){for(var i=0;i<e.length;++i){for(var c=null,s=0;s<n.length;++s)n[s]===e[i].getPlayerID()&&(c=e[i]);c&&c.getMatch().notifyDataReceived(t,a.service.getPlayerID())}},0)},disconnect:function(){this.disconnected=!0;for(var t=0;t<this.playerIDs.length;++t)for(var n=this.playerIDs[t],i=0;i<e.length;++i)if(e[i].getPlayerID()===n){var a=e[t].getMatch();a.disconnected||a.signal.emit("match","stateChanged",[a,this.service.getPlayerID(),Cocoon.Multiplayer.ConnectionState.DISCONNECTED])}},requestPlayersInfo:function(t){var e=this;setTimeout(function(){for(var n=[],i=0;i<e.playerIDs.length;++i)n[i]={userID:e.playerIDs[i],userName:"Player"+e.playerIDs[i]};t(n)},1)},getExpectedPlayerCount:function(){return 0},getPlayerIDs:function(){return this.playerIDs},getLocalPlayerID:function(){return this.service.playerID},notifyDataReceived:function(t,e){this.disconnected||(this.started?this.signal.emit("match","dataReceived",[this,t,e]):this.pendingData.push({data:t,player:e}))}},t})}();