package com.ensemblu.axiom.core.data_structure.list.data;

public sealed interface LeanVectorState<E> {
    static <E> LeanVectorState<E> empty() {
        return new Empty<>();
    }

    static <E> LeanVectorState<E> ofRoot(int size, int shift, ListNode<E> listNode){
        return new Root<>(size, shift, listNode, null);
    }

    static <E> LeanVectorState<E> ofRoot(int size, int shift, ListNode<E> listNode, Object owner) {
        return new Root<>(size, shift, listNode, owner);
    }

    record Empty<E>() implements LeanVectorState<E> {}

    record Root<E>(int size, int shift, ListNode<E> listNode, Object owner) implements LeanVectorState<E> {}
}