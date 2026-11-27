package hu.aleks.larvasecondslostextmod.queens;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** Selection and control groups rebuilt from game events (E19-S11). */
public class SelectionModelTest {

    private static final int QUEEN_A = 73400321;
    private static final int QUEEN_B = 73138177;
    private static final int QUEEN_C = 91226113;
    private static final int QUEEN_D = 102760449;

    private static void select( final SelectionModel model, final int... tags ) {
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.zeroIndices( new int[ 0 ] ), tags );
    }

    @Test
    public void selectionIsSortedByUnsignedTag() {
        final SelectionModel model = new SelectionModel();
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.none(), new int[] { 9, -5, 3 } );
        assertArrayEquals( new int[] { 3, 9, -5 }, model.activeTags() );
    }

    /** Larva morphing to eggs keep their tag; removal indices refer to the sorted order. */
    @Test
    public void removalsUseTheSortedOrder() {
        final SelectionModel model = new SelectionModel();
        select( model, 40, 10, 30, 20 );
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.mask( new boolean[] { false, true, true } ), new int[] { 20, 30 } );
        assertArrayEquals( new int[] { 10, 20, 30, 40 }, model.activeTags() );
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.oneIndices( new int[] { 0, 3 } ), new int[ 0 ] );
        assertArrayEquals( new int[] { 20, 30 }, model.activeTags() );
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.zeroIndices( new int[] { 1 } ), new int[ 0 ] );
        assertArrayEquals( new int[] { 30 }, model.activeTags() );
        model.applyDelta( SelectionModel.ACTIVE, SelectionModel.Removal.oneIndices( new int[] { 7 } ), new int[ 0 ] );
        assertArrayEquals( new int[] { 30 }, model.activeTags() );
    }

    @Test
    public void setAppendRecallAndClear() {
        final SelectionModel model = new SelectionModel();
        select( model, 1, 2 );
        model.applyControlGroupUpdate( 3, SelectionModel.UPDATE_SET, null );
        select( model, 5 );
        model.applyControlGroupUpdate( 3, SelectionModel.UPDATE_APPEND, null );
        select( model, 9 );
        model.applyControlGroupUpdate( 3, SelectionModel.UPDATE_RECALL, null );
        assertArrayEquals( new int[] { 1, 2, 5 }, model.activeTags() );
        model.applyControlGroupUpdate( 3, SelectionModel.UPDATE_CLEAR, null );
        assertArrayEquals( new int[ 0 ], model.groupTags( 3 ) );
    }

    /**
     * The queen hotkey of the scenario replay: each queen is clicked and appended to group 6 with steal; the client then recalls
     * the other groups and restores the selection. Scelight ignores update 5, so the group stayed empty.
     */
    @Test
    public void appendAndStealBuildsTheQueenHotkey() {
        final SelectionModel model = new SelectionModel();
        select( model, QUEEN_A );
        model.applyControlGroupUpdate( 4, SelectionModel.UPDATE_SET, null );
        for ( final int queen : new int[] { QUEEN_A, QUEEN_B, QUEEN_C, QUEEN_D } ) {
            select( model, queen );
            model.applyControlGroupUpdate( 6, SelectionModel.UPDATE_APPEND_AND_STEAL, null );
            model.applyControlGroupUpdate( 0, SelectionModel.UPDATE_RECALL, null );
            select( model, queen );
        }
        assertArrayEquals( new int[ 0 ], model.groupTags( 4 ) );
        select( model, 123 );
        model.applyControlGroupUpdate( 6, SelectionModel.UPDATE_RECALL, null );
        assertArrayEquals( new int[] { QUEEN_B, QUEEN_A, QUEEN_C, QUEEN_D }, model.activeTags() );
    }

    @Test
    public void setAndStealMovesUnitsBetweenGroups() {
        final SelectionModel model = new SelectionModel();
        select( model, 1, 2, 3 );
        model.applyControlGroupUpdate( 1, SelectionModel.UPDATE_SET, null );
        select( model, 2 );
        model.applyControlGroupUpdate( 2, SelectionModel.UPDATE_SET_AND_STEAL, null );
        assertArrayEquals( new int[] { 1, 3 }, model.groupTags( 1 ) );
        assertArrayEquals( new int[] { 2 }, model.groupTags( 2 ) );
    }

    @Test
    public void deadUnitsLeaveControlGroupsButNotTheSelection() {
        final SelectionModel model = new SelectionModel();
        select( model, 1, 2 );
        model.applyControlGroupUpdate( 1, SelectionModel.UPDATE_SET, null );
        model.removeFromControlGroups( 2 );
        assertArrayEquals( new int[] { 1 }, model.groupTags( 1 ) );
        assertArrayEquals( new int[] { 1, 2 }, model.activeTags() );
    }
}
