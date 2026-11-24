package hu.aleks.larvasecondslostextmod;

/** Time source seam for throttling, so tests need no sleeping. */
interface LarvaClock {

    /** The production clock. */
    LarvaClock SYSTEM = new LarvaClock() {
        @Override
        public long nowMillis() {
            return System.nanoTime() / 1000000L;
        }
    };

    /** @return monotonic milliseconds */
    long nowMillis();
}
