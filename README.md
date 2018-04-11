# cordova-plugin-googleplace

A Cordova plugin to use the Google Places SDK.

**NOTE: this is a preliminary version, as of now only iOS is supported and Android is in the making**

## Getting Started

This plugin allows to user the Google Places SDK from Cordova, for building autocomplete UIs for locations.

### Installing

```
$ SHELL COMMAND TO INSTALL
$ cordova plugin add cordova-plugin-googleplaces --variable API_KEY_FOR_IOS="XXXX"
```

### Usage

For more details you can generate the Javascript docs with `yarn doc`:

```sh
$ yarn doc
yarn run v1.1.0
$ docco www/GooglePlaces.js
docco: www/GooglePlaces.js -> docs/GooglePlaces.html
```

#### Details on the returned format for "Places"

This plugin maps naive SDKs for GooglePlaces, which have platform-specific formats for return values.
It tries as much as possible to mimick the format used on the Javascript APIs.

In the end, a typical place is returned encoded as follows (note that **many of the fields can be missing** 
depending on the contents of the request), here with sample values:

```javascript
{
  name: "The display name of the place",
  place_id: "some google id",
  //
  // ALL THE FIELDS BELOW ARE OPTIONAL AN THEIR EXISTENCE SHOULD BE CHECKED AT RUNTIME !!!
  //
  geometry: {
    // the GPS coordinate of the place, CAN BE MISSING
    location: {
      lat: 1,2345,
      lng: 6,789
    },
    // the recommended viewport to show this place on screen, CAN BE MISSING
    viewport: {
      northeast: {
        lat: 1,2345,
        lng: 6,789
      },
      southwest: {
        lat: 1,2345,
        lng: 6,789
      },
    }
  },
  // the phone number to contact the place, CAN BE MISSING
  international_phone_number: "...",
  // the complete postal address of the place, CAN BE MISSING
  formatted_address: "...",
  // the known rating of the place between 1.0 and 5.0, CAN BE MISSING
  rating: 4.3,
  // is the place opened right now ?, CAN BE MISSING
  open_now_status: true,
  // an idea of the price range of the place, between free|cheap|medium|high|expensive, CAN BE MISSING
  price_level: "medium",
  // a list of types for the place, among [the suppored types](https://developers.google.com/places/ios-api/supported_types), CAN BE MISSING
  types: [ "establishment" ],
  // the main websie for the place, CAN BE MISSING
  website: "...",
  // attribution details to the shown to the user, CAN BE MISSING
  attributions: "...",
  // a list of "detailled" address components, CAN BE MISSING OR INCOMPLEE
  address_components: [
    {
      "long_name" : "12",
      "short_name" : "12",
      "types" : [ "street_number" ]
    },
    {
      "long_name" : "Some Road",
      "short_name" : "Some Road",
      "types" : [ "route" ]
    },
    {
      "long_name" : "Some Town",
      "short_name" : "Some Town",
      "types" : [ "locality", "political" ]
    },
    {
      "long_name" : "FR",
      "short_name" : "FR",
      "types" : [ "country", "political" ]
    },
    {
      "long_name" : "75001",
      "short_name" : "75001",
      "types" : [ "postal_code" ]
    }
  ]
}
```


#### `currentPlace`

You can call the `currentPlace(success, failure)` method to find information on the current locatoin.

This method **requires** the user to have enabled geolocation in the app. 
To to so, use [`cordova-plugin-geolocation`](https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-geolocation/) (or equivalent) before calling this method.

The returned value  is composed of place informaion **and** a "likehood" estimation in percents:
```javascript
{
  likehood: 0.75, // means 75% confidence
  place: {
    // ... the place data, see above
  }
}
```

```javascript
// Authorize access to the current position using cordova-plugin-geolocation
navigator.geolocation.getCurrentPosition(function(pos) {

    // retreive the current place
    // (you don't need to pass it the current position, just ensure the app is auhorized)
    cordova.plugins.GooglePlaces.currentPlace(
        // Success callback
        place => {
          // process the place here
        },
        // Error callback
        err => console.log(err)
    );

});

```

#### `autocompleteQuery`

The `autocompleteQuery(query, [bounds], [filter], success, failure)` method find candidates ("predictions") given an input
query string.

Quey matches themselves contain a pace id and a set of description info:

```javascript
{
  description: "The full name for the place",
  // the list of matches in the description string:
  "matched_substrings" : [
    {
       "length" : 5,
       "offset" : 6
    },
    {
       "length" : 5,
       "offset" : 30
    }
  ],
  "primary_text": "additional info on the place",
  "secondary_text": "additional info on the place",
  "types": [ "establishment" ],
}
```

The `autocompleteQuery` function returns with an array of matches on success:

```javascript
cordova.plugins.GooglePlaces.autocompleteQuery("22 some random address",
  matches => {
    // process the array of matches
  },
  err => console.log(err),
);

```

This method can take optional arguments:

- `bounds` defines a regions to limit the search to.

It should be defined as a "coordinate region" object such as:

```javascript
{
  northeast: {
    lat: 1.234,
    lng: 5,667
  },
  southwest: {
    lat: 1.234,
    lng: 5,667
  }
}
```

- `filter` defines a filter to limit the results to a specific region.

Such a filter is given by a filter type taken from the `GooglePlaces.AutocompleteFilterTypes` and an (optional) country:

```javascript
{
  filter: "no_filter", // or google.plugins.GooglePlaces.AutocompleteFilterTypes.NoFilter
  country: "FR" // <= this is optional
}
```

Several values are available for the filter type:
- `AutocompleteFilterTypes.NoFilter` (or `"no_filter"`) is an empty filter; all results are returned.
- `AutocompleteFilterTypes.Geocode` (or `"geocode"`) returns only autocomplete results with a precise address. Use this type when you know the user is looking for a fully specified address.
- `AutocompleteFilterTypes.Address` (or `"address"`) returns only places that are businesses.
- `AutocompleteFilterTypes.Establishment` (or `"establishment"`) returns only places that are businesses.
- `AutocompleteFilterTypes.Region` (or `"region"`) returns only places that match one of the following types: `locality`, `sublocality`, `postal_code`, `country`, `administrative_area_level_1`, `administrative_area_level_2`
- `AutocompleteFilterTypes.City` (or `"city"`) returns only results matching `locality` or `administrative_area_level_3`.

#### `pickPlace`

`pickplace([bounds], success, [failure])` displays the native UI for picking a nearby place.

This method **requires** that the user has enabled geolocation in the app. 
To to so, use cordova-plugin-geolocation (or equivalent) before calling this method.

```javascript
cordova.plugins.GooglePlaces.pickPlace(
  place => {
    // process place information
  },
  err => console.log(err),
);
```

The optional `bounds` argument is defined as explained above:

```javascript
{
  northeast: {
    lat: 1.234,
    lng: 5,667
  },
  southwest: {
    lat: 1.234,
    lng: 5,667
  }
}
```

#### `showPlaceAutocomplete`

Displays the native UI for place autocompletion.

```javascript
cordova.plugins.GooglePlaces.showPlaceAutocomplete(
  place => {
    // process place information
  },
  err => console.log(err),
);

```

## Contributing

Feel free to contribute anytime !

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## TODO

* [x] Add Android support
* [ ] Use attribute names closer to the Javascript version of Google Places 

