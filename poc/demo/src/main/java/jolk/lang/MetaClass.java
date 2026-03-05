package jolk.lang;

/// # MetaClass
///
/// - **T**: the Jolk class that this MetaClass represents
/// @author Wouter Roose

public class MetaClass<T extends jolk.lang.Object<T>> {

    /// The default constructor
    public MetaClass(Class<? extends jolk.lang.Object<T>> clazz) {
    }
    
}