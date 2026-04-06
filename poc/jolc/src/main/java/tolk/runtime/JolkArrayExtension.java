package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ### JolkArrayExtension
 * 
 * Implements the runtime logic for the `jolk.lang.ArrayExtension`. 
 * This class augments the native `java.util.List` substrate with Jolk's 
 * associative and linear archetypal protocols.
 * 
 * It maps Jolk selectors like `#at`, `#put`, and `#new` to standard Java 
 * List operations while maintaining the **Self-Return Contract**.
 */
public class JolkArrayExtension {

    /**
     * The first-class meta-object for the Array archetype.
     */
    public static final JolkMetaClass ARRAY_TYPE;

    static {
        // Jolk collections are polite citizens: they respond to standard Object protocol.
        Map<String, Object> instanceMembers = new HashMap<>();
        Map<String, Object> metaMembers = new HashMap<>();

        ARRAY_TYPE = new JolkMetaClass(
            "Array", 
            null, // Superclass resolved at runtime or defaults to Object
            JolkFinality.FINAL, 
            JolkVisibility.PUBLIC, 
            JolkArchetype.CLASS, 
            instanceMembers, 
            new HashMap<>(), // Physical fields are managed by the Java substrate
            metaMembers, 
            new HashMap<>()
        );

        // --- Instance Methods ---

        // #at(index) -> Returns element. Narrowing Long to int for List access.
        ARRAY_TYPE.registerInstanceMethod("at", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                List<?> list = (List<?>) unwrap(args[0]);
                int index = ((Number) args[1]).intValue();
                return lift(list.get(index));
            }
        });

        // #put(index, value) -> Returns receiver (Fluent Initialisation).
        ARRAY_TYPE.registerInstanceMethod("put", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 3) throw ArityException.create(2, 2, args.length - 1);
                List<Object> list = (List<Object>) unwrap(args[0]);
                int index = ((Number) args[1]).intValue();
                list.set(index, args[2]);
                return args[0]; 
            }
        });

        // --- Meta Methods ---

        /**
         * ### meta #new(elements...)
         * 
         * Creates a java.util.ArrayList from variadic Jolk identities.
         */
        ARRAY_TYPE.registerMetaMethod("new", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) {
                // args[0] is the MetaClass; trailing args are the elements.
                Object[] elements = Arrays.copyOfRange(args, 1, args.length);
                return lift(new ArrayList<>(Arrays.asList(elements)));
            }
        });
    }
}
