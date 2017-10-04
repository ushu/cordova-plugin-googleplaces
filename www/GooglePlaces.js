/**
 * A GPS coordinate.
 */
export class Coordinate {
  /**
   * @param {!number} latitude - the latitude value.
   * @param {!number} longitude - the longitude value
   */
  constructor(latitude, longitude) {
    /**
     * the latitude value
     * @type {number}
     */
    this.latitude = latitude;
    /**
     * the longitude value
     * @type {number}
     */
    this.longitude = longitude;
  }
}

/**
 * A (rectangular) region delimited by two coordinates.
 *
 * @see https://developers.google.com/maps/documentation/ios-sdk/reference/interface_g_m_s_coordinate_bounds
 */
export class CoordinateBounds {
  /**
   * @param {!Coordinate} coord1 - the coordinate of the first corner of the region.
   * @param {!Coordinate} coord2 - the coordinate of the second corner of the region.
   */
  constructor(coord1, coord2) {
    /**
     * the coordinate of the first corner of the region.
     * @type {Coordinate}
     */
    this.coord1 = coord1;
    /**
     * the coordinate of the second corner of the region.
     * @type {Coordinate}
     */
    this.coord2 = coord2;
  }
}

/**
 * Wraps all the (non-UI) methods of the Google Places SDK to Javascript.
 */
export default class GooglePlaces {
  /**
   * Discover the place where the device is currently located.
   *
   * @param {function(results: Object[])} success - the success callback. It is passed an array of "place likehood" objects, each consisting of a "place" result and a ligkehood value between 0 and 1 (1 meaning 100% accurate).
   * @param {?function(error: string)} failure - the failure callback, which receives an error message from the SDK.
   */
  currentPlace(success, failure) {
    cordova.exec(success, failure, "CDVGooglePlaces", "currentPlace", []);
  }

  /**
   * Filers for the autocompleteQuery method.
   *
   * @type {Object}
   * @property {string} AutocompleteFilters.NoFilter An empty filter; all results are returned.
   * @property {string} AutocompleteFilters.Geocode Returns only geocoding results, rather than businesses. Use this request to disambiguate results where the specified location may be indeterminate.
   * @property {string} AutocompleteFilters.Address Returns only autocomplete results with a precise address. Use this type when you know the user is looking for a fully specified address.
   * @property {string} AutocompleteFilters.Establishment Returns only places that are businesses.
   * @property {string} AutocompleteFilters.Region Returns only places that match one of the following types: `locality`, `sublocality`, `postal_code`, `country`, `administrative_area_level_1`, `administrative_area_level_2`
   * @property {string} AutocompleteFilters.City Returns only results matching `locality` or `administrative_area_level_3`.
   */
  static AutocompleteFilters = {
    NoFilter: "no_filter",
    Geocode: "geocode",
    Address: "address",
    Establishment: "establishment",
    Region: "region",
    City: "city",
  };

  /**
   * Runs a query to offer auto-completion results from a query.
   *
   * @param {!string} query - the query, a partial address to auto-complete.
   * @param {?CoordinateBounds} bounds - a region to limit the search to.
   * @param {!string} filter - one of the filters available in GooglePlaces.AutocompleteFilters.
   * @param {?function} success - the success callback. It is passed an array of "autocomplete results" objects.
   * @param {?function} failure - the failure callback, which receives an error message from the SDK.
   */
  autocompleteQuery(query, bounds, filter, success, failure) {
    cordova.exec(success, failure, "CDVGooglePlaces", "autocompleteQeury", [
      query,
      bounds,
      filter,
    ]);
  }

  /**
   * Request place photos to display in your application.
   *
   * Note: this method returns **metadata** about the place photos, to load the actual
   * photos please call the `loadPlacePhoto` method with the metadata object.
   *
   * @param {!string} placeID - the ID of the place, obtained from Google Places.
   * @param {?function(results: Object[])} success - the success callback. It is passed an array of "place metadata" objects.
   * @param {?function(error: string)} failure - the failure callback, which receives an error message from the SDK.
   */
  lookUpPhotos(placeID, success, failure) {
    cordova.exec(success, failure, "CDVGooglePlaces", "lookupPhotos", [
      placeID,
    ]);
  }

  /**
   * Load the actual photo from photo metadata.
   *
   * Note: the photo obtained from the API will passed as a Data URI.
   *
   * @param {!string} photoMetadata - the metadata of the photo to retreive.
   * @param {?function(results: string)} success - the success callback. It is passed a string containing the photo contents as a data URI.
   * @param {?function(error: string)} failure - the failure callback, which receives an error message from the SDK.
   */
  loadPlacePhoto(photoMetadata, success, failure) {
    cordova.exec(success, failure, "CDVGooglePlaces", "loadPlacePhoto", [
      photoMetadata,
    ]);
  }
}
