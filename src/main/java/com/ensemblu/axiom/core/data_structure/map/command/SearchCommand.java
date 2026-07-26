package com.ensemblu.axiom.core.data_structure.map.command;

import com.ensemblu.axiom.core.data_structure.map.cursor.Cursor;
import com.ensemblu.axiom.core.data_structure.map.data.MapNode;

import java.util.Objects;

public interface SearchCommand {

    static <K, V> WithCursor<K, V> onNode(MapNode<K, V> mapNode) {
        return cursor -> () -> search(mapNode, cursor);
    }

    private static <K, V> V search(MapNode<K, V> mapNode, Cursor<K> cursor) {
        return switch (mapNode) {
            case MapNode.Leaf<K, V> leaf -> Objects.equals(leaf.key(), cursor.key()) ? leaf.value() : null;
            case MapNode.TransientLeaf<K, V> tl -> Objects.equals(tl.key, cursor.key()) ? tl.value : null;

            case MapNode.Branch<K, V> branch -> follow(branch.bitmap(), branch.children(), cursor);
            case MapNode.TransientBranch<K, V> tb -> follow(tb.bitmap, tb.children, cursor);
        };
    }

    private static <K, V> V follow(int bitmap, MapNode<K, V>[] children, Cursor<K> cursor) {
        if (cursor.isRoadMissing(bitmap)) return null;

        MapNode<K, V> childAt = children[cursor.calculateIndex(bitmap)];
        return SearchCommand.onNode(childAt)
                .withCursor(cursor.descend())
                .invoke();
    }

    interface WithCursor<K, V> { Trigger<K, V> withCursor(Cursor<K> cursor); }
    interface Trigger<K, V> { V invoke(); }
}