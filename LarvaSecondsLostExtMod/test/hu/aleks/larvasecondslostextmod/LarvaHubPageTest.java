package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;

import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Test;

public class LarvaHubPageTest {

    private static final IPageCompCreator< JComponent > CREATOR = new IPageCompCreator< JComponent >() {
        @Override
        public JComponent createPageComp() {
            return new JPanel();
        }
    };

    private LarvaHubPage hub;

    @Before
    public void setUp() {
        hub = new LarvaHubPage( null, CREATOR );
    }

    private static LarvaSubPage sub( final LarvaSubPageKind kind, final String name ) {
        return new LarvaSubPage( kind, name, null, CREATOR );
    }

    private void add( final LarvaSubPage page ) {
        page.setParent( hub );
        hub.getChildList().add( page );
    }

    @Test
    public void hubIsNonClosableTopLevelPageWithMutableEmptyChildList() {
        assertEquals( "Larva", hub.getDisplayName() );
        assertFalse( hub.isCloseable() );
        assertNull( hub.getParent() );
        assertNotNull( hub.getChildList() );
        assertTrue( hub.getChildList().isEmpty() );
    }

    @Test
    public void subPageIsClosableLeafWithMutableName() {
        final LarvaSubPage page = sub( LarvaSubPageKind.SINGLE_REPLAY, "game1" );
        assertTrue( page.isCloseable() );
        assertNull( page.getChildList() );
        page.setDisplayName( "renamed" );
        assertEquals( "renamed", page.getDisplayName() );
        assertEquals( "renamed", page.toString() );
    }

    @Test
    public void addedChildKeepsCreationOrderAndParent() {
        final LarvaSubPage a = sub( LarvaSubPageKind.SINGLE_REPLAY, "a" );
        final LarvaSubPage b = sub( LarvaSubPageKind.MULTI_ANALYSIS, "b" );
        add( a );
        add( b );
        assertEquals( Arrays.< IPage< ? > > asList( a, b ), hub.getChildList() );
        assertSame( hub, a.getParent() );
        assertTrue( hub.hasChild( b ) );
        assertFalse( hub.hasChild( sub( LarvaSubPageKind.SINGLE_REPLAY, "x" ) ) );
    }

    @Test
    public void kindFilterReturnsOnlyMatchingPagesInOrder() {
        final LarvaSubPage s1 = sub( LarvaSubPageKind.SINGLE_REPLAY, "s1" );
        final LarvaSubPage m1 = sub( LarvaSubPageKind.MULTI_ANALYSIS, "m1" );
        final LarvaSubPage s2 = sub( LarvaSubPageKind.SINGLE_REPLAY, "s2" );
        add( s1 );
        add( m1 );
        add( s2 );
        assertEquals( Arrays.asList( s1, s2 ), hub.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ) );
        assertEquals( Arrays.asList( m1 ), hub.getSubPages( LarvaSubPageKind.MULTI_ANALYSIS ) );
    }

    @Test
    public void filterIgnoresForeignPagesAndSeesHostRemoval() {
        final LarvaSubPage s1 = sub( LarvaSubPageKind.SINGLE_REPLAY, "s1" );
        add( s1 );
        hub.getChildList().add( new LarvaHubPage( null, CREATOR ) ); // a non-LarvaSubPage child
        assertEquals( 1, hub.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ).size() );
        hub.getChildList().remove( s1 ); // what Scelight does when the page is closed
        assertTrue( hub.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ).isEmpty() );
    }

    @Test
    public void returnedSubPageListIsACopy() {
        add( sub( LarvaSubPageKind.SINGLE_REPLAY, "s1" ) );
        hub.getSubPages( LarvaSubPageKind.SINGLE_REPLAY ).clear();
        assertEquals( 1, hub.getChildList().size() );
    }

}
