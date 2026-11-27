package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** List file placeholders and the energy-bound tolerance of the corpus check (E19-S00). */
public class LarvaAnalysisCheckCliTest {

    @Test
    public void placeholdersAreReplacedFromTheEnvironment() {
        final Map< String, String > env = new HashMap<>();
        env.put( "REPO", "/r" );
        env.put( "SC2_ACCOUNTS", "/a b" );
        assertEquals( "/r/x/${y.SC2Replay", LarvaAnalysisCheckCli.expand( "${REPO}/x/${y.SC2Replay", env ) );
        assertEquals( "/a b/1/$2.SC2Replay", LarvaAnalysisCheckCli.expand( "${SC2_ACCOUNTS}/1/$2.SC2Replay", env ) );
    }

    @Test
    public void unknownPlaceholderFails() {
        try {
            LarvaAnalysisCheckCli.expand( "${NOPE}/a.SC2Replay", new HashMap< String, String >() );
            fail();
        } catch ( final IllegalArgumentException e ) {
            assertEquals( true, e.getMessage().contains( "NOPE" ) );
        }
    }

    @Test
    public void allowedMissedInjectsFollowTheRegenerationRatio() {
        assertEquals( 1, LarvaAnalysisCheckSummary.allowedMissedInjects( 0 ) );
        assertEquals( 14, LarvaAnalysisCheckSummary.allowedMissedInjects( 12 ) );
        assertEquals( 111, LarvaAnalysisCheckSummary.allowedMissedInjects( 100 ) );
    }
}
