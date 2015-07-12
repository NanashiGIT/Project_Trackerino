package com.capsulecorp.project_trackerino;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

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
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Shark FW lib
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkKB;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.SpatialSTSet;
import net.sharkfw.knowledgeBase.SpatialSemanticTag;
import net.sharkfw.knowledgeBase.filesystem.FSSharkKB;
import net.sharkfw.knowledgeBase.geom.SharkGeometry;
import net.sharkfw.knowledgeBase.geom.inmemory.InMemoSharkGeometry;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;


public class MainActivity extends Activity implements LocationListener, MapViewConstants,MapEventsReceiver {
    public static MapView mapView;
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
    public String trackName;
    public MapEventsOverlay mapEventsOverlay;
    public Polyline myPolyline;
    public ArrayList<GeoPoint> route;
    public boolean trackingEnabled = false;
    public GeoPoint gpt;
    public SharkKB kb = new InMemoSharkKB();
    public static SpatialSTSet locations;
    public static Map<Long, myMarker> marker_map = new HashMap<Long, myMarker>();
    public Map<Long, ArrayList<myMarker> > polyline_map = new HashMap<Long, ArrayList<myMarker> >();
    public long marker_id;
    public long polyline_id;
    public static ArrayList<Long> deletedMarkers = new ArrayList<Long>();
    public static Map<Long, String> editedMarkers = new HashMap<Long, String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Boolean result = false;
        setContentView(R.layout.activity_main);
        route = new ArrayList<GeoPoint>();
        try{
            kb = new FSSharkKB(Environment.getExternalStorageDirectory()+"/sharkDB");
            locations = kb.getSpatialSTSet();
        }catch (SharkKBException e){}

        mapEventsOverlay = new MapEventsOverlay(this,this);
        Button additem = (Button)findViewById(R.id.btnAddItem);
        final Button startTracking = (Button)findViewById(R.id.btnStartTracking);
        Button syncView = (Button)findViewById(R.id.btnSync);
        Button loadView = (Button)findViewById(R.id.btnLoad);

        syncView.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                try{
                    speichereView(marker_map);
                }catch (SharkKBException e){}
            }
        });

        loadView.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                try{
                    ladeView();
                }catch (SharkKBException e){}
            }
        });

        additem.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                mLatitude = (int) (location.getLatitude() * 1E6);
                mLongtitude = (int) (location.getLongitude() * 1E6);
                gpt = new GeoPoint(mLatitude, mLongtitude);
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
                        marker_id = createID();
                        itemName = input.getText().toString();
                        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id, MainActivity.this);
                        myMarker itemMarker = new myMarker(mapView, marker_id);
                        itemMarker.setPosition(gpt);
                        itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
                        itemMarker.setTitle(itemName);
                        itemMarker.setInfoWindow(infoWindow);
                        mapView.getOverlays().add(itemMarker);
                        marker_map.put(marker_id, itemMarker);
                        Toast.makeText(MainActivity.this, "ID " + marker_id + " Hinzugefuegt", Toast.LENGTH_SHORT).show();
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
                if(trackingEnabled){
                    trackingEnabled = false;
                    InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, trackName, polyline_id, MainActivity.this);
                    myMarker itemMarker = new myMarker(mapView, polyline_id);
                    itemMarker.setPosition(gpt);
                    itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
                    itemMarker.setTitle(trackName);
                    itemMarker.setInfoWindow(infoWindow);
                    itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                    mapView.getOverlays().add(itemMarker);
                    polyline_map.get(polyline_id).add(itemMarker);
                    mapView.invalidate();
                    Iterator<GeoPoint> iterRoute = route.iterator();
                    while (iterRoute.hasNext()) {
                        GeoPoint temp_point = iterRoute.next();
                        myMarker temp_itemMarker = new myMarker(mapView, polyline_id);
                        itemMarker.setPosition(temp_point);
                        polyline_map.get(polyline_id).add(temp_itemMarker);
                    }
                    route.clear();
                    Toast.makeText(MainActivity.this, "Tracking deaktiviert", Toast.LENGTH_SHORT).show();
                }else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Neuer Track");

                    // Set up the input
                    final EditText input = new EditText(MainActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            trackName = input.getText().toString();
                            polyline_id = createID();
                            trackingEnabled = true;
                            InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, trackName, polyline_id, MainActivity.this);
                            myMarker itemMarker = new myMarker(mapView, polyline_id);
                            itemMarker.setPosition(gpt);
                            itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
                            itemMarker.setTitle(trackName);
                            itemMarker.setInfoWindow(infoWindow);
                            itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                            mapView.getOverlays().add(itemMarker);
                            mapView.invalidate();
                            route.add(gpt);
                            ArrayList<myMarker> temp_vec = new ArrayList<myMarker>();
                            polyline_map.put(polyline_id, temp_vec);
                            polyline_map.get(polyline_id).add(itemMarker);
                            Toast.makeText(MainActivity.this, "Tracking aktiviert", Toast.LENGTH_SHORT).show();
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
            }
        });
        overlays = new ArrayList<OverlayItem>();
        mapView = (MapView) this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = this.mapView.getController();
        mapController.setZoom(18);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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

        try {
            ladeView();
        }catch (SharkKBException e){}

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
                marker_id = createID();
                itemName = input.getText().toString();
                InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id, MainActivity.this);
                myMarker itemMarker = new myMarker(mapView, marker_id);
                itemMarker.setPosition(geoPoint);
                itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
                itemMarker.setTitle(itemName);
                itemMarker.setInfoWindow(infoWindow);
                marker_map.put(marker_id, itemMarker);
                mapView.getOverlays().add(itemMarker);
                //Toast.makeText(MainActivity.this, "ID " + marker_id + " Hinzugefuegt", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Marker \"" + itemName + "\" hinzugefügt", Toast.LENGTH_LONG).show();
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
        gpt = new GeoPoint(mLatitude, mLongtitude);

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

    public void speichereView(Map<Long, myMarker> m_map) throws SharkKBException {
        speichereMarker(m_map);
        speichereTracks();

        for (Map.Entry e : editedMarkers.entrySet()) {
            String m_id = e.getKey().toString();
            String text = (String) e.getValue();
            SemanticTag tagBack = locations.getSpatialSemanticTag(m_id);
            tagBack.setProperty("descr",text);
        }

        Iterator<Long> markersIterator = deletedMarkers.iterator();
        while (markersIterator.hasNext()) {
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(""+markersIterator.next());
            if(tagBack != null) {
                locations.removeSemanticTag(tagBack);
            }
        }
        deletedMarkers.clear();
        editedMarkers.clear();
        Toast.makeText(MainActivity.this, "Erfolgreich gespeichert.", Toast.LENGTH_SHORT).show();
    }

    public static Map<Long, myMarker> loescheMarker(Map<Long, myMarker> m_map, long id)throws SharkKBException{
        deletedMarkers.add(id);
        myMarker temp = marker_map.get(id);
        mapView.getOverlays().remove(temp);
        m_map.remove(id);
        mapView.invalidate();
        return m_map;
    }

    public static void editMarker(Long id, String text){
        editedMarkers.put(id, text);
    }

    public void speichereMarker(Map<Long, myMarker> m_map) throws SharkKBException{
        for (Map.Entry e : m_map.entrySet()) {
            long m_id = (long) e.getKey();
            if (!checkMarker(m_map, m_id)) {
                myMarker marker = (myMarker) e.getValue();
                GeoPoint geoPoint = marker.getPosition();

                String str_latitude = String.valueOf(geoPoint.getLatitude());
                String str_longitude = String.valueOf(geoPoint.getLongitude());
                String i_value = marker.getTitle();

                SharkGeometry geom = InMemoSharkGeometry.createGeomByWKT("POINT (" + str_latitude + " " + str_longitude + ")");
                String tag = "" + m_id;
                String[] sis = new String[]{tag};
                SemanticTag stag = locations.createSpatialSemanticTag("marker", sis, geom);
                stag.setProperty("descr", i_value);
            }
        }
    }

    public boolean checkMarker(Map<Long, myMarker> m_map, long id) throws SharkKBException{
        SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(""+id);
        if(tagBack != null)
            return true;
        else
            return false;
    }

    public void speichereTracks(){

    }

    public void ladeView() throws SharkKBException{
        mapView.getOverlays().clear();
        mapView.getOverlays().add(0, mapEventsOverlay);
        deletedMarkers.clear();
        InfoWindow.closeAllInfoWindowsOn(mapView);
        Iterator<SemanticTag> a = locations.getSemanticTagByName("marker");
        while(a.hasNext()){
            SemanticTag temp = a.next();
            String[] str_temp = temp.getSI();
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(str_temp[0]);
            String geoString = tagBack.getGeometry().getWKT();

            Pattern r = Pattern.compile("([0-9-]+\\.[0-9]+)");
            Matcher m = r.matcher(geoString);
            double[] geoData = new double[2];
            for (int i = 0; i < geoData.length; i++){
                m.find();
                geoData[i] = Double.parseDouble(m.group());
            }

            GeoPoint geop = new GeoPoint(geoData[0], geoData[1]);
            String value = temp.getProperty("descr");
            String[] temp_si = temp.getSI();
            long m_id = Long.parseLong(temp_si[0]);
            drawMarker(geop, value, m_id);
        }
        if(this.location != null)
            onLocationChanged(this.location);
    }

    public long createID(){
        long id = System.currentTimeMillis() / (long) (Math.random()+1);
        return id;
    }

    public void drawMarker(GeoPoint geop, String i_value, long m_id){
        itemName = i_value;
        marker_id = m_id;
        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id, MainActivity.this);
        myMarker itemMarker = new myMarker(mapView, marker_id);
        itemMarker.setPosition(geop);
        itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
        itemMarker.setTitle(itemName);
        itemMarker.setInfoWindow(infoWindow);
        mapView.getOverlays().add(itemMarker);
        marker_map.put(marker_id, itemMarker);
        mapView.invalidate();
    }

    public void drawTrack(){

    }

}
