//
//  FayeClient.h
//  FayeClient
//
//  Created by Yenson Tam on 2/25/15.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "MZFayeClient.h"


@interface FayeClient : CDVPlugin {
    MZFayeClient *mzFayeClient;
}

@property(nonatomic, retain) MZFayeClient *mzFayeClient;

- (void)init:(CDVInvokedUrlCommand*)command;
- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)publish:(CDVInvokedUrlCommand*)command;

@end
