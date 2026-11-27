package hu.aleks.larvasecondslostextmod.queens;

import hu.scelightapi.sc2.balancedata.model.ICommand;
import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.gameevents.IControlGroupUpdateEvent;
import hu.scelightapi.sc2.rep.model.gameevents.IGameEvents;
import hu.scelightapi.sc2.rep.model.gameevents.cmd.ICmdEvent;
import hu.scelightapi.sc2.rep.model.gameevents.selectiondelta.IDelta;
import hu.scelightapi.sc2.rep.model.gameevents.selectiondelta.ISelectionDeltaEvent;
import hu.scelightapi.util.type.IBitArray;
import hu.scelightapibase.util.IPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every command of every user, with the user's selection at that moment (E19-S11). Reads game events with {@link SelectionModel}
 * instead of Scelight's selection tracker.
 *
 * <p>Current replays send a repeated command (the same ability on another target, for example shift-injecting four hatcheries) as
 * {@code CmdUpdateTargetUnit} or {@code CmdUpdateTargetPoint} after the first {@code Cmd}; those carry only the new target and
 * reuse the ability and flags of the user's last {@code Cmd}. They are commands too.</p>
 */
public final class ReplayCommandStream {

    /** {@code SCmdFlags} bit of a queued (shift) command. */
    static final int FLAG_QUEUED = 0x2;

    static final String UPDATE_TARGET_POINT = "CmdUpdateTargetPoint";
    static final String UPDATE_TARGET_UNIT = "CmdUpdateTargetUnit";

    /** One command with its selection. */
    public static final class Command {

        final int loop;
        final int userId;
        final String abilityId;
        final String commandId;
        final int flags;
        final Integer targetTag;
        final double x;
        final double y;
        final int[] selection;

        Command( final int loop, final int userId, final String abilityId, final String commandId, final int flags, final Integer targetTag,
                final double x, final double y, final int[] selection ) {
            this.loop = loop;
            this.userId = userId;
            this.abilityId = abilityId;
            this.commandId = commandId;
            this.flags = flags;
            this.targetTag = targetTag;
            this.x = x;
            this.y = y;
            this.selection = selection;
        }

        public int getLoop() {
            return loop;
        }

        public int getUserId() {
            return userId;
        }

        public String getAbilityId() {
            return abilityId;
        }

        public String getCommandId() {
            return commandId;
        }

        /** @return the target unit tag, or null */
        public Integer getTargetTag() {
            return targetTag;
        }

        /** @return tracker-scale target x, or NaN without a target */
        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean hasTarget() {
            return !Double.isNaN( x ) && !Double.isNaN( y );
        }

        public boolean isQueued() {
            return ( flags & FLAG_QUEUED ) != 0;
        }

        /** @return unit tags of the active selection when the command was given */
        public int[] getSelection() {
            return selection.clone();
        }
    }

    /** Receives the commands in game order. */
    public interface Listener {
        void onCommand( Command command );
    }

    private final Map< Integer, SelectionModel > selectionByUserId = new HashMap<>();
    private final Map< Integer, Command > lastCommandByUserId = new HashMap<>();
    private final List< int[] > deaths;
    private int nextDeath;

    private ReplayCommandStream( final List< int[] > deaths ) {
        this.deaths = deaths;
    }

    /**
     * @param gameEvents game events in order
     * @param deaths {loop, unit tag} of every unit death in loop order (dead units leave control groups)
     * @param listener receives the commands
     */
    public static void read( final IEvent[] gameEvents, final List< int[] > deaths, final Listener listener ) {
        if ( gameEvents == null )
            return;
        final ReplayCommandStream stream = new ReplayCommandStream( deaths );
        for ( final IEvent event : gameEvents ) {
            if ( event == null || event.getUserId() < 0 )
                continue;
            stream.applyDeathsBefore( event.getLoop() );
            final Command command = stream.onEvent( event );
            if ( command != null )
                listener.onCommand( command );
        }
    }

    private void applyDeathsBefore( final int loop ) {
        while ( deaths != null && nextDeath < deaths.size() && deaths.get( nextDeath )[ 0 ] < loop ) {
            final int tag = deaths.get( nextDeath++ )[ 1 ];
            for ( final SelectionModel selection : selectionByUserId.values() )
                selection.removeFromControlGroups( tag );
        }
    }

    private Command onEvent( final IEvent event ) {
        final SelectionModel selection = selectionOf( event.getUserId() );
        if ( event.getId() == IGameEvents.ID_SELECTION_DELTA && event instanceof ISelectionDeltaEvent ) {
            onSelectionDelta( (ISelectionDeltaEvent) event, selection );
            return null;
        }
        if ( event.getId() == IGameEvents.ID_CONTROL_GROUP_UPDATE && event instanceof IControlGroupUpdateEvent ) {
            final IControlGroupUpdateEvent update = (IControlGroupUpdateEvent) event;
            if ( update.getGroupIndex() != null && update.getGroupUpdate() != null )
                selection.applyControlGroupUpdate( update.getGroupIndex().intValue(), update.getGroupUpdate().intValue(), removalOf( update.getMask() ) );
            return null;
        }
        if ( event.getId() == IGameEvents.ID_CMD && event instanceof ICmdEvent )
            return remember( fromCmd( (ICmdEvent) event, selection ) );
        if ( UPDATE_TARGET_POINT.equals( event.getName() ) || UPDATE_TARGET_UNIT.equals( event.getName() ) )
            return remember( fromTargetUpdate( event, selection ) );
        return null;
    }

    private Command remember( final Command command ) {
        if ( command != null )
            lastCommandByUserId.put( Integer.valueOf( command.userId ), command );
        return command;
    }

    private static void onSelectionDelta( final ISelectionDeltaEvent event, final SelectionModel selection ) {
        final IDelta delta = event.getDelta();
        if ( delta == null || event.getControlGroupId() == null )
            return;
        final Integer[] added = delta.getAddUnitTags();
        final int[] tags = new int[ added == null ? 0 : added.length ];
        for ( int i = 0; i < tags.length; i++ )
            tags[ i ] = added[ i ] == null ? 0 : added[ i ].intValue();
        selection.applyDelta( event.getControlGroupId().intValue(), removalOf( delta.getRemoveMask() ), tags );
    }

    private static Command fromCmd( final ICmdEvent event, final SelectionModel selection ) {
        final ICommand ability = event.getCommand();
        final String abilityId = ability == null ? null : ability.getAbilId();
        final String commandId = ability == null ? null : ability.getId();
        final int flags = event.getCmdFlags() == null ? 0 : event.getCmdFlags().intValue();
        Integer targetTag = null;
        double x = Double.NaN;
        double y = Double.NaN;
        if ( event.getTargetUnit() != null ) {
            targetTag = event.getTargetUnit().getTag();
            final double[] xy = QueenEvidenceDecoding.snapshotPointOf( event.getTargetUnit().getSnapshotPoint() );
            x = xy[ 0 ];
            y = xy[ 1 ];
        } else if ( event.getTargetPoint() != null ) {
            x = QueenEvidenceDecoding.toTrackerScale( event.getTargetPoint().getX() );
            y = QueenEvidenceDecoding.toTrackerScale( event.getTargetPoint().getY() );
        }
        return new Command( event.getLoop(), event.getUserId(), abilityId, commandId, flags, targetTag, x, y, selection.activeTags() );
    }

    @SuppressWarnings( "unchecked" )
    private Command fromTargetUpdate( final IEvent event, final SelectionModel selection ) {
        final Command last = lastCommandByUserId.get( Integer.valueOf( event.getUserId() ) );
        final Object target = event.get( "target" );
        if ( last == null || !( target instanceof Map ) )
            return null;
        final Map< String, Object > fields = (Map< String, Object >) target;
        Integer targetTag = null;
        double[] xy;
        if ( UPDATE_TARGET_UNIT.equals( event.getName() ) ) {
            targetTag = fields.get( "tag" ) instanceof Integer ? (Integer) fields.get( "tag" ) : null;
            xy = QueenEvidenceDecoding.snapshotPointOf( fields.get( "snapshotPoint" ) instanceof Map ? (Map< String, Object >) fields.get( "snapshotPoint" )
                    : null );
        } else
            xy = new double[] { QueenEvidenceDecoding.toTrackerScale( fields.get( "x" ) ), QueenEvidenceDecoding.toTrackerScale( fields.get( "y" ) ) };
        return new Command( event.getLoop(), event.getUserId(), last.abilityId, last.commandId, last.flags, targetTag, xy[ 0 ], xy[ 1 ],
                selection.activeTags() );
    }

    /** Converts Scelight's remove mask ({@code IPair} of kind and value); unknown forms remove nothing. */
    @SuppressWarnings( "unchecked" )
    static SelectionModel.Removal removalOf( final Object mask ) {
        if ( !( mask instanceof IPair ) )
            return SelectionModel.Removal.none();
        final IPair< String, Object > pair = (IPair< String, Object >) mask;
        final String kind = pair.getValue1();
        final Object value = pair.getValue2();
        if ( "Mask".equals( kind ) && value instanceof IBitArray ) {
            final IBitArray bits = (IBitArray) value;
            return SelectionModel.Removal.mask( SelectionMaskDecoding.removedFlags( bits.getCount(), bits.getArray() ) );
        }
        if ( "OneIndices".equals( kind ) && value instanceof Integer[] )
            return SelectionModel.Removal.oneIndices( toInts( (Integer[]) value ) );
        if ( "ZeroIndices".equals( kind ) && value instanceof Integer[] )
            return SelectionModel.Removal.zeroIndices( toInts( (Integer[]) value ) );
        return SelectionModel.Removal.none();
    }

    private static int[] toInts( final Integer[] values ) {
        final int[] ints = new int[ values.length ];
        for ( int i = 0; i < ints.length; i++ )
            ints[ i ] = values[ i ] == null ? -1 : values[ i ].intValue();
        return ints;
    }

    private SelectionModel selectionOf( final int userId ) {
        SelectionModel selection = selectionByUserId.get( Integer.valueOf( userId ) );
        if ( selection == null ) {
            selection = new SelectionModel();
            selectionByUserId.put( Integer.valueOf( userId ), selection );
        }
        return selection;
    }
}
