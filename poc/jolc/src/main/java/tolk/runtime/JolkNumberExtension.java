package tolk.runtime;

import java.math.BigDecimal;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * ### JolkNumberExtension
 *
 * Implements the runtime logic for the `jolk.lang.NumberExtension`.
 *
 * This class projects the **Number Protocol** onto the `java.lang.Number` 
 * substrate. It defines the universal contract for numeric identities 
 * using the `Self` type alias.
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
            JolkArchetype.PROTOCOL
        );

        // --- Instance Methods (Documentary Contract) ---

        /**
         * ### Self +(Number other)
         * 
         * Performs addition. Returns a result of type `Self` (the receiver's type)
         * unless Guided Coercion promotes the result to a wider precision.
         */
        NUMBER_TYPE.registerInstanceMethod("+", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() + o.doubleValue());
            }
        });

        /**
         * ### Self -(Number other)
         * 
         * Performs subtraction.
         */
        NUMBER_TYPE.registerInstanceMethod("-", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() - o.doubleValue());
            }
        });

        /**
         * ### Self *(Number other)
         * 
         * Performs multiplication.
         */
        NUMBER_TYPE.registerInstanceMethod("*", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() * o.doubleValue());
            }
        });

        /**
         * ### Self /(Number other)
         * 
         * Performs division.
         */
        NUMBER_TYPE.registerInstanceMethod("/", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() / o.doubleValue());
            }
        });

        /**
         * ### Self %(Number other)
         * 
         * Performs modulo.
         */
        NUMBER_TYPE.registerInstanceMethod("%", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() % o.doubleValue());
            }
        });

        /**
         * ### Self **(Number other)
         * 
         * Performs exponentiation.
         */
        NUMBER_TYPE.registerInstanceMethod("**", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number base = (Number) unwrap(args[0]);
                Number exponent = (Number) unwrap(args[1]);
                return lift(Math.pow(base.doubleValue(), exponent.doubleValue()));
            }
        });

        // --- Comparison Operators (Documentary Contract) ---

        /**
         * ### Boolean >(Number other)
         */
        NUMBER_TYPE.registerInstanceMethod(">", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() > o.doubleValue());
            }
        });

        /**
         * ### Boolean <(Number other)
         */
        NUMBER_TYPE.registerInstanceMethod("<", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() < o.doubleValue());
            }
        });

        /**
         * ### Boolean >=(Number other)
         */
        NUMBER_TYPE.registerInstanceMethod(">=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() >= o.doubleValue());
            }
        });

        /**
         * ### Boolean <=(Number other)
         */
        NUMBER_TYPE.registerInstanceMethod("<=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                Number r = (Number) unwrap(args[0]);
                Number o = (Number) unwrap(args[1]);
                return lift(r.doubleValue() <= o.doubleValue());
            }
        });

        NUMBER_TYPE.registerInstanceMethod("==", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                return InteropLibrary.getUncached().isIdentical(unwrap(args[0]), unwrap(args[1]), InteropLibrary.getUncached());
            }
        });

        NUMBER_TYPE.registerInstanceMethod("!=", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                return !InteropLibrary.getUncached().isIdentical(unwrap(args[0]), unwrap(args[1]), InteropLibrary.getUncached());
            }
        });

        NUMBER_TYPE.registerInstanceMethod("~~", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                return unwrap(args[0]).equals(unwrap(args[1]));
            }
        });

        NUMBER_TYPE.registerInstanceMethod("!~", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 2);
                return !unwrap(args[0]).equals(unwrap(args[1]));
            }
        });

        // --- Conversion Protocol ---

        NUMBER_TYPE.registerInstanceMethod("asLong", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 1);
                return ((Number) unwrap(args[0])).longValue();
            }
        });

        NUMBER_TYPE.registerInstanceMethod("asDouble", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 1);
                return ((Number) unwrap(args[0])).doubleValue();
            }
        });

        NUMBER_TYPE.registerInstanceMethod("asDecimal", new JolkBuiltinMethod() {
            @Override public Object execute(Object[] args) throws ArityException, UnsupportedMessageException {
                checkArity(args, 1);
                Number n = (Number) unwrap(args[0]);
                if (n instanceof BigDecimal d) return lift(d);
                // Guided Coercion: Preserve decimal precision when projecting
                // floating-point types into the high-rank Decimal space.
                if (n instanceof Double || n instanceof Float) {
                    return lift(new BigDecimal(n.toString()));
                }
                return lift(BigDecimal.valueOf(n.longValue()));
            }
        });
    }

    private static void checkArity(Object[] args, int expected) throws ArityException {
        if (args.length != expected) throw ArityException.create(expected, expected, args.length);
    }
}