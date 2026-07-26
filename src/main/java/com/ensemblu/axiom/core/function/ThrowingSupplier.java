package com.ensemblu.axiom.core.function;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}