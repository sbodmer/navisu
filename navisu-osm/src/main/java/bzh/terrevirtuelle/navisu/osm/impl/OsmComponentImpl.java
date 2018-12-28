package bzh.terrevirtuelle.navisu.osm.impl;


import bzh.terrevirtuelle.navisu.app.drivers.instrumentdriver.InstrumentDriver;
import bzh.terrevirtuelle.navisu.core.view.geoview.worldwind.impl.GeoWorldWindViewImpl;
import bzh.terrevirtuelle.navisu.osm.OsmComponent;
import bzh.terrevirtuelle.navisu.osm.OsmComponentServices;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.BasicLayerFactory;
import java.io.InputStream;
import org.capcaval.c3.component.ComponentState;
import org.osmbuildings.OSMBuildingsLayer;

/**
 * @author sbodmer
 * @date 24/12/2018 12:49
 */


public class OsmComponentImpl
        implements OsmComponent, OsmComponentServices, InstrumentDriver, ComponentState {

    private static final String NAME = "BUILDING";
    private static final String TYPE_0 = "OSM";
    
    @Override
    public boolean canOpen(String type) {
        boolean canOpen = false;
        if (type.equals(TYPE_0)) {
            canOpen = true;
        } else {

        }
        return canOpen;
    }

    @Override
    public void on(String... files) {
        System.out.println("ON:"+files);
    }

    @Override
    public void componentInitiated() {
        // System.out.println("INITED");
        //--- New instance with hard coded default value
        OSMBuildingsLayer layer = (OSMBuildingsLayer) GeoWorldWindViewImpl.getWW().getModel().getLayers().getLayersByClass(OSMBuildingsLayer.class).get(0);
        //--- Fine tune the layer
        layer.setMaxTiles(9);
        layer.setRows(3);
        layer.setCols(3);
        layer.setDrawProcessingBox(true);
        layer.setMinActiveAltitude(0);
        layer.setMaxActiveAltitude(8000);
        layer.setDefaultBuildingHeight(10);
        layer.setPickEnabled(false);
        // layer.setApplyRoofTextures(true);
    }

    @Override
    public void componentStarted() {
        // System.out.println("STARTED");
        // layer.addPreBuildingsRenderer(this);
        // layer.addTileListener(this);
        
    }

    @Override
    public void componentStopped() {
        // System.out.println("STOPPED");
        // layer.removePreBuildingsRenderer(this);
        // layer.removeTileListener(this);
        
    }

    @Override
    public InstrumentDriver getDriver() {
        return this;
    }

    public String getName() {
        return NAME;
    }

}
