package com.ensemblu.axiom.core.data_structure.list.command;


import com.ensemblu.axiom.core.data_structure.list.data.ListNode;

public interface PathCommand {
    static <E> WithShift<E> withValue(E value) {
        return shift -> () -> switch (shift <= 0 ? Depth.GROUND : Depth.SKY) {
            case GROUND -> ListNode.<E>createLeaf().withValues(fillFirst(value));
            case SKY -> ListNode.<E>createBranch().withChildren(new ListNode[]{
                    PathCommand.withValue(value).atShift(shift - 5).invoke()
            });
        };
    }

    private static Object[] fillFirst(Object val) {
        final var arr = new Object[32];
        arr[0] = val;
        return arr;
    }

    enum Depth {GROUND, SKY}

    interface WithShift<E> {
        Trigger<E> atShift(int shift);
    }

    interface Trigger<E> {
        ListNode<E> invoke();
    }
}