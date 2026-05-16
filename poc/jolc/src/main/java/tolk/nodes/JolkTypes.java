package tolk.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkMatch;
import tolk.runtime.JolkClosure;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkSelector;
import java.math.BigDecimal;
import tolk.runtime.JolkObject;

/// # JolkTypes
///
/// Defines the set of fundamental types for the Jolk language's Truffle implementation.
///
/// The `@TypeSystem` annotation informs the Truffle DSL about the primary data types
/// that nodes will operate on. This allows the DSL to generate optimized specializations
/// and implicit casts, leading to high-performance, type-aware execution nodes.
@TypeSystem({
    long.class,
    double.class,
    boolean.class,
    BigDecimal.class,
    String.class,
    TruffleString.class,
    Number.class,
    List.class,
    Map.class,
    Iterator.class,
    Throwable.class,
    JolkSelector.class,
    JolkClosure.class,
    JolkMetaClass.class,
    JolkMatch.class, 
    JolkNothing.class,
    JolkObject.class
})
public abstract class JolkTypes { 

    /// ### castLong
    /// 
    /// Implements **Identity Restitution** for boxed identities. 
    /// This implicit cast allows the Truffle DSL to automatically unbox 
    /// `java.lang.Long` objects into primitive longs, ensuring that 
    /// host-provided numbers can flow into optimized fast-path nodes.
    @ImplicitCast
    @TruffleBoundary
    public static long castLong(Long value) {
        return value;
    }

    /// ### castIntToLong (Primitive)
    /// 
    /// Supports the **Numerical Type Ranking** rule by promoting primitive 
    /// 32-bit integers to 64-bit longs. This ensures that internal 
    /// substrate operations remain uniform.
    @ImplicitCast
    @TruffleBoundary
    public static long castIntToLong(int value) {
        return (long) value;
    }

    /// ### castIntToLong
    /// 
    /// Supports the **Numerical Type Ranking** rule where `Int` (Integer) 
    /// is automatically promoted to `Long` (Widening) when encountering 
    /// host-provided 32-bit integers.
    @ImplicitCast
    @TruffleBoundary
    public static long castIntToLong(Integer value) {
        return value.longValue();
    }

    /// ### castDouble
    /// 
    /// Bridges the metaboundary for floating-point identities, ensuring 
    /// boxed `Double` instances from the JVM are flattened into substrate 
    /// scalars.
    @ImplicitCast
    @TruffleBoundary
    public static double castDouble(Double value) {
        return value;
    }

    /// ### castBoolean
    /// 
    /// Normalizes boxed `Boolean` identities into primitive booleans. 
    /// This allows Jolk's keyword-less control flow (`? :`) to operate 
    /// directly on values returned by Java Predicates or host methods 
    /// without manual unboxing.
    @ImplicitCast
    @TruffleBoundary
    public static boolean castBoolean(Boolean value) {
        return value;
    }
}