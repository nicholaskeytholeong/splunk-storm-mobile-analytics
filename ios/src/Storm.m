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

#include <sys/types.h>
#include <sys/sysctl.h>
#include <ifaddrs.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#import <mach/mach.h>
#import <mach/mach_host.h>

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
	NSString *requestUrl = [NSString stringWithFormat:@"https://api.splunkstorm.com/1/inputs/http?index=%@&sourcetype=ios_crash_log", projectId];
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

// http://stackoverflow.com/questions/5712527/how-to-detect-total-available-free-disk-space-on-the-iphone-ipad-device
- (NSString *)getDiskSpaceInfo {
	
	NSMutableString *disk_space_info = [[[NSMutableString alloc] init] autorelease];
	
	uint64_t totalSpace = 0;
    uint64_t totalFreeSpace = 0;
	
    NSError *error = nil;  
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);  
    NSDictionary *dictionary = [[NSFileManager defaultManager] attributesOfFileSystemForPath:[paths lastObject] error: &error];  
	
	double unit = 1024 * 1024;
	
	if (dictionary) { 
        NSNumber *fileSystemSizeInBytes = [dictionary objectForKey: NSFileSystemSize];  
        NSNumber *freeFileSystemSizeInBytes = [dictionary objectForKey:NSFileSystemFreeSize];
		
        totalSpace = [fileSystemSizeInBytes unsignedLongLongValue];
        totalFreeSpace = [freeFileSystemSizeInBytes unsignedLongLongValue];

		[disk_space_info appendFormat:@"totalDiskSpace=\"%.2f MB\"\n", totalSpace/unit];
		[disk_space_info appendFormat:@"totalFreeDiskSpace=\"%.2f MB\"", totalFreeSpace/unit];
		
	} else {
		[disk_space_info appendFormat:@"availableDiskSpace=\"Fail to get disk space stats\"\n"];
	}
	return disk_space_info;
}


// http://gamesfromwithin.com/whered-that-memory-go
- (NSString *)getMemoryInfo {
	
	NSMutableString *memory_info = [[[NSMutableString alloc] init] autorelease];
	
	mach_msg_type_number_t infoCount = HOST_VM_INFO_COUNT;
	vm_statistics_data_t vmstats;
	kern_return_t kernReturn = host_statistics(mach_host_self (), HOST_VM_INFO, (host_info_t) &vmstats, &infoCount);
	
	if (kernReturn == KERN_SUCCESS) {
		const int total = vmstats.wire_count + vmstats.active_count + vmstats.inactive_count + vmstats.free_count;
		const int available = vmstats.free_count;
		const int wired = vmstats.wire_count;
		const int active = vmstats.active_count;
		const int inactive = vmstats.inactive_count;
		const int pageins = vmstats.pageins;
		const int pageouts = vmstats.pageouts;
		
		double unit = 1024 * 1024;
		
		[memory_info appendFormat:@"totalMemory=\"%.2f MB\"\n", (double)(total * vm_page_size/unit)];
		[memory_info appendFormat:@"availableMemory=\"%.2f MB\"\n", (double)(available * vm_page_size/unit)];
		[memory_info appendFormat:@"wiredMemory=\"%.2f MB\"\n", (double)(wired * vm_page_size/unit)];
		[memory_info appendFormat:@"activeMemory=\"%.2f MB\"\n", (double)(active * vm_page_size/unit)];
		[memory_info appendFormat:@"inactiveMemory=\"%.2f MB\"\n", (double)(inactive * vm_page_size/unit)];
		[memory_info appendFormat:@"pageIn=\"%d\"\n", pageins];
		[memory_info appendFormat:@"pageOut=\"%d\"", pageouts];
				
	} else {
		[memory_info appendFormat:@"availableMemory=\"Fail to get memory stats\"\n"];
	}
	return memory_info;
}

- (NSString *) getDeviceInfo {
			
	NSMutableString *device_info_str = [[[NSMutableString alloc] init] autorelease];
	
	NSUInteger  an_Integer;
    NSArray * ipItemsArray;
    NSString *externalIP;
	
    NSURL *iPURL = [NSURL URLWithString:@"http://www.dyndns.org/cgi-bin/check_ip.cgi"];
	
    if (iPURL) {
        NSError *error = nil;
        NSString *theIpHtml = [NSString stringWithContentsOfURL:iPURL encoding:NSUTF8StringEncoding error:&error];
        if (!error) {
            NSScanner *theScanner;
            NSString *text = nil;
			
            theScanner = [NSScanner scannerWithString:theIpHtml];
			
            while ([theScanner isAtEnd] == NO) {
				
                // find start of tag
                [theScanner scanUpToString:@"<" intoString:NULL] ;
				
                // find end of tag
                [theScanner scanUpToString:@">" intoString:&text] ;
				
                // replace the found tag with a space
                //(you can filter multi-spaces out later if you wish)
                theIpHtml = [theIpHtml stringByReplacingOccurrencesOfString:
                             [ NSString stringWithFormat:@"%@>", text]
                                                                 withString:@" "] ;
                ipItemsArray =[theIpHtml  componentsSeparatedByString:@" "];
                an_Integer=[ipItemsArray indexOfObject:@"Address:"];
                externalIP =[ipItemsArray objectAtIndex:  ++an_Integer];
            }
			[device_info_str appendFormat:@"externalIP=\"%@\"\n", externalIP];
        } else {
            NSLog(@"%d, %@", [error code], [error localizedDescription]);
        }
    }
	
	
	
	
	NSString *address = @"error";
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;
	
    // retrieve the current interfaces - returns 0 on success
    success = getifaddrs(&interfaces);
    if (success == 0)
    {
        // Loop through linked list of interfaces
        temp_addr = interfaces;
        while(temp_addr != NULL)
        {
            if(temp_addr->ifa_addr->sa_family == AF_INET)
            {
				// Get NSString from C String
				address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
            }
            temp_addr = temp_addr->ifa_next;
        }
    }
	
    // Free memory
    freeifaddrs(interfaces);
	[device_info_str appendFormat:@"localIP=\"%@\"\n", address];
	
	
	
	
	NSDictionary *sys_ctl_name = [NSDictionary dictionaryWithObjectsAndKeys:
								  @"hw.model", @"model", 
								  @"hw.machine", @"machine",
								  nil];
	
	for (NSString *dictKey in [sys_ctl_name allKeys]) {
		
		size_t size;
		sysctlbyname([[sys_ctl_name objectForKey:dictKey] cStringUsingEncoding:NSUTF8StringEncoding], NULL, &size, NULL, 0);
		char *machine = malloc(size);
		sysctlbyname([[sys_ctl_name objectForKey:dictKey] cStringUsingEncoding:NSUTF8StringEncoding], machine, &size, NULL, 0);
		NSString *machine_model = [NSString stringWithCString:machine encoding:NSUTF8StringEncoding];
		free(machine);
		[device_info_str appendFormat:@"%@=\"%@\"\n", dictKey, machine_model];
	}

	
	
	[[UIDevice currentDevice] setBatteryMonitoringEnabled:YES];
	[[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
	
	[device_info_str appendFormat:@"batteryLevel=\"%.f\"\n", [[UIDevice currentDevice] batteryLevel] * 100];
	
	NSArray *batteryStatus = [NSArray arrayWithObjects: 
							  @"Unknown", 
							  @"Unplugged/Discharging)", 
							  @"Charging", 
							  @"Fully charged",
							  nil];
	
	if ([[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateUnknown) {
		[device_info_str appendFormat:@"batteryStatus=\"%@\"\n", [batteryStatus objectAtIndex:0]];
	} else {
		[device_info_str appendFormat:@"batteryStatus=\"%@\"\n", [batteryStatus objectAtIndex:[[UIDevice currentDevice] batteryState]]];
	}
	
	[device_info_str appendFormat:@"deviceName=\"%@\"\n", [[UIDevice currentDevice] name]];
	[device_info_str appendFormat:@"deviceSystemName=\"%@\"\n", [[UIDevice currentDevice] systemName]];
	[device_info_str appendFormat:@"deviceSystemVersion=\"%@\"\n", [[UIDevice currentDevice] systemVersion]];
	[device_info_str appendFormat:@"deviceModel=\"%@\"\n", [[UIDevice currentDevice] model]];
	[device_info_str appendFormat:@"deviceLocalizedModel=\"%@\"\n", [[UIDevice currentDevice] localizedModel]];
	
	NSArray *deviceOrientation = [NSArray arrayWithObjects: 
								  @"Unknown", 
								  @"Portrait", 
								  @"PortraitUpsideDown", 
								  @"LandscapeLeft",
								  @"LandscapeRight", 
								  @"FaceUp",
								  @"FaceDown",
								  nil];
	
	if ([[UIDevice currentDevice] orientation] == UIDeviceOrientationUnknown) {
		[device_info_str appendFormat:@"deviceOrientation=\"%@\"\n", [deviceOrientation objectAtIndex:0]];
	} else {
		[device_info_str appendFormat:@"deviceOrientation=\"%@\"\n", [deviceOrientation objectAtIndex:[[UIDevice currentDevice] orientation]]];
	}
	
	[device_info_str appendFormat:@"%@\n", 	[self getDiskSpaceInfo]];
	[device_info_str appendFormat:@"%@\n", [self getMemoryInfo]];
	
	[[UIDevice currentDevice] setBatteryMonitoringEnabled:NO];
	[[UIDevice currentDevice] endGeneratingDeviceOrientationNotifications];
		
	return device_info_str;
}

@end




void UncaughtExceptionHandler(NSException *exception) {
	
	CrashMessage *crashMsg = [[[CrashMessage alloc] init] autorelease];
	NSMutableString *crashLog = [[[NSMutableString alloc] initWithString:@"sourcetype=\"ios_crash_log\"\n"] autorelease];
	
	[crashLog appendFormat:@"%@", [crashMsg getDeviceInfo]];
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
