package com.ensemblu.axiom.core.io;

import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.function.Supplier;

public final class Effect<A> {

    public static final Effect<Nothing> empty = new Effect<>(() -> Nothing.INSTANCE);
    private final Supplier<A> s;

    private Effect(final Supplier<A> s) {
        this.s = s;
    }

    public static <A> Effect<A> of(final Supplier<A> effect) {
        return new Effect<>(effect);
    }

    public static <A> Effect<Nothing> of(final Runnable effect) {
        return new Effect<>(() -> {
            effect.run();
            return Nothing.INSTANCE;
        });
    }

    public static <A> Effect<Nothing> printlnToConsole(final A value) {
        return new Effect<>(() -> {
            System.out.println(value);
            return Nothing.INSTANCE;
        });
    }

    public A run() {
        return s.get();
    }
}