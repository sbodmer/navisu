package bzh.terrevirtuelle.navisu.domain.charts.vector.s57;

import bzh.terrevirtuelle.navisu.domain.charts.vector.s57.controler.analyzer.DataSet;
import java.nio.ByteOrder;

public class Node extends VectorRecord {

    protected Point point;

    /**
     * R�cup�re les coordonn�es 2D du point
     * @param fieldValue
     */
    @Override
    public void decodSG2D(byte[] fieldValue) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(fieldValue.length);

        bb.put(fieldValue);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        double y = (double) bb.getInt(0);
        double x = (double) bb.getInt(4);
        Point2D pt = new Point2D();
        pt.setX(x / DataSet.getCOMF());
        pt.setY(y / DataSet.getCOMF());
        setPoint(pt);
    }

    /**
     * R�cup�re les coordonn�es 3D du point
     * @param fieldValue
     */
    @Override
    public void decodSG3D(byte[] fieldValue) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(fieldValue.length);

        bb.put(fieldValue);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        double y = (double) bb.getInt(0);
        double x = (double) bb.getInt(4);
        double z = (double) bb.getInt(8);
        Point3D pt = new Point3D();
        pt.setX(x / DataSet.getCOMF());
        pt.setY(y / DataSet.getCOMF());
        pt.setZ(z / DataSet.getSOMF());
        setPoint(pt);
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point pt) {
        this.point = pt;
    }

    @Override
    public String toString() {
        return "Node{" + "point=" + point + '}';
    }

}
