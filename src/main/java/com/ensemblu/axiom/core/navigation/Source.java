package com.ensemblu.axiom.core.navigation;

import com.ensemblu.axiom.core.data_structure.list.PersistentList;
import com.ensemblu.axiom.core.data_structure.map.PersistentMap;


public final class Source implements SourceBehavior {
    private final Object value;
    private final Source parent;
    private final Object keyInParent;

    private Source(Object value, Source parent, Object keyInParent) {
        this.value = value;
        this.parent = parent;
        this.keyInParent = keyInParent;
    }

    public static Source of(Object value) {
        return new Source(value, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Source follow(Object key) {
        Object nextValue = null;
        if (value instanceof PersistentMap<?, ?> map) {
            nextValue = ((PersistentMap<Object, Object>) map).get(key);
        } else if (value instanceof PersistentList<?> list && key instanceof Integer idx) {
            if (idx >= 0 && idx < list.size()) {
                nextValue = list.get(idx);
            }
        }
        return new Source(nextValue, this, key);
    }

    @Override
    public Source inIndex(int idx) {
        Object nextValue = null;
        if (value instanceof PersistentList<?> list) {
            if (idx >= 0 && idx < list.size()) {
                nextValue = list.get(idx);
            }
        }

        return new Source(nextValue, this, idx);
    }

    @SuppressWarnings("unchecked")
    public Source append(Object newValue) {
        if (this.value instanceof PersistentList<?> list) {
            final var updatedList = ((PersistentList<Object>) list).append(newValue);
            return this.update(updatedList);
        }
        throw new IllegalStateException("Type Mismatch: Cannot append to a non-list structure: "
                + (value == null ? "null" : value.getClass().getSimpleName()));
    }


    public Source update(Object newValue) {
        if (parent == null) {
            return Source.of(newValue);
        }

        final var updatedParentValue = parent.evolveStructure(keyInParent, newValue);

        return parent.update(updatedParentValue);
    }

    private Object evolveStructure(Object key, Object newValue) {
        if (this.value instanceof PersistentList list && key instanceof Integer idx) {
            return list.update(idx, _ -> newValue);
        }

        if (this.value instanceof PersistentMap map) {
            return map.put(key, newValue);
        }

        throw new IllegalStateException("Structural Mismatch: Cannot evolve " + value.getClass().getSimpleName());
    }

    @Override
    public boolean exists() {
        return value != null;
    }

    @Override
    public Object getValue() {
        return value;
    }
}