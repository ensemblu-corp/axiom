package com.ensemblu.axiom.core.data_structure.map.behavior;

import com.ensemblu.axiom.api.TargetNavigator;
import com.ensemblu.axiom.core.foundation.Nothing;
import com.ensemblu.axiom.core.validation.Result;
import com.ensemblu.axiom.core.data_structure.map.MapDelta;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;
import com.ensemblu.axiom.core.foundation.DataCast;
import com.ensemblu.axiom.core.foundation.Dop;


public interface PersistentMapBehavior<K, V> {
    int size();

    V get(K key);

    default boolean exists(K key) { return get(key)!=null; }

    default TargetNavigator targetKey(K key) {
        return new TargetNavigator() {
            @Override
            public <T> Result<T> execute(DataCast.Protocol protocol) {
                final var val = get(key);
                if (val == null) return Result.failure("Key not found: " + key);
                return DataCast.cast(Dop.resolve(val), protocol);
            }
        };
    }

    default boolean isEmpty() {
        return size() == 0;
    }

    PersistentMap<K, V> put(K key, V value);

    PersistentMap<K, V> remove(K key);

    MapDelta<K, V> diff(PersistentMap<K, V> other);

    Nothing forEach(java.util.function.BiConsumer<K, V> action);

    default PersistentMap<K, V> merge(PersistentMap<K, V> other) {
        if (other == null || other.isEmpty()) return (PersistentMap<K, V>) this;

        return Dop.project((PersistentMap<K, V>) this)//
                .updateFrom(other)//
                .deploy();
    }
}