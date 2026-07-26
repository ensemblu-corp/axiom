package com.ensemblu.axiom.core.function;

import com.ensemblu.axiom.core.io.Effect;
import com.ensemblu.axiom.core.foundation.Nothing;

public interface ConsoleOutputEffect {
    default Effect<Nothing> printlnToConsole() {
        return Effect.printlnToConsole(this);
    }
}
