package com.capsulecorp.project_trackerino;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.views.MapView;

/**
 * Created by Dennis on 02.07.2015.
 */
public class myMarker extends Marker {
    long id;
    public myMarker(MapView mapView, long id) {
        super(mapView);
        this.id = id;
    }

    public long getId(){
        return id;
    }
}
