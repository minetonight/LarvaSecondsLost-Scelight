package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** module.xml parsing (E18-S08): the real generated file, and every way a file can be wrong. */
public class ModuleXmlParserTest {

    private static RemoteRelease parse( final String xml ) throws UpdateException {
        return new ModuleXmlParser().parse( new ByteArrayInputStream( xml.getBytes( UpdateTestFixtures.UTF8 ) ) );
    }

    private static String validXml() {
        final UpdateTestFixtures.ReleaseBuilder builder = new UpdateTestFixtures.ReleaseBuilder( "3.2.1" );
        try {
            return builder.moduleXml( builder.zip() );
        } catch ( final java.io.IOException e ) {
            throw new IllegalStateException( e );
        }
    }

    private static void assertRejected( final String xml, final String expectedMessagePart ) {
        try {
            parse( xml );
            fail( "expected an UpdateException containing: " + expectedMessagePart );
        } catch ( final UpdateException e ) {
            assertTrue( "message was: " + e.getMessage(), e.getMessage().contains( expectedMessagePart ) );
        }
    }

    @Test
    public void parsesTheModuleXmlGeneratedByTheAntBuild() throws Exception {
        try ( final InputStream in = Files.newInputStream( Paths.get( "release/deployment/module.xml" ) ) ) {
            final RemoteRelease release = new ModuleXmlParser().parse( in );
            assertEquals( "Larva Seconds Lost", release.getName() );
            assertEquals( "larva-seconds-lost", release.getFolder() );
            assertTrue( release.getArchiveUrlList().get( 0 ).startsWith( "https://minetonight.github.io/" ) );
            assertTrue( release.getArchiveSize() > 0 );
            assertFalse( release.getFileSha256ByPath().isEmpty() );
            for ( final String path : release.getFileSha256ByPath().keySet() )
                assertTrue( path, path.startsWith( "Scelight/mod-x/larva-seconds-lost/" + release.getVersion() + "/" ) );
            assertEquals( release.getVersion().toString(), UpdateInstaller.checkFileListLayout( release ) );
        }
    }

    @Test
    public void readsAllFields() throws Exception {
        final RemoteRelease release = parse( validXml() );
        assertEquals( new ModuleVersion( 3, 2, 1 ), release.getVersion() );
        assertEquals( "LarvaSecondsLost-3.2.1.zip", release.getArchiveFileName() );
        assertEquals( 64, release.getArchiveSha256().length() );
        assertEquals( 1, release.getArchiveUrlList().size() );
        assertEquals( 3, release.getFileSha256ByPath().size() );
    }

    @Test
    public void rejectsBrokenXml() {
        assertRejected( "<moduleBean><name>x", "not valid XML" );
    }

    @Test
    public void rejectsDoctypeSoNoExternalEntitiesAreLoaded() {
        assertRejected( "<?xml version=\"1.0\"?><!DOCTYPE moduleBean [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>" + validXml().substring(
                validXml().indexOf( "<moduleBean" ) ).replace( "<name>Larva Seconds Lost</name>", "<name>&x;</name>" ), "not valid XML" );
    }

    @Test
    public void rejectsWrongRoot() {
        assertRejected( validXml().replace( "moduleBean", "extModManifestBean" ), "expected <moduleBean>" );
    }

    @Test
    public void rejectsMissingVersion() {
        assertRejected( validXml().replaceAll( "<version [^>]*/>", "" ), "exactly one <version>" );
    }

    @Test
    public void rejectsBadChecksum() {
        assertRejected( validXml().replaceFirst( "sha256=\"[0-9a-f]{64}\"", "sha256=\"abc\"" ), "invalid sha256" );
    }

    @Test
    public void rejectsBadArchiveSize() {
        assertRejected( validXml().replaceAll( "<archiveSize>\\d+</archiveSize>", "<archiveSize>0</archiveSize>" ), "invalid archiveSize" );
    }

    @Test
    public void rejectsMissingOrNonHttpUrl() {
        assertRejected( validXml().replaceAll( "<urlList>[^<]*</urlList>", "" ), "no <urlList>" );
        assertRejected( validXml().replaceAll( "<urlList>[^<]*</urlList>", "<urlList>file:///tmp/x.zip</urlList>" ), "non-HTTP" );
    }

    @Test
    public void rejectsEmptyFileList() {
        assertRejected( validXml().replaceAll( "<fileList [^>]*/>", "" ), "no <fileList>" );
    }

}
