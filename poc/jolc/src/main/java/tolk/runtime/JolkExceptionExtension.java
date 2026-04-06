package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import java.util.HashMap;

/**
 * ### JolkExceptionExtension
 * 
 * Implements the runtime logic for the Exception archetype. 
 * It provides the Meta-Object for the base Exception type and utility 
 * methods for bridging Jolk exceptions with the JVM's stack unwinding.
 */
public class JolkExceptionExtension {

    /**
     * The first-class meta-object for the Exception archetype.
     * Even though Jolk instances extend java.lang.Throwable, this 
     * MetaClass provides the identity required for name resolution 
     * and meta-level messaging (e.g., Exception #throw).
     */
    public static final JolkMetaClass EXCEPTION_TYPE;

    static {
        EXCEPTION_TYPE = new JolkMetaClass(
            "Exception", // Nominal identity in Jolk manuscripts
            JolkFinality.OPEN, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS, 
            new HashMap<>() // instance members are handled by Interop/Throwable
        );

        // meta #throw(message) -> Triggers a RuntimeException
        EXCEPTION_TYPE.registerMetaMethod("throw", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                // args[0] is EXCEPTION_TYPE; args[1] is the optional message
                String message = (args.length > 1) ? args[1].toString() : "Jolk Exception";
                throw new RuntimeException(message);
            }
        });
    }

    /**
     * Bridges a Throwable into the JVM's unwinding mechanism.
     */
    public static void throwException(Throwable t) {
        if (t instanceof RuntimeException re) throw re;
        throw new RuntimeException(t);
    }
}