/*
 * Copyright (c) 2018, Lefteris Harteros, All rights reserved.
 *
 */

package lefteris.harteros.gr.recommendationsystemclientapp.FrontEnd;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import lefteris.harteros.gr.recommendationsystemclientapp.R;

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    private Context context;

    public CustomInfoWindow(Context context) {
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = ((Activity) context).getLayoutInflater()
                .inflate(R.layout.info_window, null);

        TextView title = view.findViewById(R.id.name);
        TextView snippet = view.findViewById(R.id.details);
        ImageView img = view.findViewById(R.id.pic);


        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());

        InfoWindowImage infoWindowData = (InfoWindowImage) marker.getTag();
        img.setImageBitmap(infoWindowData.getImage());

        return view;
    }
}