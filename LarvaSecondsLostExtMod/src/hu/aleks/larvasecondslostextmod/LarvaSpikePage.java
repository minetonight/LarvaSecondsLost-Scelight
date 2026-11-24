package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPage;
import hu.scelightapibase.gui.icon.IRIcon;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * E17-S01 spike: module-owned {@link IPage} with a mutable display name. Serves as the hub (non-closeable) and as
 * child (closeable). The child list is created lazily like Scelight's {@code BasePage}: {@code null} until needed.
 */
final class LarvaSpikePage implements IPage< JComponent > {

    private volatile String displayName;
    private final IRIcon ricon;
    private final boolean closeable;
    private final LarvaSpikeCounters counters;
    private IPage< ? > parent;
    private List< IPage< ? > > childList;

    LarvaSpikePage( final String displayName, final IRIcon ricon, final boolean closeable, final LarvaSpikeCounters counters ) {
        this.displayName = displayName;
        this.ricon = ricon;
        this.closeable = closeable;
        this.counters = counters;
    }

    void setDisplayName( final String displayName ) {
        this.displayName = displayName;
    }

    /** Adds a child (sets its parent, creates the list lazily). */
    void addChild( final IPage< ? > child ) {
        if ( childList == null )
            childList = new ArrayList< IPage< ? > >();
        childList.add( child );
        child.setParent( this );
    }

    @Override
    public JComponent createPageComp() {
        counters.created.incrementAndGet();
        counters.event( "createPageComp" );
        return new LarvaSpikePageComp( counters, "Spike page: " + displayName );
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
        return closeable;
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
