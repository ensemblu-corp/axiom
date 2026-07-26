package com.ensemblu.axiom.core.function;

@FunctionalInterface
public interface ThrowingPredicate<T> {
    boolean test(T t) throws Exception;
}