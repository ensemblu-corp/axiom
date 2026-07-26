package com.ensemblu.axiom.core.function;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}