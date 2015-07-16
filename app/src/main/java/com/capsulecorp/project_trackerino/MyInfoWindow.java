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

public class MyInfoWindow extends InfoWindow {
    public MapView mapView;
    public Context ctx;
    public String name;
    public long objID;
    public int type;             // 0 = Marker, 1 = Polyline
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
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                builder.setTitle("Optionen");



                // Set up the buttons
                builder.setPositiveButton("Editieren", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle("Editieren");

                        // Set up the input
                        final EditText input = new EditText(ctx);
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                        builder.setView(input);

                        // Set up the buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
                builder.setNegativeButton("Loeschen", new DialogInterface.OnClickListener() {
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
