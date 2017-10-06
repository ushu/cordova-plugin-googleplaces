#import "CDVGooglePlaces.h"
#import <GooglePlaces/GooglePlaces.h>
#import <GooglePlacePicker/GooglePlacePicker.h>

@interface CDVGooglePlaces(PlacePicker)<GMSPlacePickerViewControllerDelegate>
@end

@interface CDVGooglePlaces(Autocomplete)<GMSAutocompleteViewControllerDelegate>
@end

@implementation CDVGooglePlaces {
    NSString* _APIKey;
    NSString* _placePickerCallbackId;
    NSString* _placeAutocompleteCallbackId;
}

-(void)pluginInitialize {
    _APIKey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"Google Places API Key"];
    
    if (_APIKey) {
        [GMSPlacesClient provideAPIKey:_APIKey];
    }
}

-(void)currentPlace:(CDVInvokedUrlCommand *) command {
    if (!_APIKey) {
        [self failCommandMissingAPIKey:command.callbackId];
        return;
    }
    
    GMSPlacesClient* client = [GMSPlacesClient sharedClient];
    
    [client currentPlaceWithCallback:^(GMSPlaceLikelihoodList * _Nullable likelihoodList, NSError * _Nullable error) {
        CDVPluginResult* result;
        
        // check for error
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            NSDictionary* encodedResult = encodePlaceLikelihoodList(likelihoodList);
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:encodedResult];
        }
        
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }];
}

-(void)autocompleteQuery:(CDVInvokedUrlCommand *)command {
    if (!_APIKey) {
        [self failCommandMissingAPIKey:command.callbackId];
        return;
    }
    
    // load arguments
    NSString* query;
    GMSCoordinateBounds* bounds;
    GMSAutocompleteFilter* filter;
    
    if (command.arguments.count > 3) {
        [self failCommandWithCallbackId:command.callbackId message:@"wrong arguments for autocompleteQuery(query, bounds, filter)"];
        return;
    }

    if (command.arguments.count < 1) {
        [self failCommandWithCallbackId:command.callbackId message:@"autocompleteQuery needs at least 1 argument (query) to operate"];
        return;
    }
    
    id arg0 = command.arguments[0];
    if (![arg0 isKindOfClass:[NSString class]]) {
        [self failCommandWithCallbackId:command.callbackId message:@"argument 0 (query) should be a string"];
        return;
    }
    query = (NSString*)arg0;
    
    // try to decode bounds and filter, il present
    NSError* decodeError;
    if (command.arguments.count == 2) {
        id arg1 = command.arguments[1];
        BOOL argOK = false;
        
        if ([arg1 isKindOfClass:[NSDictionary class]]) {
            // bounds ?
            bounds = decodeCoordinateBounds((NSDictionary*)arg1, &decodeError);
            if (!decodeError) {
                argOK = true;
            } else {
                bounds = nil;
                filter = decodeAutocompleteFilter((NSDictionary*)arg1, &decodeError);
                if (!decodeError) {
                    argOK = true;
                }
            }
        }
        
        if (!argOK) {
            [self failCommandWithCallbackId:command.callbackId message:@"could not interpret argument 1 as either bounds or filter"];
            return;
        }
    } else if (command.arguments.count == 3) {
        id arg1 = command.arguments[1];
        if (![arg1 isKindOfClass:[NSDictionary class]]) {
            [self failCommandWithCallbackId:command.callbackId message:@"argument 1 (bounds) should be an object"];
            return;
        }
        bounds = decodeCoordinateBounds((NSDictionary*)arg1, &decodeError);
        if (decodeError) {
            [self failCommandWithCallbackId:command.callbackId message:[decodeError localizedDescription]];
            return;
        }
        
        id arg2 = command.arguments[2];
        if (![arg2 isKindOfClass:[NSDictionary class]]) {
            [self failCommandWithCallbackId:command.callbackId message:@"argument 2 (filter) should be an object"];
            return;
        }
        filter = decodeAutocompleteFilter((NSDictionary*)arg2, &decodeError);
        if (decodeError) {
            [self failCommandWithCallbackId:command.callbackId message:[decodeError localizedDescription]];
            return;
        }
    }
    
    [self autocompleteQuery:query bounds:bounds filter:filter callbackId:command.callbackId];
}

// call the Google Places API with all the arguments loaded
-(void)autocompleteQuery:(NSString*)query bounds:(GMSCoordinateBounds*) bounds filter:(GMSAutocompleteFilter*)filter callbackId:(NSString*)callbackId{
    [[GMSPlacesClient sharedClient] autocompleteQuery:query bounds:bounds filter:filter callback:^(NSArray<GMSAutocompletePrediction *> * _Nullable results, NSError * _Nullable error) {
        CDVPluginResult* result;
        
        // check for error
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            NSMutableArray<NSDictionary*>* encodedResults = [[NSMutableArray alloc] initWithCapacity:results.count];
            for(GMSAutocompletePrediction* p in results) {
                NSDictionary* encodedPrediction = encodeAutocompletePrediction(p);
                [encodedResults addObject:encodedPrediction];
            }
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:encodedResults];
        }
        
        [self.commandDelegate sendPluginResult:result callbackId:callbackId];
    }];
}

-(void)pickPlace:(CDVInvokedUrlCommand*)command {
    if (!_APIKey) {
        [self failCommandMissingAPIKey:command.callbackId];
        return;
    }
    
    // load bounds if any
    
    GMSCoordinateBounds* bounds;
    if (command.arguments.count > 1) {
        [self failCommandWithCallbackId:command.callbackId message:@"wrong arguments for pickPlace(bounds)"];
        return;
    }
    if (command.arguments.count == 1) {
        id arg0 = command.arguments[0];
        if (![arg0 isKindOfClass:[NSDictionary class]]) {
            [self failCommandWithCallbackId:command.callbackId message:@"argument 0 (bounds) should be an object"];
            return;
        }
        NSError* decodeError;
        bounds = decodeCoordinateBounds((NSDictionary*)arg0, &decodeError);
        if (decodeError) {
            [self failCommandWithCallbackId:command.callbackId message:decodeError.localizedDescription];
            return;
        }
    }
    
    // verify we are not *already* presenting
    if (_placePickerCallbackId) {
        [self failCommandWithCallbackId:command.callbackId message:@"The Place Picker is already presented onscreen !"];
        return;
    }
    
    // present the view
    GMSPlacePickerConfig* config = [[GMSPlacePickerConfig alloc] initWithViewport:bounds];
    GMSPlacePickerViewController *vc = [[GMSPlacePickerViewController alloc] initWithConfig:config];
    vc.delegate = self;
    _placePickerCallbackId = command.callbackId;
    [self.viewController presentViewController:vc animated:true completion:nil];
}

-(void)showPlaceAutocomplete:(CDVInvokedUrlCommand*)command {
    if (_placeAutocompleteCallbackId) {
        [self failCommandWithCallbackId:command.callbackId message:@"The Autocomplete view is still presented onscreen !"];
        return;
    }
    
    GMSAutocompleteViewController* vc = [[GMSAutocompleteViewController alloc] init];
    vc.delegate = self;
    _placeAutocompleteCallbackId = command.callbackId;
    [self.viewController presentViewController:vc animated:true completion:nil];
}

// MARK: Generic error handling

-(void)failCommandMissingAPIKey:(NSString*)callbackId {
    static const NSString* errorNoApiKey = @"GooglePlaces: please provide API_KEY_FOR_IOS before using the plugin.";
    [self failCommandWithCallbackId:callbackId message:errorNoApiKey];
}

-(void)failCommandWithCallbackId:(NSString*)callbackId message:(const NSString*)message {
    NSString* errorMessage = [NSString stringWithFormat:@"GooglePlaces: %@", message ];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

// MARK: Decode native Google Places types

GMSAutocompleteFilter* decodeAutocompleteFilter(NSDictionary* dict, NSError** error) {
    NSError *decodeError;
    
    id rawType = [dict objectForKey:@"type"];
    if (!rawType) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"missing \"type\" for autocomplete filter" }];
        }
        return nil;
    }
    if (![rawType isKindOfClass:[NSString class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"type\" of autocomplete filter should be an string" }];
        }
        return nil;
    }
    GMSPlacesAutocompleteTypeFilter filterType = decodeAutocompleteTypeFilter((NSString*)rawType, &decodeError);
    if (decodeError) {
        if (error) {
            *error = decodeError;
        }
        return nil;
    }

    NSString* country;
    id rawCountry = [dict objectForKey:@"country"];
    if (rawCountry) {
        if (![rawType isKindOfClass:[NSString class]]) {
            if (error) {
                *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"country\" of autocomplete filter should be an string" }];
            }
            return nil;
        }
    }
   
    GMSAutocompleteFilter* filter = [[GMSAutocompleteFilter alloc] init];
    filter.type = filterType;
    filter.country = country;
    return filter;
}

GMSPlacesAutocompleteTypeFilter decodeAutocompleteTypeFilter(NSString* s, NSError** error) {
    if ([s isEqualToString:@"no_filter"]) {
        return kGMSPlacesAutocompleteTypeFilterNoFilter;
    } else if ([s isEqualToString:@"geocode"]) {
        return kGMSPlacesAutocompleteTypeFilterGeocode;
    } else if ([s isEqualToString:@"address"]) {
        return kGMSPlacesAutocompleteTypeFilterAddress;
    } else if ([s isEqualToString:@"establishment"]) {
        return kGMSPlacesAutocompleteTypeFilterEstablishment;
    } else if ([s isEqualToString:@"region"]) {
        return kGMSPlacesAutocompleteTypeFilterRegion;
    } else if ([s isEqualToString:@"city"]) {
        return kGMSPlacesAutocompleteTypeFilterCity;
    }
    
    if (error) {
        NSString* errorMsg = [NSString stringWithFormat:@"Unknown filter type \"%@\"", s];
        *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: errorMsg }];
    }
    
    // by default: no filter
    return kGMSPlacesAutocompleteTypeFilterNoFilter;
}

GMSCoordinateBounds* decodeCoordinateBounds(NSDictionary<NSString*,id>* dict, NSError** error) {
    NSError *decodeError;
    
    id rawNorthEast = [dict objectForKey:@"northEast"];
    if (!rawNorthEast) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"missing \"northEast\" coordinate for coordinate bounds" }];
        }
        return nil;
    }
    if (![rawNorthEast isKindOfClass:[NSDictionary class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"northEast\" coordinate should be an Object" }];
        }
        return nil;
    }
    CLLocationCoordinate2D northEast = decodeCoordinate((NSDictionary*)rawNorthEast, &decodeError);
    if (decodeError) {
        if (error) {
            *error = decodeError;
        }
        return nil;
    }
    
    id rawSouthWest = [dict objectForKey:@"southWest"];
    if (!rawSouthWest) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"missing \"southWest\" coordinate for coordinate bounds" }];
        }
        return nil;
    }
    if (![rawSouthWest isKindOfClass:[NSDictionary class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"southWest\" coordinate should be an Object" }];
        }
        return nil;
    }
    CLLocationCoordinate2D southWest = decodeCoordinate((NSDictionary*)rawSouthWest, &decodeError);
    if (decodeError) {
        if (error) {
            *error = decodeError;
        }
        return nil;
    }
    
    return [[GMSCoordinateBounds alloc] initWithCoordinate:northEast coordinate:southWest];
}

CLLocationCoordinate2D decodeCoordinate(NSDictionary<NSString*,id>* dict, NSError **error) {
    id rawLatitude = [dict objectForKey:@"latitude"];
    if (!rawLatitude) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"missing \"latitude\" value for coordinate" }];
        }
        return kCLLocationCoordinate2DInvalid;
    }
    if (![rawLatitude isKindOfClass:[NSNumber class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"latitude\" value should be a number" }];
        }
        return kCLLocationCoordinate2DInvalid;
    }
    CLLocationDegrees latitude = [((NSNumber*)rawLatitude) doubleValue];
    
    id rawLongitude = [dict objectForKey:@"longitude"];
    if (!rawLongitude) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"missing \"longitude\" value for coordinate" }];
        }
        return kCLLocationCoordinate2DInvalid;
    }
    if (![rawLongitude isKindOfClass:[NSNumber class]]) {
        if (error) {
            *error = [NSError errorWithDomain:@"cordova-plugin-googleplaces" code:0 userInfo:@{ NSLocalizedDescriptionKey: @"\"longitude\" value should be a number" }];
        }
        return kCLLocationCoordinate2DInvalid;
    }
    CLLocationDegrees longitude = [((NSNumber*)rawLongitude) doubleValue];
    
    return CLLocationCoordinate2DMake(latitude, longitude);
}

// MARK: Encode native Google Places types

NSDictionary* encodeAutocompletePrediction(GMSAutocompletePrediction* prediction) {
    return @{
             @"fullText": [prediction.attributedFullText string],
             @"primaryText": [prediction.attributedPrimaryText string],
             @"secondaryText": [prediction.attributedSecondaryText string],
             @"placeID": prediction.placeID,
             @"types": prediction.types,
             };
}

NSDictionary* encodePlaceLikelihoodList(GMSPlaceLikelihoodList* list) {
    NSMutableArray<NSDictionary*>* likelihoods = [[NSMutableArray alloc] initWithCapacity:list.likelihoods.count];
    for (GMSPlaceLikelihood* l in list.likelihoods) {
        NSDictionary* encodedPlaceLikelihood = encodePlaceLikelihood(l);
        [likelihoods addObject:encodedPlaceLikelihood];
    }
    
    return @{
             @"likelihoods": likelihoods,
             @"attributions": [list.attributions string],
             };
}

NSDictionary* encodePlaceLikelihood(GMSPlaceLikelihood* likelihood) {
    NSDictionary* encodedPlace = encodePlace(likelihood.place);
    return @{
             @"place": encodedPlace,
             @"likelihood": @(likelihood.likelihood),
             };
}

NSDictionary* encodePlace(GMSPlace* place) {
    NSMutableDictionary* encodedPlace = [[NSMutableDictionary alloc]
                                         initWithDictionary:@{
                                                              @"name": place.name,
                                                              @"placeID": place.placeID
                                                              }];
    
    if (CLLocationCoordinate2DIsValid(place.coordinate)) {
        encodedPlace[@"coordinate"] = encodeCoordinate(place.coordinate);
    }
    if (place.phoneNumber) {
        encodedPlace[@"phoneNumber"] = place.phoneNumber;
    }
    if (place.formattedAddress) {
        encodedPlace[@"formattedAddress"] = place.formattedAddress;
    }
    if (place.rating != 0.0) {
        encodedPlace[@"rating"] = @(place.rating);
    }
    switch (place.openNowStatus) {
        case kGMSPlacesOpenNowStatusYes:
            encodedPlace[@"openNowStatus"] = @"yes";
            break;
        case kGMSPlacesOpenNowStatusNo:
            encodedPlace[@"openNowStatus"] = @"no";
            break;
        case kGMSPlacesOpenNowStatusUnknown:
            break;
    }
    switch (place.priceLevel) {
        case kGMSPlacesPriceLevelFree:
            encodedPlace[@"priceLevel"] = @"free";
            break;
        case kGMSPlacesPriceLevelCheap:
            encodedPlace[@"priceLevel"] = @"cheap";
            break;
        case kGMSPlacesPriceLevelMedium:
            encodedPlace[@"priceLevel"] = @"medium";
            break;
        case kGMSPlacesPriceLevelHigh:
            encodedPlace[@"priceLevel"] = @"high";
            break;
        case kGMSPlacesPriceLevelExpensive:
            encodedPlace[@"priceLevel"] = @"expensive";
            break;
        case kGMSPlacesPriceLevelUnknown:
            break;
    }
    if (place.types.count > 0) {
        encodedPlace[@"types"] = place.types;
    }
    if (place.website) {
        encodedPlace[@"website"] = [place.website description];
    }
    if (place.attributions) {
        encodedPlace[@"attributions"] = [place.attributions string];
    }
    if (place.viewport) {
        encodedPlace[@"viewport"] = encodeBounds(place.viewport);
    }
    if (place.addressComponents) {
        encodedPlace[@"addressComponents"] = encodeAddressComponents(place.addressComponents);
    }

    return encodedPlace;
}

NSDictionary* encodeBounds(GMSCoordinateBounds* bounds) {
    return @{
             @"northEast": encodeCoordinate(bounds.northEast),
             @"southWest": encodeCoordinate(bounds.southWest),
             };
}

NSDictionary* encodeCoordinate(CLLocationCoordinate2D coordinate) {
    return @{
             @"latitude": @(coordinate.latitude),
             @"longitude": @(coordinate.longitude),
             };
}

NSArray<NSDictionary*>* encodeAddressComponents(NSArray<GMSAddressComponent*>* components) {
    NSMutableArray<NSDictionary*>* result = [[NSMutableArray alloc] initWithCapacity:components.count];
    for (GMSAddressComponent* component in components) {
        NSDictionary* encodedComponent = encodeAddressComponent(component);
        [result addObject:encodedComponent];
    }
    return result;
}

NSDictionary* encodeAddressComponent(GMSAddressComponent* component) {
    return @{
             @"name": component.name,
             @"type": component.type,
             };
}


@end

@implementation CDVGooglePlaces(PlacePicker)

-(void)placePicker:(GMSPlacePickerViewController *)viewController didPickPlace:(GMSPlace *)place {
    if (_placePickerCallbackId) {
        NSDictionary* encodedPlace = encodePlace(place);
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:encodedPlace];
        [self.commandDelegate sendPluginResult:result callbackId:_placePickerCallbackId];
    }
    _placePickerCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];
}

-(void)placePicker:(GMSPlacePickerViewController *)viewController didFailWithError:(NSError *)error {
    if (_placePickerCallbackId) {
        [self failCommandWithCallbackId:_placePickerCallbackId message:error.localizedDescription];
    }
    _placePickerCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];
}

-(void)placePickerDidCancel:(GMSPlacePickerViewController *)viewController {
    if (_placePickerCallbackId) {
        // empty "valid" result
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:_placePickerCallbackId];
    }
    _placePickerCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];
}

@end

@implementation CDVGooglePlaces(Autocomplete)

-(void)viewController:(GMSAutocompleteViewController *)viewController didAutocompleteWithPlace:(GMSPlace *)place {
    if (_placeAutocompleteCallbackId) {
        NSDictionary* encodedPlace = encodePlace(place);
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:encodedPlace];
        [self.commandDelegate sendPluginResult:result callbackId:_placePickerCallbackId];
    }
    _placeAutocompleteCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];    
}

-(void)viewController:(GMSAutocompleteViewController *)viewController didFailAutocompleteWithError:(NSError *)error {
    if (_placeAutocompleteCallbackId) {
        [self failCommandWithCallbackId:_placeAutocompleteCallbackId message:error.localizedDescription];
    }
    _placeAutocompleteCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];
}

- (void)wasCancelled:(nonnull GMSAutocompleteViewController *)viewController {
    if (_placeAutocompleteCallbackId) {
        // empty result
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:_placePickerCallbackId];
    }
    _placeAutocompleteCallbackId = nil;
    [self.viewController dismissViewControllerAnimated:true completion:nil];

}


@end

