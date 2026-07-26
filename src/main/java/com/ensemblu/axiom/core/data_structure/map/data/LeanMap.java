package com.ensemblu.axiom.core.data_structure.map.data;

public sealed interface LeanMap<K, V> {
    static <K, V> LeanMap<K, V> empty() {
        return new Empty<>();
    }

    static <K, V> LeanMap<K, V> ofRoot(MapNode<K, V> mapNode, int size) {
        return new Root<>(mapNode, size, null);
    }

    static <K, V> LeanMap<K, V> ofRoot(MapNode<K, V> mapNode, int size, Object owner) {
        return new Root<>(mapNode, size, owner);
    }

    record Empty<K, V>() implements LeanMap<K, V> {}

    record Root<K, V>(MapNode<K, V> mapNode, int size, Object owner) implements LeanMap<K, V> {}
}