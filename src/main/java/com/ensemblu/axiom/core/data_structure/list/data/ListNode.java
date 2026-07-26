package com.ensemblu.axiom.core.data_structure.list.data;

public sealed interface ListNode<E> {
    static <E> WithValues<E> createLeaf() {
        return Leaf::new;
    }

    static <E> WithChildren<E> createBranch() {
        return Branch::new;
    }

    interface WithValues<E> {
        ListNode<E> withValues(Object[] values);
    }

    interface WithChildren<E> {
        ListNode<E> withChildren(ListNode<E>[] children);
    }

    record Leaf<E>(Object[] values) implements ListNode<E> {
    }

    record Branch<E>(ListNode<E>[] children) implements ListNode<E> {
    }

    default boolean isTransient(Object owner) {
        return false;
    }

    final class TransientLeaf<E> implements ListNode<E> {
        public final Object[] values;
        public final Object owner;

        public TransientLeaf(Object owner, Object[] values) {
            this.owner = owner;
            this.values = values;
        }

        @Override public boolean isTransient(Object owner) {
            return this.owner == owner;
        }
    }

    final class TransientBranch<E> implements ListNode<E> {
        public final ListNode<E>[] children;
        public final Object owner;

        public TransientBranch(Object owner, ListNode<E>[] children) {
            this.owner = owner;
            this.children = children;
        }

        @Override public boolean isTransient(Object owner) {
            return this.owner == owner;
        }
    }

    default ListNode<E> freeze() {
        return switch (this) {
            case Leaf<E> l -> l; //
            case Branch<E> b -> b; //
            case TransientLeaf<E> tl -> new Leaf<>(tl.values);
            case TransientBranch<E> tb -> {
                for (int i = 0; i < tb.children.length; i++) {
                    if (tb.children[i] != null) tb.children[i] = tb.children[i].freeze();
                }
                yield new Branch<>(tb.children);
            }
        };
    }
}