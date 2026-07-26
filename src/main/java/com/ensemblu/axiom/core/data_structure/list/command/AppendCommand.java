package com.ensemblu.axiom.core.data_structure.list.command;

import com.ensemblu.axiom.core.data_structure.list.cursor.Path;
import com.ensemblu.axiom.core.data_structure.list.data.ListNode;

public interface AppendCommand {

    static <E> WithPath<E> withNode(ListNode<E> listNode) {
        return withNode(listNode, null);
    }

    static <E> WithPath<E> withNode(ListNode<E> listNode, Object owner) {
        return path -> _value -> () -> {

            if (listNode instanceof ListNode.TransientLeaf<E> tLeaf && tLeaf.isTransient(owner)) {
                tLeaf.values[path.index() & 31] = _value;
                return tLeaf;
            }
            if (listNode instanceof ListNode.TransientBranch<E> tBranch && tBranch.isTransient(owner)) {
                final var idx = path.subIndex();
                tBranch.children[idx] = (idx < tBranch.children.length && tBranch.children[idx] != null)
                        ? AppendCommand.withNode(tBranch.children[idx], owner).atPath(path.descend()).withValue(_value).invoke()
                        : PathCommand.withValue(_value).atShift(path.shift() - 5).invoke();
                return tBranch;
            }

            return switch (listNode) {
                case ListNode.Leaf<E> leaf -> {
                    Object[] nextValues = leaf.values().clone();
                    nextValues[path.index() & 31] = _value;
                    yield (owner != null)
                            ? new ListNode.TransientLeaf<>(owner, nextValues)
                            : new ListNode.Leaf<>(nextValues);
                }
                case ListNode.Branch<E> branch -> {
                    final var idx = path.subIndex();

                    ListNode<E>[] nextChildren = (idx >= branch.children().length)
                            ? java.util.Arrays.copyOf(branch.children(), idx + 1)
                            : branch.children().clone();

                    nextChildren[idx] = (idx < branch.children().length && branch.children()[idx] != null)
                            ? AppendCommand.withNode(branch.children()[idx], owner).atPath(path.descend()).withValue(_value).invoke()
                            : PathCommand.withValue(_value).atShift(path.shift() - 5).invoke();

                    yield (owner != null)
                            ? new ListNode.TransientBranch<>(owner, nextChildren)
                            : new ListNode.Branch<>(nextChildren);
                }
                case ListNode.TransientLeaf<E> tl -> {
                    Object[] nextValues = tl.values.clone();
                    nextValues[path.index() & 31] = _value;
                    yield (owner != null) ? new ListNode.TransientLeaf<>(owner, nextValues) : new ListNode.Leaf<>(nextValues);
                }
                case ListNode.TransientBranch<E> tb -> {
                    final var idx = path.subIndex();

                    ListNode<E>[] nextChildren = (idx >= tb.children.length)
                            ? java.util.Arrays.copyOf(tb.children, idx + 1)
                            : tb.children.clone();

                    nextChildren[idx] = (idx < tb.children.length && tb.children[idx] != null)
                            ? AppendCommand.withNode(tb.children[idx], owner).atPath(path.descend()).withValue(_value).invoke()
                            : PathCommand.withValue(_value).atShift(path.shift() - 5).invoke();

                    yield new ListNode.TransientBranch<>(owner, nextChildren);
                }
            };
        };
    }

    interface WithPath<E> { WithValue<E> atPath(Path path); }
    interface WithValue<E> { Trigger<E> withValue(E value); }
    interface Trigger<E> { ListNode<E> invoke(); }
}