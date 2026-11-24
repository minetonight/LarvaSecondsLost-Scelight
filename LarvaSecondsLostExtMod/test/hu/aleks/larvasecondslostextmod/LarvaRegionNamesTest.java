package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Region suffix of player names (CR-1.4). */
public class LarvaRegionNamesTest {

    @Test
    public void knownRegionIdsMapToTheirShortNames() {
        assertEquals( "US", LarvaRegionNames.regionLabel( 1 ) );
        assertEquals( "EU", LarvaRegionNames.regionLabel( 2 ) );
        assertEquals( "KR", LarvaRegionNames.regionLabel( 3 ) );
        assertEquals( "CN", LarvaRegionNames.regionLabel( 5 ) );
        assertEquals( "PTR", LarvaRegionNames.regionLabel( 98 ) );
    }

    @Test
    public void otherRegionIdsFallBackToRPlusId() {
        assertEquals( "R4", LarvaRegionNames.regionLabel( 4 ) );
        assertEquals( "R0", LarvaRegionNames.regionLabel( 0 ) );
        assertEquals( "R99", LarvaRegionNames.regionLabel( 99 ) );
    }

    @Test
    public void suffixComesFromTheIdentityKeyRegion() {
        assertEquals( "Scorpion (US)", LarvaRegionNames.withRegionSuffix( "Scorpion", "1-S2-1-6779383" ) );
        assertEquals( "Scorpion (EU)", LarvaRegionNames.withRegionSuffix( "Scorpion", "2-S2-1-5946255" ) );
        assertEquals( "X (R7)", LarvaRegionNames.withRegionSuffix( "X", "7-S2-1-5" ) );
    }

    @Test
    public void nameFallbackMissingAndMalformedKeysGetNoSuffix() {
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "name:Bot" ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "name:2-S2-1-5" ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", null ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "" ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "abc-S2-1-5" ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "-S2-1-5" ) );
        assertEquals( "Bot", LarvaRegionNames.withRegionSuffix( "Bot", "A" ) );
    }
}
