package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JLabel;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class LarvaHubPageCompTest {

    private static final class FakeActions implements LarvaHubActions {
        final List< String > calls = new ArrayList< String >();

        @Override
        public void openReplay( final Component parent ) {
            calls.add( "open" );
        }

        @Override
        public void analyzeLatestReplay() {
            calls.add( "latest" );
        }

        @Override
        public void startMultiAnalysis( final Component parent, final boolean ladderOnly ) {
            calls.add( "multi:" + ladderOnly );
        }
    }

    private FakeActions actions;
    private LarvaHubPageComp comp;

    @Before
    public void setUp() {
        Assume.assumeFalse( GraphicsEnvironment.isHeadless() );
        actions = new FakeActions();
        comp = new LarvaHubPageComp( actions, new JLabel( "?" ), null );
    }

    @Test
    public void checkboxIsCheckedByDefault() {
        assertTrue( comp.isLadderOnly() );
    }

    @Test
    public void buttonsHaveSpecLabelsInOrder() {
        assertEquals( "Open Replay...", comp.getButtonLabels()[0] );
        assertEquals( "Analyze Latest Replay", comp.getButtonLabels()[1] );
        assertEquals( "Multi Analysis...", comp.getButtonLabels()[2] );
    }

    @Test
    public void buttonsInvokeMatchingActions() {
        comp.getOpenButton().doClick( 0 );
        comp.getLatestButton().doClick( 0 );
        comp.getMultiButton().doClick( 0 );
        assertEquals( "[open, latest, multi:true]", actions.calls.toString() );
    }

    @Test
    public void uncheckedBoxIsPassedToMultiAnalysis() {
        comp.setLadderOnly( false );
        assertFalse( comp.isLadderOnly() );
        comp.getMultiButton().doClick( 0 );
        assertEquals( "[multi:false]", actions.calls.toString() );
    }

    @Test
    public void allButtonsShareOnePreferredWidth() {
        final int w = comp.getOpenButton().getPreferredSize().width;
        assertEquals( w, comp.getLatestButton().getPreferredSize().width );
        assertEquals( w, comp.getMultiButton().getPreferredSize().width );
    }

    @Test
    public void tipResourceContainsTipText() throws Exception {
        final InputStream in = LarvaHubPageComp.class.getResourceAsStream( "help/larva-multi-tip.html" );
        assertTrue( "tip resource missing", in != null );
        final String html = new Scanner( in, "UTF-8" ).useDelimiter( "\\A" ).next();
        in.close();
        assertTrue( html.contains( LarvaHubPageComp.MULTI_TIP_TEXT ) );
    }
}
