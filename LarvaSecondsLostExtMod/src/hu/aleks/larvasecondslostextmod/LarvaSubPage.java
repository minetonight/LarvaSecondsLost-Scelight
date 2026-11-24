package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;

import java.util.List;

import javax.swing.JComponent;

/**
 * A closeable leaf page under the Larva hub (single replay or multi analysis). The display name is mutable; call
 * {@link LarvaPageNavigator#relabel} (which rebuilds the page tree) after changing it.
 *
 * <p>The component is created lazily by the host on first selection, so page state must live in a controller held
 * by the creator, never in the component (S01 finding).</p>
 */
final class LarvaSubPage implements IPage< JComponent > {

    private final LarvaSubPageKind kind;
    private final IRIcon ricon;
    private final IPageCompCreator< JComponent > contentCreator;
    private volatile String displayName;
    private IPage< ? > parent;
    private LarvaReplayPageController replayController;
    private LarvaMultiAnalysisPageController multiController;

    LarvaSubPage( final LarvaSubPageKind kind, final String displayName, final IRIcon ricon, final IPageCompCreator< JComponent > contentCreator ) {
        this.kind = kind;
        this.displayName = displayName;
        this.ricon = ricon;
        this.contentCreator = contentCreator;
    }

    LarvaSubPageKind getKind() {
        return kind;
    }

    /** Binds a single-replay page to its controller (the page's state holder, see S06). */
    void setReplayController( final LarvaReplayPageController replayController ) {
        this.replayController = replayController;
    }

    /** @return the controller of a single-replay page; <code>null</code> for other kinds */
    LarvaReplayPageController getReplayController() {
        return replayController;
    }

    /** Binds a multi-analysis page to its controller (the page's state holder, see S14). */
    void setMultiController( final LarvaMultiAnalysisPageController multiController ) {
        this.multiController = multiController;
    }

    /** @return the controller of a multi-analysis page; <code>null</code> for other kinds */
    LarvaMultiAnalysisPageController getMultiController() {
        return multiController;
    }

    void setDisplayName( final String displayName ) {
        this.displayName = displayName;
    }

    @Override
    public JComponent createPageComp() {
        return contentCreator.createPageComp();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public IRIcon getRicon() {
        return ricon;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }

    @Override
    public void setParent( final IPage< ? > parent ) {
        this.parent = parent;
    }

    @Override
    public IPage< ? > getParent() {
        return parent;
    }

    /** Sub-pages are leaves (closing a page that has children throws in the host), so there is no child list. */
    @Override
    public List< IPage< ? > > getChildList() {
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
