/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bzh.terrevirtuelle.navisu.stl.databases.impl.controller.export.stl;

import bzh.terrevirtuelle.navisu.api.progress.Job;
import bzh.terrevirtuelle.navisu.api.progress.ProgressHandle;
import bzh.terrevirtuelle.navisu.app.guiagent.GuiAgentServices;
import bzh.terrevirtuelle.navisu.core.view.geoview.worldwind.impl.GeoWorldWindViewImpl;
import bzh.terrevirtuelle.navisu.domain.geometry.Point3D;
import bzh.terrevirtuelle.navisu.geometry.geodesy.GeodesyServices;
import bzh.terrevirtuelle.navisu.geometry.jts.JTSServices;
import bzh.terrevirtuelle.navisu.stl.charts.impl.loader.dem.DemSrtmElevationLoader;
import bzh.terrevirtuelle.navisu.util.io.IO;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;

/**
 *
 * @author serge
 */
public class DaeExportToSTL {

    private final GeodesyServices geodesyServices;
    private final GuiAgentServices guiAgentServices;
    protected JTSServices jtsServices;
    protected WorldWindow wwd = GeoWorldWindViewImpl.getWW();
    protected String CACHE_FILE_NAME = System.getProperty("user.home") + "/.navisu/caches/caches.properties";
    protected String OBJECT_DIR;
    protected Properties cacheProperties = new Properties();
    private double latMin;
    private double lonMin;
    private final Map<Point3D, List<String>> meshObjectsLocationMap;

    public DaeExportToSTL(GeodesyServices geodesyServices, GuiAgentServices guiAgentServices, JTSServices jtsServices) {
        this.guiAgentServices = guiAgentServices;
        this.geodesyServices = geodesyServices;//  elvScale = (latScale + lonScale) / 2;
        this.jtsServices = jtsServices;
        this.meshObjectsLocationMap = new HashMap<>();

        InputStream input = null;
        try {
            input = new FileInputStream(CACHE_FILE_NAME);
            cacheProperties.load(input);
            input.close();
        } catch (IOException ex) {
            Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
        }
        OBJECT_DIR = cacheProperties.getProperty("OBJECT_DIR");
        if (OBJECT_DIR == null) {
            OBJECT_DIR = "data/stl/kmz";
        }
    }

    public boolean loadDae() {
        double latitude;
        double longitude;
        double elevation;

        List<File> files = IO.multipleFileChooser(guiAgentServices.getStage(), OBJECT_DIR, "Georeferenced STL files (*.stl)", "*.STL", "*.stl");
        if (files != null) {
            OBJECT_DIR = files.get(0).getParent();
            OutputStream output = null;
            Properties properties = new Properties();
            try {
                output = new FileOutputStream(CACHE_FILE_NAME);
                properties.setProperty("OBJECT_DIR", OBJECT_DIR);
                properties.store(output, null);
                output.close();
            } catch (IOException ex) {
                Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
            }
            for (File file : files) {

                String content = null;
                try {
                    content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                } catch (IOException ex) {
                    Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                }
                if (content != null) {
                    // File STL with Geographics coordinates
                    // Update stl files map
                    if (content.contains("Origin")||content.contains("bounds")) {
                        String[] contentTab = content.split("\n");
                        for (String s : contentTab) {
                            if (s.contains("solid") && !s.contains("endsolid")) {
                                try {
                                    String[] tmp = s.trim().split("\\s+");
                                    latitude = Double.parseDouble(tmp[4]);
                                    longitude = Double.parseDouble(tmp[5]);
                                    elevation = Double.parseDouble(tmp[6]);
                                    Point3D key = new Point3D(latitude, longitude, elevation);
                                    if (!meshObjectsLocationMap.containsKey(key)) {
                                        meshObjectsLocationMap.put(key, new ArrayList<>());
                                    }
                                    meshObjectsLocationMap.get(key).add(file.getAbsolutePath());
                                } catch (NumberFormatException e) {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText("Bad format file STL");
                                    alert.show();
                                }
                            }
                        }
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Bad format file STL. \n"
                                + "Use item : Import/export 3D object from STL/KMZ \n"
                                + "in Tools menu, \n"
                                + "to transform metric coordinates\n"
                                + "in geographic coordinates");
                        alert.show();
                    }
                }
            }
        }
        //  System.out.println("daeLocationObjectMap : " + daeLocationObjectMap);
        return !meshObjectsLocationMap.isEmpty();
    }

    public void loadKmzAndSaveStlWgs84() {
        File file = IO.fileChooser(guiAgentServices.getStage(), "data/stl", "Georeferenced STL files (*.stl)", "*.STL", "*.stl");
        Point3D point = parseDocKmlFile(file);
        // daeLocationObjectMap.put(new Point3D(point.getLatitude(), point.getLongitude(), point.getElevation()), file.getAbsolutePath());
        toGeographicWGS84CoordAndSave(file, point);
    }

    public void toGeographicWGS84CoordAndSave(File file, Point3D point) {
        guiAgentServices.getJobsManager().newJob("Load KMZ objects", new Job() {

            @Override
            public void run(ProgressHandle progressHandle) {
                String result = "";
                String daeWgs84Filename = file.getName();
                daeWgs84Filename = daeWgs84Filename.replace(".stl", "Wgs84.stl");

                String content = null;
                // Transform vertex coordinates in angles
                try {
                    content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                } catch (IOException ex) {
                    Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                }
                Position position0 = new Position(Angle.fromDegrees(point.getLatitude()), Angle.fromDegrees(point.getLongitude()), point.getElevation());
                try {
                    content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                } catch (IOException ex) {
                    Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                }
                String[] contentTab;
                if (content != null) {
                    contentTab = content.split("\n");
                    for (String s : contentTab) {
                        if (s.contains("solid") && !s.contains("endsolid")) {
                            s = "";
                            result += "solid " + daeWgs84Filename + " Origin :  " + point.getLatitude() + " " + point.getLongitude() + " " + point.getElevation();
                        }
                        if (s.contains("endsolid")) {
                            s = "";
                            result += "endsolid " + daeWgs84Filename;
                        }
                        if (s.contains("vertex")) {
                            Position position1;
                            Position position2;

                            String[] tmp = s.trim().split("\\s+");
                            if (tmp[1].contains("-")) {
                                position1 = geodesyServices.getPosition(position0, 270.0, (-Double.valueOf(tmp[1])), Double.valueOf(tmp[3]));
                            } else {
                                position1 = geodesyServices.getPosition(position0, 90.0, (Double.valueOf(tmp[1])), Double.valueOf(tmp[3]));
                            }
                            if (tmp[2].contains("-")) {
                                position2 = geodesyServices.getPosition(position1, 180.0, (-Double.valueOf(tmp[2])), Double.valueOf(tmp[3]));
                            } else {
                                position2 = geodesyServices.getPosition(position1, 0.0, (Double.valueOf(tmp[2])), Double.valueOf(tmp[3]));
                            }
                            double height = position2.getElevation() + +point.getElevation();
                            result += "vertex " + position2.getLongitude().getDegrees() + " " + position2.getLatitude().getDegrees() + " " + height + "\n";
                        } else {
                            result += s + "\n";
                        }
                    }
                    try {
                        if (new File(file.getParent() + "/" + daeWgs84Filename).exists()) {
                            Files.delete(Paths.get(file.getParent() + "/" + daeWgs84Filename));
                        }
                        Files.write(Paths.get(file.getParent() + "/" + daeWgs84Filename), result.getBytes(), StandardOpenOption.CREATE);
                    } catch (IOException ex) {
                        Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                    }
                }
            }
        });
    }

    public void export(Point3D[][] g, String filename, double latScale, double lonScale, double tileSideZ, double maxdepth) {
        double elvScale = (latScale + lonScale) / 2;
        latMin = g[0][0].getLatitude();
        lonMin = g[0][0].getLongitude();
        List<Point3D> bounds = new ArrayList<>();
        bounds.add(g[0][0]);
        bounds.add(g[0][g[1].length - 1]);
        bounds.add(g[g[0].length - 1][g[1].length - 1]);
        bounds.add(g[g[0].length - 1][0]);
        bounds.add(g[0][0]);

        List<Point3D> locInBound = new ArrayList<>();
        Set<Point3D> locations = meshObjectsLocationMap.keySet();
       // System.out.println("meshObjectsLocationMap : " + meshObjectsLocationMap);
        locations.stream().filter((loc) -> (jtsServices.contains(jtsServices.getPolygon(bounds), loc))).forEachOrdered((loc) -> {
            locInBound.add(loc);
        });

        for (Point3D p : locInBound) {
            List<String> contents = meshObjectsLocationMap.get(p);
            String content = null;
            // Transform vertex coordinates in angles
            for (String ss : contents) {
                try {
                    content = new String(Files.readAllBytes(Paths.get(ss)));
                } catch (IOException ex) {
                    Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                }
                if (content != null) {
                    if (content.contains("Origin")||content.contains("bounds")) {

                        // Transform vertex angle coordinates in coordinates for one tile
                        String stlResult = "";
                        String[] resultTab = content.split("\n");
                        for (String s : resultTab) {
                            if (s.contains("facet") || s.contains("loop") || s.contains("solid")) {
                                stlResult += s + "\n";
                            } else {
                                if (s.contains("vertex")) {
                                    String[] c = s.trim().split("\\s+");
                                    double lon = Double.parseDouble(c[1]);
                                    double lat = Double.parseDouble(c[2]);

                                    double latM = geodesyServices.getDistanceM(latMin, lonMin, lat, lonMin);
                                    double lonM = geodesyServices.getDistanceM(latMin, lonMin, latMin, lon);
                                    latM *= latScale;
                                    lonM *= lonScale;

                                    double elv = Double.parseDouble(c[3]) * elvScale + tileSideZ;
                                    stlResult += "vertex " + lonM + " " + latM + " " + elv + "\n";
                                }
                            }
                        }

                        //Insert dae file in stlFile.stl
                        try {
                            Files.write(Paths.get(filename), stlResult.getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException ex) {
                            Logger.getLogger(DaeExportToSTL.class.getName()).log(Level.SEVERE, ex.toString(), ex);
                        }
                    }
                }
            }
        }
    }

    private Point3D parseDocKmlFile(File file) {
        Point3D point = null;
        try {
            String latString = "";
            String lonString = "";
            double latitude = 0.0;
            double longitude = 0.0;
            String latLonString = new String(Files.readAllBytes(Paths.get(file.getParent() + "/doc.kml")));

            String[] latLonTab = latLonString.split("\n");
            boolean match = false;
            for (int i = 0; i < latLonTab.length; i++) {
                if (latLonTab[i].contains("<Model>")) {
                    while (!latLonTab[i].contains("<latitude>")) {
                        i++;
                    }
                    latString = latLonTab[i];
                    lonString = latLonTab[i + 1];
                    match = true;
                }
            }
            if (match == true) {
                latString = latString.replace("<latitude>", "");
                lonString = lonString.replace("<longitude>", "");
                latString = latString.replace("</latitude>", "");
                lonString = lonString.replace("</longitude>", "");
                latitude = Double.parseDouble(latString);
                longitude = Double.parseDouble(lonString);
            }
            point = new DemSrtmElevationLoader(geodesyServices).getElevation(latitude, longitude);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Le répertoire : \n "
                    + file.getParent()
                    + "\n du fichier : " + file.getName()
                    + "  à charger, doit aussi comporter un fichier doc.kml "
                    + "\n avec les coordonnées géographiques du repère.");
            alert.show();
        }
        return point;
    }
}
