package com.capsulecorp.project_trackerino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
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
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

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

public class MainActivity extends Activity implements LocationListener, MapViewConstants, MapEventsReceiver, View.OnClickListener {
    public static MapView mapView;                          // Ist die Darstellung der Map
    private IMapController mapController;                   //
    public ItemizedOverlay<OverlayItem> myLocationOverlay;  // Eine Ansammlung von Overlays, welche auf der Map angezeigt werden
    private ResourceProxy mResourceProxy;                   //
    public Location location;                               // Ist ein Ort auf der Map
    public ArrayList<OverlayItem> overlays;                 // Ein Array mit einzelnen Overlayitems
    public LocationManager locationManager;                 //
    public int mLatitude, mLongtitude;                      // Longitude und Latitude unserer Position
    public long marker_id, polyline_id;                     // IDs der jeweiligen Marker und Polylines (Tracks)
    public IMyLocationConsumer mMyLocationConsumer;         //
    public String permprovider, itemName, trackName;        // permprovider: Speichert, ob die Ortsdaten entweder über GPS, mobiles Internet oder beidem empfangen werden
                                                            // itemName: Bezeichnung eines Markers, trackName: Bezeichnung eines Tracks
    public MapEventsOverlay mapEventsOverlay;               //
    public Polyline myPolyline;                             // Die Trackpolyline
    public ArrayList<GeoPoint> route = new ArrayList<GeoPoint>();; // Arrayliste von Geopunkten, die die Route (Polyline) bilden
    public boolean centerEnabled = true;                    // Merker ob die Zentrierung aktiviert ist
    public boolean trackingEnabled = false;                 // Merker ob das Tracking aktiviert ist
    public GeoPoint gpt;                                    // Ein einzelner Geopunkt (Longitude/Latitude)
    public SharkKB kb = new InMemoSharkKB();                // Legt eine semantische Wissensspeicher (Datenbank)
    public static SpatialSTSet locations;                   //
    public static Map<Long, myMarker> marker_map = new HashMap<Long, myMarker>();               // Map, welche die Marker eindeutig speichert (Marker-ID, Markerobjekt)
    public static Map<Long, ArrayList<myMarker> > polyline_map = new HashMap<Long, ArrayList<myMarker> >(); // Map, welche die Punkte der Polyline eindeutig speichert (Polyline-ID, Punkte der Polyline)
    public static Map<Long, Polyline > polylineObj_map = new HashMap<Long, Polyline >();         // Map, welche Polyline eindeutig speichert (Polyline-ID, Polylineobjekt), benötigt um Polyline visuell von Map zu löschen
    public static ArrayList<Long> deletedMarkers = new ArrayList<Long>();             // Map, welche die gelöschten Marker zwischenspeichert
    public static ArrayList<Long> deletedPolylines = new ArrayList<Long>();           // Map, welche die gelöschten Polylines zwishenspeichert
    public static Map<Long, String> editedMarkers = new HashMap<Long, String>();              // Map, welche die editierten Marker zwischenspeichert
    public static Map<Long, String> editedPolylines = new HashMap<Long, String>();            // Map, welche die editierten Polylines zwischenspeichert
    public MyInfoWindow trackWindow;                                               // Ein InfoWindow, welches für Start und Endpunkt eines Tracks gleich ist
    private Button startTracking, additem, syncView, loadView;                     // Deklaration der Programmbuttons
    private ToggleButton center;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Erstellung/Öffnen der Shark-Datenbank
        try{
            kb = new FSSharkKB(Environment.getExternalStorageDirectory()+"/sharkDB");
            locations = kb.getSpatialSTSet();
        }catch (SharkKBException e){}

        mapEventsOverlay = new MapEventsOverlay(this,this);

        // Initialisierung der Buttons, Zuweisung der OnClickListener
        startTracking = (Button)findViewById(R.id.btnStartTracking);
        additem = (Button)findViewById(R.id.btnAddItem);
        syncView = (Button)findViewById(R.id.btnSync);
        loadView = (Button)findViewById(R.id.btnLoad);
        center = (ToggleButton)findViewById(R.id.tbCenter);
        syncView.setOnClickListener(this);
        loadView.setOnClickListener(this);
        additem.setOnClickListener(this);
        startTracking.setOnClickListener(this);
        center.setOnClickListener(this);

        // Initialisierung diverser Variablen
        overlays = new ArrayList<OverlayItem>();
        mapView = (MapView) this.findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = this.mapView.getController();
        mapController.setZoom(18);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mapView.getOverlays().clear();


        while(startLocationProvider(mMyLocationConsumer) == false){}

        mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays, null, mResourceProxy);
        this.mapView.getOverlays().add(0, mapEventsOverlay);
        this.mapView.getOverlays().add(this.myLocationOverlay);
        mapView.invalidate();

        try {
            ladeView();
        }catch (SharkKBException e){}

    }

    // Wird aufgerufen, wenn der Nutzer kurz auf die Map tippt
    @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
        // Schließe alle geöffneten Info-Fenster
        InfoWindow.closeAllInfoWindowsOn(mapView);
        return true;
    }

    // Wird aufgerufen, wenn der Nutzer lange auf die Map drückt
    // Es wird dann ein neuer Marker erstellt
    @Override
    public boolean longPressHelper(final GeoPoint geoPoint) {
        // Erstellung eines Fensters für das Hinzufügen eines Markers
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Marker");
        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);

        // OK-Button zur Bestätigung der Eingaben
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            // OnClickListener des Buttons
            @Override
            public void onClick(DialogInterface dialog, int which) {
                marker_id = createID();
                // Einlesen des Namens für den Marker
                itemName = input.getText().toString();
                // Erstellung eines neues InfoWindows mit der ID und dem Namen des zugehörigen Markers
                InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id, 0, MainActivity.this);
                // Erstellung des Markers mit der eindeutigen ID und dem Namen des Markers
                myMarker itemMarker = createMarker(marker_id, itemName, geoPoint, mapView);
                // Das InfoWindow wird dem Marker zugewiesen
                itemMarker.setInfoWindow(infoWindow);
                // Marker wird zur Key-Value Map hinzugefügt
                marker_map.put(marker_id, itemMarker);
                // Marker wird zum Overlay hinzugefügt
                mapView.getOverlays().add(itemMarker);
                Toast.makeText(MainActivity.this, "Marker \"" + itemName + "\" added", Toast.LENGTH_LONG).show();
                // Die Map wird aktualisiert
                mapView.invalidate();
            }
        });
        // Cancel-Button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            // Fenster wird geschlossen
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
        return false;
    }

    // Wird aufgerufen, wenn das Programm eine Positionsänderung festgestellt hat
    public void onLocationChanged(Location location) {
        this.location = location;
        mLatitude = (int) (location.getLatitude() * 1E6);
        mLongtitude = (int) (location.getLongitude() * 1E6);
        gpt = new GeoPoint(mLatitude, mLongtitude);
        if(centerEnabled)
            mapController.setCenter(gpt);
        // Entferne alten Marker für die eigene Position
        overlays.clear();
        // Erstelle neuen Marker für die eigene Position
        OverlayItem ovItem = new OverlayItem("New Overlay", "Overlay Description", gpt);
        Drawable posMarker = getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);
        ovItem.setMarker(posMarker);
        overlays.add(ovItem);
        // Lösche altes Overlay
        this.mapView.getOverlays().remove(this.myLocationOverlay);
        if(trackingEnabled){
            if(myPolyline != null)
                // Lösche die alte gezeichnete Polyline
                this.mapView.getOverlays().remove(myPolyline);
            // Füge einen neuen Punkt zum PunkteArray hinzu
            route.add(gpt);
            // Erstelle neue Polyline mit dem neuen Punkt
            myPolyline = new Polyline(mapView.getContext());
            myPolyline.setPoints(route);
            myPolyline.setColor(0xAA0000FF);
            myPolyline.setWidth(4.0f);
            myPolyline.setGeodesic(true);
            // Zeichne die Polyline auf der Map
            this.mapView.getOverlays().add(myPolyline);
            // Speichere die Polyline mit ihrer eindeutigen ID in der Key-Value Map
            polylineObj_map.put(polyline_id,myPolyline);
        }
        // Aktualisiere Map
        mapView.invalidate();
        // Erstelle neues LocationOverlay mit der eigenen Position in "overlays"
        this.myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlays, null, mResourceProxy);
        // Füge es zur MapView hinzu
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

    // Funktion zur Bestimmung, über welchen Provider (GPS, Netzwerk, Beiden) die Position ermittelt werden soll
    // Startet den Bezug der Daten von dem gefunden Provider
    public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
        mMyLocationConsumer = myLocationConsumer;
        boolean result = false;
        for (final String provider : locationManager.getProviders(true)) {
            // Ist der GPS oder der Netzwerk Provider gefunden worden?
            if (LocationManager.GPS_PROVIDER.equals(provider) || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                result = true;
                // Erhalte die Positionsdaten vom gefundenen Provider
                locationManager.requestLocationUpdates(provider, 0, 0, this);
                permprovider = provider;
            }
        }
        return result;
    }

    // Erstelle einen Marker
    public myMarker createMarker(long id, String iName, GeoPoint geoPoint, MapView mView){
        myMarker itemMarker = new myMarker(mView, id);
        itemMarker.setPosition(geoPoint);
        itemMarker.setAnchor(myMarker.ANCHOR_CENTER, myMarker.ANCHOR_BOTTOM);
        itemMarker.setTitle(iName);
        return itemMarker;
    }

    // Lösche einen Marker
    public static Map<Long, myMarker> loescheMarker(Map<Long, myMarker> m_map, long id)throws SharkKBException{
        // Füge die ID zu den IDs hinzu, welche beim Speichern endgültig gelöscht werden sollen
        deletedMarkers.add(id);
        myMarker temp = marker_map.get(id);
        mapView.getOverlays().remove(temp);
        // Lösche den Marker aus der Key-Value Map
        m_map.remove(id);
        mapView.invalidate();
        return m_map;
    }

    // Lösche eine Polyline
    public static Map<Long, ArrayList<myMarker> > loeschePolyline(Map<Long, ArrayList<myMarker> > p_map, long id){
        // Füge die ID zu den IDs hinzu, welche beim Speichern endgültig gelöscht werden sollen
        deletedPolylines.add(id);
        // Erhalte Startmarker der Polyline
        myMarker temp_start = polyline_map.get(id).get(0);
        ArrayList<myMarker> markerList = polyline_map.get(id);
        // Erhalte Endmarker der Polyline
        myMarker temp_end = polyline_map.get(id).get(markerList.size() - 1);
        // Lösche den visuellen Start- und Endpunkt von der Map
        mapView.getOverlays().remove(temp_start);
        mapView.getOverlays().remove(temp_end);
        // Lösche die Polyline aus der Key-Value Map
        p_map.remove(id);
        mapView.invalidate();
        return p_map;
    }

    // Lösche das visuelle Polyline-Objekt von der Map
    public static Map<Long, Polyline> loeschePolylineObj(Map<Long, Polyline> pObj_map, Long id){
        mapView.getOverlays().remove(pObj_map.get(id));
        pObj_map.remove(id);
        return pObj_map;
    }

    // Bearbeite die Beschreibung eines Markers
    public static void editMarker(Long id, String text){
        editedMarkers.put(id, text);
    }

    // Bearbeite die Beschreibung einer Polyline
    public static void editPolyline(Long id, String text){
        editedPolylines.put(id, text);
    }

    public void speichereView(Map<Long, myMarker> m_map, Map<Long, ArrayList<myMarker> > p_map) throws SharkKBException {
        speichereMarker(m_map);
        speichereTracks(p_map);

        for (Map.Entry e : editedMarkers.entrySet()) {
            String m_id = e.getKey().toString();
            String text = (String) e.getValue();
            SemanticTag tagBack = locations.getSpatialSemanticTag(m_id);
            if(tagBack != null)
                tagBack.setProperty("descr",text);
        }

        for (Map.Entry e : editedPolylines.entrySet()) {
            String p_id = e.getKey().toString();
            String text = (String) e.getValue();
            SemanticTag tagBack = locations.getSpatialSemanticTag(p_id);
            if(tagBack != null)
                tagBack.setProperty("descr",text);
        }

        Iterator<Long> markersIterator = deletedMarkers.iterator();
        while (markersIterator.hasNext()) {
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(""+markersIterator.next());
            if(tagBack != null) {
                locations.removeSemanticTag(tagBack);
            }
        }

        Iterator<Long> polylinesIterator = deletedPolylines.iterator();
        while (polylinesIterator.hasNext()) {
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(""+polylinesIterator.next());
            if(tagBack != null) {
                locations.removeSemanticTag(tagBack);
            }
        }

        deletedMarkers.clear();
        editedMarkers.clear();
        deletedPolylines.clear();
        editedPolylines.clear();

        Toast.makeText(MainActivity.this, "Saved successfully.", Toast.LENGTH_SHORT).show();
    }

    public void speichereMarker(Map<Long, myMarker> m_map) throws SharkKBException{
        for (Map.Entry e : m_map.entrySet()) {
            long m_id = (long) e.getKey();
            if (!checkGeom(m_id)) {
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

    public void speichereTracks(Map<Long, ArrayList<myMarker> > p_map) throws SharkKBException {
        String linestring  = "LINESTRING (";
        String text = "";
        boolean merker = true;
        for (Map.Entry e : p_map.entrySet()) {
            long p_id = (long) e.getKey();
            if (!checkGeom(p_id)){
                Iterator<myMarker> markerIterator = p_map.get(p_id).iterator();
                while (markerIterator.hasNext()) {
                    myMarker marker = markerIterator.next();
                    GeoPoint geoPoint = marker.getPosition();
                    String str_latitude = String.valueOf(geoPoint.getLatitude());
                    String str_longitude = String.valueOf(geoPoint.getLongitude());
                    if(merker) {
                        text = marker.getTitle();
                        merker = false;
                        linestring = linestring + str_latitude + " " + str_longitude;
                    }else {
                        linestring = linestring + "," +  str_latitude + " " + str_longitude;
                    }
                }
                linestring = linestring + ")";
                SharkGeometry geom = InMemoSharkGeometry.createGeomByWKT(linestring);
                String tag = "" + p_id;
                String[] sis = new String[]{tag};
                SemanticTag stag = locations.createSpatialSemanticTag("polyline", sis, geom);
                stag.setProperty("descr", text);
            }
            merker = true;
        }
    }

    public boolean checkGeom(long id) throws SharkKBException{
        SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(""+id);
        if(tagBack != null)
            return true;
        else
            return false;
    }

    public void ladeView() throws SharkKBException{
        mapView.getOverlays().clear();
        mapView.getOverlays().add(0, mapEventsOverlay);
        deletedMarkers.clear();
        deletedPolylines.clear();
        InfoWindow.closeAllInfoWindowsOn(mapView);
        Iterator<SemanticTag> markers = locations.getSemanticTagByName("marker");
        Pattern r = Pattern.compile("([0-9-]+\\.[0-9]+)");

        while(markers.hasNext()){
            SemanticTag temp = markers.next();
            String[] str_temp = temp.getSI();
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(str_temp[0]);
            String geoString = tagBack.getGeometry().getWKT();

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

        Iterator<SemanticTag> lines = locations.getSemanticTagByName("polyline");
        while(lines.hasNext()){

            ArrayList<myMarker> temp_mList = new ArrayList<myMarker>();
            ArrayList<GeoPoint> temp_gpList = new ArrayList<GeoPoint>();
            SemanticTag temp = lines.next();
            String[] str_temp = temp.getSI();
            long p_id = Long.parseLong(str_temp[0]);
            SpatialSemanticTag tagBack = locations.getSpatialSemanticTag(str_temp[0]);
            String geoString = tagBack.getGeometry().getWKT();
            polyline_map.put(p_id,temp_mList);

            Matcher m = r.matcher(geoString);
            ArrayList<Double> geoData = new ArrayList<>();
            int i = 0;

            while(m.find()){
                geoData.add(Double.parseDouble(m.group()));
                m.find();
                geoData.add(Double.parseDouble(m.group()));

                GeoPoint geop = new GeoPoint(geoData.get(geoData.size()-2), geoData.get(geoData.size()-1));
                temp_gpList.add(geop);
                String value = temp.getProperty("descr");
                myMarker itemMarker = new myMarker(mapView,p_id);
                if(i == 0){
                    trackWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, value, p_id, 1, MainActivity.this);
                    itemMarker = createMarker(p_id, value, geop, mapView);
                    itemMarker.setInfoWindow(trackWindow);
                    itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                }

                if(i == geoData.size()-2){
                    itemMarker = createMarker(p_id, value, geop, mapView);
                    itemMarker.setInfoWindow(trackWindow);
                    itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                }

                polyline_map.get(p_id).add(itemMarker);
                temp_gpList.add(geop);
                i+=2;
            }
            drawPolyline(p_id,temp_gpList);
        }
        if(this.location != null)
            onLocationChanged(this.location);
    }

    // Funktion zur Erstellung einer eindeutigen ID
    public long createID(){
        long id = System.currentTimeMillis() / (long) (Math.random()+1);
        return id;
    }

    // Funktion zum Zeichnen eines Markers
    public void drawMarker(GeoPoint geop, String i_value, long m_id){
        itemName = i_value;
        marker_id = m_id;
        // Erstellung eines neues InfoWindows mit der ID und dem Namen des zugehörigen Markers
        InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id,0, MainActivity.this);
        // Erstellung des Markers mit der eindeutigen ID und dem Namen des Markers
        myMarker itemMarker = createMarker(marker_id, itemName, geop, mapView);
        // Das InfoWindow wird dem Marker zugewiesen
        itemMarker.setInfoWindow(infoWindow);
        // Marker wird zum Overlay hinzugefügt
        mapView.getOverlays().add(itemMarker);
        // Speichere den Marker in der Key-Value Map
        marker_map.put(marker_id, itemMarker);
        mapView.invalidate();
    }

    // Funktion zum Zeichnen einer Polyline
    public void drawPolyline(Long id, ArrayList<GeoPoint> gpList){
        myPolyline = new Polyline(mapView.getContext());
        myPolyline.setPoints(gpList);
        myPolyline.setColor(0xAA0000FF);
        myPolyline.setWidth(4.0f);
        myPolyline.setGeodesic(true);
        // Füge das Polyline-Objekt zum Overlay hinzu
        this.mapView.getOverlays().add(myPolyline);
        // Speichere die Polyline in der Key-Value Map
        polylineObj_map.put(id, myPolyline);
        // Erhalte alle Marker der zugehörigen Polyline
        ArrayList<myMarker> markerList = polyline_map.get(id);
        // Füge den visuellen Start- und Endpunkt zur Map hinzu
        mapView.getOverlays().add(markerList.get(0));
        mapView.getOverlays().add(markerList.get(markerList.size() - 1));

    }

    // Funktion, welche über IDs entscheidet, was bei einem ButtonClick geschehen soll
    public void onClick(View v){
        // Falls der Sync-Button gedrückt wurde
        if(v.getId() == R.id.btnSync){
            try{
                speichereView(marker_map, polyline_map);
            }catch (SharkKBException e){}
        // Falls der Load-Button gedrückt wurde
        }else if(v.getId() == R.id.btnLoad){
            try{
                ladeView();
            }catch (SharkKBException e){}
        // Falls der Add-Button gedrückt wurde
        }else if(v.getId() == R.id.btnAddItem){
            // Speichere aktuelle Position -> Position des Markers
            mLatitude = (int) (location.getLatitude() * 1E6);
            mLongtitude = (int) (location.getLongitude() * 1E6);
            gpt = new GeoPoint(mLatitude, mLongtitude);
            // Erstelle neues Eingabefenster
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Add Marker");
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                // Bei Klick auf den OK-Button
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    marker_id = createID();
                    itemName = input.getText().toString();
                    // Erstelle neues InfoWindow für den Marker
                    InfoWindow infoWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, itemName, marker_id, 0, MainActivity.this);
                    // Erstelle neuen Marker
                    myMarker itemMarker = createMarker(marker_id, itemName, gpt, mapView);
                    itemMarker.setInfoWindow(infoWindow);
                    Toast.makeText(MainActivity.this, "Marker \"" + itemName + "\" added", Toast.LENGTH_LONG).show();
                    // Zeichne Marker auf die Map
                    mapView.getOverlays().add(itemMarker);
                    // Speichere Marker in der Key-Value Map
                    marker_map.put(marker_id, itemMarker);
                    // Map aktualisieren
                    mapView.invalidate();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                // Bei Klick auf den Cancel-Button
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        // Falls der Zentrier-Button gedrückt wurde
        }else if(v.getId() == R.id.tbCenter) {
            if (centerEnabled)
                centerEnabled = false;
            else {
                centerEnabled = true;
                if (gpt != null)
                    mapController.setCenter(gpt);
            }
        // Falls der Start Tracking Button gedrückt wurde
        }else if(v.getId() == R.id.btnStartTracking){
            if(trackingEnabled){
                // Falls Tracking schon aktiviert ist
                // Lade- und Sync-Button abschalten
                loadView.setEnabled(true);
                syncView.setEnabled(true);
                startTracking.setText("Start Tracking");
                trackingEnabled = false;
                Iterator<GeoPoint> iterRoute = route.iterator();
                // Speichere jeden Punkt der Polyline als Marker ab und speichere es in die Key-Value Map
                while (iterRoute.hasNext()) {
                    GeoPoint temp_point = iterRoute.next();
                    myMarker temp_itemMarker = new myMarker(mapView, polyline_id);
                    temp_itemMarker.setPosition(temp_point);
                    polyline_map.get(polyline_id).add(temp_itemMarker);
                }
                // Erstelle Marker für den Endpunkt
                myMarker itemMarker = createMarker(polyline_id, trackName, gpt, mapView);
                itemMarker.setInfoWindow(trackWindow);
                itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                mapView.getOverlays().add(itemMarker);
                // Füge den Marker zu allen Markern der Polyline hinzu
                polyline_map.get(polyline_id).add(itemMarker);
                mapView.invalidate();
                trackWindow = null;
                route.clear();
                Toast.makeText(MainActivity.this, "Tracking deactivated", Toast.LENGTH_SHORT).show();
            }else {
                // Falls Tracking nicht aktiviert ist
                // Hier wird der Anfang der Polyline erstellt und das Tracking gestartet. Außerdem wird der Startmarker gesetzt
                myPolyline = null;
                // Erstelle neues Eingabefenster
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("New Track");
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    // Bei Klick des OK-Buttons
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadView.setEnabled(false);
                        syncView.setEnabled(false);
                        startTracking.setText("Stop Tracking");
                        trackingEnabled = true;
                        // Erstellung einer ID für die Polyline
                        polyline_id = createID();
                        trackName = input.getText().toString();
                        // Erstellung des Infowindows des Start- und Endmarkers
                        trackWindow = new MyInfoWindow(R.layout.bonuspack_bubble, mapView, trackName, polyline_id, 1, MainActivity.this);
                        myMarker itemMarker = createMarker(polyline_id, trackName, gpt, mapView);
                        itemMarker.setInfoWindow(trackWindow);
                        // Icon für den Marker definieren
                        itemMarker.setIcon(ContextCompat.getDrawable(MainActivity.this, R.mipmap.ic_map_marker_flag));
                        mapView.getOverlays().add(itemMarker);
                        mapView.invalidate();
                        route.add(gpt);
                        ArrayList<myMarker> temp_vec = new ArrayList<myMarker>();
                        polyline_map.put(polyline_id, temp_vec);
                        polyline_map.get(polyline_id).add(itemMarker);
                        Toast.makeText(MainActivity.this, "Tracking activated", Toast.LENGTH_SHORT).show();
                    }
                });
                // Bei Klick des Cancel-Buttons
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        }

    }

}
