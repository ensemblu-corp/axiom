package com.ensemblu.axiom.core.data_structure.map.cursor;

import java.util.Objects;

public record Cursor<K>(K key, int hash, int shift) {
    public static <K> Cursor<K> forKey(K key) {
        return new Cursor<>(key, Objects.hashCode(key), 0);
    }

    public Cursor<K> descend() {
        return new Cursor<>(key, hash, shift + 5);
    }

    public int bit() {
        return 1 << ((hash >>> shift) & 31);
    }

    public int calculateIndex(int bitmap) {
        return Integer.bitCount(bitmap & (bit() - 1));
    }

    public boolean isRoadMissing(int bitmap) {
        return (bitmap & bit()) == 0;
    }
}
