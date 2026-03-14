package jolk.lang;

import java.util.Collections;
import java.util.LinkedHashSet;

/// # Set
///
/// Runtime implementation of the Jolk Set archetype.
/// Extends LinkedHashSet to maintain deterministic iteration order.
///
/// @author Wouter Roose

public final class Set<T> extends LinkedHashSet<T> {

    @SafeVarargs
    public static <T> Set<T> of(T... elements) {
        Set<T> set = new Set<>();
        Collections.addAll(set, elements);
        return set;
    }

    public Boolean includes(T element) {
        return contains(element);
    }

    @Override
    public boolean add(T element) {
        return super.add(element);
    }
    
    // Fluent alias for add
    public Set<T> push(T element) {
        add(element);
        return this;
    }
}