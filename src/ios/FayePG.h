//
//  FayePG.h
//  FayePG
//
//  Created by Yenson Tam on 2/25/15.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "MZFayeClient.h"


@interface FayePG : CDVPlugin {
    MZFayeClient *mzFayeClient;
}

@property(nonatomic, retain) MZFayeClient *mzFayeClient;
@property (nonatomic, strong) NSString *url;
@property (nonatomic, strong) NSDictionary *authToken;

- (void)init:(CDVInvokedUrlCommand*)command;
- (void)disconnect:(CDVInvokedUrlCommand*)command;
- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)sendMessage:(CDVInvokedUrlCommand*)command;
- (void)setAuthTokenFromString:(NSString *)credentialsString;

@end
