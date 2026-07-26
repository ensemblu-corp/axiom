package com.ensemblu.axiom.core.data_structure.list.cursor;

public record Path(int index, int shift) {
    public int subIndex() {
        return (index >>> (shift - 5)) & 31;
    }

    public Path descend() {
        return new Path(index, shift - 5);
    }
}