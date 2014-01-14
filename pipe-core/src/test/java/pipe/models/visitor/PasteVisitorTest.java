package pipe.models.visitor;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import pipe.models.PetriNet;
import pipe.models.component.*;
import pipe.models.strategy.arc.ArcStrategy;
import pipe.models.strategy.arc.BackwardsNormalStrategy;
import pipe.visitor.PasteVisitor;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PasteVisitorTest {
    PasteVisitor visitor;
    PetriNet petriNet;
    Collection<PetriNetComponent> pasteComponents;

    @Before
    public void setUp() {
        pasteComponents = new LinkedList<PetriNetComponent>();
        petriNet = mock(PetriNet.class);
    }

    @Test
    public void pastingPlace() {
        Place place = new Place("id", "name");
        place.setCapacity(10);
        pasteComponents.add(place);
        visitor = new PasteVisitor(petriNet, pasteComponents);

        doPaste();

        verify(petriNet).addPlace(argThat(matchesThisPlaceWithCopiedNameAndId(place)));
    }

    private void doPaste() {
        for (PetriNetComponent component : pasteComponents) {
            component.accept(visitor);
        }
    }

    private Matcher<Place> matchesThisPlaceWithCopiedNameAndId(Place place) {
        return new CopiedPlace(place);
    }

    @Test
    public void pastingPlaceWithOffset() {
        Place place = new Place("id", "name");
        place.setCapacity(10);
        pasteComponents.add(place);
        Point2D offset = new Point2D.Double(40, 20);
        visitor = new PasteVisitor(petriNet, pasteComponents, offset.getX(), offset.getY());

        doPaste();

        verify(petriNet).addPlace(argThat(matchesThisPlaceWithCopiedNameAndIdAndOffset(place, offset)));
    }

    private Matcher<Place> matchesThisPlaceWithCopiedNameAndIdAndOffset(Place place, Point2D offset) {
        return new CopiedPlace(place, offset);
    }

    @Test
    public void pastingTransition() {
        Transition transition = new Transition("id", "name");
        transition.setAngle(45);
        transition.setPriority(10);
        pasteComponents.add(transition);
        visitor = new PasteVisitor(petriNet, pasteComponents);

        doPaste();

        verify(petriNet).addTransition(argThat(matchesThisTransitionWithCopiedNameAndId(transition)));
    }

    @Test
    public void pastingTransitionWithOffset() {
        Transition transition = new Transition("id", "name");
        transition.setAngle(45);
        transition.setPriority(10);
        pasteComponents.add(transition);
        Point2D offset = new Point2D.Double(40, 20);
        visitor = new PasteVisitor(petriNet, pasteComponents, offset.getX(), offset.getY());

        doPaste();

        verify(petriNet).addTransition(argThat(matchesThisTransitionWithCopiedNameAndIdAndOffset(transition, offset)));
    }

    private Matcher<Transition> matchesThisTransitionWithCopiedNameAndIdAndOffset(Transition transition,
                                                                                  Point2D offset) {
        return new CopiedTransition(transition, offset);
    }

    private Matcher<Transition> matchesThisTransitionWithCopiedNameAndId(Transition transition) {
        return new CopiedTransition(transition);
    }

    @Test
    public void pastingArcTransitionAndPlaceInSelected() {
        Place place = new Place("id", "name");
        Transition transition = new Transition("id", "name");
        pasteComponents.add(place);
        pasteComponents.add(transition);

        Map<Token, String> weights = new HashMap<Token, String>();
        ArcStrategy<Place, Transition> arcStrategy = new BackwardsNormalStrategy(petriNet);
        Arc<Place, Transition> arc = new Arc<Place, Transition>(place, transition, weights, arcStrategy);
        pasteComponents.add(arc);
        visitor = new PasteVisitor(petriNet, pasteComponents);

        doPaste();

        verify(petriNet).addArc(argThat(hasCopiedIdAndNameAndBothComponentsAreCopied(arc)));
    }

    private Matcher<Arc<? extends Connectable, ? extends Connectable>> hasCopiedIdAndNameAndBothComponentsAreCopied(
            Arc<? extends Connectable, ? extends Connectable> arc) {
        return new CopiedArc(arc, true, true);
    }

    @Test
    public void pastingArcSourceInSelected() {
        Place place = new Place("id", "name");
        pasteComponents.add(place);

        Transition transition = new Transition("id", "name");
        Map<Token, String> weights = new HashMap<Token, String>();
        ArcStrategy<Place, Transition> arcStrategy = new BackwardsNormalStrategy(petriNet);
        Arc<Place, Transition> arc = new Arc<Place, Transition>(place, transition, weights, arcStrategy);
        pasteComponents.add(arc);
        visitor = new PasteVisitor(petriNet, pasteComponents);

        doPaste();

        verify(petriNet).addArc(argThat(hasCopiedIdAndNameAndSourceCopied(arc)));
    }

    private Matcher<Arc<? extends Connectable, ? extends Connectable>> hasCopiedIdAndNameAndSourceCopied(
            Arc<? extends Connectable, ? extends Connectable> arc) {
        return new CopiedArc(arc, true, false);
    }

    @Test
    public void pastingArcTargetInSelected() {
        Place place = new Place("id", "name");
        Transition transition = new Transition("id", "name");
        pasteComponents.add(transition);

        Map<Token, String> weights = new HashMap<Token, String>();
        ArcStrategy<Place, Transition> arcStrategy = new BackwardsNormalStrategy(petriNet);
        Arc<Place, Transition> arc = new Arc<Place, Transition>(place, transition, weights, arcStrategy);
        pasteComponents.add(arc);
        visitor = new PasteVisitor(petriNet, pasteComponents);

        doPaste();

        verify(petriNet).addArc(argThat(hasCopiedIdAndNameAndTargetCopied(arc)));
    }

    private Matcher<Arc<? extends Connectable, ? extends Connectable>> hasCopiedIdAndNameAndTargetCopied(
            Arc<? extends Connectable, ? extends Connectable> arc) {
        return new CopiedArc(arc, false, true);
    }

    /**
     * Makes sure that the argument in matches is identical to the specified place
     * except name and id have "_copied" appended to them
     */
    private static class CopiedPlace extends ArgumentMatcher<Place> {

        /**
         * Place that should be copied
         */
        private final Place place;
        private final Point2D offset;

        private CopiedPlace(Place place) {
            this.place = place;
            this.offset = new Point2D.Double(0, 0);
        }

        public CopiedPlace(Place place, Point2D offset) {
            this.place = place;
            this.offset = offset;
        }

        @Override
        public boolean matches(Object argument) {
            Place otherPlace = (Place) argument;
            return (otherPlace.getId().equals(place.getId() + "_copied") && otherPlace.getName().equals(
                    place.getName() + "_copied") && otherPlace.getX() == (place.getX() + offset.getX()) &&
                    otherPlace.getY() == (place.getY() + offset.getY()) &&
                    otherPlace.getNameXOffset() == place.getNameXOffset() &&
                    otherPlace.getNameYOffset() == place.getNameYOffset() &&
                    otherPlace.getCapacity() == place.getCapacity() &&
                    otherPlace.getTokenCounts().equals(place.getTokenCounts()) &&
                    otherPlace.getMarkingXOffset() == place.getMarkingXOffset() &&
                    otherPlace.getMarkingYOffset() == place.getMarkingYOffset());
        }
    }

    /**
     * Makes sure that the argument in matches is identical to the specified transition
     * except name and id have "_copied" appended to them
     */
    private static class CopiedTransition extends ArgumentMatcher<Transition> {

        private final Point2D offset;
        private Transition transition;

        private CopiedTransition(Transition transition) {
            this.transition = transition;
            this.offset = new Point2D.Double(0, 0);
        }


        public CopiedTransition(Transition transition, Point2D offset) {
            this.transition = transition;
            this.offset = offset;
        }


        @Override
        public boolean matches(Object argument) {
            Transition otherTransition = (Transition) argument;
            return (otherTransition.getId().equals(transition.getId() + "_copied") && otherTransition.getName().equals(
                    transition.getName() + "_copied") && otherTransition.getX() == (transition.getX() + offset.getX())
                    &&
                    otherTransition.getY() == (transition.getY() + offset.getY()) &&
                    otherTransition.getNameXOffset() == transition.getNameXOffset()  &&
                    otherTransition.getNameYOffset() == transition.getNameYOffset() &&
                    otherTransition.getRateExpr().equals(transition.getRateExpr()) &&
                    otherTransition.isInfiniteServer() == transition.isInfiniteServer() &&
                    otherTransition.getAngle() == transition.getAngle() &&
                    otherTransition.isTimed() == transition.isTimed() &&
                    otherTransition.getPriority() == transition.getPriority());
        }
    }

    private static class CopiedArc extends ArgumentMatcher<Arc<? extends Connectable, ? extends Connectable>> {

        private final Arc<? extends Connectable, ? extends Connectable> arc;
        private final boolean sourceCopied;
        private final boolean targetCopied;

        public CopiedArc(Arc<? extends Connectable, ? extends Connectable> arc, boolean sourceCopied,
                         boolean targetCopied) {

            this.arc = arc;
            this.sourceCopied = sourceCopied;
            this.targetCopied = targetCopied;
        }

        @Override
        public boolean matches(Object argument) {
            Arc<? extends Connectable, ? extends Connectable> otherArc =
                    (Arc<? extends Connectable, ? extends Connectable>) argument;
            String sourceName = sourceCopied ? arc.getSource().getName() + "_copied" : arc.getSource().getName();
            String targetName = targetCopied ? arc.getTarget().getName() + "_copied" : arc.getTarget().getName();

            return (otherArc.getSource().getName().equals(sourceName) && otherArc.getTarget().getName().equals(
                    targetName));


        }
    }

}
