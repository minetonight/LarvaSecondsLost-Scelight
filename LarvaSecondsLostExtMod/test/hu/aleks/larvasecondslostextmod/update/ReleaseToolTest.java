package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** Landing page rendering of the publish step (E18-S07). */
public class ReleaseToolTest {

    @Test
    public void fillsAllTokens() throws Exception {
        final UpdateTestFixtures.ReleaseBuilder builder = new UpdateTestFixtures.ReleaseBuilder( "3.1.2" );
        final RemoteRelease release = builder.parse( builder.moduleXml( builder.zip() ) );
        final String html = ReleaseTool.renderPage( "<a href=\"@archiveFile@\">@version@</a> @archiveSizeKb@ KB @releaseDate@", release, "2026-11-25" );
        assertEquals( "<a href=\"LarvaSecondsLost-3.1.2.zip\">3.1.2</a> " + ( release.getArchiveSize() + 1023 ) / 1024 + " KB 2026-11-25", html );
    }

    @Test
    public void realTemplateHasOnlyKnownTokens() throws Exception {
        final UpdateTestFixtures.ReleaseBuilder builder = new UpdateTestFixtures.ReleaseBuilder( "3.1" );
        final String template = new String( Files.readAllBytes( Paths.get( "release/site/index-template.html" ) ), UpdateTestFixtures.UTF8 );
        final String html = ReleaseTool.renderPage( template, builder.parse( builder.moduleXml( builder.zip() ) ), "2026-11-25" );
        assertTrue( html.contains( "href=\"LarvaSecondsLost-3.1.zip\"" ) );
        assertTrue( html.contains( "larva-seconds-lost/3.1/" ) );
    }

    @Test
    public void unknownTokenFails() throws Exception {
        final UpdateTestFixtures.ReleaseBuilder builder = new UpdateTestFixtures.ReleaseBuilder( "3.1" );
        try {
            ReleaseTool.renderPage( "@versoin@", builder.parse( builder.moduleXml( builder.zip() ) ), "x" );
            fail();
        } catch ( final UpdateException e ) {
            assertTrue( e.getMessage().contains( "@versoin@" ) );
        }
    }

}
