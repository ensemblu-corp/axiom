package com.ensemblu.axiom.core.data_structure.list.command;


import com.ensemblu.axiom.core.data_structure.list.cursor.Path;
import com.ensemblu.axiom.core.data_structure.list.data.ListNode;

public interface FetchCommand {
    @SuppressWarnings("unchecked")
    static <E> WithPath<E> onNode(ListNode<E> listNode) {
        return path -> () -> //
            switch (listNode) {
            case ListNode.Leaf<E> leaf -> (E) leaf.values()[path.index() & 31];
            case ListNode.Branch<E> b -> FetchCommand.onNode(b.children()[path.subIndex()])
                    .atPath(path.descend())
                    .invoke();

            case ListNode.TransientLeaf<E> tl -> (E) tl.values[path.index() & 31];
            case ListNode.TransientBranch<E> tb -> FetchCommand.onNode(tb.children[path.subIndex()])
                    .atPath(path.descend())
                    .invoke();
            };
    }

    interface WithPath<E> {
        Trigger<E> atPath(Path path);
    }

    interface Trigger<E> {
        E invoke();
    }
}