package com.example.matant.gpsportclient.Utilities;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.example.matant.gpsportclient.InterfacesAndConstants.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Nir B on 26/09/2015.
 */
public class AddressFetcher extends IntentService {

    public AddressFetcher(String name) {
        super(name);
    }

    protected ResultReceiver mReceiver;

    //Handle the intent that started
    @Override
     protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        String errorMessage = "";

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(
                Constants.LOCATION_DATA_EXTRA);

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            // In this sample, get just a single address.
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = "service not available";
            Log.e("service_not_available", errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "invalid lat long used";
            Log.e("invalid_lat_long_used", errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "no address found";
                Log.e("no_address_found", errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i("address_found", "address found");
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),addressFragments));
        }

    }
//Send the result of the fetch to receiver
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }


}
