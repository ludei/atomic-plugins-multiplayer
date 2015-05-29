#import "LDMultiplayerGameCenterPlugin.h"
#import "LDMultiplayerGameCenter.h"

@implementation LDMultiplayerGameCenterPlugin

- (void)pluginInitialize
{
    [super pluginInitialize];
    self.service = [[LDMultiplayerGameCenter alloc] init];
    self.service.delegate = self;
}

@end
