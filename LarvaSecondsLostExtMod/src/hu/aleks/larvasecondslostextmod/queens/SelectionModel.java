package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One player's selection and control groups, rebuilt from selection deltas and control group updates (E19-S11). Replaces Scelight's
 * {@code SelectionTracker} for the queen model, which loses most queen selections in current replays:
 * <ul>
 * <li>Scelight ignores control group updates 3 (clear), 4 (set and steal) and 5 (append and steal). Steal is how many players bind
 * hotkeys today, so their queen hotkey stays empty.</li>
 * <li>Scelight appends added units at the end. SC2 keeps a selection sorted by unit tag, and removals are by index into that order;
 * with the wrong order later removals hit the wrong units.</li>
 * <li>Masks longer than 8 bits need {@link SelectionMaskDecoding}.</li>
 * <li>A dead unit leaves control groups without a delta, so groups drop it ({@link #removeFromControlGroups(int)}). The active
 * selection gets explicit deltas for deaths.</li>
 * </ul>
 * Tags are compared unsigned. Pure, so it can be unit tested.
 */
public final class SelectionModel {

    /** Group id of the active selection in selection deltas. */
    public static final int ACTIVE = 10;

    /** Control group update types. */
    public static final int UPDATE_SET = 0;
    public static final int UPDATE_APPEND = 1;
    public static final int UPDATE_RECALL = 2;
    public static final int UPDATE_CLEAR = 3;
    public static final int UPDATE_SET_AND_STEAL = 4;
    public static final int UPDATE_APPEND_AND_STEAL = 5;

    /** Which units a delta or a recall removes. */
    public static final class Removal {

        static final Removal NONE = new Removal( null, null, false );

        private final boolean[] flags;
        private final int[] indices;
        private final boolean keepIndices;

        private Removal( final boolean[] flags, final int[] indices, final boolean keepIndices ) {
            this.flags = flags;
            this.indices = indices;
            this.keepIndices = keepIndices;
        }

        /** @return a removal of nothing */
        public static Removal none() {
            return NONE;
        }

        /** @param flags true at each removed index */
        public static Removal mask( final boolean[] flags ) {
            return new Removal( flags == null ? new boolean[ 0 ] : flags.clone(), null, false );
        }

        /** @param indices removed indices */
        public static Removal oneIndices( final int[] indices ) {
            return new Removal( null, indices == null ? new int[ 0 ] : indices.clone(), false );
        }

        /** @param indices kept indices, all others are removed */
        public static Removal zeroIndices( final int[] indices ) {
            return new Removal( null, indices == null ? new int[ 0 ] : indices.clone(), true );
        }

        /** Removes from the list in place; indices beyond the list are ignored. */
        void applyTo( final List< Integer > units ) {
            if ( flags == null && indices == null )
                return;
            final Set< Integer > marked = new HashSet<>();
            if ( flags != null ) {
                for ( int i = 0; i < flags.length && i < units.size(); i++ )
                    if ( flags[ i ] )
                        marked.add( Integer.valueOf( i ) );
            } else
                for ( final int index : indices )
                    marked.add( Integer.valueOf( index ) );
            final List< Integer > kept = new ArrayList<>();
            for ( int i = 0; i < units.size(); i++ )
                if ( marked.contains( Integer.valueOf( i ) ) == keepIndices )
                    kept.add( units.get( i ) );
            units.clear();
            units.addAll( kept );
        }
    }

    private static final Comparator< Integer > UNSIGNED_ORDER = new Comparator< Integer >() {
        @Override
        public int compare( final Integer a, final Integer b ) {
            final int x = a.intValue() ^ Integer.MIN_VALUE;
            final int y = b.intValue() ^ Integer.MIN_VALUE;
            return x < y ? -1 : x == y ? 0 : 1;
        }
    };

    private final List< List< Integer > > groups = new ArrayList<>();

    public SelectionModel() {
        for ( int i = 0; i <= ACTIVE; i++ )
            groups.add( new ArrayList< Integer >() );
    }

    /**
     * @param groupId control group 0 to 9, or {@link #ACTIVE}
     * @param removal units removed first
     * @param addTags units added
     */
    public void applyDelta( final int groupId, final Removal removal, final int[] addTags ) {
        final List< Integer > units = group( groupId );
        if ( units == null )
            return;
        ( removal == null ? Removal.NONE : removal ).applyTo( units );
        if ( addTags != null )
            for ( final int tag : addTags )
                if ( !units.contains( Integer.valueOf( tag ) ) )
                    units.add( Integer.valueOf( tag ) );
        Collections.sort( units, UNSIGNED_ORDER );
    }

    /**
     * @param groupIndex control group 0 to 9
     * @param updateType one of the {@code UPDATE_} values
     * @param removal mask of the update, applied as Scelight does: to the group after set or before append, to the selection after
     *            recall
     */
    public void applyControlGroupUpdate( final int groupIndex, final int updateType, final Removal removal ) {
        final List< Integer > group = groupIndex >= 0 && groupIndex < ACTIVE ? groups.get( groupIndex ) : null;
        if ( group == null )
            return;
        final List< Integer > active = groups.get( ACTIVE );
        final Removal mask = removal == null ? Removal.NONE : removal;
        switch ( updateType ) {
            case UPDATE_SET_AND_STEAL :
                stealFromOtherGroups( groupIndex, active );
                //$FALL-THROUGH$
            case UPDATE_SET :
                group.clear();
                group.addAll( active );
                mask.applyTo( group );
                break;
            case UPDATE_APPEND_AND_STEAL :
                stealFromOtherGroups( groupIndex, active );
                //$FALL-THROUGH$
            case UPDATE_APPEND :
                mask.applyTo( group );
                for ( final Integer tag : active )
                    if ( !group.contains( tag ) )
                        group.add( tag );
                Collections.sort( group, UNSIGNED_ORDER );
                break;
            case UPDATE_RECALL :
                active.clear();
                active.addAll( group );
                mask.applyTo( active );
                break;
            case UPDATE_CLEAR :
                group.clear();
                break;
            default :
                break;
        }
    }

    /** A dead unit leaves every control group (the active selection gets its own delta). */
    public void removeFromControlGroups( final int tag ) {
        for ( int i = 0; i < ACTIVE; i++ )
            groups.get( i ).remove( Integer.valueOf( tag ) );
    }

    /** @return tags of the active selection, in selection order */
    public int[] activeTags() {
        return toArray( groups.get( ACTIVE ) );
    }

    /** @return tags of a control group, in order */
    public int[] groupTags( final int groupIndex ) {
        final List< Integer > units = group( groupIndex );
        return units == null ? new int[ 0 ] : toArray( units );
    }

    private void stealFromOtherGroups( final int groupIndex, final List< Integer > active ) {
        for ( int i = 0; i < ACTIVE; i++ )
            if ( i != groupIndex )
                groups.get( i ).removeAll( active );
    }

    private List< Integer > group( final int groupId ) {
        return groupId >= 0 && groupId <= ACTIVE ? groups.get( groupId ) : null;
    }

    private static int[] toArray( final List< Integer > units ) {
        final int[] tags = new int[ units.size() ];
        for ( int i = 0; i < tags.length; i++ )
            tags[ i ] = units.get( i ).intValue();
        return tags;
    }
}
