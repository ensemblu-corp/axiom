package com.ensemblu.axiom.core.data_structure.map.command;

import com.ensemblu.axiom.core.data_structure.map.data.MapNode;

import java.util.function.BiConsumer;

public interface TraverseCommand {
    static <K, V> void execute(MapNode<K, V> mapNode, BiConsumer<K, V> action) {
        switch (mapNode) {
            case MapNode.Leaf<K, V> leaf -> action.accept(leaf.key(), leaf.value());
            case MapNode.TransientLeaf<K, V> tl -> action.accept(tl.key, tl.value);

            case MapNode.Branch<K, V> branch -> {
                for (MapNode<K, V> child : branch.children()) {
                    execute(child, action);
                }
            }
            case MapNode.TransientBranch<K, V> tb -> {
                for (MapNode<K, V> child : tb.children) {
                    execute(child, action);
                }
            }
        }
    }
}