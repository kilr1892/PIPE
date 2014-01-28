package pipe.views;

import pipe.controllers.PetriNetController;
import pipe.gui.ApplicationSettings;
import pipe.gui.Constants;
import pipe.gui.PetriNetTab;
import pipe.gui.widgets.ArcWeightEditorPanel;
import pipe.gui.widgets.EscapableDialog;
import pipe.handlers.ArcHandler;
import pipe.models.PipeObservable;
import pipe.models.component.Connectable;
import pipe.models.component.arc.Arc;
import pipe.models.component.arc.ArcPoint;
import pipe.views.viewComponents.ArcPath;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * This class contains the common methods for different arc types.
 * <p/>
 * At present when arc points are modified the whole arc is redrawn. At some point
 * in the future it would be good if they're more dynamic than this.
 *
 * @param <S> Model source type
 * @param <T> Model target type
 */
public abstract class ArcView<S extends Connectable, T extends Connectable>
        extends AbstractPetriNetViewComponent<Arc<S, T>> implements Cloneable, Serializable, Observer {
    /**
     * Bounds of arc need to be grown in order to avoid clipping problems.
     * This value achieves it.
     */
    protected static final int ZOOM_GROW = 10;

    /**
     * Actual visible path
     */
    final protected ArcPath arcPath;

    /**
     * This is a reference to the petri net tab that this arc is placed on.
     * It is needed to add ArcPoints to the petri net based on the models intermediate
     * points
     */
    protected PetriNetTab tab = null;

    /**
     * true if arc is not hidden when a bidirectional arc is used
     */
    boolean inView = true;

    private ArcPoint sourcePoint;

    private ArcPoint endPoint;

    public ArcView(Arc<S, T> model, PetriNetController controller) {
        super(model.getId(), model, controller);
        arcPath = new ArcPath(this, controller);


        addPathSourceLocation();
        addPathEndLocation();
        updatePath();
        updateBounds();
        addConnectableListener();
    }

    /**
     * Updates the bounding box of the arc component based on the arcs bounds
     */
    public void updateBounds() {
        bounds = arcPath.getBounds();
        bounds.grow(getComponentDrawOffset() + ZOOM_GROW, getComponentDrawOffset() + ZOOM_GROW);
        setBounds(bounds);
    }

    private void addPathSourceLocation() {
        Point2D.Double startPoint = model.getStartPoint();
        sourcePoint = new ArcPoint(startPoint, false);
        arcPath.addPoint(sourcePoint);
    }

    private void addPathEndLocation() {
        Point2D.Double targetPoint = model.getEndPoint();
        endPoint = new ArcPoint(targetPoint, false);
        arcPath.addPoint(endPoint);
    }

    /**
     * Repopulates the path with the models points
     */
    private void updatePath() {
        addIntermediatePoints();
        arcPath.createPath();
        if (tab != null) {
            arcPath.addPointsToGui(tab);
        }
        repaint();
    }

    /**
     * Loops through points in revese order adding them to the path
     * Since addPointAt inserts to the left of the index to get
     * between and the start we need to always insert left of the
     * end.
     */
    private void addIntermediatePoints() {
        Iterable<ArcPoint> points = new ArrayList<ArcPoint>(model.getIntermediatePoints());
        for (ArcPoint arcPoint : points) {
            if (!arcPath.contains(arcPoint)) {
                arcPath.insertIntermediatePoint(arcPoint);
            }
        }
    }

    private void addConnectableListener() {
        addArcChangeListener();
        addSourceTargetConnectableListener();

    }

    private void addArcChangeListener() {
        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                String name = propertyChangeEvent.getPropertyName();
                if (name.equals(Arc.NEW_INTERMEDIATE_POINT_CHANGE_MESSAGE)) {
                    updatePath();
                    arcSpecificUpdate();
                    updateBounds();
                } else if (name.equals(Arc.DELETE_INTERMEDIATE_POINT_CHANGE_MESSAGE)) {
                    ArcPoint point = (ArcPoint) propertyChangeEvent.getOldValue();
                    arcPath.deletePoint(point);
                    updatePath();
                    arcSpecificUpdate();
                    updateBounds();
                }
            }
        };
        model.addPropertyChangeListener(listener);
    }

    /**
     * Perform any updates specific to the arc type
     * E.g. NormalArc should show weights
     */
    public abstract void arcSpecificUpdate();

    /**
     * Listens to the source/target changing position
     */
    private void addSourceTargetConnectableListener() {
        model.getSource().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                String name = propertyChangeEvent.getPropertyName();
                if (name.equals(Connectable.X_CHANGE_MESSAGE) || name.equals(Connectable.Y_CHANGE_MESSAGE)) {
                    sourcePoint.setPoint(model.getStartPoint());
                    arcSpecificUpdate();
                }
            }
        });
        model.getTarget().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                String name = propertyChangeEvent.getPropertyName();
                if (name.equals(Connectable.X_CHANGE_MESSAGE) || name.equals(Connectable.Y_CHANGE_MESSAGE)) {
                    endPoint.setPoint(model.getEndPoint());
                    arcSpecificUpdate();
                }
            }
        });
    }

    public PetriNetTab getTab() {
        return tab;
    }

    /**
     * Perform any arc specific addition acitons
     */
    protected abstract void arcSpecificAdd();

    @Override
    public boolean contains(int x, int y) {
        Point2D.Double point = new Point2D.Double(x + arcPath.getBounds().getX() - getComponentDrawOffset() -
                ZOOM_GROW, y + arcPath.getBounds().getY() - getComponentDrawOffset() -
                ZOOM_GROW);
        if (!ApplicationSettings.getApplicationView().getCurrentTab().isInAnimationMode()) {
            if (arcPath.proximityContains(point) || isSelected()) {
                // show also if Arc itself selected
                arcPath.showPoints();
            } else {
                arcPath.hidePoints();
            }
        }

        return arcPath.contains(point);
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public String getId() {
        return model.getId();
    }

    @Override
    public void addedToGui() {
        // called by PetriNetTab / State viewer when adding component.
        _deleted = false;
        _markedAsDeleted = false;

        arcPath.addPointsToGui(tab);
        updateArcPosition();
        //addWeightLabelsToContainer(getParent());
    }

    //TODO: DELETE
    public void updateArcPosition() {
        //Pair<Point2D.Double, Point2D.Double> points = getArcStartAndEnd();
        //        addPathSourceLocation(points.first.x, points.first.y);
        //        setTargetLocation(points.second.x, points.second.y);
        //        if (_source != null) {
        //            _source.updateEndPoint(this);
        //        }
        //        if (_target != null) {
        //            _target.updateEndPoint(this);
        //        }
        //        arcPath.createPath();
    }

    @Override
    public void delete() {
        if (!_deleted) {
            arcSpecificDelete();

            arcPath.forceHidePoints();
            super.delete();
            _deleted = true;
        }
    }

    @Override
    public int getLayerOffset() {
        return Constants.ARC_LAYER_OFFSET;
    }

    /**
     * Method to clone an Arc object
     */
    @Override
    public AbstractPetriNetViewComponent clone() {
        return super.clone();
    }

    /**
     * Perform any arc specific deletion acitons
     */
    protected abstract void arcSpecificDelete();

    @Override
    public void translate(int x, int y) {
        // We don't translate an arc, we translate each selected arc point
    }

    @Override
    public void addToPetriNetTab(PetriNetTab tab) {
        this.tab = tab;
        updatePath();
        ArcHandler<S, T> arcHandler = new ArcHandler<S, T>(this, tab, this.model, petriNetController);
        addMouseListener(arcHandler);
        addMouseWheelListener(arcHandler);
        addMouseMotionListener(arcHandler);
    }

    // Steve Doubleday (Oct 2013): cascading clean up of Marking Views if Token View is disabled
    @Override
    public void update(Observable observable, Object obj) {
        if ((observable instanceof PipeObservable) && (obj == null)) {
            // if multiple cases are added, consider creating specific subclasses of Observable
            Object originalObject = ((PipeObservable) observable).getObservable();
            //            if (originalObject instanceof MarkingView) {
            //                MarkingView viewToDelete = (MarkingView) originalObject;
            //                _weight.remove(viewToDelete);
            //            }
        }
    }

    //TODO: DELETE AND REPOINT METHODS AT THE MODEL VERSION
    public ConnectableView<Connectable> getSource() {
        return null;
    }

    //TODO: DELETE
    void setSource(ConnectableView<?> sourceInput) {
        throw new RuntimeException("Should be setting models source");
    }

    //TODO: DELETE AND REPOINT METHODS AT THE MODEL VERSION
    public ConnectableView<Connectable> getTarget() {
        return null;
    }

    //TODO: DELETE
    public void setTarget(ConnectableView<?> targetInput) {
        throw new RuntimeException("Should be setting models target");
    }

    public int getSimpleWeight() {
        return 1;
    }

    public ArcPath getArcPath() {
        return arcPath;
    }

    public abstract String getType();

    public boolean inView() {
        return inView;
    }

    public void removeFromView() {
        if (getParent() != null) {
            arcSpecificDelete();
        }
        arcPath.forceHidePoints();
        removeFromContainer();
    }

    public void addToView(PetriNetTab view) {
        if (getParent() != null) {
            arcSpecificUpdate();
        }
        arcPath.showPoints();
        view.add(this);
    }

    public void showEditor() {
        // Build interface
        EscapableDialog guiDialog = new EscapableDialog(ApplicationSettings.getApplicationView(), "PIPE2", true);

        ArcWeightEditorPanel arcWeightEditor = new ArcWeightEditorPanel(guiDialog.getRootPane(), petriNetController,
                petriNetController.getArcController(this.model));

        guiDialog.add(arcWeightEditor);

        guiDialog.getRootPane().setDefaultButton(null);

        guiDialog.setResizable(false);

        // Make window fit contents' preferred size
        guiDialog.pack();

        // Move window to the middle of the screen
        guiDialog.setLocationRelativeTo(null);

        guiDialog.setVisible(true);

        guiDialog.dispose();
    }

    // Accessor function to check whether or not the Arc is tagged
    public boolean isTagged() {
        return false;
    }

    //TODO: DELETE
    public List<MarkingView> getWeightSimple() {
        return null;
    }

    //TODO DELETE:
    public List<MarkingView> getWeight() {
        return null;
    }
}
