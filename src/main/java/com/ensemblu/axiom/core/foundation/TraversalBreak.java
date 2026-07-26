package com.ensemblu.axiom.core.foundation;

/**
 * A high-performance "Signal" to stop a functional traversal.
 * No stack trace, no overhead.
 */
public final class TraversalBreak extends RuntimeException {
    public static final TraversalBreak INSTANCE = new TraversalBreak();

    private TraversalBreak() {
        super(null, null, false, false);
    }
}