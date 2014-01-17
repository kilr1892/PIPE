package pipe.gui;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

public class ZoomController implements Serializable {

    private final AffineTransform transform = new AffineTransform();

    private int percent;

    public ZoomController(int pct) {
        percent = pct;
    }

    public static int getZoomedValue(int x, int zoom) {
        return (int) (x * zoom * 0.01);
    }

    public static float getZoomedValue(float value, int zoom) {
        return (float) (value * zoom * 0.01);
    }

    public static AffineTransform getTransform(int zoom) {
        return AffineTransform.getScaleInstance(zoom * 0.01, zoom * 0.01);
    }

    public static double getScaleFactor(int zoom) {
        return zoom * 0.01;
    }

    public static int getUnzoomedValue(int value, int zoom) {
        return (int) (value / (zoom * 0.01));
    }

    public static Point2D.Double getZoomedValue(Point2D.Double point, int zoom) {
        return new Point2D.Double(getZoomedValue(point.getX(), zoom), getZoomedValue(point.getY(), zoom));
    }

    public static double getZoomedValue(double value, int zoom) {
        return (value * zoom * 0.01);
    }

    public static Point2D.Double getUnzoomedValue(final Point2D.Double point, final int zoom) {
        return new Point2D.Double(getUnzoomedValue(point.getX(), zoom), getUnzoomedValue(point.getY(), zoom));
    }

    public static double getUnzoomedValue(double value, int zoom) {
        return (value / (zoom * 0.01));
    }

    public boolean zoomOut() {
        percent -= Constants.ZOOM_DELTA;
        if (percent < Constants.ZOOM_MIN) {
            percent += Constants.ZOOM_DELTA;
            return false;
        } else {
            return true;
        }
    }

    public boolean zoomIn() {
        percent += Constants.ZOOM_DELTA;
        if (percent > Constants.ZOOM_MAX) {
            percent -= Constants.ZOOM_DELTA;
            return false;
        } else {
            return true;
        }
    }

    public int getPercent() {
        return percent;
    }

    private void setPercent(int newPercent) {
        if ((newPercent >= Constants.ZOOM_MIN) && (newPercent <= Constants.ZOOM_MAX)) {
            percent = newPercent;
        }
    }

    public void setZoom(int newPercent) {
        setPercent(newPercent);
    }

    public AffineTransform getTransform() {
        return transform;
    }
}
