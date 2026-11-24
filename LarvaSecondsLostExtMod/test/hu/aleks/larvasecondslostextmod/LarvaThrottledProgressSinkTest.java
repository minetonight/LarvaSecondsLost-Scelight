package hu.aleks.larvasecondslostextmod;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Progress throttling with an injectable clock (E17-S12). */
public class LarvaThrottledProgressSinkTest {

    private long now;

    private final List< String > posted = new ArrayList<>();

    private final LarvaThrottledProgressSink sink = new LarvaThrottledProgressSink( new LarvaProgressSink() {
        @Override
        public void onProgress( final int k, final int n, final String fileName, final int failedCount ) {
            posted.add( k + "/" + n + ":" + fileName + ":" + failedCount );
        }
    }, new LarvaClock() {
        @Override
        public long nowMillis() {
            return now;
        }
    } );

    private void at( final long ms, final int k ) {
        now = ms;
        sink.onProgress( k, 9, "f" + k, k - 1 );
    }

    @Test
    public void firstUpdateIsPostedImmediately() {
        at( 5000, 1 );
        assertEquals( 1, posted.size() );
        assertEquals( "1/9:f1:0", posted.get( 0 ) );
    }

    @Test
    public void updatesCloserThanTheIntervalAreSuppressedAndTheLastOneIsFlushed() {
        at( 0, 1 );
        at( 10, 2 );
        at( 99, 3 );
        assertEquals( 1, posted.size() );
        at( 100, 4 ); // exactly the interval: posted
        assertEquals( 2, posted.size() );
        at( 120, 5 );
        assertEquals( 2, posted.size() );
        sink.flush();
        assertEquals( "[1/9:f1:0, 4/9:f4:3, 5/9:f5:4]", posted.toString() );
    }

    @Test
    public void flushWithoutPendingUpdateDoesNothing() {
        at( 0, 1 );
        sink.flush();
        sink.flush();
        assertEquals( 1, posted.size() );
    }

    @Test
    public void flushAfterAPostedUpdateDoesNotRepeatIt() {
        at( 0, 1 );
        at( 150, 2 );
        sink.flush();
        assertEquals( 2, posted.size() );
    }

    @Test
    public void clockGoingBackwardsDoesNotSwallowUpdates() {
        at( 1000, 1 );
        at( 10, 2 );
        assertEquals( 2, posted.size() );
    }

    @Test
    public void fastBurstPostsAtMostOncePerInterval() {
        for ( int k = 1; k <= 200; k++ )
            at( k * 5L, k ); // 200 files in one second
        sink.flush();
        assertEquals( "last update is always delivered", "200/9:f200:199", posted.get( posted.size() - 1 ) );
        // 1 s at >= 100 ms apart: at most 10 or 11 posts plus the flushed final one
        org.junit.Assert.assertTrue( "posts: " + posted.size(), posted.size() <= 12 );
    }
}
