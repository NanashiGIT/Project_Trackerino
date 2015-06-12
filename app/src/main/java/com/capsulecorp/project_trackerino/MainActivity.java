package com.capsulecorp.project_trackerino;


import java.util.ArrayList;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.util.constants.MapViewConstants;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;
import android.app.ActionBar;

public class MainActivity extends Activity implements LocationListener, MapViewConstants{

    private MapView mapView;
    private MapController mapController;
    public ItemizedOverlay<OverlayItem> myLocationOverlay;
    private ResourceProxy mResourceProxy;
    public Location location;
    public GeoPoint currentLocation = null;
    public ArrayList<OverlayItem> overlays;
    public LocationManager locationManager;
    public int mLatitude;
    public int mLongtitude;
    public IMyLocationConsumer mMyLocationConsumer;


    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Boolean result = false;
        setContentView(R.layout.activity_main);
        //locationListener = new MyLocationListener();
        mapView = (MapView) this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = (MapController) this.mapView.getController();
        mapController.setZoom(14);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        try {
            Thread.sleep(10000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        mapView.getOverlays().clear();

        while(result == false){
            Toast.makeText(MainActivity.this,"Suche nach Provider",Toast.LENGTH_SHORT).show();
            result = startLocationProvider(mMyLocationConsumer);
        }


        overlays = new ArrayList<OverlayItem>();
        //OverlayItem ovItem = new OverlayItem("New Overlay", "Overlay Description", currentLocation);
        Drawable posMarker = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);


        mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays, new Glistener(), mResourceProxy);
        this.mapView.getOverlays().add(this.myLocationOverlay);
        mapView.invalidate();

    }

    class Glistener implements ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {
            Toast.makeText(MainActivity.this, "Item " + item.getTitle(),
                    Toast.LENGTH_LONG).show();

            return false;
        }

        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {
            Toast.makeText(MainActivity.this, "Item " + item.getTitle(),
                    Toast.LENGTH_LONG).show();
            return true; // We 'handled' this event.

        }

    }

    public void onLocationChanged(Location location) {
        mLatitude = (int) (location.getLatitude() * 1E6);
        mLongtitude = (int) (location.getLongitude() * 1E6);
        Toast.makeText(MainActivity.this,
                "Location changed. Lat:" + mLatitude + " long:" + mLongtitude,
                Toast.LENGTH_SHORT).show();
        GeoPoint gpt = new GeoPoint(mLatitude, mLongtitude);
        mapController.setCenter(gpt);
        overlays.clear(); // COMMENT OUT THIS LINE IF YOU WANT A NEW ICON FOR EACH CHANGE OF POSITION
        OverlayItem ovItem = new OverlayItem("New Overlay", "Overlay Description", gpt);
        Drawable posMarker = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);
        ovItem.setMarker(posMarker);
        overlays.add(ovItem);
        // Change the overlay
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays,
                new Glistener() , mResourceProxy);
        this.mapView.getOverlays().clear();
        this.mapView.getOverlays().add(this.myLocationOverlay);
        mapView.invalidate();
    }
    @Override
    public void onProviderDisabled(String arg0) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
        mMyLocationConsumer = myLocationConsumer;
        boolean result = false;
        for (final String provider : locationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider) || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                result = true;
                locationManager.requestLocationUpdates(provider, 0, 0, this);
            }
        }
        return result;
    }

}


