package hu.aleks.larvasecondslostextmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of a dev command: one short summary plus optional detail lines. */
final class LarvaDevCommandResult {

    private final boolean ok;
    private final String summary;
    private final List< String > detailLines;

    private LarvaDevCommandResult( final boolean ok, final String summary, final List< String > detailLines ) {
        this.ok = ok;
        this.summary = summary;
        this.detailLines = Collections.unmodifiableList( detailLines );
    }

    static LarvaDevCommandResult ok( final String summary ) {
        return new LarvaDevCommandResult( true, summary, new ArrayList< String >() );
    }

    static LarvaDevCommandResult ok( final String summary, final List< String > detailLines ) {
        return new LarvaDevCommandResult( true, summary, new ArrayList< String >( detailLines ) );
    }

    static LarvaDevCommandResult fail( final String reason ) {
        return new LarvaDevCommandResult( false, reason, new ArrayList< String >() );
    }

    boolean isOk() {
        return ok;
    }

    String getSummary() {
        return summary;
    }

    List< String > getDetailLines() {
        return detailLines;
    }

}
