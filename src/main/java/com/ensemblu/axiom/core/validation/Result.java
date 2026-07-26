package com.ensemblu.axiom.core.validation;

import com.ensemblu.axiom.core.function.ThrowingRunnable;
import com.ensemblu.axiom.core.validation.behavior.ResultBehavior;
import com.ensemblu.axiom.core.function.ConsoleOutputEffect;
import com.ensemblu.axiom.core.function.ThrowingPredicate;
import com.ensemblu.axiom.core.function.ThrowingSupplier;
import com.ensemblu.axiom.core.foundation.Dop;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public final class Result<T> implements //
        ResultBehavior.Operations.Filtering<T>, //
        ResultBehavior.Operations.Mapping<T>, //
        ResultBehavior.Operations.SpecialCase<T>, //
        ResultBehavior.Operations.SuccessFailureQueries<T>, //
        ResultBehavior.Operations.RetrievingValues<T>, //
        ResultBehavior.Operations.ErrorHandling<T>, //
        ResultBehavior.Operations.Conditional<T>, //
        ResultBehavior.Operations.Equality<T>, //
        ResultBehavior.Operations.Effects<T>,//
        ConsoleOutputEffect {


    private final State<T> state;

    private Result(State<T> state) {
        this.state = state;
    }

    public static <T> Result<T> ofNullable(T value) {
        return value == null ? Result.empty() : Result.success(value);
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(new State.Success<>(value));
    }

    public static <T> Result<T> empty() {
        return new Result<>(new State.Empty<>());
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(new State.Failure<>(new RuntimeException(message)));
    }

    public static <T> Result<T> failure(RuntimeException exception) {
        return new Result<>(new State.Failure<>(exception));
    }

    public static <T> Result<T> failure(String context, Throwable e) {
        String messagePart = (e.getMessage() != null) ? " | error message: " + e.getMessage() : "none";
        String causePart = (e.getCause() != null) ? " | error cause: " + e.getCause().getMessage() : "none";

        String finalMsg = String.format("AXIOM BREACH | %s\n%s\n%s", context, messagePart, causePart);

        return new Result<>(new State.Failure<>(new RuntimeException(finalMsg, e)));
    }

    public static <T> Result<T> of(T value) {
        return value == null ? empty() : success(value);
    }

    public static <T> Result<T> of(T value, String failureMessage) {
        return value == null ? failure(failureMessage) : success(value);
    }

    public static <T> Result<T> of(ThrowingSupplier<T> supplier) {
        try {
            return of(supplier.get());
        }
        catch ( RuntimeException e) {
            return failure(e);
        }
        catch (Exception e) {
            return failure("Axiom Breach Detected:\n" + e);
        }
    }

    private static <T> Result<T> breach(String context, Exception e) {
        return failure(String.format("AXIOM BREACH | %s\nRAW ERROR: %s", context, e.getMessage()));
    }

    public static Result<Nothing> of(ThrowingRunnable action) {
        try {
            action.run();
            return Result.success(Nothing.INSTANCE);
        }    catch ( RuntimeException e) {
            return failure(e);
        }
        catch (Exception e) {
            return failure("Axiom Breach Detected:\n" + e);
        }
    }

    public static <A, B> Function<Result<A>, Result<B>> lift(Function<A, B> mapper) {
        return result -> result == null ? failure("Input Result was null") : result.map(mapper);
    }

    public static <A, B, C> Function<Result<A>, Function<Result<B>, Result<C>>> lift2(Function<A, Function<B, C>> combiner) {
        return first -> second -> first.map(combiner).flatMap(second::map);
    }

    public static <A, B, C, D> Function<Result<A>, Function<Result<B>, Function<Result<C>, Result<D>>>> lift3(
            Function<A, Function<B, Function<C, D>>> combiner) {
        return first -> second -> third -> lift2(combiner).apply(first).apply(second).flatMap(third::map);
    }

    public State<T> state() {
        return state;
    }

    public Result<Result<T>> lift() {
        return success(this);
    }

    public Result<Result<T>> mapToNested() {
        return this.map(i -> this);
    }

    public String getFailureMessageOrDefault() {
        return switch (state) {
            case State.Failure<T> f -> f.exception().getMessage();
            default -> "state of Result value is successful";
        };
    }

    @Override
    public String toString() {
        return switch (state) {
            case State.Success<T> v -> "✅ Success(" + v.value + ")";
            case State.Failure<T> f -> "❌ Failure: " + f.exception().getMessage();
            case State.Empty<T> _ -> "⚪ Empty";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Result<?> other)) return false;

        return switch (this.state) {
            case State.Success(var v1) -> other.state instanceof State.Success(var v2) && Dop.isEqual(v1, v2);
            case State.Failure(var f1) ->//
                    other.state instanceof State.Failure(var f2) &&//
                            f1.getClass().equals(f2.getClass()) && //
                            Objects.equals(f1.getMessage(), f2.getMessage());//
            case State.Empty() -> other.state instanceof State.Empty;
        };
    }

    @Override
    public int hashCode() {
        return switch (state) {
            case State.Success<T> s -> Dop.hashCode(s.value());
            case State.Failure<T> f -> Objects.hashCode(f.exception().getMessage());
            case State.Empty<T> _ -> 0;
        };
    }

    @Override
    public Result<T> or(Supplier<Result<T>> alternativeIfTrue) {
        return switch (state) {
            case State.Success<T> _ -> this;
            default -> alternativeIfTrue.get();
        };
    }

    @Override
    public Result<T> and(Supplier<Result<T>> next) {
        return switch (state) {
            case State.Success<T> _ -> next.get();
            default -> this;
        };
    }

    @Override
    public Result<T> orElse(Supplier<Result<T>> defaultValue) {
        return switch (state) {
            case State.Success<T> _ -> this;
            default -> defaultValue.get();
        };
    }

    @Override
    public boolean isNotSuccess() {
        return !(state instanceof State.Success);
    }

    @Override
    public Result<T> mapFailure(Function<RuntimeException, String> messageMapper) {
        if (isFailure()) {
            return Result.failure(messageMapper.apply(failureValue()));
        }
        return this;
    }


    @Override
    public Result<T> mapError(Function<RuntimeException, RuntimeException> messageMapper) {
        if (isFailure()) {
            return Result.failure(messageMapper.apply(failureValue()));
        }
        return this;
    }

    @Override
    public <F> Result<F> castFailure() {
        return switch (state) {
            case State.Failure<T> f -> failure(f.exception());
            case State.Empty<T> _ -> empty();
            case State.Success<T> _ -> throw new IllegalStateException("Cannot map failure of a Success state");
        };
    }

    @Override
    public T getOrCreateFailureInstanceWithMessage(Function<String, T> failureHandler) {
        return switch (state) {
            case State.Success<T> s -> s.value();
            case State.Failure<T> f -> failureHandler.apply(f.exception().getMessage());
            case State.Empty<T> _ -> failureHandler.apply("Result is empty");
        };
    }

    @Override
    public Result<T> validate(Predicate<T> predicate) {
        return validate(predicate, "Predicate validation failed");
    }

    @Override
    public Result<T> validate(Predicate<T> predicate, String failureMessage) {
        return switch (state) {
            case State.Success<T> s -> predicate.test(s.value()) ? this : failure(failureMessage);
            default -> this;
        };
    }

    @Override
    public WithFailureMessage<T> validateTry(ThrowingPredicate<T> predicate) {
        return failureMessage -> nameMapper -> {
            try {
                return switch (state) {
                    case State.Success<T> s -> predicate.test(s.value()) ? this : failure(failureMessage); //
                    default -> this;
                };
            }
            catch ( RuntimeException e) {
                return failure(nameMapper.apply(e));
            }
            catch (Exception e) {
                return failure("Axiom Breach Detected:\n" + e);
            }
        };
    }

    @Override
    public Result<T> reject(Predicate<T> predicate) {
        return reject(predicate, "Predicate rejection failed");
    }

    @Override
    public Result<T> reject(Predicate<T> predicate, String failureMessage) {
        return switch (state) {
            case State.Success<T> s -> !predicate.test(s.value()) ? this : failure(failureMessage);
            default -> this;
        };
    }

    @Override
    public WithFailureMessage<T> rejectTry(ThrowingPredicate<T> predicate) {
        return failureMessage -> nameMapper -> {
            try {
                return switch (state) {
                    case State.Success<T> s ->
                            (!predicate.test(s.value())) ? this : failure(failureMessage); //
                    default -> this;
                };
            }
            catch ( RuntimeException e) {
                return failure(nameMapper.apply(e));
            }
            catch (Exception e) {
                return failure("Axiom Breach Detected:\n" + e);
            }
        };
    }

    @Override
    public <U> WithNamingThrowingPredicate<U> mapTry(Function<T, U> mapper) {
        return nameMapper -> {
            try {
                return switch (state) {
                    case State.Success<T> s -> success(mapper.apply(s.value()));
                    case State.Failure<T> f -> failure(f.exception());
                    case State.Empty<T> _ -> empty();
                };
            }
            catch ( RuntimeException e) {
                return failure(nameMapper.apply(e));
            }
            catch (Exception e) {
                return failure("Axiom Breach Detected:\n" + e);
            }
        };

    }

    @Override
    public <U> WithNamingThrowingPredicate<U> flatMapTry(Function<T, Result<U>> flatMapper) {
        return nameMapper -> {
            try {
                return switch (state) {
                    case State.Success<T> s -> flatMapper.apply(s.value());
                    case State.Failure<T> f -> failure(f.exception());
                    case State.Empty<T> _ -> empty();
                };
            }
            catch ( RuntimeException e) {
                return failure(nameMapper.apply(e));
            }
            catch (Exception e) {
                return failure("Axiom Breach Detected:\n" + e);
            }
        };
    }

    @Override
    public <U> Result<U> map(Function<T, U> mapper) {
        return switch (state) {
            case State.Success<T> s -> success(mapper.apply(s.value()));
            case State.Failure<T> f -> failure(f.exception());
            case State.Empty<T> _ -> empty();
        };
    }

    @Override
    public <U> Result<U> flatMap(Function<T, Result<U>> flatMapper) {
        return switch (state) {
            case State.Success<T> s -> flatMapper.apply(s.value());
            case State.Failure<T> f -> failure(f.exception());
            case State.Empty<T> _ -> empty();
        };
    }


    @Override
    @SuppressWarnings("unchecked")
    public Result<T> flatten() {
        return switch (state) {
            case State.Success<T> s when s.value() instanceof Result<?> nested -> (Result<T>)nested;
            case State.Success<T> _ ->  this;
            case State.Failure<T> _ -> castFailure();
            case State.Empty<T> _ -> empty();
        };
    }

    @Override
    public T getOrElse(T defaultValue) {
        return switch (state) {
            case State.Success<T> s -> s.value();
            default -> defaultValue;
        };
    }

    @Override
    public T getOrElse(Supplier<T> defaultValueSupplier) {
        return switch (state) {
            case State.Success<T> s -> s.value();
            default -> defaultValueSupplier.get();
        };
    }

    @Override
    public T getOrThrow() {
        return switch (state) {
            case State.Success<T> s -> s.value();
            case State.Failure<T> f -> throw f.exception();
            case State.Empty<T> _ -> throw new RuntimeException("The state of Result is empty!");
        };
    }

    @Override
    public RuntimeException failureValue() {
        return switch (state) {
            case State.Failure<T> f -> f.exception();
            default -> throw new IllegalStateException("Called failureValue() on a non-failure Result: " + state);
        };
    }

    @Override
    public Result<Nothing> mapEmpty() {
        return switch (state) {
            case State.Success<T> _ -> success(Nothing.INSTANCE);
            case State.Failure<T> f -> failure(f.exception());
            case State.Empty<T> _ -> empty();
        };
    }

    @Override
    public boolean isSuccess() {
        return state instanceof State.Success;
    }

    @Override
    public boolean isFailure() {
        return state instanceof State.Failure;
    }

    @Override
    public boolean isEmpty() {
        return state instanceof State.Empty;
    }

    @Override
    public Result<T> recover(Function<RuntimeException, T> recoveryLogic) {
        return switch (state) {
            case State.Failure<T> f -> success(recoveryLogic.apply(f.exception()));
            default -> this;
        };
    }

    @Override
    public Result<T> recoverWith(Supplier<Result<T>> fallback) {
        return isSuccess() ? this : fallback.get();
    }

    @Override
    public Result<T> peekSuccess(Consumer<T> action) {
        return switch (state) {
            case State.Success<T> s -> {
                action.accept(s.value());
                yield this;
            }
            default -> this;
        };
    }

    @Override
    public Result<T> peekFailure(Consumer<RuntimeException> action) {
        return switch (state) {
            case State.Failure<T> f -> {
                action.accept(f.exception());
                yield this;
            }
            default -> this;
        };
    }

    @Override
    public Result<T> peekEmpty(Runnable action) {
        return switch (state) {
            case State.Empty<T> _ -> {
                action.run();
                yield this;
            }
            default -> this;
        };
    }

    public sealed interface State<T> {
        record Success<T>(T value) implements State<T> {}

        record Failure<T>(RuntimeException exception) implements State<T> {}

        record Empty<T>() implements State<T> {}
    }

    public interface WithFailureMessage<T> {
        WithNamingThrowingPredicate<T> withFailureMessage(final String failureMessage);
    }

    public interface WithNamingThrowingPredicate<U> {
        Result<U> nameThrowingPredicate(Function<Exception, String> nameMapper);
    }
}