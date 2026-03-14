package jolk.lang;

import java.util.LinkedHashMap;

/// # Map
///
/// Runtime implementation of the Jolk Map archetype.
/// Extends LinkedHashMap to maintain deterministic iteration order.
///
/// @author Wouter Roose

public final class Map<K, V> extends LinkedHashMap<K, V> {

    /// Static factory that accepts a flat array of Key, Value, Key, Value...
    /// This supports the transpiled output of #( k -> v, k2 -> v2 )
    public static <K, V> Map<K, V> of(java.lang.Object... entries) {
        Map<K, V> map = new Map<>();
        for (int i = 0; i < entries.length; i += 2) {
            if (i + 1 < entries.length) {
                @SuppressWarnings("unchecked")
                K key = (K) entries[i];
                @SuppressWarnings("unchecked")
                V value = (V) entries[i + 1];
                map.put(key, value);
            }
        }
        return map;
    }

    public V atKey(K key) {
        return get(key);
    }

    // Fluent alias for put (renamed to avoid return type conflict with java.util.Map.put)
    public Map<K, V> set(K key, V value) {
        super.put(key, value);
        return this;
    }

    public Set<K> keys() {
        Set<K> keys = new Set<>();
        keys.addAll(keySet());
        return keys;
    }
}