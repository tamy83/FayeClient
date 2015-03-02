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
	NSLog("fayeClient obj c init with command: %@", [command argumentAtIndex:0]);
	 /*
	       if([command.arguments count] > 0
	              && [[command argumentAtIndex:0] isKindOfClass:[NSString class]])
	                  {
	                         mzFayeClient = [[MZFayeClient alloc] initWithURL:[NSURL URLWithString:[command argumentAtIndex:0]]];
	                                  mzFayeClient.delegate = mzFayeClient;
	                                      }
	                                       */

}
- (void)subscribe:(CDVInvokedUrlCommand *)command
{
	 NSLog("fayeClient obj c subscribe with command: %@", [command argumentAtIndex:0]);
	     /*
	           if([command.arguments count] > 0
	                  && [[command argumentAtIndex:0] isKindOfClass:[NSString class]])
	                      {
	                              [mzFayeClient subscribeToChannel:[command argumentAtIndex:0] usingBlock:^(NSDictionary *message) {
	                                          NSLog(@"received msg in FayeClient obj c subscribe %@",message);
	                                                      
	                                                              }];
	                                                                      [mzFayeClient connect];
	                                                                          }
	                                                                               */

}


- (void)publish:(CDVInvokedUrlCommand *)command
{
	NSLog("fayeClient obj c publish with command: %@", [command argumentAtIndex:0]);    
}

@end
