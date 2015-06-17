package com.capsulecorp.project_trackerino;

import java.util.ArrayList;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.util.constants.MapViewConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener, MapViewConstants,MapEventsReceiver {
    private MapView mapView;
    private IMapController mapController;
    public ItemizedOverlay<OverlayItem> myLocationOverlay;
    private ResourceProxy mResourceProxy;
    public Location location;
    public ArrayList<OverlayItem> overlays;
    public ArrayList<OverlayItem> itemsOverlays;
    public LocationManager locationManager;
    public int mLatitude;
    public int mLongtitude;
    public IMyLocationConsumer mMyLocationConsumer;
    public String permprovider;
    public String itemName;
    public MapEventsOverlay mapEventsOverlay;
    public Polyline myPolyline;
    public ArrayList<GeoPoint> route;
    public boolean trackingEnabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Boolean result = false;
        setContentView(R.layout.activity_main);
        route = new ArrayList<GeoPoint>();

        mapEventsOverlay = new MapEventsOverlay(this,this);
        Button additem = (Button)findViewById(R.id.btnAddItem);
        final Button startTracking = (Button)findViewById(R.id.btnStartTracking);
        additem.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                mLatitude = (int) (location.getLatitude() * 1E6);
                mLongtitude = (int) (location.getLongitude() * 1E6);
                final GeoPoint gpt = new GeoPoint(mLatitude, mLongtitude);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Marker hinzufügen");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        itemName = input.getText().toString();
                        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName);
                        Marker itemMarker = new Marker(mapView);
                        itemMarker.setPosition(gpt);
                        itemMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        //startMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
                        itemMarker.setTitle(itemName);
                        itemMarker.setInfoWindow(infoWindow);
                        mapView.getOverlays().add(itemMarker);
                        Toast.makeText(MainActivity.this, "Hinzugef�gt", Toast.LENGTH_SHORT).show();
                        //MarkerInfoWindow window = new MarkerInfoWindow(R.layout.activity_main,mapView);
                        mapView.invalidate();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
            });
        startTracking.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trackingEnabled)
                    trackingEnabled = false;
                else
                    trackingEnabled = true;
                Toast.makeText(MainActivity.this, "Tracking Status Changed", Toast.LENGTH_SHORT).show();
            }
        });
        overlays = new ArrayList<OverlayItem>();
        itemsOverlays = new ArrayList<>();
        mapView = (MapView) this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = this.mapView.getController();
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

        //OverlayItem ovItem = new OverlayItem("New Overlay", "Overlay Description", currentLocation);
        Drawable posMarker = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);

        mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays, new Glistener(), mResourceProxy);
        this.mapView.getOverlays().add(0, mapEventsOverlay);
        this.mapView.getOverlays().add(this.myLocationOverlay);
        //mapView.getOverlays().add();
        mapView.invalidate();

    }

    @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        return true;
    }

    @Override
    public boolean longPressHelper(final GeoPoint geoPoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Marker hinzufügen");

        // Set up the input
        final EditText input = new EditText(MainActivity.this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                itemName = input.getText().toString();
                InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName);
                Marker itemMarker = new Marker(mapView);
                itemMarker.setPosition(geoPoint);
                itemMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                //startMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
                itemMarker.setTitle(itemName);
                itemMarker.setInfoWindow(infoWindow);
                mapView.getOverlays().add(itemMarker);
                Toast.makeText(MainActivity.this, "Hinzugef�gt", Toast.LENGTH_SHORT).show();
                //MarkerInfoWindow window = new MarkerInfoWindow(R.layout.activity_main,mapView);
                mapView.invalidate();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        return false;
    }

    class Glistener implements ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {
            Toast.makeText(MainActivity.this, "LANG" + item.getTitle(),
                    Toast.LENGTH_LONG).show();

            return false;
        }

        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {
            Toast.makeText(MainActivity.this, "" + item.getTitle(),
                    Toast.LENGTH_LONG).show();
            return true; // We 'handled' this event.

        }

    }

    public void onLocationChanged(Location location) {
        this.location = location;
        mLatitude = (int) (location.getLatitude() * 1E6);
        mLongtitude = (int) (location.getLongitude() * 1E6);
        /*Toast.makeText(MainActivity.this,
                "Location changed. Lat:" + mLatitude + " long:" + mLongtitude,
                Toast.LENGTH_SHORT).show();
                */
        GeoPoint gpt = new GeoPoint(mLatitude, mLongtitude);
        mapController.setCenter(gpt);
        overlays.clear(); // COMMENT OUT THIS LINE IF YOU WANT A NEW ICON FOR EACH CHANGE OF POSITION
        OverlayItem ovItem = new OverlayItem("New Overlay", "Overlay Description", gpt);
        Drawable posMarker = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);
        ovItem.setMarker(posMarker);
        overlays.add(ovItem);
        // Change the overlay
        this.mapView.getOverlays().remove(this.myLocationOverlay);
        if(trackingEnabled){
            route.add(gpt);
            myPolyline = new Polyline(mapView.getContext());
            myPolyline.setPoints(route);
            myPolyline.setColor(0xAA0000FF);
            myPolyline.setWidth(4.0f);
            myPolyline.setGeodesic(true);
            this.mapView.getOverlays().add(myPolyline);
        }
        mapView.invalidate();
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays, new Glistener() , mResourceProxy);
        //this.mapView.getOverlays().clear();
        this.mapView.getOverlays().add(this.myLocationOverlay);
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
                permprovider = provider;
            }
        }
        return result;
    }

    private class MyInfoWindow extends InfoWindow {
        public String name;
        public MyInfoWindow(int layoutResId, MapView mapView, String name) {
            super(layoutResId, mapView);
            this.name = name;
        }
        public void onClose() {
        }

        public void onOpen(Object arg0) {
            LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
            Button btnMoreInfo = (Button) mView.findViewById(R.id.bubble_moreinfo);
            TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
            TextView txtDescription = (TextView) mView.findViewById(R.id.bubble_description);
            TextView txtSubdescription = (TextView) mView.findViewById(R.id.bubble_subdescription);
            InfoWindow.closeAllInfoWindowsOn(mapView);
            txtTitle.setText(name);
            txtDescription.setText("Klicke hier zum bearbeiten!");
            txtSubdescription.setText("You can also edit the subdescription");
            layout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Editieren");

                    // Set up the input
                    final EditText input = new EditText(MainActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            name = input.getText().toString();
                            InfoWindow.closeAllInfoWindowsOn(mapView);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            });
        }
    }
}


