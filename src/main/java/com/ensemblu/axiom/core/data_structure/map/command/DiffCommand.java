package com.ensemblu.axiom.core.data_structure.map.command;

import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.MapDelta;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;
import com.ensemblu.axiom.core.data_structure.map.data.MapNode;
import com.ensemblu.axiom.core.foundation.Dop;

public final class DiffCommand<K, V> {
    private final PersistentMap<K, V> rootBefore;
    private final PersistentMap<K, V> rootAfter;
    private PersistentMap<K, V> added = PersistentMap.empty();
    private PersistentMap<K, V> removed = PersistentMap.empty();
    private PersistentMap<K, V> updated = PersistentMap.empty();

    private DiffCommand(PersistentMap<K, V> rootBefore, PersistentMap<K, V> rootAfter) {
        this.rootBefore = rootBefore;
        this.rootAfter = rootAfter;
    }

    public static <K, V> MapDelta<K, V> execute(PersistentMap<K, V> before, PersistentMap<K, V> after, MapNode<K, V> n1, MapNode<K, V> n2) {
        final var command = new DiffCommand<>(before, after);
        command.sync(n1, n2);
        return MapDelta.<K, V>withAdded(command.added).withRemoved(command.removed).withUpdated(command.updated);
    }

    private void sync(MapNode<K, V> n1, MapNode<K, V> n2) {
        if (n1 == n2) return;
        if (n1 instanceof MapNode.Leaf<K, V> l1 && n2 instanceof MapNode.Leaf<K, V> l2) {
            if (Dop.isEqual(l1.key(), l2.key())) {
                if (!Dop.isEqual(l1.value(), l2.value())) {
                    processValue(l1.key(), l1.value(), l2.value());
                }
            } else {
                collect(n1, 0xFFFFFFFF, true);
                collect(n2, 0xFFFFFFFF, false);
            }
        } else if (n1 instanceof MapNode.Branch<K, V> b1 && n2 instanceof MapNode.Branch<K, V> b2) {
            int common = b1.bitmap() & b2.bitmap();
            if ((b1.bitmap() & ~b2.bitmap()) != 0) collect(b1, b1.bitmap() & ~b2.bitmap(), true);
            if ((b2.bitmap() & ~b1.bitmap()) != 0) collect(b2, b2.bitmap() & ~b1.bitmap(), false);
            for ( var i = 0; i < 32; i++) {
                final var bit = 1 << i;
                if ((common & bit) != 0) sync(getChild(b1, bit), getChild(b2, bit));
            }
        } else {
            collect(n1, 0xFFFFFFFF, true);
            collect(n2, 0xFFFFFFFF, false);
        }
    }

    private void processValue(K key, V v1, V v2) {
        if (v1 instanceof PersistentMap<?,?> || v2 instanceof PersistentMap<?,?> ||
                v1 instanceof PersistentList<?> || v2 instanceof PersistentList<?>) {
            removed = removed.put(key, v1);
            added = added.put(key, v2);
        }
        else {
            updated = updated.put(key, v2);
        }
    }

    private void checkGlobal(K key, V value, boolean isBefore) {
        if (isBefore) {
            final var newValue = rootAfter.get(key);
            if (newValue == null) removed = removed.put(key, value);
            else if (!Dop.isEqual(value, newValue)) processValue(key, value, newValue);
        } else {
            if (rootBefore.get(key) == null) added = added.put(key, value);
        }
    }

    private void collect(MapNode<K, V> n, int mask, boolean isBefore) {
        TraverseCommand.execute(n, (k, v) -> checkGlobal(k, v, isBefore));
    }

    private MapNode<K, V> getChild(MapNode.Branch<K, V> b, int bit) {
        return b.children()[Integer.bitCount(b.bitmap() & (bit - 1))];
    }
}