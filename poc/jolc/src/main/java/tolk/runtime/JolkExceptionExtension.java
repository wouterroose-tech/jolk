package tolk.runtime;

/// # JolkExceptionExtension
///
/// Implements the Jolk **Augmentation Protocol** for [java.lang.Throwable].
///
/// This extension is a cornerstone of **Shim-less Integration**. It allows any native Java
/// exception to participate in Jolk message chains as a first-class identity. Instead of 
/// wrapping host exceptions in a guest-language proxy, this library exports the Jolk 
/// messaging protocol directly onto the host type.
///
/// ### Architectural Role
/// - **Control Flow**: Provides the `#throw` selector, allowing host exceptions to be
///   propagated using guest syntax.
public final class JolkExceptionExtension {

    /**
     * ### throwException
     * 
     * Performs a **"Sneaky Throw"** to propagate the original [Throwable] without 
     * wrapping it in a [RuntimeException]. 
     * 
     * This preserves the original stack trace and type identity, which is essential 
     * for Jolk's `#catch` blocks to correctly filter by exception type.
     */
    public static RuntimeException throwException(Throwable t) {
        JolkExceptionExtension.<RuntimeException>doThrow(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }
}