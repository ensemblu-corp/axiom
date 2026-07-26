package com.ensemblu.axiom.core.foundation;

import com.ensemblu.axiom.core.io.Effect;

/**
 * Represents an absence of a meaningful return value.
 * <p>
 * Typically used in contexts where only the modified state is of interest.
 * See {@link Effect}.
 * <p>
 * Similar to {@code void}, but as a first-class object to avoid instantiation issues.
 * Use {@code Nothing.INSTANCE} to represent a "no value" state.
 */
public enum Nothing {
    INSTANCE;
}