#Using this logging library with iOS app
- Create a Splunk Storm account
- Download splunkmobileanalytics.zip
- Unzip it and drag the "splunkmobileanalytics" folder into the project
- Select "Relative to Project" at "Reference Type"
- Then click "Add"

- In the AppDelegate interface file (AppDelegate.h), import Storm.h, like such:

```objective-c
#import <UIKit/UIKit.h>
#import "Storm.h"

// other awesome codes that you are writing
```

- In the AppDelegate implementation file (AppDelegate.m), provide the stormAPIProjectId and stormAPIAccessToken values in the message

```objective-c
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {    
    
    // Override point for customization after application launch.

	// Set the view controller as the window's root view controller and display.
    self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];
	
	[Storm stormAPIProjectId:@"YOUR_STORM_PROJECT_ID" stormAPIAccessToken:@"YOUR_STORM_ACCESS_TOKEN];
		
    return YES;
}
```

- You are set
- Go to the Storm dashboard to perform crash analytics on iOS apps!
- Thank you for trying this logging library for iOS!

#LIABILITY 
Splunk does not assume any liability upon the usage of this library. Use it as is.

#SUPPORT
This is not a Splunk officially supported library.

#License
The Mobile Analytics with Splunk Storm is licensed under the Apache License 2.0. Details can be found in the LICENSE file.

#Contact
nicholas@splunk.com