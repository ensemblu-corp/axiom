package com.ensemblu.axiom.core.data_structure.list;

/**
 * Mirrors {@link com.ensemblu.axiom.core.data_structure.map.MapDelta}, the diff type
 * used by the map-side sync engine (see {@code SyncStrike}, {@code DiffCommand}).
 * <p>
 * Intentionally unwired for now — no {@code ListDiffCommand} or list-side sync engine
 * exists yet. This is the shape it should take once that lands, not dead code.
 */
public record ListDelta<E>(PersistentList<E> added, PersistentList<E> removed, boolean isOverwrite) {
    public static <E> ListDelta<E> empty() { return new ListDelta<>(PersistentList.empty(), PersistentList.empty(), false); }
}