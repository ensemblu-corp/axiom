package com.ensemblu.axiom.core.data_structure.map;

import com.ensemblu.axiom.core.foundation.Dop;

import java.util.Objects;

public final class MapDelta<K, V>{
    private final PersistentMap<K, V> added;
    private final PersistentMap<K, V> removed;
    private final PersistentMap<K, V> updated;

    private MapDelta(PersistentMap<K, V> added,
                     PersistentMap<K, V> removed,
                     PersistentMap<K, V> updated) {
        this.added = Objects.requireNonNull(added, "added is null in MapDelta");
        this.removed = Objects.requireNonNull(removed, "removed is null in MapDelta");
        this.updated = Objects.requireNonNull(updated, "updated is null in MapDelta");
    }

    public static <K, V> MapDelta<K, V> empty() {
        return new MapDelta<>(
                PersistentMap.empty(),
                PersistentMap.empty(),
                PersistentMap.empty()
        );
    }

    public static <K, V> WithRemoved<K, V> withAdded(PersistentMap<K, V> added) {
        return  removed -> updated -> new MapDelta<K, V>(added, removed, updated);
    }

    public static <K, V> WithRemoved<K, V> withoutAdded() {
        return  removed -> updated ->
                new MapDelta<K, V>(PersistentMap.<K, V>empty(), removed, updated);
    }

    public PersistentMap<K, V> added() { return added; }
    public PersistentMap<K, V> removed() { return removed; }
    public PersistentMap<K, V> updated() { return updated; }

     public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && updated.isEmpty();
    }

     public interface WithRemoved<K, V> {
        WithUpdated<K, V> withRemoved(PersistentMap<K, V> removed);

         default WithUpdated<K, V> withoutRemoved() {
             return withRemoved(PersistentMap.<K, V>empty());
         }

     }

    public interface WithUpdated<K, V> {

         MapDelta<K, V> withUpdated(PersistentMap<K, V> updated);

         default MapDelta<K, V> withoutUpdated() {
             return withUpdated(PersistentMap.<K, V>empty());
         }
     }

    @SuppressWarnings("unchecked")
    public MapDelta<K, V> invert() {
        final var invertedUpdated = Dop.project(this.updated)
                .mapValues(v -> (v instanceof MapDelta m) ? m.invert() : v)
                .deploy();

        return new MapDelta<>(this.removed, this.added, (PersistentMap<K, V>) invertedUpdated);
    }

    @Override
    public String toString() {
        return "MapDelta{\n" +
                "  added=" + added + ",\n" +
                "  removed=" + removed + ",\n" +
                "  updated=" + updated + "\n" +
                "}";
    }
}