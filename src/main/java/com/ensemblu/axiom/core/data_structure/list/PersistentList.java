package com.ensemblu.axiom.core.data_structure.list;


import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.list.behavior.PersistentListBehavior;
import com.ensemblu.axiom.core.data_structure.list.command.AppendCommand;
import com.ensemblu.axiom.core.data_structure.list.command.FetchCommand;
import com.ensemblu.axiom.core.data_structure.list.command.PathCommand;
import com.ensemblu.axiom.core.data_structure.list.command.UpdateCommand;
import com.ensemblu.axiom.core.data_structure.list.cursor.Path;
import com.ensemblu.axiom.core.data_structure.list.data.LeanVectorState;
import com.ensemblu.axiom.core.data_structure.list.data.ListNode;
import com.ensemblu.axiom.core.io.Effect;
import com.ensemblu.axiom.core.foundation.Dop;
import com.ensemblu.axiom.core.foundation.Nothing;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PersistentList<E> implements PersistentListBehavior<E>  {
    private final LeanVectorState<E> state;

    private PersistentList(LeanVectorState<E> state) {
        this.state = state;
    }

    public static <E> PersistentList<E> empty() {
        return new PersistentList<>(LeanVectorState.empty());
    }

    public static PersistentList<Integer> range(int start, int end) {
        var builder = PersistentList.<Integer>empty().asTransient();

        for (var i = start; i <= end; i++) {
            builder = builder.append(i);
        }

        return builder.freeze();
    }

    @SafeVarargs
    public static <E> PersistentList<E> list(E... elements) {
        if (elements == null || elements.length == 0) return PersistentList.empty();

        var builder = PersistentList.<E>empty().asTransient();

        for (final var element : elements) {
            builder = builder.append(element);
        }

        return builder.freeze();
    }

    public static <E> PersistentList<E> fromJavaList(List<E> list) {
        var builder = PersistentList.<E>empty().asTransient();

        for (final var item : list) {
            builder = builder.append(item);
        }

        return builder.freeze();
    }

    @Override
    public E get(int index) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> null;
            case LeanVectorState.Root<E> r -> {
                if (index < 0 || index >= r.size()) yield null;
                yield FetchCommand.onNode(r.listNode()).atPath(new Path(index, r.shift())).invoke();
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistentList<E> append(E value) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> {
                final var leaf = PathCommand.withValue(value).atShift(0).invoke();
                yield new PersistentList<>(LeanVectorState.ofRoot(1, 5, leaf));
            }
            case LeanVectorState.Root<E> r -> {
                final var owner = r.owner(); //

                if (r.size() >= (1 << r.shift())) {
                    final var newRoot = ListNode.<E>createBranch().withChildren(new ListNode[]{
                            r.listNode(),
                            PathCommand.withValue(value).atShift(r.shift()).invoke()
                    });
                    yield new PersistentList<E>(LeanVectorState.ofRoot(r.size() + 1, r.shift() + 5, newRoot, owner));
                }

                final var nextNode = AppendCommand.withNode(r.listNode(), owner)//
                        .atPath(new Path(r.size(), r.shift()))//
                        .withValue(value)//
                        .invoke();
                yield new PersistentList<>(LeanVectorState.ofRoot(r.size() + 1, r.shift(), nextNode, owner));
            }
        };
    }

    @Override
    public int size() {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> 0;
            case LeanVectorState.Root<E> r -> r.size();
        };
    }

    @Override
    public <R> PersistentList<R> map(Function<E, R> mapper) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> PersistentList.empty();
            case LeanVectorState.Root<E> r -> {
                final var newRootNode = mapNode(r.listNode(), mapper);
                yield new PersistentList<>(LeanVectorState.ofRoot(r.size(), r.shift(), newRootNode));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <R> ListNode<R> mapNode(ListNode<E> listNode, Function<E, R> mapper) {
        return switch (listNode) {
            case ListNode.Leaf<E> leaf -> {
                final var mappedValues = new Object[leaf.values().length];
                for (var i = 0; i < leaf.values().length; i++) {
                    if (leaf.values()[i] != null) {
                        mappedValues[i] = mapper.apply((E) leaf.values()[i]);
                    }
                }
                yield ListNode.<R>createLeaf().withValues(mappedValues);
            }
            case ListNode.TransientLeaf<E> tl -> {
                final var mappedValues = new Object[tl.values.length];
                for (var i = 0; i < tl.values.length; i++) {
                    if (tl.values[i] != null) mappedValues[i] = mapper.apply((E) tl.values[i]);
                }
                yield ListNode.<R>createLeaf().withValues(mappedValues);
            }
            case ListNode.Branch<E> branch -> {
                final var mappedChildren = new ListNode[branch.children().length];
                for (var i = 0; i < branch.children().length; i++) {
                    if (branch.children()[i] != null) {
                        mappedChildren[i] = mapNode(branch.children()[i], mapper);
                    }
                }
                yield ListNode.<R>createBranch().withChildren(mappedChildren);
            }
            case ListNode.TransientBranch<E> tb -> {
                final var mappedChildren = new ListNode[tb.children.length];
                for (var i = 0; i < tb.children.length; i++) {
                    if (tb.children[i] != null) mappedChildren[i] = mapNode(tb.children[i], mapper);
                }
                yield ListNode.<R>createBranch().withChildren(mappedChildren);
            }
        };
    }

    @Override
    public <U> U fold(U identity, BiFunction<U, E, U> accumulator) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> identity;
            case LeanVectorState.Root<E> r -> foldNode(r.listNode(), identity, accumulator);
        };
    }

    @SuppressWarnings("unchecked")
    private <U> U foldNode(ListNode<E> listNode, U acc, BiFunction<U, E, U> accumulator) {
        return switch (listNode) {
            case ListNode.Leaf<E> leaf -> {
                var currentAcc = acc;
                for (final var value : leaf.values()) {
                    if (value != null) currentAcc = accumulator.apply(currentAcc, (E) value);
                }
                yield currentAcc;
            }
            case ListNode.TransientLeaf<E> tl -> {
                var currentAcc = acc;
                for (final var value : tl.values) if (value != null) currentAcc = accumulator.apply(currentAcc, (E) value);
                yield currentAcc;
            }
            case ListNode.Branch<E> branch -> {
                var currentAcc = acc;
                for (final var child : branch.children()) {
                    if (child != null) currentAcc = foldNode(child, currentAcc, accumulator);
                }
                yield currentAcc;
            }
            case ListNode.TransientBranch<E> tb -> {
                var currentAcc = acc;
                for (final var child : tb.children) if (child != null) currentAcc = foldNode(child, currentAcc, accumulator);
                yield currentAcc;
            }
        };
    }

    @Override
    public Effect<Nothing> forEach(Consumer<E> action) {
        if (state instanceof LeanVectorState.Root<E> r) {
            traverse(r.listNode(), action);
        }
        return Effect.empty;

    }

    @SuppressWarnings("unchecked")
    private void traverse(ListNode<E> listNode, Consumer<E> action) {
        switch (listNode) {
            case ListNode.Leaf<E> leaf -> {
                for (final var value : leaf.values()) {
                    if (value != null) action.accept((E) value);
                }
            }
            case ListNode.TransientLeaf<E> tl -> {
                for (final var value : tl.values) if (value != null) action.accept((E) value);
            }
            case ListNode.Branch<E> branch -> {
                for (final var child : branch.children()) {
                    if (child != null) traverse(child, action);
                }
            }
            case ListNode.TransientBranch<E> tb -> {
                for (final var child : tb.children) if (child != null) traverse(child, action);
            }
        }
    }

    @Override
    public Result<E> findFirst(Predicate<E> predicate) {
        return this.foldUntil(
                Result.failure("No element matches the predicate"),
                (acc, element) -> predicate.test(element)
                        ? Accumulator.stop(Result.success(element))
                        : Accumulator.cont(acc)
        );
    }

    @Override
    public <U> U foldUntil(U identity, BiFunction<U, E, Accumulator<U>> accumulator) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> identity;
            case LeanVectorState.Root<E> r -> foldNodeUntil(r.listNode(), Accumulator.cont(identity), accumulator).value();
        };
    }

    @Override
    public <U> U foldUntilIndexed(U identity, TriFunction<U, E, Integer, Accumulator<U>> func) {
        var index = new java.util.concurrent.atomic.AtomicInteger(0);
        return foldUntil(identity, (acc, item) -> func.apply(acc, item, index.getAndIncrement()));
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @SuppressWarnings("unchecked")
    private <U> Accumulator<U> foldNodeUntil(ListNode<E> listNode, Accumulator<U> acc, BiFunction<U, E, Accumulator<U>> func) {
        Accumulator<U> current = acc;

        return switch (listNode) {
            case ListNode.Leaf<E> leaf -> {
                for (final var value : leaf.values()) {
                    if (current.shouldStop()) yield current;
                    if (value != null) current = func.apply(current.value(), (E) value);
                }
                yield current;
            }
            case ListNode.TransientLeaf<E> tl -> {
                for (final var value : tl.values) { //
                    if (current.shouldStop()) yield current;
                    if (value != null) current = func.apply(current.value(), (E) value);
                }
                yield current;
            }
            case ListNode.Branch<E> branch -> {
                for (ListNode<E> child : branch.children()) {
                    if (current.shouldStop()) yield current;
                    if (child != null) current = foldNodeUntil(child, current, func);
                }
                yield current;
            }
            case ListNode.TransientBranch<E> tb -> {
                for (ListNode<E> child : tb.children) { //
                    if (current.shouldStop()) yield current;
                    if (child != null) current = foldNodeUntil(child, current, func);
                }
                yield current;
            }
        };
    }

    @Override
    public Partitioned<E> partition(Predicate<E> predicate) {
        return this.fold(
                new Partitioned<>(PersistentList.empty(), PersistentList.empty()),
                (acc, element) -> predicate.test(element)
                        ? new Partitioned<>(acc.matching().append(element), acc.remaining())
                        : new Partitioned<>(acc.matching(), acc.remaining().append(element))
        );
    }

    @Override
    public Nothing copyInto(Object[] target) {
        if (state instanceof LeanVectorState.Root<E> r) {
            copyNodeInto(r.listNode(), target, new int[]{0});
        }
        return Nothing.INSTANCE;
    }

    private void copyNodeInto(ListNode<E> node, Object[] target, int[] offset) {
        switch (node) {
            case ListNode.Leaf<E> l -> {
                for (final var val : l.values()) {
                    if (val != null) target[offset[0]++] = val;
                }
            }
            case ListNode.TransientLeaf<E> tl -> {
                for (final var val : tl.values) {
                    if (val != null) target[offset[0]++] = val;
                }
            }
            case ListNode.Branch<E> b -> {
                for (ListNode<E> child : b.children()) {
                    if (child != null) copyNodeInto(child, target, offset);
                }
            }
            case ListNode.TransientBranch<E> tb -> {
                for (ListNode<E> child : tb.children) {
                    if (child != null) copyNodeInto(child, target, offset);
                }
            }
        }
    }

    @Override
    public <S> PersistentList<Pair<E, S>> zip(PersistentList<S> other) {
        final var minSize = Math.min(this.size(), other.size());
        var zipped = PersistentList.<Pair<E, S>>empty();

        for (var i = 0; i < minSize; i++) {
            zipped = zipped.append(new Pair<>(this.get(i), other.get(i)));
        }
        return zipped;
    }

    @Override
    public PersistentList<E> update(int index, Function<E, E> mapper) {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> this;
            case LeanVectorState.Root<E> r -> {
                final var owner = r.owner();
                final var currentValue = FetchCommand.onNode(r.listNode()).atPath(new Path(index, r.shift())).invoke();
                final var newValue = mapper.apply(currentValue);

                final var updatedRoot = UpdateCommand.withNode(r.listNode(), owner)//
                        .atPath(new Path(index, r.shift()))//
                        .withValue(newValue)//
                        .invoke();//
                yield new PersistentList<>(LeanVectorState.ofRoot(r.size(), r.shift(), updatedRoot, owner));
            }
        };
    }

    @Override
    public PersistentList<E> slice(int start, int end) {
        if (start < 0 || end > size() || start >= end) return PersistentList.empty();

        return this.foldUntilIndexed(
                PersistentList.<E>empty().asTransient(),
                (acc, element, index) -> {
                    if (index >= start && index < end) {
                        return Accumulator.cont(acc.append(element));
                    }
                    if (index >= end) {
                        return Accumulator.stop(acc);
                    }
                    return Accumulator.cont(acc);
                }
        ).freeze();
    }

    @Override
    public PersistentList<E> take(int n) {
        return slice(0, n);
    }

    @Override
    public PersistentList<E> drop(int n) {
        return slice(n, size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistentList<E> addAll(PersistentList<E> other) {
        return other.fold(this, PersistentList::append);
    }

    @Override
    public PersistentList<E> asTransient() {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> this;
            case LeanVectorState.Root<E> r -> {
                if (r.owner() != null) yield this; //
                yield new PersistentList<>(LeanVectorState.ofRoot(r.size(), r.shift(), r.listNode(), new Object()));
            }
        };
    }

    @Override
    public PersistentList<E> freeze() {
        return switch (state) {
            case LeanVectorState.Empty<E> _ -> this;
            case LeanVectorState.Root<E> r -> {
                if (r.owner() == null) yield this; //
                ListNode<E> frozenListNode = r.listNode().freeze();
                yield new PersistentList<>(LeanVectorState.ofRoot(r.size(), r.shift(), frozenListNode, null));
            }
        };
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersistentList<?> other)) return false;
        if (this.size() != other.size()) return false;

        return this.foldUntil(0, (index, element) ->
             Dop.isEqual(element, other.get(index))
                    ? Accumulator.cont(index + 1)
                    : Accumulator.stop(-1)
        ) != -1;
    }

    @Override
    public int hashCode() {
        if (state instanceof LeanVectorState.Empty) return 1;

        return this.fold(1, (h, element) -> 31 * h + Dop.hashCode(element));
    }

    @Override
    public String toString() {
        if (size() == 0) return "[]";

        final var sb = new StringBuilder("[");
        this.forEach(element -> {
            if (sb.length() > 1) sb.append(", ");
            sb.append(Dop.toString(element)); //
        });
        return sb.append("]").toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistentList<E> shuffle() {
        if (this.size() <= 1) return this;

        final var array = new Object[this.size()];
        this.copyInto(array);

        java.util.Random rnd = new java.util.Random();
        for (var i = array.length - 1; i > 0; i--) {
            final var index = rnd.nextInt(i + 1);
            final var temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }

        var builder = PersistentList.<E>empty().asTransient();
        for (final var obj : array) {
            builder = builder.append((E) obj);
        }
        return builder.freeze();
    }

    public record Accumulator<U>(U value, boolean shouldStop) {
        public static <U> Accumulator<U> cont(U value) {
            return new Accumulator<>(value, false);
        }

        public static <U> Accumulator<U> stop(U value) {
            return new Accumulator<>(value, true);
        }
    }

    public record Partitioned<E>(PersistentList<E> matching, PersistentList<E> remaining) {}

    public record Pair<A, B>(A left, B right) {}
}