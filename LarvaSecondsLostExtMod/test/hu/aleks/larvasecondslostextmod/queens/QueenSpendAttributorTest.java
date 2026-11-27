package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** Attribution of spends to queens (E19-S07, S09). */
public class QueenSpendAttributorTest {

    private static final int MAIN = 100;
    private static final int NATURAL = 200;
    private static final int END = 30000;

    private static QueenSpendAttributor.Result run( final MissedInjectInput input ) {
        return QueenSpendAttributor.attribute( input, new QueenSpendAttributor.Settings( 12.0d, END, 886, 32 ) );
    }

    private static MissedInjectInput twoHatches( final java.util.List< int[] > mainWindows ) {
        return new MissedInjectInput().addHatch( new MissedInjectInput.Hatch( MAIN, 100, 100, 0, -1, mainWindows ) )
                .addHatch( new MissedInjectInput.Hatch( NATURAL, 130, 100, 0, -1, Collections.< int[] >emptyList() ) );
    }

    private static QueenSpendAttributor.TrackedQueen queen( final QueenSpendAttributor.Result result, final int tag ) {
        for ( final QueenSpendAttributor.TrackedQueen queen : result.getQueens() )
            if ( queen.getQueen().getTag() == tag )
                return queen;
        throw new AssertionError( "no queen " + tag );
    }

    private static String labels( final QueenSpendAttributor.TrackedQueen queen ) {
        final StringBuilder b = new StringBuilder();
        for ( final QueenPresence.Point point : queen.getPoints() )
            b.append( point.getLabel() ).append( '@' ).append( point.getLoop() ).append( ' ' );
        return b.toString().trim();
    }

    @Test
    public void injectWithoutCommandGoesToTheNearestQueen() {
        final MissedInjectInput input = twoHatches( Arrays.asList( new int[] { 2000, 2642 } ) )
                .addQueen( new MissedInjectInput.Queen( 1, "a", 1000, 100, 103, -1, 0, 0 ) )
                .addQueen( new MissedInjectInput.Queen( 2, "b", 1000, 130, 103, -1, 0, 0 ) );
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( "born@1000 inject@2000", labels( queen( result, 1 ) ) );
        assertEquals( "born@1000", labels( queen( result, 2 ) ) );
        assertEquals( 1, result.getAmbiguous() );
        assertEquals( "[1, 0, 1]", Arrays.toString( result.countersOf( MAIN ) ) );
    }

    @Test
    public void commandSelectionWinsOverDistanceAndQueuedInjectUsesTheCommandLoop() {
        final MissedInjectInput input = twoHatches( Arrays.asList( new int[] { 2000, 2642 }, new int[] { 2642, 3284 } ) )
                .addQueen( new MissedInjectInput.Queen( 1, "a", 0, 100, 103, -1, 0, 0 ) )
                .addQueen( new MissedInjectInput.Queen( 2, "b", 0, 130, 103, -1, 0, 0 ) )
                .addInjectCommand( new MissedInjectInput.InjectCommand( 1990, MAIN, new int[] { 2 } ) )
                .addInjectCommand( new MissedInjectInput.InjectCommand( 2100, MAIN, new int[] { 2 } ) ); // queued: main still injected
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( "born@0 inject@2000 inject@2100", labels( queen( result, 2 ) ) );
        assertEquals( "[2, 2, 0]", Arrays.toString( result.countersOf( MAIN ) ) );
    }

    @Test
    public void spendGoesToTheNextCandidateWhenTheNearestCannotAffordIt() {
        final MissedInjectInput input = twoHatches( Collections.< int[] >emptyList() )
                .addQueen( new MissedInjectInput.Queen( 1, "a", 1000, 100, 103, -1, 0, 0 ) )
                .addQueen( new MissedInjectInput.Queen( 2, "b", 1000, 130, 103, -1, 0, 0 ) )
                .addTumor( new QueenSpend( 1100, QueenSpend.Kind.TUMOR, -1, 101, 104, null ) )
                .addTumor( new QueenSpend( 1200, QueenSpend.Kind.TUMOR, -1, 101, 105, null ) );
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( "born@1000 tumor@1100", labels( queen( result, 1 ) ) );
        assertEquals( "born@1000 tumor@1200", labels( queen( result, 2 ) ) );
        assertEquals( 0, queen( result, 1 ).getEnergy().getConflictCount() );
    }

    @Test
    public void transfusionNobodyCanAffordIsDropped() {
        final MissedInjectInput input = twoHatches( Collections.< int[] >emptyList() )
                .addQueen( new MissedInjectInput.Queen( 1, "a", 1000, 100, 103, -1, 0, 0 ) )
                .addTransfusion( new QueenSpend( 1100, QueenSpend.Kind.TRANSFUSE, -1, Double.NaN, Double.NaN, new int[] { 1 } ) );
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( 1, result.getDroppedTransfusions() );
        assertEquals( 0, queen( result, 1 ).getEnergy().getSpendCount() );
    }

    @Test
    public void pointsGetTheNearestStandingHatcheryOrAway() {
        final MissedInjectInput input = new MissedInjectInput()
                .addHatch( new MissedInjectInput.Hatch( MAIN, 100, 100, 0, -1, Collections.< int[] >emptyList() ) )
                .addHatch( new MissedInjectInput.Hatch( NATURAL, 108, 100, 5000, -1, Collections.< int[] >emptyList() ) )
                .addQueen( new MissedInjectInput.Queen( 1, "a", 1000, 106, 100, 9000, 150, 150 ) )
                .addPosition( new MissedInjectInput.Position( 1, 6000, 106, 100 ) );
        final QueenSpendAttributor.Result result = run( input );
        final java.util.List< QueenPresence.Point > points = queen( result, 1 ).getPoints();
        assertEquals( MAIN, points.get( 0 ).getHomeHatchTag() ); // the natural was not done yet
        assertEquals( NATURAL, points.get( 1 ).getHomeHatchTag() );
        assertEquals( QueenPresence.AWAY, points.get( 2 ).getHomeHatchTag() );
        assertEquals( "died", points.get( 2 ).getLabel() );
    }

    /** A queen that just cast at the natural cannot cast at the main 100 loops later; the farther queen can have walked there. */
    @Test
    public void aQueenWhoCannotHaveWalkedThereIsNotChosen() {
        final MissedInjectInput input = twoHatches( Arrays.asList( new int[] { 1100, 1742 } ) );
        input.hatches.set( 1, new MissedInjectInput.Hatch( NATURAL, 130, 100, 0, -1, Arrays.asList( new int[] { 1000, 1642 } ) ) );
        input.addQueen( new MissedInjectInput.Queen( 1, "far", 0, 300, 100, -1, 0, 0 ) );
        input.addQueen( new MissedInjectInput.Queen( 2, "natural", 0, 130, 100, -1, 0, 0 ) );
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( "born@0 inject@1000", labels( queen( result, 2 ) ) );
        assertEquals( "born@0 inject@1100", labels( queen( result, 1 ) ) );
    }

    /** Scenario 7:15: queens standing together after a group move; the one whose next own order is at the natural cast there. */
    @Test
    public void queensStandingTogetherAreToldApartByTheirNextOrder() {
        final MissedInjectInput input = twoHatches( Collections.< int[] >emptyList() );
        input.hatches.set( 1, new MissedInjectInput.Hatch( NATURAL, 130, 100, 0, -1, Arrays.asList( new int[] { 1000, 1642 } ) ) );
        input.addQueen( new MissedInjectInput.Queen( 1, "a", 0, 100, 103, -1, 0, 0 ) );
        input.addQueen( new MissedInjectInput.Queen( 2, "b", 0, 100, 103, -1, 0, 0 ) );
        input.addMove( new MissedInjectInput.Move( 3000, new int[] { 2 }, 131, 102, false, false, false, "move" ) );
        final QueenSpendAttributor.Result result = run( input );
        assertEquals( "born@0 inject@1000", labels( queen( result, 2 ) ) );
        assertEquals( "born@0", labels( queen( result, 1 ) ) );
    }
}
