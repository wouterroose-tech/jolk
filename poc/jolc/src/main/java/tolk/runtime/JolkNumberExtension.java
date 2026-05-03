package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * ### JolkNumberExtension
 *
 * Implements the runtime logic for the `jolk.lang.NumberExtension`.
 * This class augments the native `java.lang.Number` substrate with Jolk's
 * abstract numerical protocol.
 *
 * It serves a documentary purpose, outlining the general numerical contract.
 * Concrete numeric types (like Long and Double) will implement the specific
 * behavior for these operations, often with guided coercion for cross-type
 * interactions.
 */
public class JolkNumberExtension {

    /**
     * The first-class meta-object for the Number archetype.
     */
    public static final JolkMetaClass NUMBER_TYPE;

    static {
        NUMBER_TYPE = new JolkMetaClass(
            "Number",
            null, // Number is the root protocol; no superclass
            JolkFinality.ABSTRACT, // Number is an abstract protocol
            JolkVisibility.PUBLIC,
            JolkArchetype.PROTOCOL,
            null // No direct host class for the abstract protocol
        );

        // --- Instance Methods (Documentary Protocol) ---

        // Arithmetic Operators
        NUMBER_TYPE.registerInstanceMethod("+", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() + o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("-", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() - o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("*", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() * o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("/", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() / o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("%", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() % o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("**", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                // InteropLibrary doesn't have a direct power operation, so we'll use Math.pow
                Number base = (Number) unwrap(args[0]);
                Number exponent = (Number) unwrap(args[1]);
                return lift(Math.pow(base.doubleValue(), exponent.doubleValue()));
            }
        });

        // Comparison Operators
        NUMBER_TYPE.registerInstanceMethod(">", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() > o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("<", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() < o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod(">=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() >= o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("<=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() <= o.doubleValue());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("==", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                return InteropLibrary.getUncached().isIdentical(unwrap(args[0]), unwrap(args[1]), InteropLibrary.getUncached());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("!=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                return !InteropLibrary.getUncached().isIdentical(unwrap(args[0]), unwrap(args[1]), InteropLibrary.getUncached());
            }
        });
        NUMBER_TYPE.registerInstanceMethod("~~", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                return unwrap(args[0]).equals(unwrap(args[1]));
            }
        });
        NUMBER_TYPE.registerInstanceMethod("!~", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                if (args.length != 2) throw ArityException.create(1, 1, args.length - 1);
                return !unwrap(args[0]).equals(unwrap(args[1]));
            }
        });
    }
}
