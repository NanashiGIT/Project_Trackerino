package com.capsulecorp.project_trackerino;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.sharkfw.knowledgeBase.SharkKBException;

import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.views.MapView;

import java.util.Vector;

// Eigene Klasse zur Erweiterung der vorgefertigten InfoWindow-Klasse
// InfoWindow ist das Fenster eines jeden Markers, welches sich öffnet, wenn man auf den Marker klickt (Auch bei Start/Endpunkt einer Polyline)

public class MyInfoWindow extends InfoWindow {
    public MapView mapView;                                                                         // MapView, welcher von der MainActivity übergeben wird
    public Context ctx;                                                                             // Context, welcher von der MainActivity übergeben wird
    public String name;                                                                             // Name des Markers/der Polyline
    public long objID;                                                                              // Die eindeutige ID des Items
    public int type;                                                                                // 0 = Marker, 1 = Polyline

    // Konstruktor
    public MyInfoWindow(int layoutResId, MapView mapView, String name,long id,int type, Context ctx) {
        super(layoutResId, mapView);
        this.mapView = mapView;
        this.ctx = ctx;
        this.name = name;
        this.type = type;
        objID = id;
    }
    public void onClose() {
    }

    // Funktion, welche aufgerufen wird, wenn das InfoWindow geöffnet wird
    // Zeigt den Namen des angeklickten Items und ermöglicht bei Klick auf das InfoWindow, den Namen zu ändern
    public void onOpen(Object arg0) {
        LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble_layout);
        Button btnMoreInfo = (Button) mView.findViewById(R.id.bubble_moreinfo);
        TextView txtTitle = (TextView) mView.findViewById(R.id.bubble_title);
        TextView txtDescription = (TextView) mView.findViewById(R.id.bubble_description);
        TextView txtSubdescription = (TextView) mView.findViewById(R.id.bubble_subdescription);
        // Schließe alle offenen InfoWindows auf der Map
        InfoWindow.closeAllInfoWindowsOn(mapView);
        // Zeige den Namen des zugehörigen Items im InfoFenster an
        txtTitle.setText(name);
        txtDescription.setText("Click here to edit!");
        layout.setOnClickListener(new View.OnClickListener() {
            // Bei Klick auf das InfoWindow
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle("Options");

                // Die Buttons einrichten
                builder.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                    // Bei Klick auf den Edit Button
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Erstellung eines neuen Eingabefensters
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle("Edit");

                        final EditText input = new EditText(ctx);
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                        builder.setView(input);

                        // Einrichtung neuer Buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Eingabe des neuen Namens
                                name = input.getText().toString();
                                if(type == 0)
                                    MainActivity.editMarker(objID, name);
                                else
                                    MainActivity.editPolyline(objID, name);
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
                builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    // Bei Klick des Delete-Buttons
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (type == 0)
                                MainActivity.marker_map = MainActivity.loescheMarker(MainActivity.marker_map, objID);
                            else {
                                MainActivity.polyline_map = MainActivity.loeschePolyline(MainActivity.polyline_map, objID);
                                MainActivity.polylineObj_map = MainActivity.loeschePolylineObj(MainActivity.polylineObj_map, objID);
                            }
                        } catch (SharkKBException e) {
                            e.printStackTrace();
                        }
                        InfoWindow.closeAllInfoWindowsOn(mapView);
                    }
                });

                builder.show();
            }
        });
    }
}
