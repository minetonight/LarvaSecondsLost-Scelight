package hu.aleks.larvasecondslostextmod.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses the SDK's <code>module.xml</code> (a Scelight {@code moduleBean}) into a {@link RemoteRelease} (E18-S08).
 *
 * <p>Uses the JDK's DOM parser on purpose: the launcher's JAXB beans are not part of the ext-mod API. DOCTYPEs are refused,
 * so a hostile file cannot pull in external entities.</p>
 */
public final class ModuleXmlParser {

    private static final String SHA256_PATTERN = "[0-9a-f]{64}";

    /**
     * @param in the module.xml content; not closed
     * @return the release, fully validated
     * @throws UpdateException if the content is not a valid module.xml
     */
    public RemoteRelease parse( final InputStream in ) throws UpdateException {
        final Element root = readRoot( in );
        if ( !"moduleBean".equals( root.getTagName() ) )
            throw new UpdateException( "module.xml has root <" + root.getTagName() + ">, expected <moduleBean>" );

        final String name = requiredText( root, "name" );
        final String folder = requiredText( root, "folder" );
        final ModuleVersion version = parseVersion( requiredChild( root, "version" ) );

        final Element archive = requiredChild( root, "archiveFile" );
        final String archiveFileName = requiredAttribute( archive, "path" );
        final String archiveSha256 = sha256Attribute( archive, "archiveFile" );
        final long archiveSize = parsePositiveLong( requiredText( root, "archiveSize" ), "archiveSize" );

        final List< String > urlList = new ArrayList<>();
        for ( final Element url : children( root, "urlList" ) ) {
            final String text = url.getTextContent().trim();
            if ( !text.startsWith( "https://" ) && !text.startsWith( "http://" ) )
                throw new UpdateException( "module.xml has a non-HTTP archive URL: " + text );
            urlList.add( text );
        }
        if ( urlList.isEmpty() )
            throw new UpdateException( "module.xml has no <urlList> archive URL" );

        final Map< String, String > files = new LinkedHashMap<>();
        for ( final Element file : children( root, "fileList" ) ) {
            final String path = requiredAttribute( file, "path" ).replace( '\\', '/' );
            if ( files.put( path, sha256Attribute( file, "fileList" ) ) != null )
                throw new UpdateException( "module.xml lists a file twice: " + path );
        }
        if ( files.isEmpty() )
            throw new UpdateException( "module.xml has no <fileList> entries" );

        return new RemoteRelease( name, folder, version, archiveFileName, archiveSha256, archiveSize, urlList, files );
    }

    private static Element readRoot( final InputStream in ) throws UpdateException {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( false );
            factory.setExpandEntityReferences( false );
            factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );
            factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler( null ); // errors are thrown as SAXException, no printing to stderr
            final Document document = builder.parse( in );
            return document.getDocumentElement();
        } catch ( final ParserConfigurationException e ) {
            throw new UpdateException( "XML parser cannot be configured safely: " + e.getMessage(), e );
        } catch ( final SAXException e ) {
            throw new UpdateException( "module.xml is not valid XML: " + e.getMessage(), e );
        } catch ( final IOException e ) {
            throw new UpdateException( "could not read module.xml: " + e.getMessage(), e );
        }
    }

    private static ModuleVersion parseVersion( final Element version ) throws UpdateException {
        final int major = parseNonNegativeInt( requiredAttribute( version, "major" ), "version major" );
        final int minor = parseNonNegativeInt( requiredAttribute( version, "minor" ), "version minor" );
        final String revisionText = version.getAttribute( "revision" );
        final int revision = revisionText.length() == 0 ? 0 : parseNonNegativeInt( revisionText, "version revision" );
        return new ModuleVersion( major, minor, revision );
    }

    private static List< Element > children( final Element parent, final String tag ) {
        final List< Element > result = new ArrayList<>();
        final NodeList nodes = parent.getChildNodes();
        for ( int i = 0; i < nodes.getLength(); i++ ) {
            final Node node = nodes.item( i );
            if ( node.getNodeType() == Node.ELEMENT_NODE && tag.equals( ( (Element) node ).getTagName() ) )
                result.add( (Element) node );
        }
        return result;
    }

    private static Element requiredChild( final Element parent, final String tag ) throws UpdateException {
        final List< Element > list = children( parent, tag );
        if ( list.size() != 1 )
            throw new UpdateException( "module.xml must have exactly one <" + tag + ">, found " + list.size() );
        return list.get( 0 );
    }

    private static String requiredText( final Element parent, final String tag ) throws UpdateException {
        final String text = requiredChild( parent, tag ).getTextContent().trim();
        if ( text.length() == 0 )
            throw new UpdateException( "module.xml has an empty <" + tag + ">" );
        return text;
    }

    private static String requiredAttribute( final Element element, final String attribute ) throws UpdateException {
        final String value = element.getAttribute( attribute ).trim();
        if ( value.length() == 0 )
            throw new UpdateException( "module.xml <" + element.getTagName() + "> has no " + attribute + " attribute" );
        return value;
    }

    private static String sha256Attribute( final Element element, final String context ) throws UpdateException {
        final String value = requiredAttribute( element, "sha256" ).toLowerCase( Locale.ROOT );
        if ( !value.matches( SHA256_PATTERN ) )
            throw new UpdateException( "module.xml <" + context + "> has an invalid sha256: " + value );
        return value;
    }

    private static int parseNonNegativeInt( final String text, final String what ) throws UpdateException {
        try {
            final int value = Integer.parseInt( text );
            if ( value >= 0 )
                return value;
        } catch ( final NumberFormatException e ) {
            // reported below with the field name
        }
        throw new UpdateException( "module.xml has an invalid " + what + ": " + text );
    }

    private static long parsePositiveLong( final String text, final String what ) throws UpdateException {
        try {
            final long value = Long.parseLong( text );
            if ( value > 0 )
                return value;
        } catch ( final NumberFormatException e ) {
            // reported below with the field name
        }
        throw new UpdateException( "module.xml has an invalid " + what + ": " + text );
    }

}
