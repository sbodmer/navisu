/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bzh.terrevirtuelle.navisu.charts.vector.s57.charts.impl.view;

import bzh.terrevirtuelle.navisu.core.view.geoview.worldwind.impl.GeoWorldWindViewImpl;
import bzh.terrevirtuelle.navisu.domain.charts.vector.s57.model.geo.Buoyage;
import bzh.terrevirtuelle.navisu.domain.charts.vector.s57.model.geo.Landmark;
import bzh.terrevirtuelle.navisu.topology.TopologyServices;
import bzh.terrevirtuelle.navisu.util.Pair;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author serge
 */
public class BuoyageView {

    protected TopologyServices topologyServices;
    protected RenderableLayer layer;
    protected List<Buoyage> buoyages;
    protected PointPlacemark placemark;
    protected PointPlacemarkAttributes attrs;
    protected String acronym;
    protected Map<Pair<Double, Double>, String> topMarkMap;
    protected WorldWindow wwd = GeoWorldWindViewImpl.getWW();
    protected double lat;
    protected double lon;
    protected String topMark;
    protected String label;
    protected boolean dev = false;
    protected Map<Pair<Double, Double>, String> marsysMap;

    public BuoyageView(TopologyServices topologyServices,
            Map<Pair<Double, Double>, String> topMarkMap,
            RenderableLayer layer,
            String acronym,
            Map<Pair<Double, Double>, String> marsysMap) {
        this.topologyServices = topologyServices;
        this.topMarkMap = topMarkMap;
        this.marsysMap = marsysMap;
        this.acronym = acronym;
        this.layer = layer;
    }

    @SuppressWarnings("unchecked")
    public void display(List<Buoyage> buoyages) {

        List<PointPlacemark> pointPlacemarks = new ArrayList<>();
        for (Buoyage buoyage : buoyages) {
            String geom = buoyage.getGeom();
            LatLon latLon;
            if ((geom.contains("MULTIPOINT") || geom.contains("POINT")) && !geom.contains("EMPTY")) {
                latLon = topologyServices.wktMultiPointToWwjLatLon(geom);
                buoyage.setLatitude(latLon.getLatitude().getDegrees());
                buoyage.setLongitude(latLon.getLongitude().getDegrees());
                lat = buoyage.getLatitude();
                lon = buoyage.getLongitude();
                String ma = marsysMap.get(new Pair(lat, lon));
                if (ma == null) {
                    ma = "1";
                }
                buoyage.setMarsys(ma);
                placemark = new PointPlacemark(Position.fromDegrees(lat, lon, 0));
                placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                label = buoyage.getClass().getSimpleName() + "\n"
                        + buoyage.getObjectName() + "\n"
                        + "Lat : " + new Float(lat).toString() + "\n"
                        + "Lon : " + new Float(lon).toString();
                buoyage.setLabel(label);
                placemark.setValue(AVKey.DISPLAY_NAME, label);
                topMark = topMarkMap.get(new Pair(lat, lon));
                if (topMark == null) {
                    topMark = "0";
                }
                String imageAddress = "";
                if (acronym.equals("LNDMRK")) {
                    imageAddress = "img/landmarks_" + buoyage.getMarsys() + "/"
                            + acronym + "_"
                            + buoyage.getCategoryOfMark() + "_"
                            + buoyage.getConspicuousVisually() + "_"
                            + ((Landmark) buoyage).getFunction() + "_"
                            + buoyage.getColour() + "_"
                            + buoyage.getColourPattern() + "_"
                            + buoyage.getMarsys()
                            + ".png";
                } else {
                    if (acronym.equals("DAYMAR")) {
                       // System.out.println("buoyage");
                        imageAddress = "img/daymarks_"
                                + buoyage.getMarsys() + "/"
                                + acronym + "_"
                                + buoyage.getShape() + "_"
                                + buoyage.getCategoryOfMark() + "_"
                                + buoyage.getColour() + "_"
                                + buoyage.getColourPattern() + "_"
                                + buoyage.getNatureOfConstruction() + "_"
                                + buoyage.getMarsys()
                                + ".png";
                    } else {
                        imageAddress = "img/buoyage_"
                                + buoyage.getMarsys() + "/"
                                + acronym + "_"
                                + buoyage.getShape() + "_"
                                + buoyage.getCategoryOfMark() + "_"
                                + buoyage.getColour() + "_"
                                + buoyage.getColourPattern() + "_"
                                + topMark + "_"
                                + buoyage.getMarsys() + ".png";
                    }
                }
                buoyage.setImageAddress(imageAddress);
                attrs = new PointPlacemarkAttributes();
                attrs.setImageAddress(imageAddress);
                attrs.setImageOffset(Offset.BOTTOM_CENTER);
                attrs.setScale(0.65);//0.9
                placemark.setAttributes(attrs);
                if (dev) {
                    placemark.setValue(AVKey.DISPLAY_NAME, imageAddress);
                }
                pointPlacemarks.add(placemark);
            }
            layer.addRenderables(pointPlacemarks);
        }
    }
}
