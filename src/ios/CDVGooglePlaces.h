#import <Cordova/CDVPlugin.h>

@interface CDVGooglePlaces: CDVPlugin

-(void)currentPlace:(CDVInvokedUrlCommand*)command;

-(void)autocompleteQuery:(CDVInvokedUrlCommand*)command;

-(void)pickPlace:(CDVInvokedUrlCommand*)command;

-(void)showPlaceAutocomplete:(CDVInvokedUrlCommand*)command;

@end

