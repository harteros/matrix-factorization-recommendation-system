/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.FrontEnd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import lefteris.harteros.gr.recommendationsystemclientapp.BackEnd.ClientNode;
import lefteris.harteros.gr.recommendationsystemclientapp.BackEnd.Poi;
import lefteris.harteros.gr.recommendationsystemclientapp.R;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> latlngs = new ArrayList<LatLng>();
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    public ArrayList<Bitmap> photos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ((LinearLayout) findViewById(R.id.popup)).setVisibility(View.INVISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        ((LinearLayout) findViewById(R.id.placeholder)).setVisibility(View.GONE);
        String[] arraySpinner = new String[]{"All", "Bars", "Food", "Arts & Entertainment"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.white_spinner, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) findViewById(R.id.category)).setAdapter(adapter);


        ((ImageButton) findViewById(R.id.hide)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((LinearLayout) findViewById(R.id.popup)).setVisibility(View.INVISIBLE);
            }
        });
        findViewById(R.id.swap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((LinearLayout) findViewById(R.id.swapable)).getVisibility() == View.VISIBLE) {
                    ((EditText) findViewById(R.id.latitude)).setHint("Poi location id");
                    ((LinearLayout) findViewById(R.id.swapable)).setVisibility(View.GONE);
                    ((LinearLayout) findViewById(R.id.placeholder)).setVisibility(View.INVISIBLE);


                } else {
                    ((EditText) findViewById(R.id.latitude)).setHint("latitude");
                    ((LinearLayout) findViewById(R.id.swapable)).setVisibility(View.VISIBLE);
                    ((LinearLayout) findViewById(R.id.placeholder)).setVisibility(View.GONE);


                }
            }
        });
        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((LinearLayout) findViewById(R.id.popup)).getVisibility() == View.INVISIBLE) {
                    ((LinearLayout) findViewById(R.id.popup)).setVisibility(View.VISIBLE);

                } else {
                    boolean good = true;
                    if (((EditText) findViewById(R.id.user)).getText().toString().equals("")) {
                        ((EditText) findViewById(R.id.user)).setError("Empty field");
                        good = false;
                    }
                    if (((EditText) findViewById(R.id.latitude)).getText().toString().equals("")) {
                        ((EditText) findViewById(R.id.latitude)).setError("Empty field");
                        good = false;
                    }
                    if (((LinearLayout) findViewById(R.id.swapable)).getVisibility() == View.VISIBLE) {
                        if (((EditText) findViewById(R.id.longitude)).getText().toString().equals("")) {
                            ((EditText) findViewById(R.id.longitude)).setError("Empty field");
                            good = false;
                        }
                    }
                    if (((EditText) findViewById(R.id.pois)).getText().toString().equals("")) {
                        ((EditText) findViewById(R.id.pois)).setError("Empty field");
                        good = false;
                    }
                    if (((EditText) findViewById(R.id.radius)).getText().toString().equals("")) {
                        ((EditText) findViewById(R.id.radius)).setError("Empty field");
                        good = false;
                    }
                    if (good) {
                        Async runner = new Async();
                        runner.execute();
                    }

                }
            }
        });


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng new_york = new LatLng(40.730610, -73.935242);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new_york));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
        mMap.setTrafficEnabled(true);


    }


    public void hideSearch() {
        ((LinearLayout) findViewById(R.id.popup)).setVisibility(View.INVISIBLE);
        ((EditText) findViewById(R.id.user)).getText().clear();
        ((EditText) findViewById(R.id.latitude)).getText().clear();
        ((EditText) findViewById(R.id.longitude)).getText().clear();
        ((EditText) findViewById(R.id.pois)).getText().clear();
        ((EditText) findViewById(R.id.radius)).getText().clear();


    }

    //put the markers on the map based on the recommended poi that master returned for the specific user
    public void setMarkers(List<Poi> recommendedPoi) {
        for (Poi poi : recommendedPoi) {
            latlngs.add(new LatLng(poi.getLatitude(), poi.getLongitude()));
        }

        int i = 1;
        for (Poi point : recommendedPoi) {

            options.position(new LatLng(point.getLatitude(), point.getLongitude()));
            if (i == 1) {
                options.title("You are here");
            } else {
                options.title("Recommendation " + (i - 1));
            }
            options.snippet("Poi : " + point.getID() + "\nName : " + point.getName() + "\nCategory : " + point.getCategory());

            InfoWindowImage info = new InfoWindowImage();
            info.setImage(photos.get(i - 1));

            CustomInfoWindow customInfoWindow = new CustomInfoWindow(this);
            mMap.setInfoWindowAdapter(customInfoWindow);


            Marker m = mMap.addMarker(options);
            m.setTag(info);
            if (i == 1)
                m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            markers.add(m);

            i++;
        }

        LatLng location = latlngs.get(0);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));

    }


    public class Async extends AsyncTask<Void, Void, List<Poi>> {
        private ClientNode client;

        //connects to the server , sends the data needed to calculate the recommended pois and waits for master recommended pois
        @Override
        protected List<Poi> doInBackground(Void... voids) {
            client.initializeAndroidClient();
            client.sendQuery();
            client.getResults();
            for (Poi poi : client.getRecommendedPoi()) {//for each poi returned as best get its image from the url given and load it to the marker's info window
                URL url;
                if (!poi.getPhoto().equals("Not exists")) {
                    try {
                        url = new URL(poi.getPhoto());
                        photos.add(BitmapFactory.decodeStream(url.openConnection().getInputStream()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        photos.add(null);
                    }
                } else {
                    photos.add(null);
                }
            }
            return client.getRecommendedPoi();
        }

        //add the markers on the map and if no recommended pois found show alert message
        @Override
        protected void onPostExecute(List<Poi> result) {
            client.showResults();
            setMarkers(result);
            if (result.size() == 1) {
                AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);
                alert.setMessage("Δεν βρέθηκαν προτεινόμενα poi").create();
                alert.show();
            }
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);

        }

        //creates a new query for the client who requested it
        @Override
        protected void onPreExecute() {
            for (int i = 0; i < markers.size(); i++) {
                markers.get(i).remove();
            }
            latlngs.clear();
            photos.clear();
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            int user = Integer.parseInt(((EditText) findViewById(R.id.user)).getText().toString());
            double latitude = Double.parseDouble(((EditText) findViewById(R.id.latitude)).getText().toString());
            double longitude = -1;
            if (((LinearLayout) findViewById(R.id.swapable)).getVisibility() == View.VISIBLE) {
                longitude = Double.parseDouble(((EditText) findViewById(R.id.longitude)).getText().toString());
            }
            int pois = Integer.parseInt(((EditText) findViewById(R.id.pois)).getText().toString());
            double radius = Double.parseDouble(((EditText) findViewById(R.id.radius)).getText().toString());
            radius = radius * 0.01;
            String category = ((Spinner) findViewById(R.id.category)).getSelectedItem().toString();
            hideSearch();
            client = new ClientNode(user);
            if (((LinearLayout) findViewById(R.id.swapable)).getVisibility() == View.VISIBLE) {
                client.createQuery(latitude, longitude, pois, radius, category);
            } else {
                client.createQuery(latitude, longitude, pois, radius, category);
            }
        }

    }


}



