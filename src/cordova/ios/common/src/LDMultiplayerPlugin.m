#import "LDMultiplayerPlugin.h"


static NSDictionary * playerToDictionary(LDMultiplayerPlayer * player)
{
    return @{
             @"playerID": player.playerID,
             @"alias": player.playerAlias,
             };
}

static NSDictionary * matchToDictionary(LDMultiplayerMatch * match, NSString * key)
{
    return @{
        @"key": key,
        @"expectedPlayerCount": [NSNumber numberWithUnsignedInteger:match.expectedPlayerCount],
        @"playerIDs": match.playerIDs,
        @"localPlayerID": match.localPlayer.playerID
        };
}

static LDMultiplayerMatchRequest * toMatchRequest(NSDictionary * dic)
{
    LDMultiplayerMatchRequest * result = [[LDMultiplayerMatchRequest alloc] init];
    NSNumber * minPlayers = [dic objectForKey:@"minPlayers"];
    NSNumber * maxPlayers = [dic objectForKey:@"maxPlayers"];
    NSArray * playersToInvite = [dic objectForKey:@"playersToInvite"];
    NSNumber * playerGroup = [dic objectForKey:@"playerGroup"];
    NSNumber * playerAttributes = [dic objectForKey:@"playerAttributes"];
    
    result.minPlayers = 2;
    result.maxPlayers = 2;
    
    if (minPlayers && [minPlayers isKindOfClass:[NSNumber class]]) {
        result.minPlayers = minPlayers.integerValue;
    }
    if (maxPlayers && [maxPlayers isKindOfClass:[NSNumber class]]) {
        result.maxPlayers = maxPlayers.integerValue;
    }
    if (playersToInvite && [playersToInvite isKindOfClass:[NSArray class]]) {
        result.playersToInvite = playersToInvite;
    }
    if (playerGroup && [playerGroup isKindOfClass:[NSNumber class]]) {
        result.playerGroup = playerGroup.unsignedIntValue;
    }
    if (playerAttributes && [playerAttributes isKindOfClass:[NSNumber class]]) {
        result.playerAttributes = playerAttributes.unsignedIntValue;
    }

    return result;
}

static NSDictionary * errorToDic(NSError * error)
{
    return @{@"code":[NSNumber numberWithInteger:error.code], @"message":error.localizedDescription};
}
static NSDictionary * toError(NSString * message)
{
    return @{@"code":[NSNumber numberWithInteger:0], @"message":message};
}


@implementation LDMultiplayerPlugin
{
    NSString * _serviceListenerCallbackId;
    NSString * _matchListenerCallbackId;
    NSInteger _matchIndex;
    NSMutableDictionary * _matches;
}

- (void)pluginInitialize
{
    _matches = [NSMutableDictionary dictionary];
    _matchIndex = 0;
}

#pragma mark JavaScript Service API

-(void) setServiceListener:(CDVInvokedUrlCommand*) command
{
    _serviceListenerCallbackId = command.callbackId;
}

-(void) setMatchListener:(CDVInvokedUrlCommand*) command
{
    _matchListenerCallbackId = command.callbackId;
}

-(void) findMatch:(CDVInvokedUrlCommand*) command
{
    NSDictionary * data = [command argumentAtIndex:0 withDefault:nil andClass:[NSDictionary class]];
    if (!data) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Invalid argument")] callbackId:command.callbackId];
        return;
    }
    
    LDMultiplayerMatchRequest * request = toMatchRequest(data);
    [_service findMatch:request fromViewController:self.viewController completion:^(LDMultiplayerMatch * match, NSError *error) {
        CDVPluginResult * result;
        if (match) {
            NSString * matchKey = [self storeMatch:match];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:matchToDictionary(match, matchKey)];
        }
        else if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
        }
        else {
            //cancelled
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void) findAutoMatch:(CDVInvokedUrlCommand*) command
{
    NSDictionary * data = [command argumentAtIndex:0 withDefault:nil andClass:[NSDictionary class]];
    if (!data) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Invalid argument")] callbackId:command.callbackId];
        return;
    }
    
    LDMultiplayerMatchRequest * request = toMatchRequest(data);
    [_service findAutoMatch:request completion:^(LDMultiplayerMatch *match, NSError *error) {
        CDVPluginResult * result;
        if (match) {
            NSString * matchKey = [self storeMatch:match];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:matchToDictionary(match, matchKey)];
        }
        else if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
        }
        else {
            //cancelled
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void) cancelAutoMatch:(CDVInvokedUrlCommand*) command
{
    [_service cancelAutoMatch];
    [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
}

-(void) addPlayersToMatch:(CDVInvokedUrlCommand*) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    NSDictionary * data = [command argumentAtIndex:1 withDefault:nil andClass:[NSDictionary class]];
    if (!data) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Invalid argument request")] callbackId:command.callbackId];
        return;
    }
     LDMultiplayerMatchRequest * request = toMatchRequest(data);
    
    __weak LDMultiplayerPlugin * weakSelf = self;
    [_service addPlayersToMatch:match request:request completion:^(NSError *error) {
        CDVPluginResult * result = nil;
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
        }
        else {

            NSString * key = [command argumentAtIndex:0];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:matchToDictionary(match, key)];
        }
        [weakSelf.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

#pragma mark JavaScript Match API

-(void) start:(CDVInvokedUrlCommand*) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    [match start];
    [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
}

-(void) sendDataToAllPlayers:(CDVInvokedUrlCommand*) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    
    NSString * message = [command argumentAtIndex:1 withDefault:@"" andClass:[NSString class]];
    NSNumber * sendMode = [command argumentAtIndex:2 withDefault:@0 andClass:[NSNumber class]];
    
    NSData * data = [message dataUsingEncoding:NSUTF8StringEncoding];
    LDMultiplayerConnection mode = sendMode.intValue == 0 ? LDMultiplayerConnectionReliable : LDMultiplayerConnectionUnreliable;
    
    NSError * error = nil;
    BOOL sent = [match sendDataToAllPlayers:data mode:mode error:&error];
    if (sent) {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    else if (error) {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
    }
    else {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
}

-(void) sendData:(CDVInvokedUrlCommand*) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    
    NSString * message = [command argumentAtIndex:1 withDefault:@"" andClass:[NSString class]];
    NSArray * players = [command argumentAtIndex:2 withDefault:@[] andClass:[NSArray class]];
    NSNumber * sendMode = [command argumentAtIndex:3 withDefault:@0 andClass:[NSNumber class]];
    
    NSData * data = [message dataUsingEncoding:NSUTF8StringEncoding];
    LDMultiplayerConnection mode = sendMode.intValue == 0 ? LDMultiplayerConnectionReliable : LDMultiplayerConnectionUnreliable;
    
    NSError * error = nil;
    BOOL sent = [match sendData:data playerIDs:players mode:mode error:&error];
    if (sent) {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    else if (error) {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
    }
    else {
        [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }
}

-(void) disconnect:(CDVInvokedUrlCommand*) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    [match disconnect];
    [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
}

-(void) requestPlayersInfo:(CDVInvokedUrlCommand *) command
{
    LDMultiplayerMatch * match = [self matchFromCommand:command];
    if (!match) {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:toError(@"Match not found")] callbackId:command.callbackId];
        return;
    }
    [match requestPlayersInfo:^(NSArray *players, NSError *error) {
        CDVPluginResult * result;
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:errorToDic(error)];
        }
        else {
            NSMutableArray * array = [NSMutableArray array];
            for (LDMultiplayerPlayer * player in players) {
                [array addObject:playerToDictionary(player)];
            }
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:array];
        }
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
    
}

#pragma mark Helpers

-(void) notifyMatchEvent:(LDMultiplayerMatch *) match arguments:(NSArray *) arguments
{
    if (_matchListenerCallbackId) {
        NSString * key = [self keyForMatch:match];
        if (key) {
            NSMutableArray * array = [NSMutableArray array];
            [array addObject:key];
            [array addObjectsFromArray:arguments];
            CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:array];
            [result setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:result callbackId:_matchListenerCallbackId];
        }
    }
}

-(NSString *) storeMatch:(LDMultiplayerMatch*) match
{
    match.delegate = self;
    _matchIndex++;
    NSString * key = [NSString stringWithFormat:@"%ld", (long)_matchIndex];
    [_matches setObject:match forKey:key];
    return key;
}

-(LDMultiplayerMatch *) matchFromCommand:(CDVInvokedUrlCommand*) command
{
    NSString * key = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    LDMultiplayerMatch * result = nil;
    if (key) {
        result = [_matches objectForKey:key];
    }
    return result;
}

-(NSString *) keyForMatch:(LDMultiplayerMatch *) match
{
    for (NSString * key in _matches.keyEnumerator) {
        if ([_matches objectForKey:key] == match) {
            return key;
        }
    }
    return nil;
}

#pragma mark LDMultiplayerMatchDelegate
-(void) multiplayerMatch:(LDMultiplayerMatch *)match didReceiveData:(NSData *)data fromPlayer:(NSString *)playerID
{
    NSString * msg = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] ?: @"";
    [self notifyMatchEvent:match arguments:@[@"dataReceived", msg, playerID]];
}

-(void) multiplayerMatch:(LDMultiplayerMatch *)match player:(NSString *)playerID didChangeState:(LDMultiplayerState)state
{
    NSInteger value = 0;
    if (state == LDMultiplayerStateConnected) {
        value = 1;
    }
    else if (state == LDMultiplayerStateDisconnected) {
        value = 2;
    }
    NSString * key = [self keyForMatch:match];
    if (key) {
        [self notifyMatchEvent:match arguments:@[@"stateChanged", playerID, [NSNumber numberWithInteger:value], matchToDictionary(match, key)]];
    }
    
}

-(void) multiplayerMatch:(LDMultiplayerMatch *)match connectionWithPlayerFailed:(NSString *)playerID withError:(NSError *)error;
{
    [self notifyMatchEvent:match arguments:@[@"connectionWithPlayerFailed", playerID, errorToDic(error)]];
}

-(void) multiplayerMatch:(LDMultiplayerMatch *)match didFailWithError:(NSError *)error
{
    [self notifyMatchEvent:match arguments:@[@"failed", errorToDic(error)]];
}

#pragma mark LDMultiplayerServiceDelegate

-(UIViewController *) multiplayerDidReceiveInvitation:(LDMultiplayerService *) service
{
    if (_matchListenerCallbackId) {
        CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:@[@"invitationReceived"]];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:_serviceListenerCallbackId];
    }
    
    return self.viewController;
}

-(void) multiplayerDidLoadInvitation:(LDMultiplayerService *) service  match:(LDMultiplayerMatch *) match error:(NSError *) error
{
    NSString * msg = @"invitationLoaded";
    if (_serviceListenerCallbackId) {
        CDVPluginResult * result;
        if (match) {
            NSString * matchKey = [self storeMatch:match];
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:@[msg, matchToDictionary(match, matchKey)]];
        }
        else if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:@[msg, errorToDic(error)]];
        }
        else {
            //match cancelled
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsArray:@[msg]];
        }
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:_serviceListenerCallbackId];
    }
}

#pragma mark LDMultiplayerMatchDelegate

@end
