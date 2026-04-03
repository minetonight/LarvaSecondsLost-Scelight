package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalized idle-inject opportunity diagnostics for one hatchery identity.
 */
public class HatcheryIdleInjectTimeline {

    /** Hatchery tag. */
    private final int hatcheryTag;

    /** Formatted hatchery tag text. */
    private final String hatcheryTagText;

    /** Player owning the hatchery. */
    private final String playerName;

    /** Final hatchery type on the tag. */
    private final String hatcheryType;

    /** Dedicated queen-to-hatchery radius used for qualification. */
    private final double injectRadius;

    /** Number of singleton-selected queen commands confidently attributed. */
    private final int attributedQueenCommandCount;

    /** Number of singleton-selected SpawnLarva commands confidently attributed to queens. */
    private final int attributedInjectCommandCount;

    /** Number of candidate windows suppressed for uncertainty. */
    private final int uncertaintyDiscardCount;

    /** Normalized idle-inject windows kept for this hatchery. */
    private final List< HatcheryIdleInjectWindow > idleWindowList;

    /** Deterministic diagnostics explaining kept and suppressed decisions. */
    private final List< String > diagnosticLineList;

    /**
     * Creates a new per-hatchery idle-inject timeline.
     *
     * @param hatcheryTag hatchery tag
     * @param hatcheryTagText formatted hatchery tag text
     * @param playerName player owning the hatchery
     * @param hatcheryType final hatchery type on the tag
     * @param injectRadius dedicated queen-to-hatchery radius used for qualification
     * @param attributedQueenCommandCount number of singleton-selected queen commands confidently attributed
     * @param attributedInjectCommandCount number of singleton-selected SpawnLarva commands confidently attributed
     * @param uncertaintyDiscardCount number of candidate windows suppressed for uncertainty
     * @param idleWindowList normalized idle-inject windows kept for this hatchery
     * @param diagnosticLineList deterministic diagnostics explaining kept and suppressed decisions
     */
    public HatcheryIdleInjectTimeline( final int hatcheryTag, final String hatcheryTagText, final String playerName,
            final String hatcheryType, final double injectRadius, final int attributedQueenCommandCount,
            final int attributedInjectCommandCount, final int uncertaintyDiscardCount,
            final List< HatcheryIdleInjectWindow > idleWindowList, final List< String > diagnosticLineList ) {
        this.hatcheryTag = hatcheryTag;
        this.hatcheryTagText = hatcheryTagText;
        this.playerName = playerName;
        this.hatcheryType = hatcheryType;
        this.injectRadius = injectRadius;
        this.attributedQueenCommandCount = attributedQueenCommandCount;
        this.attributedInjectCommandCount = attributedInjectCommandCount;
        this.uncertaintyDiscardCount = uncertaintyDiscardCount;
        this.idleWindowList = Collections.unmodifiableList( new ArrayList<>( idleWindowList ) );
        this.diagnosticLineList = Collections.unmodifiableList( new ArrayList<>( diagnosticLineList ) );
    }

    public int getHatcheryTag() {
        return hatcheryTag;
    }

    public String getHatcheryTagText() {
        return hatcheryTagText;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHatcheryType() {
        return hatcheryType;
    }

    public double getInjectRadius() {
        return injectRadius;
    }

    public int getAttributedQueenCommandCount() {
        return attributedQueenCommandCount;
    }

    public int getAttributedInjectCommandCount() {
        return attributedInjectCommandCount;
    }

    public int getUncertaintyDiscardCount() {
        return uncertaintyDiscardCount;
    }

    public List< HatcheryIdleInjectWindow > getIdleWindowList() {
        return idleWindowList;
    }

    public List< String > getDiagnosticLineList() {
        return diagnosticLineList;
    }

    public int getKeptWindowCount() {
        return idleWindowList.size();
    }

}