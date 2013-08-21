//
//  Storm.m
//  SplunkStormMobileAnalytics
//
//  Created by Nicholas Key (nicholas@splunk.com)
//

/**
 * Copyright 2013 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

#import <UIKit/UIKit.h>

#import "Storm.h"
#import "TCPClient.h"


static NSString *projectId = @"";
static NSString *accessToken = @"";
static NSString *receivingURL = @"";
static NSInteger TCPPortNumber = 0;


@interface CrashMessage : NSOperation {
	
}
@end


@implementation CrashMessage

- (NSString *) flattenStackSymbols:(NSArray *)inputArray {
	NSMutableString *stackSym = [[[NSMutableString alloc] initWithString:@"\n"] autorelease];
	for (NSObject *itemObj in inputArray) {
		[stackSym appendFormat:@"\t%@\n", itemObj];
	}
	return stackSym;
}

- (NSString *) flattenArrayKeyName:(NSString *)dictKey flattenArrayInput:(NSArray *)inputArray {
	
	NSMutableString *arrayElement = [[[NSMutableString alloc] init] autorelease];
	if ([dictKey length] != 0) {
		NSMutableString *tempHolder = [[[NSMutableString alloc] initWithString:@"\n"] autorelease];
		if ([inputArray count] == 1) {
			[arrayElement appendFormat:@"%@=\"%@\"\n", dictKey, [inputArray objectAtIndex:0]];
		} else if ([inputArray count] > 1) {
			for (NSObject *itemObj in inputArray) {
				[tempHolder appendFormat:@"\t%@\n", itemObj];
			}
			[arrayElement appendFormat:@"%@=\"%@\"\n", dictKey, tempHolder];
		} 
	} else {
		[arrayElement appendString:@"\n"];
		for (NSObject *itemObj in inputArray) {
			[arrayElement appendFormat:@"\t%@\n", itemObj];
		}
	}
	return arrayElement;
}

- (void) flattenDictKeyName:(NSString *)dictKey flattenDictInput:(NSDictionary *)inputDict {
	NSLog(@"\"%@\" = \"%@\"", dictKey, inputDict);
}

- (NSString *) flattenpList:(NSDictionary *)inputplistInfo {	
	NSMutableString *plistElement = [[[NSMutableString alloc] init] autorelease];
	for (NSString *dictKey in [inputplistInfo allKeys]) {
		
		if ([[inputplistInfo objectForKey:dictKey] isKindOfClass:[NSString class]]) {
			[plistElement appendFormat:@"%@=\"%@\"\n", dictKey, [inputplistInfo objectForKey:dictKey]];
		} else if ([[inputplistInfo objectForKey:dictKey] isKindOfClass:[NSArray class]]) {
			[plistElement appendFormat:@"%@", [self flattenArrayKeyName:dictKey 
													  flattenArrayInput:[inputplistInfo objectForKey:dictKey]]];
		} else if ([[inputplistInfo objectForKey:dictKey] isKindOfClass:[NSDictionary class]]) {
			
		} else {
			[plistElement appendFormat:@"%@=\"%@\"\n", dictKey, [inputplistInfo objectForKey:dictKey]];
		}
	}
	return plistElement;
}


// http://www.calebmadrigal.com/string-to-base64-string-in-objective-c/
- (NSString *)base64String:(NSString *)str {
    NSData *theData = [str dataUsingEncoding: NSASCIIStringEncoding];
    const uint8_t* input = (const uint8_t*)[theData bytes];
    NSInteger length = [theData length];
	
    static char table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
	
    NSMutableData* data = [NSMutableData dataWithLength:((length + 2) / 3) * 4];
    uint8_t* output = (uint8_t*)data.mutableBytes;
	
    NSInteger i;
    for (i=0; i < length; i += 3) {
        NSInteger value = 0;
        NSInteger j;
        for (j = i; j < (i + 3); j++) {
            value <<= 8;
			
            if (j < length) {
                value |= (0xFF & input[j]);
            }
        }
		
        NSInteger theIndex = (i / 3) * 4;
        output[theIndex + 0] =                    table[(value >> 18) & 0x3F];
        output[theIndex + 1] =                    table[(value >> 12) & 0x3F];
        output[theIndex + 2] = (i + 1) < length ? table[(value >> 6)  & 0x3F] : '=';
        output[theIndex + 3] = (i + 2) < length ? table[(value >> 0)  & 0x3F] : '=';
    }
	
    return [[NSString alloc] initWithData:data encoding:NSASCIIStringEncoding];
}


- (void) stormAPI:(NSString *)crashLog {
	
	NSLog(@"\n%@\n", crashLog);
	NSString *requestUrl = [NSString stringWithFormat:@"https://api.splunkstorm.com/1/inputs/http?index=%@&sourcetype=iphone_crash_log", projectId];
	NSString *token = [NSString stringWithFormat:@"x:%@", accessToken];
	NSURL *url=[NSURL URLWithString:requestUrl];
	
	NSMutableURLRequest *request = [[[NSMutableURLRequest alloc] initWithURL:url] autorelease];
	[request setHTTPMethod:@"POST"];
	[request setValue:[NSString stringWithFormat:@"Basic %@", [self base64String:token]] forHTTPHeaderField:@"Authorization"];
	[request setValue:[NSString stringWithFormat:@"%d", [crashLog length]] forHTTPHeaderField:@"Content-Length"];
	[request setValue:@"application/x-www-form-urlencoded" forHTTPHeaderField:@"Content-Type"];
	[request setHTTPBody:[crashLog dataUsingEncoding:NSUTF8StringEncoding]];
	
	NSError *error;
	NSURLResponse *response;
	NSData *urlData=[NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&error];
	NSString *data=[[NSString alloc]initWithData:urlData encoding:NSUTF8StringEncoding];
	NSLog(@"%@",data);
	
}

@end




void UncaughtExceptionHandler(NSException *exception) {
	
	CrashMessage *crashMsg = [[[CrashMessage alloc] init] autorelease];
	NSMutableString *crashLog = [[[NSMutableString alloc] initWithString:@"sourcetype=\"iphone_crash_log\"\n"] autorelease];
	
	[crashLog appendFormat:@"exceptionReason=\"%@\"\n", [exception reason]];
	[crashLog appendFormat:@"exceptionName=\"%@\"\n", [exception name]];
	[crashLog appendFormat:@"stackSymbols=\"%@\"\n", [crashMsg flattenStackSymbols:[exception callStackSymbols]]];
	[crashLog appendFormat:@"stackReturnAddresses=\"%@\"\n",  [crashMsg flattenArrayKeyName:@""
																		  flattenArrayInput:[exception callStackReturnAddresses]]];
	
	NSDictionary *plistInfo = [[NSBundle mainBundle] infoDictionary];
	[crashLog appendFormat:@"%@\n", [crashMsg flattenpList:plistInfo]];
		
	if (([projectId length]) && ([accessToken length])) {
		NSLog(@"Using API input");
		NSLog(@"received projectId:[%@] accessToken:[%@]", projectId, accessToken);
		[crashMsg stormAPI:crashLog];
	} else if (([receivingURL length]) && (TCPPortNumber > 0)) {
		//NSLog(@"Sending data with TCP input");
		//NSLog(@"received receivingURL:[%@] TCPPortNumber:[%d]", receivingURL, TCPPortNumber);
		
		TCPClient *splunkTCPclient = [[TCPClient alloc] init];
		[splunkTCPclient TCPClientSetup:receivingURL TCPPort:TCPPortNumber writeOut:crashLog];
		
		//NSLog(@"Stack trace for TCP:\n%@", crashLog);
	} 
}


@interface StormProject : NSObject {
	
}

- (id) initStormProjID:(NSString *)projId initStormAccessToken:(NSString *)accToken;
- (id) initStormTCPRecvUrl:(NSString *)rcvUrl initStormTCPPrtNum:(NSInteger)prtNumber;
@end

@implementation StormProject

- (id) initStormProjID:(NSString *)projId initStormAccessToken:(NSString *)accToken {
	self = [super init];
    if (self) {
		projectId = projId;
		accessToken = accToken;
    }
	NSSetUncaughtExceptionHandler (&UncaughtExceptionHandler);
    return self;
}

- (id) initStormTCPRecvUrl:(NSString *)rcvUrl initStormTCPPrtNum:(NSInteger)prtNumber {
	self = [super init];
    if (self) {
		receivingURL = rcvUrl;
		TCPPortNumber = prtNumber;
    }
	NSSetUncaughtExceptionHandler (&UncaughtExceptionHandler);
    return self;
}

- (void) dealloc {
    [super dealloc];
}

@end


@implementation Storm

+ (void) stormAPIProjectId:(NSString *)projectId stormAPIAccessToken:(NSString *)accessToken {
	[[StormProject alloc] initStormProjID:projectId initStormAccessToken:accessToken];
}

+ (void) TCPHost:(NSString *)receivingURL TCPPortNum:(NSInteger)TCPPortNumber {
	[[StormProject alloc] initStormTCPRecvUrl:receivingURL initStormTCPPrtNum:TCPPortNumber];
}

@end
