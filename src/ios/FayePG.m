//
//  FayePG.m
//  FayePG
//
//  Created by Yenson Tam on 2/25/15.
//
//

#import "FayePG.h"

@implementation FayePG

@synthesize mzFayeClient;

- (void)dealloc
{
    mzFayeClient = nil;
    //[super dealloc];
}

- (void)pluginInitialize
{
    [super pluginInitialize];
}

- (void)init:(CDVInvokedUrlCommand *)command
{
    NSLog(@"fayePG init with url: %@",[command argumentAtIndex:0]);
    if([command.arguments count] > 0 && [[command argumentAtIndex:0] isKindOfClass:[NSString class]] && [[command argumentAtIndex:1] isKindOfClass:[NSString class]]) {
        self.url = [command argumentAtIndex:0];
        NSString *credString = [command argumentAtIndex:1];
                            
        [self setAuthTokenFromString:credString];
        if (mzFayeClient == nil) {
            mzFayeClient = [[MZFayeClient alloc] initWithURL:[NSURL URLWithString:self.url]];
            mzFayeClient.delegate = mzFayeClient;
            mzFayeClient.shouldRetryConnection = true;
        }
        NSString *channel = @"/";
        channel = [channel stringByAppendingString:[self.authToken objectForKey:@"user"]];
        [mzFayeClient setExtension:self.authToken forChannel:channel];
        [mzFayeClient connect];
        NSLog(@"connected: %s", mzFayeClient.isConnected ? "true" : "false");

    }
}

// user:sid
- (void)setAuthTokenFromString:(NSString *) credentialsString
{
    NSArray *keys = [NSArray arrayWithObjects:@"user", @"sid", nil];
    NSString *user = [[credentialsString componentsSeparatedByString:@":"] objectAtIndex:0];
    NSString *sid = [[credentialsString componentsSeparatedByString:@":"] objectAtIndex:1];
    NSArray *credentials = [NSArray arrayWithObjects:user, sid, nil];
    
    self.authToken = [NSDictionary dictionaryWithObjects:credentials forKeys:keys];
}

- (void)disconnect:(CDVInvokedUrlCommand *)command
{
   NSLog(@"fayePG objc disconnect call");
   if (mzFayeClient != nil) {
       NSLog(@"fayePG objc disconnecting");
       mzFayeClient.shouldRetryConnection = false;
       [mzFayeClient disconnect];
       self.url = nil;
   }
}
- (void)subscribe:(CDVInvokedUrlCommand *)command
{
    NSLog(@"FayePG subscribe to channel: %@",[command argumentAtIndex:0]);
    NSLog(@"FayePG callback: %@",[command argumentAtIndex:1]);
    
    if([command.arguments count] > 0 && [[command argumentAtIndex:0] isKindOfClass:[NSString class]] && [[command argumentAtIndex:1] isKindOfClass:[NSString class]]) {
        [mzFayeClient subscribeToChannel:[command argumentAtIndex:0] usingBlock:^(NSDictionary *message) {
            NSLog(@"received msg in FayePG obj c subscribe %@",message);
            NSLog(@"FayePG callback: %@",[command argumentAtIndex:1]);
            NSError *error;
            NSData *jsonData = [NSJSONSerialization dataWithJSONObject:message
                                                               options:0 // Pass 0 if you don't care about the readability of the generated string
                                                                 error:&error];
            if (! jsonData) {
                NSLog(@"Got an error: %@", error);
            } else {
                NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
                NSString *cmd = [command argumentAtIndex:1];
                cmd = [cmd stringByAppendingString:@"("];
                cmd = [cmd stringByAppendingString:jsonString];
                cmd = [cmd stringByAppendingString:@");"];
                NSLog(@"cmd: %@",cmd);
                [self.commandDelegate evalJs:cmd];
            }
           
        }];
    }
}


- (void)sendMessage:(CDVInvokedUrlCommand *)command
{
	NSLog(@"fayePG obj c send message to channel: %@", [command argumentAtIndex:0]);
  	NSLog(@"fayePG obj c send message with data: %@", [command argumentAtIndex:1]);
    if([command.arguments count] > 0 && [[command argumentAtIndex:0] isKindOfClass:[NSString class]] && [[command argumentAtIndex:1] isKindOfClass:[NSDictionary class]]) {
        [mzFayeClient sendMessage:[command argumentAtIndex:1] toChannel:[command argumentAtIndex:0] usingExtension:self.authToken];
//        [mzFayeClient sendMessage:[command argumentAtIndex:1] toChannel:[command argumentAtIndex:0]];
    }

}

- (void)setNotifTexts:(CDVInvokedUrlCommand *)command
{
}

@end
