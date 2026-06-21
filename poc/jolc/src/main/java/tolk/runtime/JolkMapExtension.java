package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import tolk.nodes.JolkNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ### JolkMapExtension
 *
 * Implements the runtime logic for the `jolk.lang.MapExtension`.
 * This class augments the native `java.util.Map` substrate with Jolk's
 * associative archetypal protocols.
 *
 * It maps Jolk selectors like `#size`, `#at`, `#put`, `#containsKey`, and `#forEach`
 * to standard Java Map operations while maintaining the **Self-Return Contract**.
 */
public class JolkMapExtension {

    /**
     * The first-class meta-object for the Map archetype.
     */
    public static final JolkMetaClass MAP_TYPE;

    static {
        MAP_TYPE = new JolkMetaClass(
            "Map",
            JolkFinality.FINAL,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS
        );

        // --- Instance Methods ---

        // #size -> Returns the number of key-value mappings in this map.
        MAP_TYPE.registerInstanceMethod("size", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 1) throw ArityException.create(0, 0, args.length - 1);
                Map<?, ?> map = (Map<?, ?>) unwrap(args[0]);
                return JolkNode.interopLift((long) map.size()); // Interop-safe lifting
            }
        });

        // #at(key) -> Returns the value to which the specified key is mapped, or Nothing if this map contains no mapping for the key.
        MAP_TYPE.registerInstanceMethod("at", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Map<?, ?> map = (Map<?, ?>) unwrap(args[0]);
                Object key = lift(args[1]); 
                return JolkNode.interopLift(map.get(key));
            }
        });

        // #put(key, value) -> Associates the specified value with the specified key in this map. Returns receiver.
        MAP_TYPE.registerInstanceMethod("put", new JolkBuiltinMethod() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 3) throw ArityException.create(2, 2, args.length - 1);
                Map<Object, Object> map = (Map<Object, Object>) unwrap(args[0]);
                Object key = lift(args[1]);
                Object value = lift(args[2]);
                map.put(key, value);
                return args[0]; // Self-Return Contract
            }
        });

        // #containsKey(key) -> Returns true if this map contains a mapping for the specified key.
        MAP_TYPE.registerInstanceMethod("containsKey", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Map<?, ?> map = (Map<?, ?>) unwrap(args[0]);
                Object key = lift(args[1]);
                return JolkNode.interopLift(map.containsKey(key));
            }
        });

        // #forEach(closure) -> Performs the given action for each entry in this map. Returns receiver.
        MAP_TYPE.registerInstanceMethod("forEach", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Map<?, ?> map = (Map<?, ?>) unwrap(args[0]);
                Object action = args[1]; // Should be a JolkClosure
                
                InteropLibrary interop = InteropLibrary.getUncached();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    try {
                        // Identity Restitution: Lift key and value before passing to closure
                        Object guestKey = lift(entry.getKey());
                        Object guestValue = lift(entry.getValue());

                        if (action instanceof JolkClosure closure) {
                            closure.execute(new Object[]{guestKey, guestValue});
                        } else {
                            interop.execute(action, guestKey, guestValue);
                        }
                    } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                        throw new RuntimeException("Error executing #forEach closure on map entry: " + e.getMessage(), e);
                    }
                }
                return args[0]; // Self-Return Contract
            }
        });

        // #map(closure) -> Applies closure to each value and returns a new Map with
        // same keys and transformed values.
        MAP_TYPE.registerInstanceMethod("map", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                if (args.length != 2)
                    throw ArityException.create(1, 1, args.length - 1);
                Map<?, ?> map = (Map<?, ?>) unwrap(args[0]);
                Object action = args[1]; // expected JolkClosure or interop callable
                Map<Object, Object> result = new LinkedHashMap<>();
                InteropLibrary interop = InteropLibrary.getUncached();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object guestKey = lift(entry.getKey());
                    Object guestVal = lift(entry.getValue());
                    Object newVal;
                    try {
                        if (action instanceof JolkClosure closure) {
                            newVal = closure.execute(new Object[] { guestKey, guestVal });
                        } else {
                            newVal = interop.execute(action, guestKey, guestVal);
                        }
                    } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                        throw new RuntimeException("Error executing #map closure on map entry: " + e.getMessage(), e);
                    }
                    result.put(lift(entry.getKey()), lift(newVal));
                }
                return JolkNode.interopLift(result);
            }
        });

        /**
         * ### meta #new(key1, value1, key2, value2, ...)
         *
         * Creates a java.util.LinkedHashMap from variadic Jolk identities.
         * Expects arguments in key-value pairs.
         */
        MAP_TYPE.registerMetaMethod("new", new JolkBuiltinMethod() {
            @Override
            public Object execute(Object[] args) throws ArityException {
                // args[0] is the MetaClass; trailing args are key-value pairs.
                if ((args.length - 1) % 2 != 0) {
                    throw ArityException.create((args.length - 1) / 2 * 2, (args.length - 1) / 2 * 2, args.length - 1);
                }

                Map<Object, Object> newMap = new LinkedHashMap<>();
                for (int i = 1; i < args.length; i += 2) {
                    newMap.put(lift(args[i]), lift(args[i + 1]));
                }
                return JolkNode.interopLift(newMap);
            }
        });
    }
}