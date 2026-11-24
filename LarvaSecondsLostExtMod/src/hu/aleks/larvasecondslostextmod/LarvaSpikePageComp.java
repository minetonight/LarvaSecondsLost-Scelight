package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.comp.multipage.IPageClosingListener;
import hu.scelightapibase.gui.comp.multipage.IPageDisposedListener;
import hu.scelightapibase.gui.comp.multipage.IPageSelectedListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** E17-S01 spike: trivial page component that reports the host lifecycle callbacks. */
@SuppressWarnings( "serial" )
final class LarvaSpikePageComp extends JPanel implements IPageSelectedListener, IPageClosingListener, IPageDisposedListener {

    private final LarvaSpikeCounters counters;

    LarvaSpikePageComp( final LarvaSpikeCounters counters, final String text ) {
        this.counters = counters;
        add( new JLabel( text ) );
    }

    @Override
    public void pageSelected() {
        counters.selected.incrementAndGet();
        counters.event( "pageSelected" );
    }

    @Override
    public boolean pageClosing() {
        counters.closing.incrementAndGet();
        counters.event( "pageClosing veto=" + counters.vetoClose );
        return !counters.vetoClose;
    }

    @Override
    public void pageDisposed() {
        counters.disposed.incrementAndGet();
        counters.event( "pageDisposed" );
    }

}
