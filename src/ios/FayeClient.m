//
//  FayeClient.m
//  FayeClient
//
//  Created by Yenson Tam on 2/25/15.
//
//

#import "FayeClient.h"

@implementation FayeClient

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
    if([command.arguments count] > 0
       && [[command argumentAtIndex:0] isKindOfClass:[NSString class]])
    {
        mzFayeClient = [[MZFayeClient alloc] initWithURL:[NSURL URLWithString:[command argumentAtIndex:0]]];
    }
}

- (void)subscribe:(CDVInvokedUrlCommand *)command
{
    //[self setBackgroundSecondsWithSeconds:[NSNumber numberWithInteger:NSIntegerMax]];
}

- (void)publish:(CDVInvokedUrlCommand *)command
{
    
    //[self setBackgroundSecondsWithSeconds:[NSNumber numberWithInteger:backgroundSecondsCounter]];
}

@end
