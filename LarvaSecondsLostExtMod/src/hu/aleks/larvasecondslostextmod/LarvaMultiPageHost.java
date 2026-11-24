package hu.aleks.larvasecondslostextmod;

import hu.scelightapibase.gui.icon.IRIcon;

import javax.swing.JComponent;

/** What the navigator needs from the module to create multi-analysis pages. */
interface LarvaMultiPageHost {

    /** @return the icon of multi-analysis pages */
    IRIcon getMultiIcon();

    /** @return creates the batch runner of a page */
    LarvaMultiAnalysisPageController.RunFactory getRunFactory();

    /** Creates the page component for a controller (EDT, called lazily by Scelight on first selection). */
    JComponent createMultiPageComp( LarvaMultiAnalysisPageController controller );
}
