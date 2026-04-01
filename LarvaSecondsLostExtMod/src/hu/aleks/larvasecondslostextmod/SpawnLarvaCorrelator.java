package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.sc2.balancedata.model.IAbility;
import hu.scelightapi.sc2.rep.model.IEvent;
import hu.scelightapi.sc2.rep.model.gameevents.IGameEvents;
import hu.scelightapi.sc2.rep.model.gameevents.cmd.ICmdEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects Spawn Larva command targets so Epic 6 can use them as a strong correlation signal.
 */
public class SpawnLarvaCorrelator {

    /**
     * Collects Spawn Larva command loops grouped by target hatchery tag.
     *
     * @param gameEventArray replay game events
     * @return spawn larva loops grouped by hatchery tag
     */
    public Map< Integer, List< Integer > > collectByTargetTag( final IEvent[] gameEventArray ) {
        final Map< Integer, List< Integer > > injectLoopsByTag = new HashMap<>();

        if ( gameEventArray == null )
            return injectLoopsByTag;

        for ( final IEvent event : gameEventArray ) {
            if ( event == null || event.getId() != IGameEvents.ID_CMD || !( event instanceof ICmdEvent ) )
                continue;

            final ICmdEvent cmdEvent = (ICmdEvent) event;
            if ( cmdEvent.getCommand() == null || !IAbility.ID_SPAWN_LARVA.equals( cmdEvent.getCommand().getAbilId() ) || cmdEvent.getTargetUnit() == null
                    || cmdEvent.getTargetUnit().getTag() == null )
                continue;

            final Integer hatcheryTag = cmdEvent.getTargetUnit().getTag();
            List< Integer > injectLoops = injectLoopsByTag.get( hatcheryTag );
            if ( injectLoops == null ) {
                injectLoops = new ArrayList<>();
                injectLoopsByTag.put( hatcheryTag, injectLoops );
            }

            injectLoops.add( event.getLoop() );
        }

        return injectLoopsByTag;
    }

}