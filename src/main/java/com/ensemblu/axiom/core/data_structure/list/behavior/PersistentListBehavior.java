package com.ensemblu.axiom.core.data_structure.list.behavior;


import com.ensemblu.axiom.api.TargetNavigator;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.io.Effect;
import com.ensemblu.axiom.core.foundation.DataCast;
import com.ensemblu.axiom.core.foundation.Dop;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.function.*;

public interface PersistentListBehavior<E> {

    E get(int index);

    default TargetNavigator targetIndex(int index) {
        return new TargetNavigator() {
            @Override
            public <T> Result<T> execute(DataCast.Protocol protocol) {
                return (index < 0 || index >= size()) //
                        ? Result.failure("Index out of bounds: " + index)//
                        : DataCast.cast(Dop.resolve(get(index)), protocol);
            }
        };
    }

    default TargetNavigator first() {
        return targetIndex(0);
    }

    default TargetNavigator last() {
        return targetIndex(size() - 1);
    }

    PersistentList<E> append(E value);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    <R> PersistentList<R> map(Function<E, R> mapper);

    <U> U fold(U identity, BiFunction<U, E, U> accumulator);

    Effect<Nothing> forEach(Consumer<E> action);

    Result<E> findFirst(Predicate<E> predicate);

    <U> U foldUntil(U identity, BiFunction<U, E, PersistentList.Accumulator<U>> accumulator);

    <U> U foldUntilIndexed(U identity, PersistentList.TriFunction<U, E, Integer, PersistentList.Accumulator<U>> func);

    PersistentList.Partitioned<E> partition(Predicate<E> predicate);

    Nothing copyInto(Object[] target);

    <S> PersistentList<PersistentList.Pair<E, S>> zip(PersistentList<S> other);

    default boolean anyMatch(Predicate<E> predicate) {
        return findFirst(predicate).isSuccess();
    }

    PersistentList<E> update(int index, Function<E, E> mapper);

    PersistentList<E> slice(int start, int end);

    PersistentList<E> take(int n);

    PersistentList<E> drop(int n);

    PersistentList<E> addAll(PersistentList<E> other);

    PersistentList<E> asTransient();

    PersistentList<E> freeze();

    PersistentList<E> shuffle();
}
