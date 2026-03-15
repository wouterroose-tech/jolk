package tolk.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;

import tolk.runtime.JolkObject;

@TypeSystem({
    int.class,
    long.class,
    boolean.class,
    String.class,
    JolkObject.class
})
public abstract class JolkTypes {

    // Truffle DSL will generate a JolkTypesGen class from this definition.
    // This is where implicit casts and type checks will be defined.

}