package com.ensemblu.axiom.core.validation.behavior;

import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.function.ThrowingPredicate;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public interface ResultBehavior {

    interface Operations<T> extends ResultBehavior {

        interface ErrorHandling<T> extends Operations<T> {
            /**
             * The Master Method: Now takes the full Exception data.
             */
            Result<T> mapFailure(Function<RuntimeException, String> messageMapper);



            Result<T> mapError(Function<RuntimeException, RuntimeException> messageMapper);

            default Result<T> prependFailureMessage(String prefix) {
                return mapFailure(e -> prefix + "\n" + e.getMessage());
            }

            default Result<T> prependMethodNameToFailureMessage(final String methodName) {
                return prependFailureMessage("[" + methodName + "] ");
            }

            default Result<T> mapFailure(String newMessage) {
                return mapFailure(msg -> newMessage);
            }


            <F> Result<F> castFailure();

            T getOrCreateFailureInstanceWithMessage(final Function<String, T> failureHandler);

            Result<T> recover(Function<RuntimeException, T> recoveryLogic);

            Result<T> recoverWith(Supplier<Result<T>> fallback);
        }

        interface Mapping<T> extends Operations<T> {
            <U> Result.WithNamingThrowingPredicate<U> mapTry(Function<T, U> mapper);

            <U> Result.WithNamingThrowingPredicate<U> flatMapTry(Function<T, Result<U>> flatMapper);

            <U> Result<U> map(Function<T, U> mapper);

            <U> Result<U> flatMap(Function<T, Result<U>> flatMapper);

            Result<T> flatten();
        }

        interface Filtering<T> extends Operations<T> {
            Result<T> validate(Predicate<T> predicate);

            Result<T> validate(Predicate<T> predicate, String failureMessage);

            Result.WithFailureMessage<T> validateTry(ThrowingPredicate<T> predicate);

            Result<T> reject(Predicate<T> predicate);

            Result<T> reject(Predicate<T> predicate, String failureMessage);

            Result.WithFailureMessage<T> rejectTry(ThrowingPredicate<T> predicate);
        }

        interface SpecialCase<T> extends Operations<T> {
            Result<Nothing> mapEmpty();
        }

        interface SuccessFailureQueries<T> extends Operations<T> {
            boolean isSuccess();

            boolean isFailure();

            boolean isEmpty();
        }

        interface RetrievingValues<T> extends Operations<T> {
            T getOrElse(final T defaultValue);

            T getOrElse(final Supplier<T> defaultValueSupplier);

            T getOrThrow();

            RuntimeException failureValue();
        }


        interface Conditional<T> extends Operations<T> {
            Result<T> or(final Supplier<Result<T>> alternativeIfTrue);

            Result<T> and(final Supplier<Result<T>> next);

            Result<T> orElse(final Supplier<Result<T>> defaultValue);
        }

        interface Equality<T> extends Operations<T> {
            boolean isNotSuccess();

        }

        interface Effects<T> extends Operations<T> {

            /**
             * The Observer: Executes an action on the entire Result state
             * regardless of Success/Failure, and returns 'this' for chaining.
             */
            default Result<T> peek(Consumer<Result<T>> action) {
                action.accept((Result<T>) this);
                return (Result<T>) this;
            }

            Result<T> peekSuccess(Consumer<T> action);

            Result<T> peekFailure(Consumer<RuntimeException> action);

            Result<T> peekEmpty(Runnable action);

        }
    }
}
