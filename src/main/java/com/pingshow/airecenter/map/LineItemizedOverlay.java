package com.pingshow.airecenter.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class LineItemizedOverlay extends Overlay
{

    private List<GeoPoint> mOverlays = new ArrayList<GeoPoint>();
    private static final int ALPHA = 120;
    private static final float STROKE = 7;
    private final Path path;
    private final Point p;
    private final Paint paint;

    public LineItemizedOverlay(List<GeoPoint> mOverlays)
    {
        this.mOverlays = mOverlays;
        path = new Path();
        p = new Point();
        paint = new Paint();

    }
 
    @Override

    public void draw(Canvas canvas, MapView mapView, boolean shadow)

    {
        super.draw(canvas, mapView, shadow); 
        paint.setColor(Color.argb(120, 7, 240, 248));
        paint.setAlpha(ALPHA);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(STROKE);
        paint.setStyle(Paint.Style.STROKE); 
        Projection prj = mapView.getProjection();
        path.rewind();
        Iterator<GeoPoint> it = mOverlays.iterator();
        prj.toPixels(it.next(), p);
        path.moveTo(p.x, p.y);
 
        while (it.hasNext())
        {
            prj.toPixels(it.next(), p);
            path.lineTo(p.x, p.y);
        }
        path.setLastPoint(p.x, p.y);
         
        canvas.drawPath(path, paint);
    }
 
}
