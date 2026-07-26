package com.ensemblu.axiom.core.validation.behavior;

import com.ensemblu.axiom.core.validation.If;
import com.ensemblu.axiom.core.validation.If.Condition;
import com.ensemblu.axiom.core.validation.If.Condition.WillCondition.OrGet;
import com.ensemblu.axiom.core.validation.If.GetSoft;
import com.ensemblu.axiom.core.validation.If.SoftCondition;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface CheckFlow {

    interface HardCheckFlow extends CheckFlow {

        interface IfStep<T> extends HardCheckFlow {

            Condition<T> isNull();

            Condition<T> isNonNull();

            Condition<T> is(Predicate<T> condition);

            Condition<T> isNot(Predicate<T> condition);

            Condition<T> isNull(String name);

            Condition<T> isNonNull(String name);

            Condition<T> is(Predicate<T> condition, String errorMessage);

            Condition<T> isNot(Predicate<T> condition, String errorMessage);
        }

        interface ConditionStep<T> extends HardCheckFlow {

            Condition<T> andIs(Predicate<T> condition);

            Condition<T> andIsNot(Predicate<T> condition);

            Condition<T> andIs(Predicate<T> condition, String errorMessage);

            Condition<T> andIsNot(Predicate<T> condition, String errorMessage);

            <U> Condition<T> andOtherObjectIsNotNull(U value, String name);

            <U> Condition<U> andForOtherCondition(Condition<U> condition);

            If.Condition.WillCondition<T> will();
        }

        interface ActionStep<T> extends HardCheckFlow {
            Result<T> getResult();

            <W> OrGet<W> mapTo(Function<T, W> f);

            <W> OrGet<W> flatMapTo(Function<T, Result<W>> f);

            T getValueOrElseThrow(Supplier<RuntimeException> exceptionSupplier);

            Nothing thenApprovedOrElseThrowException();

            <W> Result<String> assertionsElementInSoftMode(Function<GetSoft<T>, SoftCondition<W>> function);
        }

        interface OrElseStep<T> extends HardCheckFlow {
            Result<T> getResult();

            T orThrowException(Supplier<RuntimeException> exceptionSupplier);

            If<T> andAfterThisTransformationCheckIfTransformedObject();
        }
    }

    interface SoftCheckFlow extends CheckFlow {

        interface SoftCheck<T> extends SoftCheckFlow {
            SoftCondition<T> is(Predicate<T> condition, String errorMessage);

            SoftCondition<T> isNot(Predicate<T> condition, String errorMessage);
        }

        interface SoftConditionStep<T> extends SoftCheckFlow {
            <W> SoftCondition<W> mapTo(Function<T, W> f);

            <W> SoftCondition<W> flatMapTo(Function<T, Result<W>> f);

            <U> SoftCondition<T> andForOtherSoftCondition(SoftCondition<U> other);

            SoftCondition<T> andIs(Predicate<T> condition, String errorMessage);

            SoftCondition<T> andIsNot(Predicate<T> condition, String errorMessage);

            PersistentList<Result<T>> getErrors();

            If.SoftCondition.WillCondition<T> will();
        }

        interface SoftResult<T> extends SoftCheckFlow {
            boolean isThereAnyError();

            PersistentList<Result<T>> getErrors();

            T thenGetOrElse(Supplier<T> defaultValue);

            Result<T> thenGetOrErrorMessage();

            Result<String> generateResultErrorIfExists();
        }
    }
}