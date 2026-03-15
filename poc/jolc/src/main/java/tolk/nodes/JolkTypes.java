package tolk.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;

import tolk.runtime.JolkNothing;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkObject;

/// # JolkTypes
///
/// Defines the set of fundamental types for the Jolk language's Truffle implementation.
///
/// The `@TypeSystem` annotation informs the Truffle DSL about the primary data types
/// that nodes will operate on. This allows the DSL to generate optimized specializations
/// and implicit casts, leading to high-performance, type-aware execution nodes.
@TypeSystem({
    int.class,
    long.class,
    boolean.class,
    String.class,
    JolkObject.class,
    JolkNothing.class,
    JolkMatch.class
})
public abstract class JolkTypes { 
    // This class is intentionally left empty. It serves as a marker for the Truffle DSL
    // to recognize the types used in the Jolk language.
}