package com.example.matant.gpsportclient.Controllers.Fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.matant.gpsportclient.Controllers.DBcontroller;
import com.example.matant.gpsportclient.InterfacesAndConstants.AsyncResponse;
import com.example.matant.gpsportclient.InterfacesAndConstants.Constants;
import com.example.matant.gpsportclient.InterfacesAndConstants.OnLocationChangedListener;
import com.example.matant.gpsportclient.R;
import com.example.matant.gpsportclient.Utilities.LocationTool;
import com.example.matant.gpsportclient.Utilities.MapMarker;
import com.example.matant.gpsportclient.Utilities.SessionManager;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Thia Fragment handle the Home screen and loading the events that close to the current user location
 * Created by matant on 8/24/2015.
 */
public class GoogleMapFragmentController extends Fragment implements AsyncResponse, OnLocationChangedListener, com.google.android.gms.location.LocationListener{

    MapView mMapView;
    private GoogleMap googleMap;
    private MarkerOptions currentMarkerOption;
    private Marker currentMarker;
    private List<MapMarker> eventsMarkers;
    private HashMap<Marker, MapMarker> mMarkersHashMap;
    private SessionManager sm;
    private LatLng iniLoc;
    private DBcontroller dbController;
    private ProgressDialog progress;
    private LayoutInflater inflater;
    private Fragment fragment = null;
    private LocationTool locationTool;
    private String mode;
    private double lati, longi;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflat and return the layout
        this.inflater = inflater;
        mMarkersHashMap = new HashMap<Marker, MapMarker>();
        View v = inflater.inflate(R.layout.fragment_google_map_fragment_controller, container, false);
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();//    display map immediately
        sm = SessionManager.getInstance(getActivity());
        locationTool = new LocationTool(this, this);

        if (mMapView!=null)
            Log.d("mMapView!=null", "mMapView!=null");

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        googleMap = mMapView.getMap();
        if (googleMap != null)
        {
            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
            {
                @Override
                public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker)
                {
                    marker.showInfoWindow();
                    return true;
                }
            });

            googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {
                    MapMarker mapMarker = mMarkersHashMap.get(marker);
                    if (mapMarker != null) { //if marker == null this marker isn't in the hash => current location marker
                        fragment = new ViewEventFragmentController();
                        Bundle args = new Bundle();
                        args.putString("event",mapMarker.getmJsonObject().toString());
                        fragment.setArguments(args);
                        if (fragment != null) {
                            FragmentManager fragmentManager = getFragmentManager();
                            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
                        } else {
                            Log.e("GoogleMap Fragment", "Error in creating fragment");
                        }
                    }
                }
            });

        }
        else
            Toast.makeText(this.getActivity(), "Unable to create Maps", Toast.LENGTH_SHORT).show();

        Log.d("googleMap", "googleMap");

        mode = Constants.MODE_SEARCH_DEF;
        Bundle b = getArguments();
        if(b != null) {
            mode = b.getString(Constants.TAG_REQUEST);
            lati = Double.valueOf(b.getString(Constants.TAG_LAT));
            longi = Double.valueOf(b.getString(Constants.TAG_LONG));
            Log.d("b!=null",b.getString(Constants.TAG_LAT) + " " + b.getString(Constants.TAG_LONG));
            Log.d("mode",mode);

            if (mode.equals(Constants.MODE_SEARCH_REQ)) {
                getActivity().setTitle("Search Results");
                Log.d("mode","==search by REQ");
                try {
                    JSONObject json = new JSONObject(b.getString("json"));
                    Log.d("events from search", json.toString());
                    GetEventsFromJSONToMapMarkers(json);
                    updateUI();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mode.equals(Constants.MODE_SEARCH_DEF))
            if (locationTool.getmGoogleApiClient() != null)
                locationTool.getmGoogleApiClient().connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if(progress !=null)
            progress.dismiss();
        if (mode.equals(Constants.MODE_SEARCH_DEF)) {
            locationTool.checkPlayServices();
            if (locationTool.getmGoogleApiClient().isConnected() && !locationTool.ismRequestingLocationUpdates()) {
               startLocationUpdates();
             }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if(progress !=null)
            progress.dismiss();
        if (mode.equals(Constants.MODE_SEARCH_DEF))
            if (locationTool.getmGoogleApiClient().isConnected())
                stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(progress !=null)
            progress.dismiss();
        if (mode.equals(Constants.MODE_SEARCH_DEF))
            if (locationTool.getmGoogleApiClient().isConnected())
                locationTool.getmGoogleApiClient().disconnect();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mode.equals(Constants.MODE_SEARCH_DEF)) {
            if (locationTool.getmGoogleApiClient().isConnected()) {
                stopLocationUpdates();
                locationTool.getmGoogleApiClient().disconnect();
            }
        }
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public void startLocationUpdates() {
        if (mode.equals(Constants.MODE_SEARCH_DEF)) {
            if (locationTool.getmLocationRequest() != null)
                LocationServices.FusedLocationApi.requestLocationUpdates(locationTool.getmGoogleApiClient(), locationTool.getmLocationRequest(), this);
        }
    }

    public void stopLocationUpdates() {
        if (mode.equals(Constants.MODE_SEARCH_DEF))
            LocationServices.FusedLocationApi.removeLocationUpdates(locationTool.getmGoogleApiClient(), this);
    }
    @Override
    public void handleResponse(String resStr) {
        progress.dismiss();

        if (resStr != null){
            Log.d("handleResponse events", resStr);
            try {
                JSONObject jsonObj = new JSONObject(resStr);
                String flg = jsonObj.getString(Constants.TAG_FLG);
                switch (flg){
                    case Constants.TAG_REQUEST_SUCCEED:
                    {
                        GetEventsFromJSONToMapMarkers(jsonObj);
                        break;
                    }

                    case Constants.TAG_REQUEST_FAILED:
                    {
                        Log.d("message from server",jsonObj.getString("msg"));
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void GetEventsFromJSONToMapMarkers (JSONObject jsonObj)
    {
        try {
            JSONArray jsonarr = jsonObj.getJSONArray("events");
            Log.d("creating list", jsonarr.toString());
            drawEventsMarkersOnMap(jsonarr);
        }catch (JSONException e) {
            e.printStackTrace();
    }

    }


    private void drawEventsMarkersOnMap(JSONArray jsonarr) {
        if (googleMap != null)
            if (jsonarr.length() != 0)
            {
                if (eventsMarkers != null)
                    deletePreviousMarkers();
                eventsMarkers = new ArrayList<MapMarker>();
                Log.d("drawEventsMarkersOnMap", "before for loop");

                for (int i = 0; i < jsonarr.length(); i++) {
                    try {
                    //adding marker to the arraylist eventsmarkers which every object is a class MapMarker variable
                    //marker info is saved upon MapMarker constructor
                        eventsMarkers.add(new MapMarker(jsonarr.getJSONObject(i)));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }//for
                plotMarkers();
            }//if
        else {
                //no events has found

                if (mode.equals(Constants.MODE_SEARCH_REQ)) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("No events found")
                            .setMessage("There is no results according to your search.")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .show();
                }
            }
    }
    private void deletePreviousMarkers()
    {
        int eventsMarkersSize = eventsMarkers.size();
        Log.d("deletePreviousMarkers", "before for loop");
        for (int i = 0; i < eventsMarkersSize; i++) {
            Log.d("deletePreviousMarkers", "remove marker: " + String.valueOf(i));
            mMarkersHashMap.remove(eventsMarkers.get(i).getmMarker()); //remove MapMarker from hash
            eventsMarkers.get(i).getmMarker().remove();                //remove Marker from MapMarker
        }
        eventsMarkers.clear();                                          //remove all MapMarkers from list
        Log.d("deletePreviousMarkers", "markers removed");

    }
    private void plotMarkers()
    {
        if(eventsMarkers.size() > 0)
        {   Log.d("plotMarkers","eventsMarkers.size() > 0");
            for (MapMarker mapMarker : eventsMarkers)
            {
                // Create event marker with custom icon and other options
                MarkerOptions markerOption = new MarkerOptions().position(new LatLng(mapMarker.getmLatitude(), mapMarker.getmLongitude()));
                markerOption.icon(mapMarker.getmIcon());
                markerOption.title(mapMarker.getmLabel() + String.valueOf(mapMarker.getmLatitude()) + String.valueOf(mapMarker.getmLongitude()));
                // Save Marker pointer for updating later
                currentMarker = googleMap.addMarker(markerOption);
                mapMarker.setmMarker(currentMarker);
                // Set Info for this marker and marker to hashmap for later use
                mMarkersHashMap.put(currentMarker, mapMarker);
                googleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
            }
            Log.d("plotMarkers","after for loop");
        }

    }
    @Override
    public void sendDataToDBController() {
        Log.d("sendDataToDBController", "sendDataToDBController");

        BasicNameValuePair tagreq = new BasicNameValuePair(Constants.TAG_REQUEST, "search_events");
        BasicNameValuePair search = new BasicNameValuePair(Constants.TAG_SEARCH,"search_by_default");
        BasicNameValuePair radius = new BasicNameValuePair(Constants.TAG_RADIUS, String.valueOf(Constants.DEFAULT_RADIUS));
        BasicNameValuePair lat = new BasicNameValuePair(Constants.TAG_LONG,String.valueOf(locationTool.getmLastLocation().getLongitude()));
        BasicNameValuePair lon = new BasicNameValuePair(Constants.TAG_LAT,String.valueOf(locationTool.getmLastLocation().getLatitude()));

        List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
        nameValuePairList.add(tagreq);
        nameValuePairList.add(search);
        nameValuePairList.add(radius);
        nameValuePairList.add(lat);
        nameValuePairList.add(lon);

        dbController = new DBcontroller(getActivity().getApplicationContext(),this);
        dbController.execute(nameValuePairList);
    }

    @Override
    public void preProcess() {
        progress = ProgressDialog.show(this.getActivity(), "Search events", "Updating map with events near by...", true);

    }

     /**
      * method which handling the requests for  back button in the  device
      * @param savedInstanceState
      */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();

        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        Toast.makeText(getActivity(), "Back Pressed", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mode.equals(Constants.MODE_SEARCH_DEF)) {
            locationTool.setmLastLocation(location);
            updateUI();
        }
    }

    public void updateUI() {

        double latitude, longitude;

        if (mode.equals(Constants.MODE_SEARCH_DEF)) {
            latitude = locationTool.getmLastLocation().getLatitude();
            longitude =  locationTool.getmLastLocation().getLongitude();
        }
        else
        {
            latitude = lati;
            longitude =  longi;
        }

        Log.d("lat and long", latitude+" "+longitude );

        if (googleMap != null) {
            Log.d("googleMap", "googleMap");
            if (currentMarker != null)
                currentMarker.remove();
            iniLoc = new LatLng(latitude, longitude);
            CameraPosition cp = new CameraPosition.Builder().target(iniLoc).zoom(12).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
            googleMap.getUiSettings().setZoomGesturesEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            currentMarkerOption = new MarkerOptions();
            currentMarkerOption.position(iniLoc);
            currentMarker = googleMap.addMarker(currentMarkerOption);
            currentMarker.setIcon((BitmapDescriptorFactory.fromResource(R.drawable.current_location_marker_icon)));
            currentMarker.setTitle(sm.getUserDetails().get("name") + " " + latitude + " " + longitude);
            currentMarker.showInfoWindow();
            currentMarker.setVisible(true);
            if (mode.equals(Constants.MODE_SEARCH_DEF))
                sendDataToDBController();

        }
    }

    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter
    {
        public MarkerInfoWindowAdapter()
        {}

        @Override
        public View getInfoWindow(Marker marker)
        {
            return null;
        }

        @Override
        public View getInfoContents(Marker markerMapMarkerKey)
        {
            View v  = inflater.inflate(R.layout.infowindow_layout, null);
            MapMarker marker = mMarkersHashMap.get(markerMapMarkerKey);
            if (marker != null) { //if marker == null this marker isn't in the hash => current location marker
                ImageView markerIcon = (ImageView) v.findViewById(R.id.marker_icon);
                TextView markerLabel = (TextView) v.findViewById(R.id.marker_label);
                markerIcon.setImageResource(marker.getmBitmap());
                markerLabel.setText(marker.getmLabel());
            }
            return v;
        }
        }


}
