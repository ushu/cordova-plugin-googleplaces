import isFunction from "lodash/isFunction";

//
// # GooglePlaces
//
// Wraps the (non-UI) methods of the Google Places SDK to Javascript.
//
class GooglePlaces {
  // ## currentPlace
  //
  // `currentPlace`(`sucess`, `failure`)
  //
  // Discover the place where the device is currently located.
  //
  // ### Important notice
  //
  // This method *requires* that the user has enabled geolocation in the app.
  // To to so, use the [dedicated cordova plugin](https://github.com/apache/cordova-plugin-geolocation) before calling this method.
  //
  // ### Parameters
  //
  // - `success` is called in case of success, it will contain "place likehood" objects
  //   with a `place` and `likehood` fields:
  //   ```javascript
  //   {
  //    place: {
  //      name: "some place name",
  //      placeID: "XXXXX"
  //    },
  //    likehood: 0.87 // <= means 87% accurate
  //   }
  //   ```
  // - `failure` is called in case of an error, with an error objects
  //
  currentPlace(success, failure) {
    cordova.exec(
      success,
      err => failure(new Error(err)),
      "GooglePlaces",
      "currentPlace",
      [],
    );
  }

  // ## autocompleteQuery
  //
  // `autocompleteQuery`(`query`, `[bounds]`, `[filter]`, `success`, `[failure]`)
  //
  // Runs a query to offer auto-completion results from a query.
  //
  // #### Note
  //
  // this method takes a *variable* number of arguments.
  //
  // ### Parameters
  //
  // - `query`: the actual query: an incomplete address.
  // - `bounds`: a region to limit the search to.
  //
  //    It should be defined as a "coordinate region" object such as:
  //
  //    ```javascript
  //    {
  //     northEast: {
  //       latitude: 1.234,
  //       longitude: 5,667
  //     },
  //     southWest: {
  //       latitude: 1.234,
  //       longitude: 5,667
  //     }
  //    }
  //    ```
  //
  // - `filter`: a filter to limit the results to a specific region.
  //
  //   Such a filter is given by a filter type taken from the `GooglePlaces.AutocompleteFilterTypes` and an (optional) country:
  //
  //   ```javascript
  //   {
  //     filter: AutocompleteFilterTypes.NoFilter,
  //     country: "FR" // <= this is optional
  //   }
  //   ```
  //
  // - `success` is called in case of success, it will contain "autocomplete prediction" objects
  //   with info fields:
  //   ```javascript
  //   {
  //     fullText: "description of the place",
  //     primaryText: "partial description of the place",
  //     secondaryText: "partial description of the place",
  //     placeID: "XXXXX",
  //     types: [ "a", "list", "of", "types", "for", "the", "result" ]
  //   }
  //   ```
  // - `failure` is called in case of an error, with an error object.
  autocompleteQuery(...args) {
    let params = [];
    let callbacks = [];

    for (let arg of args) {
      if (isFunction(arg)) {
        callbacks.push(arg);
      } else {
        params.push(arg);
      }
    }

    let success = () => {};
    let failure = () => {};
    if (callbacks.length > 0) {
      success = callbacks[0];
      if (callbacks.length > 1) {
        failure = err => callbacks[1](new Error(err));
      }
    }

    if (params.length > 3 || callbacks.length > 2) {
      const err = new Error(
        "GooglePlaces: wrong arguments for autocompleteQuery(query, bounds, filter, success, failure)",
      );
      failure(err);
      return;
    }

    cordova.exec(success, failure, "GooglePlaces", "autocompleteQuery", params);
  }

  // ## showPlaceAutocomplete
  //
  // `showPlaceAutocomplete`(`sucess`, `failure`)
  //
  // Show the native UI for Picking a nearby place
  //
  // ### Important notice
  //
  // This method *requires* that the user has enabled geolocation in the app.
  // To to so, use the [dedicated cordova plugin](https://github.com/apache/cordova-plugin-geolocation) before calling this method.
  //
  // ### Parameters
  //
  // - `bounds`: (optinal) a region to limit the search to.
  //
  //    It should be defined as a "coordinate region" object such as:
  //
  //    ```javascript
  //    {
  //     northEast: {
  //       latitude: 1.234,
  //       longitude: 5,667
  //     },
  //     southWest: {
  //       latitude: 1.234,
  //       longitude: 5,667
  //     }
  //    }
  //    ```
  //
  // - `success` is called in case of success, it will contain "place" objects
  //   with a `place` and `likehood` fields:
  //   ```javascript
  //   {
  //      name: "some place name",
  //      placeID: "XXXXX",
  //      // and lots of other fields, depending on the place info available
  //   }
  //   ```
  // - `failure` is called in case of an error, with an error objects
  //
  pickPlace(...args) {
    let params = [];
    let callbacks = [];

    for (let arg of args) {
      if (isFunction(arg)) {
        callbacks.push(arg);
      } else {
        params.push(arg);
      }
    }

    let success = () => {};
    let failure = () => {};
    if (callbacks.length > 0) {
      success = callbacks[0];
      if (callbacks.length > 1) {
        failure = err => callbacks[1](new Error(err));
      }
    }

    if (params.length > 1 || callbacks.length > 2) {
      const err = new Error(
        "GooglePlaces: wrong arguments for pickPlace(bounds, success, failure)",
      );
      failure(err);
      return;
    }

    cordova.exec(success, failure, "GooglePlaces", "pickPlace", []);
  }

  // ## showPlaceAutocomplete
  //
  // `showPlaceAutocomplete`(`sucess`, `failure`)
  //
  // Show the native UI for Place Autocomplete
  //
  // ### Parameters
  //
  // - `success` is called in case of success, it will contain "place" objects
  //   with a `place` and `likehood` fields:
  //   ```javascript
  //   {
  //      name: "some place name",
  //      placeID: "XXXXX",
  //      // and lots of other fields, depending on the place info available
  //   }
  //   ```
  // - `failure` is called in case of an error, with an error objects
  //
  showPlaceAutocomplete(success, failure) {
    cordova.exec(
      success,
      err => failure(new Error(err)),
      "GooglePlaces",
      "showPlaceAutocomplete",
      [],
    );
  }
}

// ## Filters for the `autocompleteQuery` method.
const AutocompleteFilterTypes = {
  // - `AutocompleteFilterTypes.NoFilter` is an empty filter; all results are returned.
  NoFilter: "no_filter",
  // - `AutocompleteFilterTypes.Geocode` returns only autocomplete results with a precise address. Use this type when you know the user is looking for a fully specified address.
  Geocode: "geocode",
  // - `AutocompleteFilterTypes.Address` returns only places that are businesses.
  Address: "address",
  // - `AutocompleteFilterTypes.Establishment` returns only places that are businesses.
  Establishment: "establishment",
  // - `AutocompleteFilterTypes.Region` returns only places that match one of the following types: `locality`, `sublocality`, `postal_code`, `country`, `administrative_area_level_1`, `administrative_area_level_2`
  Region: "region",
  // - `AutocompleteFilterTypes.City` returns only results matching `locality` or `administrative_area_level_3`.
  City: "city",
};

module.exports = new GooglePlaces();
module.exports.AutocompleteFilterTypes = AutocompleteFilterTypes;
