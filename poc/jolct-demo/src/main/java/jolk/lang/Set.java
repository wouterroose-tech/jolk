package jolk.lang;

import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * # Set
 *
 * The **Set** represents a collection of unique identities, excising duplication.
 * Extends {@link LinkedHashSet} to maintain deterministic iteration order.
 *
 * @author Wouter Roose
 */
@Intrinsic
public final class Set<T> extends LinkedHashSet<T> {

    @SafeVarargs
    public static <T> Set<T> of(T... elements) {
        Set<T> set = new Set<>();
        Collections.addAll(set, elements);
        return set;
    }

    public boolean includes(T element) {
        return contains(element);
    }

    /**
     * Adds an element to the set.
     * Returns {@code Self} to allow for fluent message chaining.
     */
    public Set<T> add(T element) {
        super.add(element);
        return this;
    }
}