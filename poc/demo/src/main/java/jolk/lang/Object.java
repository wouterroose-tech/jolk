package jolk.lang;

/// # Object
///
/// The root class of the Jolk object hierarchy.
///
/// @author Wouter Roose

public class Object<Self extends Object<Self>> {

    @SuppressWarnings("unchecked")
    MetaClass<Self> meta() {
        return new MetaClass<>((Class<? extends Object<Self>>) getClass());
    };
}