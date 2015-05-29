#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "LDMultiplayerService.h"

@interface LDMultiplayerPlugin : CDVPlugin<LDMultiplayerServiceDelegate, LDMultiplayerMatchDelegate>

@property (nonatomic, strong) LDMultiplayerService * service;

@end
