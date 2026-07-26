package com.ensemblu.axiom.core.data_structure.map.command;

import com.ensemblu.axiom.core.data_structure.map.cursor.Cursor;
import com.ensemblu.axiom.core.data_structure.map.data.MapNode;

import java.util.Objects;

public interface InsertCommand {

    static <K, V> InsertCommand.WithCursor<K, V> withNode(MapNode<K, V> mapNode) {
        return extracted(mapNode, null);
    }

    static <K, V> InsertCommand.WithCursor<K, V> withNode(MapNode<K, V> mapNode, Object owner) {
        return extracted(mapNode, owner);
    }

    private static <K, V> InsertCommand.WithCursor<K, V> extracted(MapNode<K, V> mapNode, Object owner) {
        return cursor -> _value -> () -> {
            if (mapNode instanceof MapNode.TransientBranch<K, V> tb && tb.isTransient(owner)) {
                return transientUpdate(tb, cursor, _value, owner);
            }
            if (mapNode instanceof MapNode.TransientLeaf<K, V> tl && tl.isTransient(owner)) {
                if (Objects.equals(tl.key, cursor.key())) {
                    tl.value = _value; // In-place value swap
                    return tl;
                }
            }

            return switch (mapNode) {
                case MapNode.Leaf<K, V> leaf -> {
                    if (Objects.equals(leaf.key(), cursor.key())) {
                        yield (owner != null)
                                ? new MapNode.TransientLeaf<>(owner, cursor.hash(), cursor.key(), _value)
                                : new MapNode.Leaf<>(cursor.hash(), cursor.key(), _value);
                    }
                    yield split(leaf, cursor, _value, owner);
                }
                case MapNode.Branch<K, V> branch -> cursor.isRoadMissing(branch.bitmap())
                        ? expand(branch, cursor, _value, owner)
                        : update(branch, cursor, _value, owner);

                case MapNode.TransientLeaf<K, V> tl -> {
                    if (Objects.equals(tl.key, cursor.key())) {
                        yield (owner != null)
                                ? new MapNode.TransientLeaf<>(owner, tl.hash, tl.key, _value)
                                : new MapNode.Leaf<>(tl.hash, tl.key, _value);
                    }
                    yield split(new MapNode.Leaf<>(tl.hash, tl.key, tl.value), cursor, _value, owner);
                }
                case MapNode.TransientBranch<K, V> tb -> cursor.isRoadMissing(tb.bitmap)
                        ? expand(new MapNode.Branch<>(tb.bitmap, tb.children), cursor, _value, owner)
                        : update(new MapNode.Branch<>(tb.bitmap, tb.children), cursor, _value, owner);
            };
        };
    }

    private static <K, V> MapNode<K, V> transientUpdate(MapNode.TransientBranch<K, V> tb, Cursor<K> c, V val, Object owner) {
        if (c.isRoadMissing(tb.bitmap)) {
            final var idx = c.calculateIndex(tb.bitmap);
            MapNode<K, V>[] next = new MapNode[tb.children.length + 1];
            System.arraycopy(tb.children, 0, next, 0, idx);
            next[idx] = (owner != null)
                    ? new MapNode.TransientLeaf<>(owner, c.hash(), c.key(), val)
                    : new MapNode.Leaf<>(c.hash(), c.key(), val);
            System.arraycopy(tb.children, idx, next, idx + 1, tb.children.length - idx);

            tb.bitmap |= c.bit();
            tb.children = next;
            return tb;
        }

        final var idx = c.calculateIndex(tb.bitmap);
        tb.children[idx] = InsertCommand.withNode(tb.children[idx], owner)
                .withCursor(c.descend())//
                .withValue(val)//
                .invoke();
        return tb;
    }

    private static <K, V> MapNode<K, V> update(MapNode.Branch<K, V> b, Cursor<K> c, V val, Object owner) {
        final var idx = c.calculateIndex(b.bitmap());
        MapNode<K, V>[] nextChildren = b.children().clone();
        nextChildren[idx] = InsertCommand.withNode(nextChildren[idx], owner)//
                .withCursor(c.descend())//
                .withValue(val)//
                .invoke();

        return (owner != null)
                ? new MapNode.TransientBranch<>(owner, b.bitmap(), nextChildren)
                : new MapNode.Branch<>(b.bitmap(), nextChildren);
    }

    private static <K, V> MapNode<K, V> expand(MapNode.Branch<K, V> b, Cursor<K> c, V val, Object owner) {
        final var idx = c.calculateIndex(b.bitmap());
        MapNode<K, V>[] next = new MapNode[b.children().length + 1];
        System.arraycopy(b.children(), 0, next, 0, idx);
        next[idx] = (owner != null)
                ? new MapNode.TransientLeaf<>(owner, c.hash(), c.key(), val)
                : new MapNode.Leaf<>(c.hash(), c.key(), val);
        System.arraycopy(b.children(), idx, next, idx + 1, b.children().length - idx);

        final var nextBitmap = b.bitmap() | c.bit();
        return (owner != null)
                ? new MapNode.TransientBranch<>(owner, nextBitmap, next)
                : new MapNode.Branch<>(nextBitmap, next);
    }

    private static <K, V> MapNode<K, V> split(MapNode.Leaf<K, V> leaf, Cursor<K> cursor, V value, Object owner) {
        MapNode<K, V> fresh = (owner != null)
                ? new MapNode.TransientBranch<>(owner, 0, new MapNode[0])
                : new MapNode.Branch<>(0, new MapNode[0]);

        final var leafCursor = new Cursor<K>(leaf.key(), leaf.hash(), cursor.shift());

        final var newNode = InsertCommand.withNode(fresh, owner)//
                .withCursor(leafCursor)//
                .withValue(leaf.value())//
                .invoke();

        return InsertCommand.withNode(newNode, owner)//
                .withCursor(cursor)//
                .withValue(value)//
                .invoke();
    }

    interface WithCursor<K, V> {
        InsertCommand.WithValue<K, V> withCursor(final Cursor<K> cursor);
    }

    interface WithValue<K, V> {
        InsertCommand.Trigger<K, V> withValue(final V value);
    }

    interface Trigger<K, V> {
        MapNode<K, V> invoke();
    }
}