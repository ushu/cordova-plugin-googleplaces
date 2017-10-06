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

#### `currentPlace`

You can call the `currentPlace` method to find information on the current locatoin.

This method **requires** that the user has enabled geolocation in the app. 
To to so, use cordova-plugin-geolocation (or equivalent) before calling this method.

```javascript
// Authorize access to the current position using cordova-plugin-geolocation
navigator.geolocation.getCurrentPosition(function(pos) {

  // retreive the current place
  cordova.plugins.GooglePlaces.currentPlace(
    // place contains the API result
    place => {
      console.log(place);
      //   {
      //    place: {
      //      name: "some place name",
      //      placeID: "XXXXX"
      //    },
      //    likehood: 0.87 // <= means 87% accurate
      //   }
    },
    err => console.log(err)
  );
});

```

#### `autocompleteQuery`

The `autocompleteQuery(query, [bounds], [filter], success, failure)` method find candidates ("predictions") given an input
query string:

```javascript
cordova.plugins.GooglePlaces.autocompleteQuery("22 some random address",
  // results contains an array of predictions, the first being the most pobable
  results => {
    console.log(res);
    //   {
    //     fullText: "description of the place",
    //     primaryText: "partial description of the place",
    //     secondaryText: "partial description of the place",
    //     placeID: "XXXXX",
    //     types: [ "a", "list", "of", "types", "for", "the", "result" ]
    //   }
  },
  err => console.log(err),
);

```

This method can take optional arguments:

- `bounds` defines a regions to limit the search to.
  
  It should be defined as a "coordinate region" object such as:
  
  ```javascript
  {
   northEast: {
     latitude: 1.234,
     longitude: 5,667
   },
   southWest: {
     latitude: 1.234,
     longitude: 5,667
   }
  }
  ```

- `filter` defines a filter to limit the results to a specific region.

  Such a filter is given by a filter type taken from the `GooglePlaces.AutocompleteFilterTypes` and an (optional) country:

  ```javascript
  {
    filter: google.plugins.GooglePlaces.AutocompleteFilterTypes.NoFilter,
    country: "FR" // <= this is optional
  }
  ```

  Several values are available for the filter type:
  - `AutocompleteFilterTypes.NoFilter` is an empty filter; all results are returned.
  - `AutocompleteFilterTypes.Geocode` returns only autocomplete results with a precise address. Use this type when you know the user is looking for a fully specified address.
  - `AutocompleteFilterTypes.Address` returns only places that are businesses.
  - `AutocompleteFilterTypes.Establishment` returns only places that are businesses.
  - `AutocompleteFilterTypes.Region` returns only places that match one of the following types: `locality`, `sublocality`, `postal_code`, `country`, `administrative_area_level_1`, `administrative_area_level_2`
  - `AutocompleteFilterTypes.City` returns only results matching `locality` or `administrative_area_level_3`.

#### `pickPlace`

`pickplace([bounds], success, [failure])` displays the native UI for picking a nearby place.

This method **requires** that the user has enabled geolocation in the app. 
To to so, use cordova-plugin-geolocation (or equivalent) before calling this method.

```javascript
cordova.plugins.GooglePlaces.pickPlace(
  // place contains complete place information on the selected target
  place => {
    console.log(res);
    //   {
    //      name: "some place name",
    //      placeID: "XXXXX",
    //      // and lots of other fields, depending on the place info available
    //   }
  },
  err => console.log(err),
);
```

The optional `bounds` argument is defined as explained above:
  
```javascript
{
 northEast: {
   latitude: 1.234,
   longitude: 5,667
 },
 southWest: {
   latitude: 1.234,
   longitude: 5,667
 }
}
```

#### `showPlaceAutocomplete`

Displays the native UI for place autocompletion.

```javascript
cordova.plugins.GooglePlaces.showPlaceAutocomplete(
  // place contains complete place information on the selected target
  place => {
    console.log(res);
    //   {
    //      name: "some place name",
    //      placeID: "XXXXX",
    //      // and lots of other fields, depending on the place info available
    //   }
  },
  err => console.log(err),
);

```

## Contributing

Feel free to contribute anytime !

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## TODO

* [ ] Add Android support

