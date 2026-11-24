package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * The "Larva" hub: a module-owned, non-closeable top-level {@link IPage} whose child list is the single registry of
 * open sub-pages. Scelight itself removes a closed page from {@code getChildList()}, so no second list is kept.
 *
 * <p>The child list must only be mutated on the EDT (see {@link LarvaPageNavigator}).</p>
 */
final class LarvaHubPage implements IPage< JComponent > {

    static final String DISPLAY_NAME = "Larva";

    private final IRIcon ricon;
    private final IPageCompCreator< JComponent > contentCreator;
    private final List< IPage< ? > > childList = new ArrayList< IPage< ? > >();

    LarvaHubPage( final IRIcon ricon, final IPageCompCreator< JComponent > contentCreator ) {
        this.ricon = ricon;
        this.contentCreator = contentCreator;
    }

    /** @return the open sub-pages of the given kind in creation order (a copy, safe to iterate while closing) */
    List< LarvaSubPage > getSubPages( final LarvaSubPageKind kind ) {
        return filterSubPages( childList, kind );
    }

    /** @return the sub-pages of the given kind found in the given page list, in list order */
    static List< LarvaSubPage > filterSubPages( final List< IPage< ? > > pages, final LarvaSubPageKind kind ) {
        final List< LarvaSubPage > result = new ArrayList< LarvaSubPage >();
        for ( final IPage< ? > page : pages )
            if ( page instanceof LarvaSubPage && ( (LarvaSubPage) page ).getKind() == kind )
                result.add( (LarvaSubPage) page );
        return result;
    }

    /** @return the open single-replay page bound to this normalized path, or <code>null</code> (OQ-6 dedupe) */
    LarvaSubPage findSingleReplayPage( final Path normalizedReplayFile ) {
        return findSingleReplayPage( childList, normalizedReplayFile );
    }

    /** @return the single-replay page of the given page list that is bound to the normalized path, or <code>null</code> */
    static LarvaSubPage findSingleReplayPage( final List< IPage< ? > > pages, final Path normalizedReplayFile ) {
        for ( final LarvaSubPage page : filterSubPages( pages, LarvaSubPageKind.SINGLE_REPLAY ) ) {
            final LarvaReplayPageController controller = page.getReplayController();
            if ( controller != null && controller.getReplayFile().equals( normalizedReplayFile ) )
                return page;
        }
        return null;
    }

    /** @return <code>true</code> if the page is a direct child of this hub */
    boolean hasChild( final IPage< ? > page ) {
        return childList.contains( page );
    }

    @Override
    public JComponent createPageComp() {
        return contentCreator.createPageComp();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public IRIcon getRicon() {
        return ricon;
    }

    @Override
    public boolean isCloseable() {
        return false;
    }

    @Override
    public void setParent( final IPage< ? > parent ) {
        // top-level page: nothing to remember
    }

    @Override
    public IPage< ? > getParent() {
        return null;
    }

    @Override
    public List< IPage< ? > > getChildList() {
        return childList;
    }

    @Override
    public String toString() {
        return DISPLAY_NAME;
    }

}
