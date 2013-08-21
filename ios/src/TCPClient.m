//
//  TCPClient.m
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


#import "TCPClient.h"

CFReadStreamRef readStream;
CFWriteStreamRef writeStream;

NSInputStream *inputStream;
NSOutputStream *outputStream;

@implementation TCPClient

- (void)TCPClientSetup:(NSString *)host TCPPort:(NSInteger)port writeOut:(NSString *)stackMsg{
	NSURL *url = [NSURL URLWithString:host];
	
	NSLog(@"Setting up connection to %@ : %i", [url absoluteString], port);
	
	//CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault, (CFStringRef)[url host], port, &readStream, &writeStream);
	CFStreamCreatePairWithSocketToHost(NULL, (CFStringRef)host, port, &readStream, &writeStream);
	
	if(!CFWriteStreamOpen(writeStream)) {
		NSLog(@"ERROR ... writeStream (CFWriteStreamRef) not open");
		return;
	}
	
	inputStream = (NSInputStream *)readStream;
	outputStream = (NSOutputStream *)writeStream;
	
	[inputStream setDelegate:self];
	[outputStream setDelegate:self];
	
	[inputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
	[outputStream scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
	
	[inputStream open];
	[outputStream open];
	
	NSLog(@"Stack trace for TCP:\n%@", stackMsg);
	
	NSData *data = [[NSData alloc] initWithData:[stackMsg dataUsingEncoding:NSASCIIStringEncoding]];
	[outputStream write:[data bytes] maxLength:[data length]];
	
	[inputStream close];
	[outputStream close];
	
	[inputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
	[outputStream removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
	
	[inputStream setDelegate:nil];
	[outputStream setDelegate:nil];
	
	[inputStream release];
	[outputStream release];
	
	inputStream = nil;
	outputStream = nil;
}
	
@end