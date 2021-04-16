package com.example.fishfinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fishfinder.data.FishInfo;
import com.example.fishfinder.util.RestAPIUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchForFishActivity extends AppCompatActivity implements OnMapReadyCallback {

    /* Variables */
    private final String DEFAULT_SPECIES = "cyanellus"; // TODO: Change the default
    private final String DEFAULT_GENUS = "Lepomis";
    private String SPECIES;
    private String GENUS;
    private GoogleMap mMap;
    private FishInfo fishInfo;
    private Context ctx;

    // Set the colors that we will be using for API and Community locations

    // Settings for the user
    private boolean showUSGSLocations = true;
    private boolean showCommunitySaves = true;

    // FireBase - Database Info
    FirebaseDatabase firebase;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    //Components
    private TextView        textViewSpecies;
    private EditText        edtSearch;
    //    private TextView        tvGoogleMap; // TODO: Get rid of later
    private Button          btnGoToFish;
    private FrameLayout     map_container;

    private TextView tvShowLat;
    private TextView tvShowLong;
    private Button btnCaughtFish;

    private ProgressBar progressBarLoadResults;

    private String LatitudeClicked;
    private String LongitudeClicked;


    // TODO: Add other possible fish location APIs (Found Here: http://www.fishmap.org/technology.html)
    // 1. http://fishnet2.net/api/v1/apihelp.htm#params (Waiting for API Key)
    // 2. https://explorer.natureserve.org/api-docs/    (Haven't found latitude and longitude endpoint, How to pass data: https://stackoverflow.com/questions/52974330/spring-post-method-required-request-body-is-missing)
    // NAS API (Find coordinates of fish) - DOCUMENTATION: https://nas.er.usgs.gov/api/documentation.aspx
    private final String    APIBase = "https://nas.er.usgs.gov/api/v2/occurrence/search?";  // TODO: Add Limit to add markers incrementally
    private final String    genusQuery = "genus=";
    private final String    speciesQuery = "species=";
    private final String    spatialAccQuery = "spatialAcc=";
    private final String    ACCURATE_SPATIAL_ACCURACY = "Accurate";
    private final String    APPROXIMATE_SPATIAL_ACCURACY = "Approximate";
    private final String    CENTROID_SPATIAL_ACCURACY = "Centroid";

    private String apiResult = "";
    private DecimalFormat locationDF = new DecimalFormat("#.#####");


    ExecutorService service = Executors.newFixedThreadPool(1);

    // TODO: Add textbox for users to decide radius of markers on the map
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_for_fish);

        /* Initialize Variables */
        ctx = this.getBaseContext();

        firebase = FirebaseDatabase.getInstance(); //get the root node point of the database, this is so we can get the references based on the root node to get the desired data references
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser(); //get the current user based on the auth

        /* Get Bundle Info */
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                SPECIES = DEFAULT_SPECIES;
                GENUS = DEFAULT_GENUS;
                fishInfo = new FishInfo();
                Toast.makeText(ctx, "Error No Fish Info Found!", Toast.LENGTH_SHORT).show();
            } else {
                fishInfo = (FishInfo) extras.getSerializable("fishInfo");
                SPECIES = fishInfo.getSpecies();
                GENUS = fishInfo.getGenus();
            }
        }

        /* Initialize Components */
        textViewSpecies = findViewById(R.id.textViewSpecies);
        edtSearch       = findViewById(R.id.edtSearch);
        btnGoToFish     = findViewById(R.id.btnGoToFish);
        map_container   = findViewById(R.id.map_container);
        tvShowLat = findViewById(R.id.tvShowLat);
        tvShowLong = findViewById(R.id.tvShowLong);
        btnCaughtFish = findViewById(R.id.btnCaughtFish);
        progressBarLoadResults = (ProgressBar) findViewById(R.id.progressBarLoadResults);


        /* Setup */
        // Display the Name of the fish you are searching locations for
        textViewSpecies.setText("Searching for: " + fishInfo.getFBname());
        progressBarLoadResults.setVisibility(View.GONE);

        // Adds the map Fragment inside the map_container
        addMapFragment();

        /* Listeners */
        btnGoToFish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (showUSGSLocations) { // TODO: Move this so that it just changes the visibility of the markers
                    // Call NAS API (Finds Locations of Fish)
                    Log.i("Info", "Getting coordinates from USGS API..."); // DEBUGGING
                    String name = edtSearch.getText().toString();
                    getCordinates(name); //This is for calling the markers on the map for each state after the initial google maps initialization.
                }

            }
        });

        btnCaughtFish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToCaughtFishActivity = new Intent(v.getContext(), CaughtFishActivity.class);
                goToCaughtFishActivity.putExtra("fishInfo", fishInfo);
                goToCaughtFishActivity.putExtra("latitude", LatitudeClicked);
                goToCaughtFishActivity.putExtra("longitude", LongitudeClicked);
                //TODO other fill ins like species name and etc... to putExtra

                startActivity(goToCaughtFishActivity);
            }
        });


    }

    @Override
    public void onBackPressed() {
        /* Stop Asynchronous Thread */
        service.shutdownNow();

        super.onBackPressed();
    }

    private void getCordinates(String str) {
//        String[] pieces = str.trim().split(",");

        //Some Default Values
        final String DEFAULT_STATE     = "MA"; // TODO: Get current State you're in

        /* Clean State Input */
        String state = FishListActivity.cleanSpeciesSearch(str);
        if (str.length() <= 1) state = DEFAULT_STATE;
        else {
            state = str.trim();
            if (state.length() >= 2){
                state = state.substring(0, 2).toUpperCase();
            }
        }

        String urlString = String.format("%sspecies=%s&state=%s", APIBase, SPECIES, state);

        /* Debugging */
        Log.i("Debug", urlString);

        fetch(urlString);
//        parseCordinates(response);
    }

    private void longLatToMap(String[] cordinates){

    }

    /**
     * Fetches data from the API Endpoint URL and set's the coordinates
     * @param urlString - The URL the function will fetch data from
     */
    private void fetch(String urlString){
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    /* Send GET Request to obtain data from URL, and parse as JSON */
                    String content = RestAPIUtil.get(urlString);
                    ArrayList<LatLng> latLngCoordinates = parseCordinates(content.toString());

                    // This will post a command to the main UI Thread
                    // This is necessary so that the code knows the variables for this class
                    // https://stackoverflow.com/questions/27737769/how-to-properly-use-a-handler
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            /* Add Markers of Fish Locations */
                            for (int i = 0; i < latLngCoordinates.size(); i++) {
                                LatLng coordinates = latLngCoordinates.get(i);
                                mMap.addMarker(new MarkerOptions().position(coordinates).title("Marker " + i));
                            }

                            /* Navigate to last Marker if one exists */
                            if (latLngCoordinates.size() > 0) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLngCoordinates.get(latLngCoordinates.size() - 1)));
                            }

                        }
                    });

                } catch (Exception e){
                    e.printStackTrace();
                    System.out.println(e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     *
     * @param input - String in JSON format
     * @return LatLng Object parsed from the String JSON
     */
    private ArrayList<LatLng> parseCordinates(String input){
        StringBuffer buffer = new StringBuffer();
        ArrayList<LatLng> latLngCoordinates = new ArrayList<>();

        try {
            JSONObject job           = new JSONObject(input);
            JSONArray results        = job.getJSONArray("results");

            for (int i=0; i< results.length(); i++){
                JSONObject element   = results.getJSONObject(i);

                // Surround in try-catch in case latitudes or longitudes are not parsable as Doubles
                try {
                    Double latitude      =  element.getDouble("decimalLatitude");
                    Double longitude     = element.getDouble("decimalLongitude");

                    latLngCoordinates.add(new LatLng(latitude, longitude));
                    buffer.append(String.format("%.5f,                    %.5f\n", latitude, longitude));
                } catch (Exception e) {
                    Log.e("Error", e.getLocalizedMessage());
                }
            }
        } catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }

        return latLngCoordinates;
    }

    /* Google Map Methods */

    /**
     * This method adds map fragment to the container.
     */
    private void addMapFragment() {
        SupportMapFragment mMapFragment = SupportMapFragment.newInstance();
        mMapFragment.getMapAsync(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, mMapFragment)
                .commit();
    }

    /**
     * The method that runs when the map is ready to be displayed
     * @param googleMap - A GoogleMap Object
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        progressBarLoadResults.setVisibility(View.VISIBLE);

        /* Change Map Options */
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        /* Complete the API call to GET all LatLng's of where a fish can be caught */
        String spatialAcc = ACCURATE_SPATIAL_ACCURACY; // TODO: Decide which spatialAcc method to use (filter/settings)
        // TODO: Add search by "commonName" because there are multiple fish in a species
        String urlString = APIBase + genusQuery + GENUS + "&" + speciesQuery + SPECIES + "&" + spatialAccQuery + spatialAcc;   // API call that will get all locations this fish can be caught
        Log.i("Info", "URL: " + urlString); // DEBUGGING
        Log.i("Info", "<FishInfo> Species: " + fishInfo.getSpecies() + ", Genus: " + fishInfo.getGenus()); // DEBUGGING

        /* Reduce Marker Density */
        ArrayList<LatLng> addedCoordinates = new ArrayList<>();

        service.execute(new Runnable() {
            @Override
            public void run() {

                // Grab markers from firebase
                if (showCommunitySaves) {
                    // Show the locations from the community
//                    Log.i("Info", "Getting coordinates from Firebase Community..."); // DEBUGGING
                    DatabaseReference communitySavesReference = firebase.getReference().child("CommunitySaves");
                    communitySavesReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                // Data is each child element in the "CommunitySaves" Database Reference now
                                String genus = null;
                                String species = null;

                                try {
                                    genus = data.child("genus").getValue(String.class);
                                    species = data.child("species").getValue(String.class);
                                } catch (Exception e) {
                                    Log.e("Error", "Could not get genus or species");
                                }

//                                Log.i("Info", "<COMMUNITY> Species: " + species + ", Genus: " + genus); // DEBUGGING

                                // Check if the fish we are on matches the fish posted in the database
                                if (fishInfo.getGenus().equals(genus) && fishInfo.getSpecies().equals(species)) {

                                    Log.i("Info", "Fish Match Found On Community!"); // DEBUGGING

                                    LatLng communityCoords = null;
                                    double latitude;
                                    double longitude;

                                    try {
                                        latitude = Double.parseDouble(data.child("latitude").getValue(String.class));
                                        longitude = Double.parseDouble(data.child("longitude").getValue(String.class));
                                        communityCoords = new LatLng(latitude, longitude);
                                        Log.i("Info", "Location: " + communityCoords); // DEBUGGING

                                        MarkerOptions communityMarker = new MarkerOptions()
                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                                .alpha(1f)
                                                .position(communityCoords)
                                                .title("Community Catch!");
                                        mMap.addMarker(communityMarker);

                                    } catch(Exception e) {
                                        Log.e("Error", "Could not get Latitude or Longitude! Latitude: " + data.child("latitude").getValue(String.class) + ", Longitude: " + data.child("longitude").getValue(String.class));
                                    }

                                }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                // TODO: Somehow the latitude and longitude appear to be switched
                String lat = String.valueOf(latLng.latitude);
                String lng = String.valueOf(latLng.longitude);

                Toast.makeText(SearchForFishActivity.this, "Location:" + latLng.latitude + latLng.longitude, Toast.LENGTH_SHORT).show();

                LatitudeClicked = lat; //update what we clicked so we can pass into next screen intent if that is the case final destination clicked
                LongitudeClicked = lng;

                String latFormatted = locationDF.format(latLng.latitude); //format the latitude value to 5 decimal places
                String lngFormatted = locationDF.format(latLng.longitude); //format the longitude value to 5 decimal places to display better in tv

                tvShowLat.setText(latFormatted); //show it in a textview what was clicked onto the screen
                tvShowLong.setText(lngFormatted); //keeps track of what marker was last clicked and lets user know this is where the fish was caught if he/she intends to submit based on this clicked location
            }
        });

        //Setting up the marker click to show which one was clicked
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                // TODO: Somehow the latitude and longitude appear to be switched
                String lat = String.valueOf(marker.getPosition().latitude);
                String lng = String.valueOf(marker.getPosition().longitude);
                Toast.makeText(SearchForFishActivity.this, "Location:" + lat + lng, Toast.LENGTH_SHORT).show();

                LatitudeClicked = lat; //update what we clicked so we can pass into next screen intent if that is the case final destination clicked
                LongitudeClicked = lng;

                String latFormatted = locationDF.format(marker.getPosition().latitude); //format the latitude value to 5 decimal places
                String lngFormatted = locationDF.format(marker.getPosition().longitude); //format the longitude value to 5 decimal places to display better in tv

                tvShowLat.setText(latFormatted); //show it in a textview what was clicked onto the screen
                tvShowLong.setText(lngFormatted); //keeps track of what marker was last clicked and lets user know this is where the fish was caught if he/she intends to submit based on this clicked location


                return false;
            }
        });

        final int KILOMETER = 1000;

        /* Add Markers of Fish Locations */
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    /* Send GET Request to obtain data from URL, and parse as JSON */
                    String content = RestAPIUtil.get(urlString);
                    ArrayList<LatLng> latLngCoordinates = parseCordinates(content);

                    // This will post a command to the main UI Thread
                    // This is necessary so that the code knows the variables for this class
                    // https://stackoverflow.com/questions/27737769/how-to-properly-use-a-handler
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            /* Add Markers of Fish Locations */
                            for (int i = 0; i < latLngCoordinates.size(); i++) {
                                LatLng coordinates = latLngCoordinates.get(i);
                                if (!isInRadius(coordinates, addedCoordinates, KILOMETER * 250)) {
                                    MarkerOptions marker = new MarkerOptions().position(coordinates).title("Marker " + i);
                                    mMap.addMarker(marker);
                                    addedCoordinates.add(coordinates);
                                }
                            }

                            /* Navigate to last Marker if one exists */
                            if (latLngCoordinates.size() > 0) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLngCoordinates.get(latLngCoordinates.size() - 1)));
                            }

                            progressBarLoadResults.setVisibility(View.GONE);

                        }
                    });

                } catch (Exception e){
                    Log.e("Error","Error loading the locations of the fish!");

                    // This will post a command to the main UI Thread
                    // This is necessary so that the code knows the variables for this class
                    // https://stackoverflow.com/questions/27737769/how-to-properly-use-a-handler
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            progressBarLoadResults.setVisibility(View.GONE);
                            Toast.makeText(ctx, "Error Loading Markers!", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        });




    }

    private boolean isInRadius(LatLng coordinate, List<LatLng> coordinates, int radius) { // Radius in meters
        for (LatLng coord : coordinates) {
            if (SphericalUtil.computeDistanceBetween(coordinate, coord) < radius)
                return true;
        }
        return false;
    }

    /*----------------------*/
}