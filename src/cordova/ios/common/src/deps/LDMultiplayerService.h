#pragma once

#import "LDMultiplayerMatch.h"

/**
 ** Match Request data
 */
@interface LDMultiplayerMatchRequest: NSObject

@property (nonatomic, assign) NSUInteger minPlayers;
@property (nonatomic, assign) NSUInteger maxPlayers;
@property (nonatomic, strong) NSArray * playersToInvite;
//A mask that specifies the role that the local player would like to play in the game
@property (nonatomic, assign) uint32_t playerAttributes;
//A number identifying a subset of players allowed to join the match.
@property (nonatomic, assign) uint32_t playerGroup;

@end



@protocol LDMultiplayerServiceDelegate;

/**
 *  Multiplayer Service Protocol
 */
@protocol LDMultiplayerServiceProtocol
@property (nonatomic, weak) NSObject<LDMultiplayerServiceDelegate> * delegate;

/**
 Presents a system View for the matchmaking and creates a new Match
 */
-(void) findMatch:(LDMultiplayerMatchRequest*) request fromViewController:(UIViewController *) controller completion:(void(^)(LDMultiplayerMatch * match, NSError * error)) completion;

/**
 Sends an automatch request to join the authenticated user to a match waiting for players to start.
 Only one automatch request can be active at the same time.
 If an automatch request is requested while a previous request is still active, it will be ignored.
 */
-(void) findAutoMatch:(LDMultiplayerMatchRequest*) request completion:(void(^)(LDMultiplayerMatch * match, NSError * error)) completion;

/**
 Cancels the ongoing automatch request.
 If there are no ongoing request it will be ignored.
 */
-(void) cancelAutoMatch;

/**
 Automatically adds players to an ongoing match owned by the user.
 @see MultiplayerServiceListener
 */
-(void) addPlayersToMatch:(LDMultiplayerMatch* ) match request:(LDMultiplayerMatchRequest *) request completion:(void(^)(NSError* error)) completion;

/**
 Get the current match reference.
 @return Current match reference.
 */
-(LDMultiplayerMatch*) currentMatch;

@end

typedef NSObject<LDMultiplayerServiceProtocol> LDMultiplayerService;


/**
 *  Multiplayer Service Delegate
 */

@protocol LDMultiplayerServiceDelegate<NSObject>
@required

/**
 * An invitation has been received from another player and the user has accepted it
 * @param matchManager The match manager instace.
 * @return viewController The viewController where to show the invitation UI from
 */
-(UIViewController *) multiplayerDidReceiveInvitation:(LDMultiplayerService *) service;
/**
 * An invitation is ready to be processed (either start the match or handle the error)
 */
-(void) multiplayerDidLoadInvitation:(LDMultiplayerService *) service match:(LDMultiplayerMatch *) match error:(NSError *) error;

@end




