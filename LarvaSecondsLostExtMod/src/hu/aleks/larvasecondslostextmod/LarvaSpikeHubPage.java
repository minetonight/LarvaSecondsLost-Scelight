package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * E17-S01 spike: the "Larva" page as a module-owned, non-closeable {@link IPage} with an always non-null, mutable
 * child list. Its component is the unchanged Larva replay page (delegated to the original creator).
 */
final class LarvaSpikeHubPage implements IPage< JComponent > {

    private final String displayName;
    private final IRIcon ricon;
    private final IPageCompCreator< JComponent > contentCreator;
    private final List< IPage< ? > > childList = new ArrayList< IPage< ? > >();
    private IPage< ? > parent;

    LarvaSpikeHubPage( final String displayName, final IRIcon ricon, final IPageCompCreator< JComponent > contentCreator ) {
        this.displayName = displayName;
        this.ricon = ricon;
        this.contentCreator = contentCreator;
    }

    void addChild( final IPage< ? > child ) {
        childList.add( child );
        child.setParent( this );
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
        return false;
    }

    @Override
    public void setParent( final IPage< ? > parent ) {
        this.parent = parent;
    }

    @Override
    public IPage< ? > getParent() {
        return parent;
    }

    @Override
    public List< IPage< ? > > getChildList() {
        return childList;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
