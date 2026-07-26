package com.ensemblu.axiom.core.data_structure.map.command;

import com.ensemblu.axiom.core.data_structure.map.cursor.Cursor;
import com.ensemblu.axiom.core.data_structure.map.data.MapNode;

import java.util.Objects;
public interface RemoveCommand {

    static <K, V> WithCursor<K, V> onNode(MapNode<K, V> mapNode) {
        return onNode(mapNode, null);
    }

    static <K, V> WithCursor<K, V> onNode(MapNode<K, V> mapNode, Object owner) {
        return cursor -> () -> remove(mapNode, cursor, owner);
    }

    private static <K, V> MapNode<K, V> remove(MapNode<K, V> mapNode, Cursor<K> cursor, Object owner) {
        if (mapNode instanceof MapNode.TransientBranch<K, V> tb && tb.isTransient(owner)) {
            return transientRemove(tb, cursor, owner);
        }

        return switch (mapNode) {
            case MapNode.Leaf<K, V> leaf -> Objects.equals(leaf.key(), cursor.key()) ? null : leaf;
            case MapNode.TransientLeaf<K, V> tl -> Objects.equals(tl.key, cursor.key()) ? null : tl;

            case MapNode.Branch<K, V> branch -> {
                if (cursor.isRoadMissing(branch.bitmap())) yield branch;

                int idx = cursor.calculateIndex(branch.bitmap());
                MapNode<K, V> updatedChild = remove(branch.children()[idx], cursor.descend(), owner);

                yield updatedChild == branch.children()[idx] ? branch :
                        updatedChild == null ? shrink(branch.bitmap(), branch.children(), cursor, owner)
                        : update(branch.bitmap(), branch.children(), idx, updatedChild, owner);
            }
            case MapNode.TransientBranch<K, V> tb -> {
                if (cursor.isRoadMissing(tb.bitmap)) yield tb;
                int idx = cursor.calculateIndex(tb.bitmap);
                MapNode<K, V> updatedChild = remove(tb.children[idx], cursor.descend(), owner);

                yield updatedChild == tb.children[idx] ? tb :
                        updatedChild == null ? shrink(tb.bitmap, tb.children, cursor, owner)
                        : update(tb.bitmap, tb.children, idx, updatedChild, owner);
            }
        };
    }

    private static <K, V> MapNode<K, V> transientRemove(MapNode.TransientBranch<K, V> tb, Cursor<K> c, Object owner) {
        if (c.isRoadMissing(tb.bitmap)) return tb;

        final var idx = c.calculateIndex(tb.bitmap);
        MapNode<K, V> updatedChild = remove(tb.children[idx], c.descend(), owner);

        if (updatedChild == tb.children[idx]) return tb;

        if (updatedChild == null) {
            MapNode<K, V> result = shrink(tb.bitmap, tb.children, c, owner);
            if (result instanceof MapNode.TransientBranch<K, V> nextTb) {
                tb.bitmap = nextTb.bitmap;
                tb.children = nextTb.children;
                return tb;
            }
            return result;
        }

        tb.children[idx] = updatedChild;
        return tb;
    }

    private static <K, V> MapNode<K, V> update(int bitmap, MapNode<K, V>[] children, int idx, MapNode<K, V> child, Object owner) {
        MapNode<K, V>[] nextChildren = children.clone();
        nextChildren[idx] = child;
        return (owner != null) ? new MapNode.TransientBranch<>(owner, bitmap, nextChildren)
                : new MapNode.Branch<>(bitmap, nextChildren);
    }

    private static <K, V> MapNode<K, V> shrink(int bitmap, MapNode<K, V>[] children, Cursor<K> c, Object owner) {
        final var newBitmap = bitmap ^ c.bit();
        if (newBitmap == 0) return null;

        MapNode<K, V>[] nextChildren = new MapNode[children.length - 1];
        final var idxToRemove = c.calculateIndex(bitmap);

        System.arraycopy(children, 0, nextChildren, 0, idxToRemove);
        System.arraycopy(children, idxToRemove + 1, nextChildren, idxToRemove, nextChildren.length - idxToRemove);

        if (nextChildren.length == 1 && (nextChildren[0] instanceof MapNode.Leaf || nextChildren[0] instanceof MapNode.TransientLeaf)) {
            return nextChildren[0];
        }

        return (owner != null) ? new MapNode.TransientBranch<>(owner, newBitmap, nextChildren)
                : new MapNode.Branch<>(newBitmap, nextChildren);
    }

    interface WithCursor<K, V> { Trigger<K, V> withCursor(Cursor<K> cursor); }
    interface Trigger<K, V> { MapNode<K, V> invoke(); }
}