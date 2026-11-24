package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.icon.IRIcon;

import javax.swing.JComponent;

/** What the navigator needs from the module to create single-replay pages (keeps the navigator free of module types). */
interface LarvaSinglePageHost {

    /** @return the icon of single-replay pages */
    IRIcon getReplayIcon();

    /** @return the shared analysis queue */
    LarvaReplayAnalysisQueue getAnalysisQueue();

    /** Creates the page component for a controller (EDT, called lazily by Scelight on first selection). */
    JComponent createReplayPageComp( LarvaReplayPageController controller );

}
