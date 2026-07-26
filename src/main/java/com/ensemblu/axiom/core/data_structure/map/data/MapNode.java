package com.ensemblu.axiom.core.data_structure.map.data;

public sealed interface MapNode<K, V> {
    static <K, V> WithHash<K, V> createLeaf() {
        return hashCode -> key -> _value -> new Leaf<K, V>(hashCode, key, _value);
    }

    static <K, V> WithBitmap<K, V> createBranch() {
        return bitmap -> children -> new Branch<K, V>(bitmap, children);
    }

    interface WithHash<K, V> {
        WithKey<K, V> withHash(final int hashCode);
    }

    interface WithKey<K, V> {
        WithValue<K, V> withKey(final K key);
    }

    interface WithValue<K, V> {
        MapNode<K, V> withValue(final V value);
    }

    interface WithBitmap<K, V> {
        WithChildren<K, V> withBitmap(final int bitmap);
    }

    interface WithChildren<K, V> {
        MapNode<K, V> withChildren(MapNode<K, V>[] children);
    }

    default boolean isTransient(Object owner) { return false; }

    record Leaf<K, V>(int hash, K key, V value) implements MapNode<K, V> {}

    record Branch<K, V>(int bitmap, MapNode<K, V>[] children) implements MapNode<K, V> {}

    final class TransientLeaf<K, V> implements MapNode<K, V> {
        public final int hash;
        public final K key;
        public V value; // ⚔️ Mutable Value
        public final Object owner;

        public TransientLeaf(Object owner, int hash, K key, V value) {
            this.owner = owner;
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        @Override public boolean isTransient(Object owner) { return this.owner == owner; }
    }

    final class TransientBranch<K, V> implements MapNode<K, V> {
        public int bitmap; // ⚔️ Mutable Bitmap
        public MapNode<K, V>[] children; // ⚔️ Mutable Array
        public final Object owner;

        public TransientBranch(Object owner, int bitmap, MapNode<K, V>[] children) {
            this.owner = owner;
            this.bitmap = bitmap;
            this.children = children;
        }

        @Override public boolean isTransient(Object owner) { return this.owner == owner; }
    }

    default MapNode<K, V> freeze() {
        return switch (this) {
            case Leaf<K, V> l -> l;
            case Branch<K, V> b -> b;
            case TransientLeaf<K, V> tl -> new Leaf<>(tl.hash, tl.key, tl.value);
            case TransientBranch<K, V> tb -> {
                for ( var i = 0; i < tb.children.length; i++) {
                    if (tb.children[i] != null) tb.children[i] = tb.children[i].freeze();
                }
                yield new Branch<>(tb.bitmap, tb.children);
            }
        };
    }
}
