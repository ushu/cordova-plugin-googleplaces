package plugin.google.places;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CordovaGooglePlaces extends CordovaPlugin implements GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "CordovaGooglePlaces";
    public static final int PLACE_PICKER_REQUEST = 1;
    public static final int PLACE_AUTOCOMPLETE_REQUEST = 2;

    private CallbackContext mPickPlaceCallbackContext;
    private CallbackContext mShowPlaceAutocompleteCallbackContext;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        mGoogleApiClient = new GoogleApiClient
                .Builder(cordova.getActivity())
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        try {
            if (action.equals("currentPlace")) {
                currentPlace(callbackContext);
                return true;
            } else if (action.equals("autocompleteQuery")) {
                autocompleteQuery(args, callbackContext);
                return true;
            } else if (action.equals("pickPlace")) {
                pickPlace(args, callbackContext);
                return true;
            } else if (action.equals("showPlaceAutocomplete")) {
                showPlaceAutocomplete(callbackContext);
                return true;
            }
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "Error while loading Google Play Services", e);
            callbackContext.error(e.getMessage());
            return true;
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, "Error while using Google Play Services", e);
            callbackContext.error(e.getMessage());
            return true;
        }

        return false;
    }

    private void currentPlace(final CallbackContext callbackContext) throws JSONException {
        if (ActivityCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callbackContext.error("Missing permission ACCESS_FINE_LOCATION");
            return;
        }
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                try {
                    JSONArray result = new JSONArray();
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        JSONObject encodedLikelihood = encodePlaceLikelihood(placeLikelihood);
                        result.put(encodedLikelihood);
                    }
                    callbackContext.success(result);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                } finally {
                    likelyPlaces.release();
                }
            }
        });
    }

    private void autocompleteQuery(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (args.length() > 1) {
            callbackContext.error("wrong arguments for autocompleteQuery(query, bounds, filter)");
            return;
        }
        if (args.length() < -1) {
            callbackContext.error("autocompleteQuery needs at least 1 argument (query) to operate");
            return;
        }

        Object rawQuery = args.get(0);
        if (!(rawQuery instanceof String)) {
            callbackContext.error("argument 0 (query) should be a string");
            return;
        }
        String query = (String)rawQuery;

        LatLngBounds bounds = null;
        AutocompleteFilter filter = null;
        if (args.length() == 2) {
            Object rawArg1 = args.get(1);
            if (!(rawQuery instanceof JSONObject)) {
                callbackContext.error("could not interpret argument 1 as either bounds or filter");
                return;
            }
            JSONObject arg1 = (JSONObject)rawArg1;

            try {
                bounds = decodeCoordinateBounds(arg1);
            } catch (JSONException e) {
                bounds = null;
                try {
                    filter = decodeAutocompleteFilter(arg1);
                } catch (JSONException e2) {
                    callbackContext.error("could not interpret argument 1 as either bounds or filter");
                    return;
                }
            }
        } else {
            Object rawArg1 = args.get(1);
            if (!(rawArg1 instanceof String)) {
                callbackContext.error("argument 1 (bounds) should be an object");
                return;
            }
            JSONObject arg1 = (JSONObject)rawArg1;
            bounds = decodeCoordinateBounds(arg1);

            Object rawArg2 = args.get(2);
            if (!(rawArg2 instanceof String)) {
                callbackContext.error("argument 2 (filter) should be an object");
                return;
            }
            JSONObject arg2 = (JSONObject)rawArg2;
            filter = decodeAutocompleteFilter(arg2);
        }

        PendingResult<AutocompletePredictionBuffer> result =
                Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, query, bounds, filter);
        result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
            @Override
            public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                try {
                    JSONArray result = new JSONArray();
                    for (AutocompletePrediction autocompletePrediction : autocompletePredictions) {
                        JSONObject encodedLikelihood = encodeAutocompletePrediction(autocompletePrediction);
                        result.put(encodedLikelihood);
                    }
                    callbackContext.success(result);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                } finally {
                    autocompletePredictions.release();
                }
            }
        });
    }

    private void pickPlace(JSONArray args, CallbackContext callbackContext) throws JSONException, GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        LatLngBounds bounds = null;

        if (args.length() > 1) {
            callbackContext.error("wrong arguments for pickPlace(bounds)");
            return;
        }
        if (args.length() == 1) {
            Object rawArg0 = args.get(0);
            if (!(rawArg0 instanceof JSONObject)) {
                callbackContext.error("argument 0 (bounds) should be an object");
                return;
            }
            bounds = decodeCoordinateBounds((JSONObject)rawArg0);
        }

        // set this plugin as the callback for the next activity result
        cordova.setActivityResultCallback(this);

        // open activity
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        if (bounds != null) {
            builder.setLatLngBounds(bounds);
        }

        Activity activity = cordova.getActivity();
        Intent intent = builder.build(activity);
        activity.startActivityForResult(intent, PLACE_PICKER_REQUEST);

        mPickPlaceCallbackContext = callbackContext;
    }

    private void showPlaceAutocomplete(CallbackContext callbackContext) throws JSONException, GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        // set this plugin as the callback for the next activity result
        cordova.setActivityResultCallback(this);

        PlaceAutocomplete.IntentBuilder builder =  new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);
        Activity activity = cordova.getActivity();
        Intent intent = builder.build(activity);
        activity.startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST);

        mShowPlaceAutocompleteCallbackContext = callbackContext;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        CallbackContext callbackContext;

        switch (requestCode) {
            case PLACE_PICKER_REQUEST:
                callbackContext = mPickPlaceCallbackContext;
                if (callbackContext == null) {
                    return;
                }
                mPickPlaceCallbackContext = null;

                if (resultCode == Activity.RESULT_OK) {
                    Place place = PlacePicker.getPlace(cordova.getActivity(), intent);
                    try {
                        JSONObject encodedPlace = encodePlace(place);
                        callbackContext.success(encodedPlace);
                    } catch (JSONException e) {
                        callbackContext.error(e.getMessage());
                    }
                } else if (resultCode == PlacePicker.RESULT_ERROR) {
                    Status status = PlacePicker.getStatus(cordova.getActivity(), intent);
                    callbackContext.error(status.getStatusMessage());
                } else {
                    // canceled: send no result
                    callbackContext.success();
                }
                break;
            case PLACE_AUTOCOMPLETE_REQUEST:
                callbackContext = mShowPlaceAutocompleteCallbackContext;
                if (callbackContext == null) {
                    return;
                }
                mShowPlaceAutocompleteCallbackContext = null;

                if (resultCode == Activity.RESULT_OK) {
                    Place place = PlacePicker.getPlace(cordova.getActivity(), intent);
                    try {
                        JSONObject encodedPlace = encodePlace(place);
                        callbackContext.success(encodedPlace);
                    } catch (JSONException e) {
                        callbackContext.error(e.getMessage());
                    }
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(cordova.getActivity(), intent);
                    callbackContext.error(status.getStatusMessage());
                } else {
                    // canceled: send no result
                    callbackContext.success();
                }
                break;
        }
    }

    private AutocompleteFilter decodeAutocompleteFilter(JSONObject obj) throws  JSONException {
        Object rawType = obj.get("type");
        if (!(rawType instanceof String)) {
            throw new JSONException("\"type\" should be an String");
        }
        String typeName = (String)rawType;
        int type = decodeAutocompleteFilterType(typeName);

        String country = null;
        Object rawCountry = obj.optString("country");
        if (rawCountry == null) {
            country = (String)rawType;
        } else if (!(rawCountry instanceof String)) {
            throw new JSONException("\"country\" should be an String");
        }

        AutocompleteFilter.Builder builder = new AutocompleteFilter.Builder();
        builder.setTypeFilter(type);
        if (country != null) {
            builder.setCountry(country);
        }
        return builder.build();
    }

    private int decodeAutocompleteFilterType(String s) throws JSONException {
        if (s.equals("no_filter")) {
            return AutocompleteFilter.TYPE_FILTER_NONE;
        } else if (s.equals("geocode")) {
            return AutocompleteFilter.TYPE_FILTER_GEOCODE;
        } else if (s.equals("address")) {
            return AutocompleteFilter.TYPE_FILTER_ADDRESS;
        } else if (s.equals("establishment")) {
            return AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT;
        } else if (s.equals("region")) {
            return AutocompleteFilter.TYPE_FILTER_REGIONS;
        } else if (s.equals("city")) {
            return AutocompleteFilter.TYPE_FILTER_CITIES;
        }
        throw new JSONException("Unknown filter type" + s);
    }

    private LatLngBounds decodeCoordinateBounds(JSONObject obj) throws JSONException {
        Object rawNorthEast = obj.get("northEast");
        if (!(rawNorthEast instanceof JSONObject)) {
            throw new JSONException("\"northEast\" should be an object");
        }
        LatLng northEast = decodeCoordinate((JSONObject) rawNorthEast);

        Object rawSouthWest = obj.get("southWest");
        if (!(rawSouthWest instanceof JSONObject)) {
            throw new JSONException("\"southWest\" should be an object");
        }
        LatLng southWest = decodeCoordinate((JSONObject) rawSouthWest);

        return new LatLngBounds(northEast, southWest);
    }

    private LatLng decodeCoordinate(JSONObject obj) throws JSONException {
        Object rawLatitude = obj.get("latitude");
        if (!(rawLatitude instanceof Double)) {
            throw new JSONException("\"latitude\" should be a number");
        }
        double latitude = (Double)rawLatitude;

        Object rawLongitude = obj.get("longitude");
        if (!(rawLongitude instanceof Double)) {
            throw new JSONException("\"longitude\" should be a number");
        }
        double longitude = (Double)rawLongitude;

        return new LatLng(latitude, longitude);
    }

    private  JSONObject encodeAutocompletePrediction(AutocompletePrediction prediciton) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("fullText", prediciton.getFullText(null).toString());
        result.put("primaryText", prediciton.getPrimaryText(null).toString());
        result.put("secondaryText", prediciton.getSecondaryText(null).toString());
        result.put("placeID", prediciton.getPlaceId());
        result.put("types", encodePlaceTypes(prediciton.getPlaceTypes()));
        return result;
    }

    private JSONObject encodePlaceLikelihood(PlaceLikelihood placeLikelihood) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("likelihood", placeLikelihood.getLikelihood());
        result.put("place", encodePlace(placeLikelihood.getPlace()));
        return result;
    }

    private JSONObject encodePlace(Place place) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("name", place.getName());
        result.put("placeID", place.getId());

        if (place.getPhoneNumber() != null) {
            result.put("phoneNumber", place.getPhoneNumber());
        }
        if (place.getAddress() != null) {
            result.put("formattedAddress", place.getAddress());
        }
        if (place.getRating() != 0.0) {
            result.put("rating", place.getRating());
        }
        List<Integer> types = place.getPlaceTypes();
        if (types != null) {
            result.put("types", encodePlaceTypes(types));
        }
        if (place.getPriceLevel() >= 0) {
            switch (place.getPriceLevel()) {
                case 0:
                    result.put("priceLevel", "free");
                    break;
                case 1:
                    result.put("priceLevel", "cheap");
                    break;
                case 2:
                    result.put("priceLevel", "medium");
                    break;
                case 3:
                    result.put("priceLevel", "high");
                    break;
                case 4:
                    result.put("priceLevel", "expensive");
                    break;
            }
        }
        if (place.getLatLng() != null) {
            result.put("coordinate", encodeCoordinate(place.getLatLng()));
        }
        if (place.getWebsiteUri() != null) {
            result.put("website", place.getWebsiteUri().toString());
        }
        if (place.getViewport() != null) {
            result.put("viewport", encodeCoordinateBounds(place.getViewport()));
        }
        if (place.getAttributions() != null) {
            result.put("attributions", place.getAttributions());
        }

        return result;
    }

    private JSONObject encodeCoordinateBounds(LatLngBounds bounds) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("northEast", encodeCoordinate(bounds.northeast));
        result.put("southWest", encodeCoordinate(bounds.southwest));
        return result;
    }

    private JSONObject encodeCoordinate(LatLng latLng) throws JSONException {
        if (latLng == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.put("latitude", latLng.latitude);
        result.put("longitude", latLng.longitude);
        return result;
    }

    private JSONArray encodePlaceTypes(List<Integer> types) {
        if (types == null) {
            return null;
        }
        JSONArray results = new JSONArray();
        for (int t: types) {
            String type = encodePlaceType(t);
            results.put(type);
        }
        return results;
    }

    private String encodePlaceType(int type) {
        switch (type) {
            case Place.TYPE_OTHER: return "other";
            case Place.TYPE_ACCOUNTING: return "accounting";
            case Place.TYPE_AIRPORT: return "airport";
            case Place.TYPE_AMUSEMENT_PARK: return "amusement_park";
            case Place.TYPE_AQUARIUM: return "aquarium";
            case Place.TYPE_ART_GALLERY: return "art_gallery";
            case Place.TYPE_ATM: return "atm";
            case Place.TYPE_BAKERY: return "bakery";
            case Place.TYPE_BANK: return "bank";
            case Place.TYPE_BAR: return "bar";
            case Place.TYPE_BEAUTY_SALON: return "beauty_salon";
            case Place.TYPE_BICYCLE_STORE: return "bicycle_store";
            case Place.TYPE_BOOK_STORE: return "book_store";
            case Place.TYPE_BOWLING_ALLEY: return "bowling_alley";
            case Place.TYPE_BUS_STATION: return "bus_station";
            case Place.TYPE_CAFE: return "cafe";
            case Place.TYPE_CAMPGROUND: return "campground";
            case Place.TYPE_CAR_DEALER: return "car_dealer";
            case Place.TYPE_CAR_RENTAL: return "car_rental";
            case Place.TYPE_CAR_REPAIR: return "car_repair";
            case Place.TYPE_CAR_WASH: return "car_wash";
            case Place.TYPE_CASINO: return "casino";
            case Place.TYPE_CEMETERY: return "cemetery";
            case Place.TYPE_CHURCH: return "church";
            case Place.TYPE_CITY_HALL: return "city_hall";
            case Place.TYPE_CLOTHING_STORE: return "clothing_store";
            case Place.TYPE_CONVENIENCE_STORE: return "convenience_store";
            case Place.TYPE_COURTHOUSE: return "courhouse";
            case Place.TYPE_DENTIST: return "dentist";
            case Place.TYPE_DEPARTMENT_STORE: return "department_store";
            case Place.TYPE_DOCTOR: return "doctor";
            case Place.TYPE_ELECTRICIAN: return "electrician";
            case Place.TYPE_ELECTRONICS_STORE: return "electronics_store";
            case Place.TYPE_EMBASSY: return "embassy";
            case Place.TYPE_ESTABLISHMENT: return "establishment";
            case Place.TYPE_FINANCE: return "finance";
            case Place.TYPE_FIRE_STATION: return "fire_station";
            case Place.TYPE_FLORIST: return "florist";
            case Place.TYPE_FOOD: return "food";
            case Place.TYPE_FUNERAL_HOME: return "funeral_home";
            case Place.TYPE_FURNITURE_STORE: return "furniture_store";
            case Place.TYPE_GAS_STATION: return "gas_station";
            case Place.TYPE_GENERAL_CONTRACTOR: return "general_contractor";
            case Place.TYPE_GROCERY_OR_SUPERMARKET: return "grocery_or_supermarket";
            case Place.TYPE_GYM: return "gym";
            case Place.TYPE_HAIR_CARE: return "hair_care";
            case Place.TYPE_HARDWARE_STORE: return "hardware_store";
            case Place.TYPE_HEALTH: return "health";
            case Place.TYPE_HINDU_TEMPLE: return "hindu_temple";
            case Place.TYPE_HOME_GOODS_STORE: return "home_goods_store";
            case Place.TYPE_HOSPITAL: return "hospital";
            case Place.TYPE_INSURANCE_AGENCY: return "insurance_agency";
            case Place.TYPE_JEWELRY_STORE: return "jewelry_store";
            case Place.TYPE_LAUNDRY: return "lanudry";
            case Place.TYPE_LAWYER: return "lawyer";
            case Place.TYPE_LIBRARY: return "library";
            case Place.TYPE_LIQUOR_STORE: return "liquor_store";
            case Place.TYPE_LOCAL_GOVERNMENT_OFFICE: return "local_government_office";
            case Place.TYPE_LOCKSMITH: return "locksmith";
            case Place.TYPE_LODGING: return "lodging";
            case Place.TYPE_MEAL_DELIVERY: return "meal_delivery";
            case Place.TYPE_MEAL_TAKEAWAY: return "meal_takeway";
            case Place.TYPE_MOSQUE: return "mosque";
            case Place.TYPE_MOVIE_RENTAL: return "movie_rental";
            case Place.TYPE_MOVIE_THEATER: return "movie_theatre";
            case Place.TYPE_MOVING_COMPANY: return "moving_company";
            case Place.TYPE_MUSEUM: return "museum";
            case Place.TYPE_NIGHT_CLUB: return "night_club";
            case Place.TYPE_PAINTER: return "painter";
            case Place.TYPE_PARK: return "park";
            case Place.TYPE_PARKING: return "parking";
            case Place.TYPE_PET_STORE: return "pet_store";
            case Place.TYPE_PHARMACY: return "pharmacy";
            case Place.TYPE_PHYSIOTHERAPIST: return "physiotherapist";
            case Place.TYPE_PLACE_OF_WORSHIP: return "place_of_worship";
            case Place.TYPE_PLUMBER: return "plumber";
            case Place.TYPE_POLICE: return "police";
            case Place.TYPE_POST_OFFICE: return "post_office";
            case Place.TYPE_REAL_ESTATE_AGENCY: return "real_estate_agency";
            case Place.TYPE_RESTAURANT: return "restaurant";
            case Place.TYPE_ROOFING_CONTRACTOR: return "roofing_contractor";
            case Place.TYPE_RV_PARK: return "rv_park";
            case Place.TYPE_SCHOOL: return "school";
            case Place.TYPE_SHOE_STORE: return "shoe_store";
            case Place.TYPE_SHOPPING_MALL: return "shopping_mall";
            case Place.TYPE_SPA: return "spa";
            case Place.TYPE_STADIUM: return "stadium";
            case Place.TYPE_STORAGE: return "storage";
            case Place.TYPE_STORE: return "store";
            case Place.TYPE_SUBWAY_STATION: return "subway_station";
            case Place.TYPE_SYNAGOGUE: return "synagogue";
            case Place.TYPE_TAXI_STAND: return "taxi_stand";
            case Place.TYPE_TRAIN_STATION: return "train_station";
            case Place.TYPE_TRAVEL_AGENCY: return "travel_agency";
            case Place.TYPE_UNIVERSITY: return "university";
            case Place.TYPE_VETERINARY_CARE: return "veterinary_case";
            case Place.TYPE_ZOO: return "zoo";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_1: return "administrative_area_level_1";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_2: return "administrative_area_level_2";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_3: return "administrative_area_level_3";
            case Place.TYPE_COLLOQUIAL_AREA: return "colloquial_area";
            case Place.TYPE_COUNTRY: return "country";
            case Place.TYPE_FLOOR: return "floor";
            case Place.TYPE_GEOCODE: return "geocode";
            case Place.TYPE_INTERSECTION: return "intersection";
            case Place.TYPE_LOCALITY: return "locality";
            case Place.TYPE_NATURAL_FEATURE: return "natural_feature";
            case Place.TYPE_NEIGHBORHOOD: return "neighborhood";
            case Place.TYPE_POLITICAL: return "political";
            case Place.TYPE_POINT_OF_INTEREST: return "point_of_interest";
            case Place.TYPE_POST_BOX: return "post_box";
            case Place.TYPE_POSTAL_CODE: return "postal_code";
            case Place.TYPE_POSTAL_CODE_PREFIX: return "postal_code_prefix";
            case Place.TYPE_POSTAL_TOWN: return "postal_town";
            case Place.TYPE_PREMISE: return "premise";
            case Place.TYPE_ROOM: return "room";
            case Place.TYPE_ROUTE: return "route";
            case Place.TYPE_STREET_ADDRESS: return "street_address";
            case Place.TYPE_SUBLOCALITY: return "sublocality";
            case Place.TYPE_SUBLOCALITY_LEVEL_1: return "sublocality_level_1";
            case Place.TYPE_SUBLOCALITY_LEVEL_2: return "sublocality_level_2";
            case Place.TYPE_SUBLOCALITY_LEVEL_3: return "sublocality_level_3";
            case Place.TYPE_SUBLOCALITY_LEVEL_4: return "sublocality_level_4";
            case Place.TYPE_SUBLOCALITY_LEVEL_5: return "sublocality_level_5";
            case Place.TYPE_SUBPREMISE: return "subpremise";
            case Place.TYPE_SYNTHETIC_GEOCODE: return "synthetic_geocode";
            case Place.TYPE_TRANSIT_STATION: return "transit_station";
        }

        // defaults to "other"
        return "other";
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Could not initalize Google Places SDK: " + connectionResult.getErrorMessage());
    }
}
