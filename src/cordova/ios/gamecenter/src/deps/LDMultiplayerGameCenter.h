#pragma once

#import "LDMultiplayerService.h"
#import <GameKit/GameKit.h>

@interface LDMultiplayerGameCenter: NSObject<LDMultiplayerServiceProtocol, GKMatchmakerViewControllerDelegate>

@property (nonatomic, weak) NSObject<LDMultiplayerServiceDelegate> * delegate;

-(instancetype) init;

@end

@interface LDGameCenterMatch : LDMultiplayerMatch<GKMatchDelegate>

@property (nonatomic, strong) GKMatch * match;
@property (nonatomic, weak) NSObject<LDMultiplayerMatchDelegate> * delegate;

-(instancetype) initWithGKMatch:(GKMatch *) gkMatch;

@end
