package com.ensemblu.axiom.core.validation;

import com.ensemblu.axiom.core.validation.behavior.CheckFlow;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class If<T> implements CheckFlow.HardCheckFlow.IfStep<T> {

    private static final String MUST_NOT_BE_NULL = "must not be null.";
    private static final String MUST_BE_NULL = "must be null.";
    private static final String VALUE_FITS_ALL_REQUIREMENTS = "The object fits all the requirements";

    private final Result<T> ifValue;

    private If(final T value) {
        this.ifValue = Result.success(value);
    }

    private If(final Result<T> value) {
        this.ifValue = value;
    }

    public static <T> If<T> givenObject(final T value) {
        return new If<>(value);
    }

    public static String formatErrorMessageForNullValue(final String name) {
        return formatMessage(name, MUST_NOT_BE_NULL);
    }

    private static String formatMessage(final String argumentName, final String message) {
        return argumentName + " " + message;
    }

    public static <T> GetSoft<T> givenNonNullForSoftValidation(final T value) {
        Objects.requireNonNull(value, If.MUST_NOT_BE_NULL);
        return GetSoft.of(Result.success(value), PersistentList.empty());
    }

    @Override
    public Condition<T> isNull() {
        return isNull("value");
    }

    @Override
    public Condition<T> isNull(final String name) {
        return Condition.of(ifValue, ifValue.validate(Objects::isNull, If.formatMessage(name, If.MUST_BE_NULL)));
    }

    @Override
    public Condition<T> isNonNull() {
        return isNonNull("value");
    }

    @Override
    public Condition<T> isNonNull(final String name) {
        return Condition.of(ifValue, ifValue.validate(Objects::nonNull, If.formatMessage(name, If.MUST_NOT_BE_NULL)));
    }

    @Override
    public Condition<T> is(Predicate<T> condition) {
        return Condition.of(ifValue, ifValue.validate(condition));
    }

    @Override
    public Condition<T> is(final Predicate<T> condition, final String errorMessage) {
        return Condition.of(ifValue, ifValue.validate(condition, errorMessage));
    }

    @Override
    public Condition<T> isNot(Predicate<T> condition) {
        return Condition.of(ifValue, ifValue.reject(condition));
    }


    @Override
    public Condition<T> isNot(final Predicate<T> condition, final String errorMessage) {
        return Condition.of(ifValue, ifValue.reject(condition, errorMessage));
    }

    public final static class Condition<T> implements CheckFlow.HardCheckFlow.ConditionStep<T> {
        private final Result<T> originalValue;
        private final Result<T> conditionResult;

        public Condition(final Result<T> ifValue, final Result<T> obj) {
            this.originalValue = ifValue;
            this.conditionResult = obj;
        }

        private static <T> Condition<T> of(final Result<T> ifValue, final Result<T> obj) {
            return new Condition<>(ifValue, obj);
        }

        @Override
        public <U> Condition<U> andForOtherCondition(final Condition<U> condition) {
            return Condition.of(originalValue.flatMap(obj -> condition.originalValue),
                    conditionResult.flatMap(obj -> condition.conditionResult));
        }

        @Override
        public <U> Condition<T> andOtherObjectIsNotNull(final U value, final String name) {
            return this.andIsNot(ignore -> Objects.isNull(value), If.formatMessage(name, If.MUST_NOT_BE_NULL));
        }

        @Override
        public Condition<T> andIs(Predicate<T> condition) {
            return Condition.of(originalValue, conditionResult.validate(condition));
        }

        @Override
        public Condition<T> andIs(final Predicate<T> condition, final String errorMessage) {
            return Condition.of(originalValue, conditionResult.validate(condition, errorMessage));
        }

        @Override
        public Condition<T> andIsNot(Predicate<T> condition) {
            return Condition.of(originalValue, conditionResult.reject(condition));
        }

        @Override
        public Condition<T> andIsNot(final Predicate<T> condition, final String errorMessage) {
            return Condition.of(originalValue, conditionResult.reject(condition, errorMessage));
        }

        @Override
        public WillCondition<T> will() {
            return new WillCondition<>(originalValue, conditionResult);
        }

        public final static class WillCondition<T> implements CheckFlow.HardCheckFlow.ActionStep<T> {
            private final Result<T> originalValue;
            private final Result<T> conditionResult;

            public WillCondition(final Result<T> originalValue, final Result<T> conditionResult) {
                this.originalValue = originalValue;
                this.conditionResult = conditionResult;
            }

            @Override
            public Result<T> getResult() {
                return conditionResult;
            }

            @Override
            public <W> Result<String> assertionsElementInSoftMode(
                    final Function<GetSoft<T>, SoftCondition<W>> function) {

                return this.getResult()//
                        .flatMap(elemenet -> {//
                            Objects.requireNonNull(elemenet, If.MUST_NOT_BE_NULL);
                            final GetSoft<T> softIntial = GetSoft.of(Result.success(elemenet), PersistentList.empty());

                            return function.apply(softIntial)//
                                    .will()//
                                    .generateResultErrorIfExists();
                        });// ;
            }

            @Override
            public <W> OrGet<W> mapTo(final Function<T, W> f) {
                return new OrGet<>(this.conditionResult.map(f));
            }

            @Override
            public <W> OrGet<W> flatMapTo(final Function<T, Result<W>> f) {
                return new OrGet<>(this.conditionResult.flatMap(f));
            }

            @Override
            public T getValueOrElseThrow(final Supplier<RuntimeException> defaultValue) {
                if (conditionResult.isFailure())
                    throw defaultValue.get();
                return originalValue.getOrThrow();

            }

            @Override
            public Nothing thenApprovedOrElseThrowException() {
                if (conditionResult.isFailure())
                    throw conditionResult.failureValue();

                return Nothing.INSTANCE;
            }

            public final static class OrGet<T> implements CheckFlow.HardCheckFlow.OrElseStep<T> {
                private final Result<T> value;

                public OrGet(final Result<T> value) {
                    this.value = value;
                }

                public T orGet(final Supplier<T> defaultValue) {
                    return value.getOrElse(defaultValue);
                }

                @Override
                public T orThrowException(final Supplier<RuntimeException> defaultValue) {
                    if (value.isFailure())
                        throw defaultValue.get();
                    return value.getOrThrow();
                }

                @Override
                public Result<T> getResult() {
                    return value;
                }

                @Override
                public If<T> andAfterThisTransformationCheckIfTransformedObject() {
                    return new If<>(value);
                }
            }
        }
    }

    public final static class GetSoft<T> implements CheckFlow.SoftCheckFlow.SoftCheck<T> {
        private final Result<T> origin;
        private final PersistentList<Result<T>> errors;

        private GetSoft(Result<T> origin, PersistentList<Result<T>> errors) {
            this.origin = origin;
            this.errors = errors;
        }

        static <T> GetSoft<T> of(Result<T> origin, PersistentList<Result<T>> errors) {
            return new GetSoft<>(origin, errors);
        }

        private SoftCondition<T> check(Result<T> res) {
            return SoftCondition.of(origin, res.isFailure() ? errors.append(res) : errors);
        }

        @Override
        public SoftCondition<T> is(Predicate<T> p, String msg) {
            return check(origin.validate(p, msg));
        }

        @Override
        public SoftCondition<T> isNot(Predicate<T> p, String msg) {
            return check(origin.reject(p, msg));
        }
    }

    public final static class SoftCondition<T> implements CheckFlow.SoftCheckFlow.SoftConditionStep<T> {
        private final Result<T> origin;
        private final PersistentList<Result<T>> errors;

        private SoftCondition(Result<T> origin, PersistentList<Result<T>> errors) {
            this.origin = origin;
            this.errors = errors;
        }

        static <T> SoftCondition<T> of(Result<T> origin, PersistentList<Result<T>> errors) {
            return new SoftCondition<>(origin, errors);
        }

        @Override
        public SoftCondition<T> andIs(Predicate<T> condition, String errorMessage) {
            final var r = origin.validate(condition, errorMessage);
            return r.isFailure() ? SoftCondition.of(origin, errors.append(r)) : this;
        }

        @Override
        public SoftCondition<T> andIsNot(Predicate<T> condition, String errorMessage) {
            final var r = origin.reject(condition, errorMessage);
            return r.isFailure() ? SoftCondition.of(origin, errors.append(r)) : this;
        }

        @Override
        public PersistentList<Result<T>> getErrors() {
            return errors;
        }

        @Override
        public <W> SoftCondition<W> mapTo(Function<T, W> f) {
            final var mapped = origin.map(f);
            if (mapped.isFailure()) throw mapped.failureValue();

            PersistentList<Result<W>> mappedErrors = errors.map(Result::castFailure);

            return SoftCondition.of(mapped, mappedErrors);
        }

        @Override
        public <W> SoftCondition<W> flatMapTo(Function<T, Result<W>> f) {
            final var mapped = origin.flatMap(f);
            if (mapped.isNotSuccess()) {
                throw new IllegalStateException("Soft Validation pipeline broke: flatMapTo produced a non-success state.");
            }

            PersistentList<Result<W>> mappedErrors = errors.map(Result::castFailure);

            return SoftCondition.of(mapped, mappedErrors);
        }

        @Override
        public <U> SoftCondition<T> andForOtherSoftCondition(SoftCondition<U> other) {
           final var  merged = //
                    other.getErrors().fold(//
                    this.errors,//
                    (acc, error) -> //
                            acc.append(error.castFailure()));//

            return of(origin, merged);
        }

        @Override
        public WillCondition<T> will() {
            return new WillCondition<>(origin, errors);
        }


        public final static class WillCondition<T> implements CheckFlow.SoftCheckFlow.SoftResult<T> {
            private final Result<T> origin;
            private final PersistentList<Result<T>> errors;

            public WillCondition(Result<T> origin, PersistentList<Result<T>> errors) {
                this.origin = origin;
                this.errors = errors;
            }

            @Override
            public boolean isThereAnyError() {
                return !errors.isEmpty();
            }

            @Override
            public PersistentList<Result<T>> getErrors() {
                return errors;
            }

            @Override
            public T thenGetOrElse(Supplier<T> s) {
                return origin.getOrElse(s);
            }

            @Override
            public Result<String> generateResultErrorIfExists() {
                if (errors.isEmpty()) return Result.success(VALUE_FITS_ALL_REQUIREMENTS);

                final var sb = new StringBuilder();
                final var count = errors.size();

                sb.append(count == 1 ? "Breach detected:\n" : count + " Breaches detected:\n");

                errors.forEach(err -> {
                    sb.append(" - ").append(err.failureValue().getMessage()).append("\n");
                }).run();

                return Result.failure(sb.toString().trim());
            }

            @Override
            public Result<T> thenGetOrErrorMessage() {
                return isThereAnyError() ? generateResultErrorIfExists().castFailure() : origin;
            }
        }
    }
}
