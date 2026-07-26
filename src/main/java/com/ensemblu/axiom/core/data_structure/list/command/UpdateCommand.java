package com.ensemblu.axiom.core.data_structure.list.command;

import com.ensemblu.axiom.core.data_structure.list.cursor.Path;
import com.ensemblu.axiom.core.data_structure.list.data.ListNode;

public final class UpdateCommand<E> {
    private final ListNode<E> listNode;
    private final Object owner;
    private Path path;
    private E value;

    private UpdateCommand(ListNode<E> listNode, Object owner) {
        this.listNode = listNode;
        this.owner = owner;
    }


    public static <E> UpdateCommand<E> withNode(ListNode<E> listNode) {
        return withNode(listNode, null);
    }

    public static <E> UpdateCommand<E> withNode(ListNode<E> listNode, Object owner) {
        return new UpdateCommand<>(listNode, owner);
    }

    public UpdateCommand<E> atPath(Path path) { this.path = path; return this; }
    public UpdateCommand<E> withValue(E value) { this.value = value; return this; }

    public ListNode<E> invoke() {
        return update(listNode, path.shift(), path.index(), value);
    }

    private ListNode<E> update(ListNode<E> current, int shift, int index, E val) {
        if (current.isTransient(owner)) {
            if (current instanceof ListNode.TransientLeaf<E> tl) {
                tl.values[index & 31] = val;
                return tl;
            }
            if (current instanceof ListNode.TransientBranch<E> tb) {
                final var subIndex = (index >>> shift) & 31;
                tb.children[subIndex] = update(tb.children[subIndex], shift - 5, index, val);
                return tb;
            }
        }

        return switch (current) {
            case ListNode.Leaf<E> leaf -> {
                final var newValues = leaf.values().clone();
                newValues[index & 31] = val;
                yield (owner != null) ? new ListNode.TransientLeaf<>(owner, newValues) : new ListNode.Leaf<>(newValues);
            }
            case ListNode.Branch<E> branch -> {
                final var newChildren = branch.children().clone();
                final var subIdx = (index >>> shift) & 31;
                newChildren[subIdx] = update(branch.children()[subIdx], shift - 5, index, val);
                yield (owner != null) ? new ListNode.TransientBranch<>(owner, newChildren) : new ListNode.Branch<>(newChildren);
            }
            case ListNode.TransientLeaf<E> tl -> {
                final var newValues = tl.values.clone();
                newValues[index & 31] = val;
                yield (owner != null) ? new ListNode.TransientLeaf<>(owner, newValues) : new ListNode.Leaf<>(newValues);
            }
            case ListNode.TransientBranch<E> tb -> {
                final var newChildren = tb.children.clone();
                final var subIdx = (index >>> shift) & 31;
                newChildren[subIdx] = update(tb.children[subIdx], shift - 5, index, val);
                yield (owner != null) ? new ListNode.TransientBranch<>(owner, newChildren) : new ListNode.Branch<>(newChildren);
            }
        };
    }
}