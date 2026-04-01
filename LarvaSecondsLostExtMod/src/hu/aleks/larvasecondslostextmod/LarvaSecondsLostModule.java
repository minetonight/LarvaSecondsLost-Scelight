package hu.aleks.larvasecondslostextmod;

import hu.scelightapi.BaseExtModule;
import hu.scelightapi.IModEnv;
import hu.scelightapi.IServices;
import hu.scelightapibase.bean.IExtModManifestBean;
import hu.scelightapibase.gui.comp.multipage.IPageCompCreator;
import hu.scelightapibase.gui.icon.IRIcon;
import hu.scelightapibase.util.IRHtml;

import javax.swing.JComponent;

/**
 * Epic 01 baseline external module for the larva analysis feature.
 *
 * <p>This module intentionally keeps runtime behavior minimal: it adds a visible page
 * to Scelight and logs lifecycle transitions so packaging and installation can be
 * verified before replay-analysis work begins.</p>
 */
public class LarvaSecondsLostModule extends BaseExtModule {

    /** Icon used by the placeholder page. */
    private IRIcon larvaIcon;

    /** Help content for the placeholder page. */
    private IRHtml helpContent;

    @Override
    public void init( final IExtModManifestBean manifest, final IServices services, final IModEnv modEnv ) {
        super.init( manifest, services, modEnv );

        try {
            loadResources();
            installStatusPage();
            logger.debug( manifest.getName() + " module started successfully." );
        } catch ( final RuntimeException e ) {
            logger.error( "Failed to initialize " + manifest.getName() + " module.", e );
            throw e;
        } catch ( final Error e ) {
            logger.error( "Fatal error while initializing " + manifest.getName() + " module.", e );
            throw e;
        }
    }

    /**
     * Loads static resources that are packaged inside the module jar.
     */
    private void loadResources() {
        larvaIcon = guiFactory.newRIcon( getClass().getResource( "icon/larva-module-icon.png" ) );
        helpContent = guiFactory.newRHtml( "Larva Module Help", getClass().getResource( "help/larva-module-help.html" ) );
    }

    /**
     * Adds a simple status page to Scelight as visible proof that the module is active.
     */
    private void installStatusPage() {
        guiUtils.runInEDT( new Runnable() {
            @Override
            public void run() {
                services.getMainFrame().getMultiPageComp().addPage( guiFactory.newPage( "Larva", larvaIcon, false,
                        new IPageCompCreator< JComponent >() {
                            @Override
                            public JComponent createPageComp() {
                                return new LarvaStatusPageComp( LarvaSecondsLostModule.this );
                            }
                        } ) );
                services.getMainFrame().getMultiPageComp().rebuildPageTree( false );
            }
        } );
    }

    /**
     * Returns the page icon.
     *
     * @return the page icon
     */
    IRIcon getLarvaIcon() {
        return larvaIcon;
    }

    /**
     * Returns the placeholder help content.
     *
     * @return the help content
     */
    IRHtml getHelpContent() {
        return helpContent;
    }

    @Override
    public void destroy() {
        logger.debug( manifest.getName() + " module stopped." );
    }

}
