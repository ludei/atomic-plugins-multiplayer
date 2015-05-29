//
//  LDMultiplayerMatch.m
//  HelloCordova
//
//  Created by Imanol Fernandez Gorostizag on 20/5/15.
//
//

#import "LDMultiplayerMatch.h"

@implementation LDMultiplayerPlayer

-(instancetype) initWithPlayerId:(NSString *) playerID alias:(NSString *) alias
{
    if (self = [super init]) {
        self.playerID = playerID;
        self.playerAlias = alias;
    }
    return self;
}

@end
