/usr/bin/ogr2ogr -f "ESRI Shapefile" tmp/tmp.shp PG:"host=localhost user=admin password=admin dbname=s57NP5DB" -sql "SELECT geom,drval1,drval2 FROM depare WHERE geom && ST_MakeEnvelope(-5.0169605104730195,48.1825832198153,-4.572030314454884,48.478382377543674)"
/usr/bin/ogr2ogr -clipdst -5.0169605104730195 48.1825832198153 -4.572030314454884 48.478382377543674 tmp/depare.shp tmp/tmp.shp