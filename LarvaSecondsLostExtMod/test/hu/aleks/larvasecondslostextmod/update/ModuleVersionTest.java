package hu.aleks.larvasecondslostextmod.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Version parsing, ordering and folder names (E18-S08). */
public class ModuleVersionTest {

    @Test
    public void parsesTwoAndThreePartVersions() {
        assertEquals( new ModuleVersion( 3, 1, 0 ), ModuleVersion.parseOrNull( "3.1" ) );
        assertEquals( new ModuleVersion( 3, 1, 2 ), ModuleVersion.parseOrNull( "3.1.2" ) );
    }

    @Test
    public void zeroRevisionEqualsTwoPartForm() {
        assertEquals( ModuleVersion.parseOrNull( "3.1" ), ModuleVersion.parseOrNull( "3.1.0" ) );
        assertEquals( ModuleVersion.parseOrNull( "3.1" ).hashCode(), ModuleVersion.parseOrNull( "3.1.0" ).hashCode() );
    }

    @Test
    public void folderNameDropsZeroRevisionLikeScelight() {
        assertEquals( "3.1", new ModuleVersion( 3, 1, 0 ).toString() );
        assertEquals( "3.1.2", new ModuleVersion( 3, 1, 2 ).toString() );
    }

    @Test
    public void ordersNumericallyNotAsText() {
        assertTrue( ModuleVersion.parseOrNull( "3.10" ).isNewerThan( ModuleVersion.parseOrNull( "3.9" ) ) );
        assertTrue( ModuleVersion.parseOrNull( "3.1.1" ).isNewerThan( ModuleVersion.parseOrNull( "3.1" ) ) );
        assertTrue( ModuleVersion.parseOrNull( "4.0" ).isNewerThan( ModuleVersion.parseOrNull( "3.99.99" ) ) );
        assertFalse( ModuleVersion.parseOrNull( "3.1" ).isNewerThan( ModuleVersion.parseOrNull( "3.1.0" ) ) );
    }

    @Test
    public void nonVersionNamesAreNull() {
        for ( final String text : new String[] { null, "", "3", "3.", ".1", "3.1.", "3.1.2.4", "3.x", "3.1.part", ".incoming-3.1", "3.-1", "v3.1",
                "3.1 ", "99999999999.1" } )
            assertNull( "should not parse: " + text, ModuleVersion.parseOrNull( text ) );
    }

}
