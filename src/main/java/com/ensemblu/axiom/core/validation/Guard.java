package com.ensemblu.axiom.core.validation;

import com.ensemblu.axiom.core.foundation.Nothing;
import com.ensemblu.axiom.core.io.Effect;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Guard {

    private Guard() {
        throw new AssertionError("Guard Validator: The constructor is sealed; structural integrity must be maintained.");
    }

    public static Cond supplyThat(BooleanSupplier condition) {
        return condition::getAsBoolean;
    }

    @FunctionalInterface
    public interface Cond {
        boolean test();

        default Cond andSupplyThat(BooleanSupplier other) {
            return () -> (this.test() && (other.getAsBoolean()));
        }

        default Cond andSupplyThatNot(BooleanSupplier other) {
            return () -> (this.test() && (!other.getAsBoolean()));
        }

        default Cond onFail(Runnable diagnostic) {
            return () -> {
                boolean passed = this.test();
                if (!passed) diagnostic.run();
                return passed;
            };
        }

        default Cond onFail( Effect<Nothing> diagnostic) {
            return () -> {
                boolean passed = this.test();
                if (!passed) diagnostic.run();
                return passed;
            };
        }

        default <T> OrFail<T> orFail(Supplier<String> message) {
            return value -> this.test() ? Result.success(value.get()) : Result.failure(message.get());
        }
    }

    @FunctionalInterface
    public interface OrFail<T> {
        Result<T> yield(Supplier<T> value);
    }
}