package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Detects inferred inject completions from replay-derived larva birth bursts.
 */
public class InferredInjectDetector {

    /** Required larva births for one inferred inject completion. */
    public static final int REQUIRED_BIRTH_COUNT = 3;

    /** Maximum burst span in replay loops for one inferred inject completion. */
    public static final int MAX_BURST_SPAN_LOOPS = 8;

    /**
     * One inferred inject-completion evidence point.
     */
    public static class BurstEvidence {

        /** First birth loop participating in the burst. */
        private final int firstBirthLoop;

        /** Loop of the third qualifying larva birth that closes the burst. */
        private final int evidenceLoop;

        /** Number of births participating in the detected burst. */
        private final int birthCount;

        /** Span in replay loops between the first and last qualifying births. */
        private final int burstSpanLoops;

        /**
         * Creates a new burst evidence record.
         *
         * @param firstBirthLoop first birth loop participating in the burst
         * @param evidenceLoop loop of the qualifying burst completion
         * @param birthCount number of births participating in the detected burst
         * @param burstSpanLoops span in replay loops between first and last qualifying births
         */
        public BurstEvidence( final int firstBirthLoop, final int evidenceLoop, final int birthCount, final int burstSpanLoops ) {
            this.firstBirthLoop = firstBirthLoop;
            this.evidenceLoop = evidenceLoop;
            this.birthCount = birthCount;
            this.burstSpanLoops = burstSpanLoops;
        }

        public int getFirstBirthLoop() {
            return firstBirthLoop;
        }

        public int getEvidenceLoop() {
            return evidenceLoop;
        }

        public int getBirthCount() {
            return birthCount;
        }

        public int getBurstSpanLoops() {
            return burstSpanLoops;
        }

    }

    /**
     * Detects inferred inject-completion evidence from assigned larva birth loops.
     *
     * <p>One inferred inject completion is recorded when 3 larva births happen for the
     * same hatchery within 8 replay loops. Overlapping births inside the same local burst
     * cluster are collapsed so a single inject does not create duplicate evidence points.</p>
     *
     * @param assignedLarvaBirthLoopList assigned larva birth loops for one hatchery
     * @return chronological inferred inject evidence list
     */
    public List< BurstEvidence > detect( final List< Integer > assignedLarvaBirthLoopList ) {
        if ( assignedLarvaBirthLoopList == null || assignedLarvaBirthLoopList.isEmpty() )
            return Collections.emptyList();

        final List< Integer > sortedLoopList = new ArrayList<>( assignedLarvaBirthLoopList.size() );
        for ( final Integer loop_ : assignedLarvaBirthLoopList ) {
            if ( loop_ == null || loop_.intValue() < 0 )
                continue;
            sortedLoopList.add( loop_ );
        }

        if ( sortedLoopList.size() < REQUIRED_BIRTH_COUNT )
            return Collections.emptyList();

        Collections.sort( sortedLoopList, new Comparator< Integer >() {
            @Override
            public int compare( final Integer left, final Integer right ) {
                return left.intValue() < right.intValue() ? -1 : left.intValue() == right.intValue() ? 0 : 1;
            }
        } );

        final List< BurstEvidence > evidenceList = new ArrayList<>();
        int index = 0;
        while ( index <= sortedLoopList.size() - REQUIRED_BIRTH_COUNT ) {
            final int firstLoop = sortedLoopList.get( index ).intValue();
            final int qualifyingLoop = sortedLoopList.get( index + REQUIRED_BIRTH_COUNT - 1 ).intValue();
            final int spanLoops = qualifyingLoop - firstLoop;
            if ( spanLoops > MAX_BURST_SPAN_LOOPS ) {
                index++;
                continue;
            }

            int clusterBirthCount = REQUIRED_BIRTH_COUNT;
            int nextIndex = index + REQUIRED_BIRTH_COUNT;
            while ( nextIndex < sortedLoopList.size() && sortedLoopList.get( nextIndex ).intValue() - firstLoop <= MAX_BURST_SPAN_LOOPS ) {
                clusterBirthCount++;
                nextIndex++;
            }

            evidenceList.add( new BurstEvidence( firstLoop, qualifyingLoop, clusterBirthCount, spanLoops ) );
            index = nextIndex;
        }

        return evidenceList;
    }

}