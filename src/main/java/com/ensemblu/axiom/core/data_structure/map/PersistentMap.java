package com.ensemblu.axiom.core.data_structure.map;

import com.ensemblu.axiom.core.data_structure.map.behavior.PersistentMapBehavior;
import com.ensemblu.axiom.core.data_structure.map.command.*;
import com.ensemblu.axiom.core.foundation.Nothing;
import com.ensemblu.axiom.core.data_structure.map.command.*;
import com.ensemblu.axiom.core.data_structure.map.cursor.Cursor;
import com.ensemblu.axiom.core.data_structure.map.data.LeanMap;
import com.ensemblu.axiom.core.data_structure.map.data.MapNode;
import com.ensemblu.axiom.core.foundation.Dop;

import java.util.Map;

public final class PersistentMap<K, V> implements PersistentMapBehavior<K, V> {
    private final LeanMap<K, V> state;

    private PersistentMap(LeanMap<K, V> state) {
        this.state = state;
    }

    public static <K, V> PersistentMap<K, V> empty() {
        return new PersistentMap<>(LeanMap.empty());
    }

    public PersistentMap<K, V> asTransient() {
        return switch (state) {
            case LeanMap.Empty<K, V> _ -> this;
            case LeanMap.Root<K, V> r -> {
                if (r.owner() != null) yield this; //
                yield new PersistentMap<>(LeanMap.ofRoot(r.mapNode(), r.size(), new Object()));
            }
        };
    }

    public PersistentMap<K, V> freeze() {
        return switch (state) {
            case LeanMap.Empty<K, V> _ -> this;
            case LeanMap.Root<K, V> r -> {
                if (r.owner() == null) yield this; //
                final var frozenNode = r.mapNode().freeze();
                yield new PersistentMap<>(LeanMap.ofRoot(frozenNode, r.size(), null));
            }
        };
    }

    public static <V, K> PersistentMap<K, V> fromJavaMap(Map<K, V> map) {
        var result = PersistentMap.<K, V>empty().asTransient();
        for (final var entry : map.entrySet()) {
            result = result.put(entry.getKey(), entry.getValue());
        }
        return result.freeze();
    }

    @Override
    public PersistentMap<K, V> put(K key, V value) {
        final var cursor = Cursor.forKey(key);
        final var existing = get(key);
        final var exists = existing != null;

        if (exists && Dop.isEqual(existing, value)) return this;

        var nextState = switch (state) {
            case LeanMap.Empty<K, V> _ -> {

                final var node = MapNode.<K, V>createLeaf().withHash(cursor.hash()).withKey(key).withValue(value);
                yield LeanMap.ofRoot(node, 1);
            }
            case LeanMap.Root<K, V> r -> {
                final var newNode = InsertCommand.withNode(r.mapNode(), r.owner())//
                        .withCursor(cursor)//
                        .withValue(value)//
                        .invoke();
                yield LeanMap.ofRoot(newNode, exists ? r.size() : r.size() + 1, r.owner());
            }
        };
        return new PersistentMap<>(nextState);
    }

    @Override
    public int size() {
        return switch (state) {
            case LeanMap.Empty<K, V> _ -> 0;
            case LeanMap.Root<K, V> r -> r.size();
        };
    }

    @Override
    public V get(K key) {
        return switch (state) {
            case LeanMap.Empty<K, V> _ -> null;
            case LeanMap.Root<K, V> r -> SearchCommand.onNode(r.mapNode())//
                    .withCursor(Cursor.forKey(key))//
                    .invoke();
        };
    }

    @Override
    public PersistentMap<K, V> remove(K key) {
        final var val = get(key);
        if (val == null) return this; //

        var nextState = switch (state) {
            case LeanMap.Empty<K, V> _ -> state;
            case LeanMap.Root<K, V> r -> {
                MapNode<K, V> newMapNode = RemoveCommand.onNode(r.mapNode())//
                        .withCursor(Cursor.forKey(key))//
                        .invoke();
                yield (newMapNode == null) ? LeanMap.<K, V>empty() : LeanMap.ofRoot(newMapNode, r.size() - 1);
            }
        };
        return new PersistentMap<>(nextState);
    }

    @Override
    public MapDelta<K, V> diff(PersistentMap<K, V> other) {
        MapNode<K, V> n1 = (this.state instanceof LeanMap.Root<K, V> r1) ? r1.mapNode() : null;
        MapNode<K, V> n2 = (other.state instanceof LeanMap.Root<K, V> r2) ? r2.mapNode() : null;

        if (n1 == null && n2 == null) return MapDelta.empty();
        if (n1 == null) return MapDelta.withAdded(other).withoutRemoved().withoutUpdated();
        if (n2 == null) return MapDelta.<K, V>withoutAdded().withRemoved(this).withoutUpdated();

        return DiffCommand.execute(this, other, n1, n2);
    }

    @Override
    public Nothing forEach(java.util.function.BiConsumer<K, V> action) {
        if (state instanceof LeanMap.Root<K, V> r) {
            TraverseCommand.execute(r.mapNode(), action);
        }

        return Nothing.INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistentMap<?, ?> otherRaw)) return false;
        if (this.size() != otherRaw.size()) return false;

        final var other = (PersistentMap<Object, Object>) otherRaw;

        if (this.state instanceof LeanMap.Empty) return true;

        if (this.state instanceof LeanMap.Root<K, V> r) {
            try {
                TraverseCommand.execute(r.mapNode(), (k, v) -> {
                    Object otherVal = other.get(k);

                    if (!Dop.isEqual(v, otherVal)) {
                        throw EqualityBreak.INSTANCE;
                    }
                });
                return true;
            } catch (EqualityBreak e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (state instanceof LeanMap.Empty) return "{}";

        final var sb = new StringBuilder("{");
        if (state instanceof LeanMap.Root<K, V> r) {
            TraverseCommand.execute(r.mapNode(), (k, v) -> {
                if (sb.length() > 1) sb.append(", ");
                sb.append(k).append(":").append(Dop.toString(v)); //
            });
        }
        return sb.append("}").toString();
    }

    @Override
    public int hashCode() {
        if (state instanceof LeanMap.Empty) return 0;

        final int[] hash = {0};

        if (state instanceof LeanMap.Root<K, V> r) {
            TraverseCommand.execute(r.mapNode(), (k, v) -> {
                int entryHash = Dop.hashCode(k) ^ Dop.hashCode(v);
                hash[0] += entryHash;
            });
        }

        return hash[0];
    }

    private static final class EqualityBreak extends RuntimeException {
        static final EqualityBreak INSTANCE = new EqualityBreak();

        private EqualityBreak() {
            super(null, null, false, false);
        }
    }
}