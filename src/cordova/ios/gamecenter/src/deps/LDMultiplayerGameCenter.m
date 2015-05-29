#import "LDMultiplayerGameCenter.h"

static GKMatchRequest * toGKMatchRequest(LDMultiplayerMatchRequest * request)
{
    GKMatchRequest* gkRequest = [[GKMatchRequest alloc] init];
    gkRequest.minPlayers = request.minPlayers;
    gkRequest.maxPlayers = request.maxPlayers;
    if (request.playerAttributes) {
        gkRequest.playerAttributes = request.playerAttributes;
    }
    else if (request.playerGroup) {
        gkRequest.playerGroup = request.playerGroup;
    }
    if (request.playersToInvite && request.playersToInvite.count) {
        gkRequest.playersToInvite = request.playersToInvite;
    }
    return gkRequest;
}


#pragma mark Multiplayer Service

@implementation LDMultiplayerGameCenter
{
    BOOL _invitation;
    void (^_matchCallback)(LDMultiplayerMatch * match, NSError * error);
    LDMultiplayerMatch * _match;
}


-(instancetype) init
{
    if (self = [super init]) {
        // IMPORTANT NOTE:
        // The authentication and invitation handler should be set as soon as possible after the application launch as stated in the Apple official Gamekit developer doc.
        // This code is called right after the Gamekit service initialization, so it depends on when the service is initiallized that the invite handler is correctly set.
        [GKMatchmaker sharedMatchmaker].inviteHandler = ^(GKInvite *acceptedInvite, NSArray *playersToInvite) {
            [self invitationReceived:acceptedInvite playersToInvite:playersToInvite];
        };

    }
    
    return self;
}

-(void) invitationReceived:(GKInvite *) acceptedInvite playersToInvite:(NSArray *) playersToInvite
{
    UIViewController * controller = nil;
    if (_delegate && [_delegate respondsToSelector:@selector(multiplayerDidReceiveInvitation:)]) {
        controller = [_delegate multiplayerDidReceiveInvitation: self];
    }
    
    // "Your application receives an invitation while your game is running, it should clean up any existing gameplay
    // (including disconnecting from any current matches) and then process the invitation."
    if (acceptedInvite)
    {
        GKMatchmakerViewController *matchmakerViewController = [[GKMatchmakerViewController alloc] initWithInvite:acceptedInvite];
        _invitation = YES;
        matchmakerViewController.matchmakerDelegate = self;
        
        // Notify the invitation from other player to the delegate
        [controller presentViewController:matchmakerViewController animated:YES completion:nil];
    }
    else if (playersToInvite)
    {
        GKMatchRequest *request = [[GKMatchRequest alloc] init];
        request.minPlayers = [playersToInvite count];
        request.maxPlayers = [playersToInvite count];
        request.playersToInvite = playersToInvite;
        
        
        GKMatchmakerViewController *matchmakerViewController = [[GKMatchmakerViewController alloc] initWithMatchRequest:request];
        _invitation = YES;
        matchmakerViewController.matchmakerDelegate = self;
        
        // Notify the invitation from the Game Center to the delegate
        [controller presentViewController:matchmakerViewController animated:YES completion:nil];
    }

}

-(void) findMatch:(LDMultiplayerMatchRequest*) request fromViewController:(UIViewController *) controller completion:(void(^)(LDMultiplayerMatch * match, NSError * error)) completion
{
    _matchCallback = completion;
    _invitation = NO;
    GKMatchRequest* gkRequest = toGKMatchRequest(request);
    GKMatchmakerViewController * vc = [[GKMatchmakerViewController alloc] initWithMatchRequest:gkRequest];
    vc.matchmakerDelegate = self;
    [controller presentViewController:vc animated:YES completion:nil];
}

-(void) findAutoMatch:(LDMultiplayerMatchRequest*) request completion:(void(^)(LDMultiplayerMatch * match, NSError * error)) completion
{
    GKMatchRequest* gkRequest = toGKMatchRequest(request);
    [[GKMatchmaker sharedMatchmaker] findMatchForRequest:gkRequest withCompletionHandler:^(GKMatch * foundMatch, NSError *error) {
        if (error || !foundMatch)
        {
            completion(nil, error.code == GKErrorCancelled ? nil: error);
        }
        else
        {
            completion([[LDGameCenterMatch alloc] initWithGKMatch:foundMatch], nil);
        }
        
    } ];

}

-(void) cancelAutoMatch
{
    [[GKMatchmaker sharedMatchmaker] cancel];
}

-(void) addPlayersToMatch:(LDMultiplayerMatch* ) match request:(LDMultiplayerMatchRequest *) request completion:(void(^)(NSError* error)) completion
{
    GKMatch * gkMatch = ((LDGameCenterMatch*)match).match;
    GKMatchRequest * gkRequest = toGKMatchRequest(request);
    [[GKMatchmaker sharedMatchmaker] addPlayersToMatch:gkMatch matchRequest:gkRequest completionHandler:^(NSError *error) {
        if (completion) {
            completion(error);
        }
    }];
}

-(LDMultiplayerMatch*) currentMatch
{
    return _match;
}


#pragma mark GKMatchmakerViewControllerDelegate

-(void) matchmakerViewControllerWasCancelled:(GKMatchmakerViewController *)controller
{
    [controller dismissViewControllerAnimated:YES completion:nil];
    if (_invitation && _delegate && [_delegate respondsToSelector:@selector(multiplayerDidLoadInvitation:match:error:)]) {
        [_delegate multiplayerDidLoadInvitation:self match:nil error:nil];
    }
    else if (!_invitation && _matchCallback) {
        _matchCallback(nil, nil);
        _matchCallback = nil;
    }
}

-(void) matchmakerViewController:(GKMatchmakerViewController *)controller didFailWithError:(NSError *)error
{
    [controller dismissViewControllerAnimated:YES completion:nil];
    if (_invitation && _delegate && [_delegate respondsToSelector:@selector(multiplayerDidLoadInvitation:match:error:)]) {
        [_delegate multiplayerDidLoadInvitation:self match:nil error:error];
    }
    else if (!_invitation && _matchCallback) {
        _matchCallback(nil, error);
        _matchCallback = nil;
    }
}

-(void) matchmakerViewController:(GKMatchmakerViewController *)controller didFindMatch:(GKMatch *)foundMatch
{
    _match = [[LDGameCenterMatch alloc] initWithGKMatch:foundMatch];
    [controller dismissViewControllerAnimated:YES completion:nil];
    if (_invitation && _delegate && [_delegate respondsToSelector:@selector(multiplayerDidLoadInvitation:match:error:)]) {
        [_delegate multiplayerDidLoadInvitation:self match:_match error:nil];
    }
    else if (!_invitation && _matchCallback) {
        _matchCallback(_match, nil);
        _matchCallback = nil;
    }
}


@end


#pragma mark Helper data



#pragma mark Multiplayer Match

@implementation LDGameCenterMatch
{
    BOOL _ready;
    NSMutableArray * _pendingNotifications;
}

-(instancetype) initWithGKMatch:(GKMatch *) gkMatch
{
    if (self = [super init]) {
        _match = gkMatch;
        _match.delegate = self;
        _ready = NO;
        _pendingNotifications = [NSMutableArray array];
    }
    return self;
}

-(void) dealloc
{
    _match.delegate = nil;
}

-(NSArray *) playerIDs
{
    NSMutableArray * array = [NSMutableArray arrayWithArray:self.match.playerIDs];
    //ensure local player ID inside match player Ids
    NSString * localPlayerID = GKLocalPlayer.localPlayer.playerID;
    if (![array containsObject:localPlayerID]) {
        [array addObject:localPlayerID];
    }
    return array;
}

-(LDMultiplayerPlayer*) localPlayer
{
    GKLocalPlayer * player = [GKLocalPlayer localPlayer];
    return [[LDMultiplayerPlayer alloc] initWithPlayerId:player.playerID alias:player.alias];
}

-(void) requestPlayersInfo:(void(^)(NSArray * players, NSError * error)) completion
{
    [GKPlayer loadPlayersForIdentifiers:self.playerIDs withCompletionHandler:^(NSArray *players, NSError *error) {
        
        if (error) {
            completion(nil, error);
        }
        else {
            NSMutableArray * data = [NSMutableArray array];
            for (GKPlayer * player in players) {
                [data addObject:[[LDMultiplayerPlayer alloc] initWithPlayerId:player.playerID alias:player.alias]];
            }
            completion(data, nil);
        }
    }];
}

-(NSUInteger) expectedPlayerCount
{
    return _match.expectedPlayerCount;
}

-(BOOL) sendData:(NSData *) data playerIDs:(NSArray *) playerIDs mode:(LDMultiplayerConnection) mode error:(NSError **) error
{
    GKMatchSendDataMode sendMode = mode == LDMultiplayerConnectionUnreliable ? GKMatchSendDataUnreliable : GKMatchSendDataReliable;
    BOOL result = [_match sendData:data toPlayers:playerIDs withDataMode:sendMode error:error];
    return result;
}


-(BOOL) sendDataToAllPlayers:(NSData *) data mode:(LDMultiplayerConnection) mode error:(NSError **) error
{
    GKMatchSendDataMode sendMode = mode == LDMultiplayerConnectionUnreliable ? GKMatchSendDataUnreliable : GKMatchSendDataReliable;
    BOOL result = [_match sendDataToAllPlayers:data withDataMode:sendMode error:error];
    
    //Mortimer: Also send data to local player => Better Game Architecture :)
    if (result)
    {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0), dispatch_get_main_queue(), ^{
            [self notifyDataReceived:data fromPlayer:[GKLocalPlayer localPlayer].playerID];
        });
    }
    
    return result;
}

-(void) disconnect
{
    [_match disconnect];
    _match.delegate = nil;
}

-(void) start
{
    _ready = YES;
    for (int i = 0; i< _pendingNotifications.count; ++i) {
        void (^block)() = [_pendingNotifications objectAtIndex:i];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0), dispatch_get_main_queue(), ^{
            block();
        });
    }
    [_pendingNotifications removeAllObjects];
}


-(void) notifyDataReceived:(NSData *) data fromPlayer:(NSString *) playerID
{
    if (_ready) {
        if (_delegate && [_delegate respondsToSelector:@selector(multiplayerMatch:didReceiveData:fromPlayer:)]) {
            [_delegate multiplayerMatch:self didReceiveData:data fromPlayer:playerID];
        }
    }
    else {
        __weak LDGameCenterMatch * weakSelf = self;
        [_pendingNotifications addObject:^{
            [weakSelf notifyDataReceived:data fromPlayer:playerID];
        }];
    }
    
}

#pragma mark GKMatchDelegate

-(void) match:(GKMatch *)match didReceiveData:(NSData *)data fromPlayer:(NSString *)playerID
{
    [self notifyDataReceived:data fromPlayer:playerID];
}

-(void) match:(GKMatch *)match player:(NSString *)playerID didChangeState:(GKPlayerConnectionState)state
{
    LDMultiplayerState connectionState;
    switch (state)
    {
        case GKPlayerStateUnknown: connectionState = LDMultiplayerStateUnkown; break;
        case GKPlayerStateConnected: connectionState = LDMultiplayerStateConnected; break;
        case GKPlayerStateDisconnected: connectionState = LDMultiplayerStateDisconnected; break;
    }
    if (_delegate && [_delegate respondsToSelector:@selector(multiplayerMatch:player:didChangeState:)]) {
        [_delegate multiplayerMatch:self player:playerID didChangeState:connectionState];
    }
}

-(void) match:(GKMatch *)match connectionWithPlayerFailed:(NSString *)playerID withError:(NSError *)error
{
    if (_delegate && [_delegate respondsToSelector:@selector(multiplayerMatch:connectionWithPlayerFailed:withError:)]) {
        [_delegate multiplayerMatch:self connectionWithPlayerFailed:playerID withError:error];
    }
}

-(void) match:(GKMatch *)match didFailWithError:(NSError *)error
{
    if (_delegate && [_delegate respondsToSelector:@selector(multiplayerMatch:didFailWithError:)]) {
        [_delegate multiplayerMatch:self didFailWithError:error];
    }
}



@end

