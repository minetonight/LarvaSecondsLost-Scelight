package hu.aleks.larvasecondslostextmod.queens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Pure decoding rules for queen evidence in replay events (E19-S07), kept apart from the Scelight API so they can be unit tested.
 * See {@code knowhow/replay-tracker-facts.md}.
 */
public final class QueenEvidenceDecoding {

    /** Command target points have 12 fraction bits relative to tracker x/y: tracker = raw / 4096 (not Scelight's getXFloat()). */
    static final double TARGET_POINT_SCALE = 4096.0d;

    /** A tumor matches a tumor command at most this many loops after it (60 s on the game clock). */
    public static final int TUMOR_COMMAND_MAX_AGE_LOOPS = hu.aleks.larvasecondslostextmod.LarvaGameClock.secondsToLoops( 60 );

    /** A tumor matches a tumor command whose target is at most this many cells away on each axis. */
    public static final double TUMOR_COMMAND_MAX_OFFSET = 1.5d;

    /** A queen creep tumor command with its target and the queens of the selection. */
    public static final class TumorCommand {

        final int loop;
        final double x;
        final double y;
        final int[] queenTags;

        public TumorCommand( final int loop, final double x, final double y, final int[] queenTags ) {
            this.loop = loop;
            this.x = x;
            this.y = y;
            this.queenTags = queenTags == null ? new int[ 0 ] : Arrays.copyOf( queenTags, queenTags.length );
        }

        public int[] getQueenTags() {
            return Arrays.copyOf( queenTags, queenTags.length );
        }
    }

    private QueenEvidenceDecoding() {
    }

    /** @return the unit index part of a unit tag (the part {@code UnitPositions} uses) */
    public static int unitIndexOf( final int tag ) {
        return tag >>> 18;
    }

    /** @return the tag from the tracker's index and recycle fields */
    public static int tagOf( final int index, final int recycle ) {
        return index << 18 | recycle;
    }

    /**
     * Decodes a tracker {@code UnitPositions} event: {@code items} holds triples (unit index delta, x, y), the first delta is added to
     * {@code firstUnitIndex}, later deltas to the previous index.
     *
     * @param firstUnitIndex the event's first unit index
     * @param items the event's items
     * @return {@code [unit index, x, y]} per unit
     */
    public static List< int[] > decodeUnitPositions( final int firstUnitIndex, final Object[] items ) {
        final List< int[] > positions = new ArrayList<>();
        if ( items == null )
            return positions;
        int index = firstUnitIndex;
        for ( int i = 0; i + 2 < items.length; i += 3 ) {
            if ( !( items[ i ] instanceof Number ) || !( items[ i + 1 ] instanceof Number ) || !( items[ i + 2 ] instanceof Number ) )
                break;
            index += ( (Number) items[ i ] ).intValue();
            positions.add( new int[] { index, ( (Number) items[ i + 1 ] ).intValue(), ( (Number) items[ i + 2 ] ).intValue() } );
        }
        return positions;
    }

    /** @return a raw command target coordinate in tracker scale, or NaN */
    public static double toTrackerScale( final Object raw ) {
        return raw instanceof Number ? ( (Number) raw ).doubleValue() / TARGET_POINT_SCALE : Double.NaN;
    }

    /** @return tracker-scale {x, y} of a target unit snapshot point map, or NaNs */
    public static double[] snapshotPointOf( final Map< String, Object > snapshotPoint ) {
        if ( snapshotPoint == null )
            return new double[] { Double.NaN, Double.NaN };
        return new double[] { toTrackerScale( snapshotPoint.get( "x" ) ), toTrackerScale( snapshotPoint.get( "y" ) ) };
    }

    /**
     * Finds the tumor command that placed a tumor: the latest command at most {@link #TUMOR_COMMAND_MAX_AGE_LOOPS} before it whose
     * target is within {@link #TUMOR_COMMAND_MAX_OFFSET} cells on each axis.
     *
     * @param commands tumor commands of the player
     * @param tumorLoop loop of the tumor's {@code UnitInit}
     * @param x tumor x
     * @param y tumor y
     * @return the command, or null
     */
    public static TumorCommand matchTumor( final List< TumorCommand > commands, final int tumorLoop, final double x, final double y ) {
        TumorCommand best = null;
        for ( final TumorCommand command : commands ) {
            if ( command.loop > tumorLoop || tumorLoop - command.loop > TUMOR_COMMAND_MAX_AGE_LOOPS )
                continue;
            if ( Math.abs( command.x - x ) > TUMOR_COMMAND_MAX_OFFSET || Math.abs( command.y - y ) > TUMOR_COMMAND_MAX_OFFSET )
                continue;
            if ( best == null || command.loop > best.loop )
                best = command;
        }
        return best;
    }

    /** Lower-case letters and digits only, for tolerant ability and unit name checks. */
    public static String normalize( final String value ) {
        if ( value == null )
            return "";
        final StringBuilder builder = new StringBuilder( value.length() );
        for ( int i = 0; i < value.length(); i++ ) {
            final char c = value.charAt( i );
            if ( Character.isLetterOrDigit( c ) )
                builder.append( Character.toLowerCase( c ) );
        }
        return builder.toString();
    }

    /** @return true for a Spawn Larva command (ability or command id) */
    public static boolean isInjectCommand( final String abilityId, final String commandId ) {
        final String text = normalize( abilityId ) + "/" + normalize( commandId );
        return text.contains( "spawnlarva" ) || text.contains( "injectlarva" );
    }

    /** @return true for a queen creep tumor command (not a tumor spreading itself, not training a queen) */
    public static boolean isQueenTumorCommand( final String abilityId, final String commandId ) {
        return ( normalize( abilityId ) + "/" + normalize( commandId ) ).contains( "creeptumorqueen" );
    }

    /** @return true for a stop or hold position command (no target) */
    public static boolean isStopCommand( final String abilityId, final String commandId ) {
        final String text = normalize( abilityId ) + "/" + normalize( commandId );
        return text.contains( "stop" ) || text.contains( "hold" );
    }

    /** @return true for a transfusion command */
    public static boolean isTransfusionCommand( final String abilityId, final String commandId ) {
        return ( normalize( abilityId ) + "/" + normalize( commandId ) ).contains( "transfusion" );
    }
}
