#pragma once


typedef NS_ENUM(NSInteger, LDMultiplayerConnection) {
    LDMultiplayerConnectionReliable = 0, //TCP
    LDMultiplayerConnectionUnreliable, //UDP
};

typedef NS_ENUM(NSInteger, LDMultiplayerState) {
    LDMultiplayerStateUnkown = 0,
    LDMultiplayerStateConnected,
    LDMultiplayerStateDisconnected
};

@interface LDMultiplayerPlayer : NSObject
@property (nonatomic, strong) NSString * playerID;
@property (nonatomic, strong) NSString * playerAlias;

-(instancetype) initWithPlayerId:(NSString *) playerID alias:(NSString *) alias;

@end


@protocol LDMultiplayerMatchDelegate;


/**
 *  Multiplayer Match Protocol
 */
@protocol LDMultiplayerMatchProtocol
@property (nonatomic, weak) NSObject<LDMultiplayerMatchDelegate> * delegate;

/**
 Returns all the identifiers of the players currently in the match.
 @return An array with the players identifiers
 */
-(NSArray *) playerIDs;

/**
 Gets the current player info
 */
-(LDMultiplayerPlayer*) localPlayer;

/**
 Requests additional player info of the players currently in the match.
 */
-(void) requestPlayersInfo:(void(^)(NSArray * players, NSError * error)) completion;

/**
 Returns the expected player number for the match to be considered complete and ready to start.
 @return The expected player count.
 */
-(NSUInteger) expectedPlayerCount;

/**
 Asynchronously send data to one or more players. Returns YES if delivery started, NO if unable to start sending and error will be set.
 @param data The data to be sent.
 @param playerIDs Array with the player ids to send the data to.
 @param mode The data mode.
 @param error The error information
 @return YES if delivery started, NO if unable to start sending and error will be set.
 */
-(BOOL) sendData:(NSData *) data playerIDs:(NSArray *) playerIDs mode:(LDMultiplayerConnection) mode error:(NSError **) error;


/**
 Asynchronously broadcasts data to all players. Returns YES if delivery started, NO if unable to start sending and error will be set.
 @param data The data to be sent.
 @param mode The data mode.
 @param error The error information
 @return YES if delivery started, NO if unable to start sending and error will be set.
 */
-(BOOL) sendDataToAllPlayers:(NSData *) data mode:(LDMultiplayerConnection) mode error:(NSError **) error;

/**
 Disconnect the match. This will show all other players in the match that the local player has disconnected. This should be called before releasing the match instance.
 */
-(void) disconnect;

/**
 Start processing received messages. The user must call this method when the game is ready to process messages. Messages received before being prepared are stored and processed later.
 */
-(void) start;


@end

typedef NSObject<LDMultiplayerMatchProtocol> LDMultiplayerMatch;


/**
 *  Multiplayer Match Delegate
 */

@protocol LDMultiplayerMatchDelegate<NSObject>
@optional

-(void) multiplayerMatch:(LDMultiplayerMatch *)match didReceiveData:(NSData *)data fromPlayer:(NSString *)playerID;
-(void) multiplayerMatch:(LDMultiplayerMatch *)match player:(NSString *)playerID didChangeState:(LDMultiplayerState)state;
-(void) multiplayerMatch:(LDMultiplayerMatch *)match connectionWithPlayerFailed:(NSString *)playerID withError:(NSError *)error;
-(void) multiplayerMatch:(LDMultiplayerMatch *)match didFailWithError:(NSError *)error;

@end