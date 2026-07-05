package tolk.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.math.MathContext;
import java.math.BigDecimal;

/**
 * ### JolkArithmeticNode
 * 
 * Implements the **Semantic Flattening** optimization for common arithmetic 
 * operations (+, -, *, /, %, **). It provides a fast path for primitive numeric 
 * types, bypassing the general message dispatch logic, while falling back 
 * to the [JolkDispatchNode] for non-primitive receivers or complex protocols.
 */
@NodeInfo(shortName = "arithmetic")
@NodeChild(value = "leftNode", type = JolkNode.class)
@NodeChild(value = "rightNode", type = JolkNode.class)
@NodeField(name = "operator", type = String.class)
public abstract class JolkArithmeticNode extends JolkExpressionNode {

    // INDUSTRIAL OPTIMIZATION: Interned constants for identity comparison (==).
    // This allows Graal to fold these comparisons into integer-like checks.
    private static final String OP_PLUS = "+".intern();
    private static final String OP_MINUS = "-".intern();
    private static final String OP_MUL = "*".intern();
    private static final String OP_DIV = "/".intern();
    private static final String OP_MOD = "%".intern();
    private static final String OP_POW = "**".intern();

    public abstract String getOperator();

    @Override
    public abstract long executeLong(VirtualFrame frame) throws UnexpectedResultException;

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException;

    @Override
    public abstract TruffleString executeTruffleString(VirtualFrame frame) throws UnexpectedResultException;

    /**
     * Specialized fast-path for primitive longs. 
     */
    @Specialization(guards = "isPlus()")
    protected long doAdd(long l1, long l2) { return l1 + l2; }

    @Specialization(guards = "isMinus()")
    protected long doSub(long l1, long l2) { return l1 - l2; }

    @Specialization(guards = "isMultiply()")
    protected long doMul(long l1, long l2) { return l1 * l2; }

    @Specialization(guards = "isDivide()")
    protected long doDiv(long l1, long l2) { return l1 / l2; }

    @Specialization(guards = "isModulo()")
    protected long doMod(long l1, long l2) { return l1 % l2; }

    @Specialization(guards = "isPower()")
    protected long doPow(long l1, long l2) { return (long) Math.pow(l1, l2); }

    /**
     * Handles cases where one or both operands are already boxed (e.g. from Host Interop).
     */
    @Specialization(replaces = {"doAdd", "doSub", "doMul", "doDiv", "doMod", "doPow"})
    protected Object doNumbers(Number n1, Number n2) {
        // Guided Coercion: Highest Rank (Decimal / BigDecimal)
        if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
            BigDecimal d1 = (n1 instanceof BigDecimal) ? (BigDecimal) n1 : new BigDecimal(n1.toString());
            BigDecimal d2 = (n2 instanceof BigDecimal) ? (BigDecimal) n2 : new BigDecimal(n2.toString());
            String op = getOperator();
            if (op == OP_PLUS) return JolkNode.lift(d1.add(d2));
            if (op == OP_MINUS) return JolkNode.lift(d1.subtract(d2));
            if (op == OP_MUL) return JolkNode.lift(d1.multiply(d2));
            if (op == OP_DIV) return JolkNode.lift(d1.divide(d2, MathContext.DECIMAL128));
            if (op == OP_MOD) return JolkNode.lift(d1.remainder(d2));
            if (op == OP_POW) return JolkNode.lift(d1.pow(n2.intValue())); 
            throw new RuntimeException("Unsupported decimal operator: " + op);
        }

        // Guided Coercion: If either is a Double, promote both to Double for the operation.
        if (n1 instanceof Double || n2 instanceof Double || n1 instanceof Float || n2 instanceof Float) {
            double d1 = n1.doubleValue();
            double d2 = n2.doubleValue();
            String op = getOperator();
            if (op == OP_PLUS) return JolkNode.lift(d1 + d2);
            if (op == OP_MINUS) return JolkNode.lift(d1 - d2);
            if (op == OP_MUL) return JolkNode.lift(d1 * d2);
            if (op == OP_DIV) return JolkNode.lift(d1 / d2);
            if (op == OP_MOD) return JolkNode.lift(d1 % d2);
            if (op == OP_POW) return JolkNode.lift(Math.pow(d1, d2));
            throw new RuntimeException("Unsupported floating-point operator: " + op);
        }
        // Otherwise, both are Longs (or can be treated as Longs)
        long r = n1.longValue();
        long o = n2.longValue();
        String op = getOperator();
        if (op == OP_PLUS) return JolkNode.lift(r + o);
        if (op == OP_MINUS) return JolkNode.lift(r - o);
        if (op == OP_MUL) return JolkNode.lift(r * o);
        if (op == OP_DIV) return JolkNode.lift(r / o);
        if (op == OP_MOD) return JolkNode.lift(r % o);
        if (op == OP_POW) return JolkNode.lift((long) Math.pow(r, o));

        throw new RuntimeException("Unsupported integer operator: " + op);
    }

    @Specialization(guards = "isPlus()")
    protected TruffleString doStrings(TruffleString s1, TruffleString s2,
                                     @Shared("concat") @Cached TruffleString.ConcatNode concatNode) {
        return concatNode.execute(s1, s2, TruffleString.Encoding.UTF_16, true);
    }

    @Specialization(guards = "isPlus()")
    protected TruffleString doStringLong(TruffleString s1, Number n2,
                                         @Shared("fromLong") @Cached TruffleString.FromLongNode fromLongNode,
                                         @Shared("concat") @Cached TruffleString.ConcatNode concatNode) {
        TruffleString s2 = fromLongNode.execute(n2.longValue(), TruffleString.Encoding.UTF_16, true);
        return concatNode.execute(s1, s2, TruffleString.Encoding.UTF_16, true);
    }

    @Specialization(guards = "isPlus()")
    protected TruffleString doJavaStringLong(String s1, long l2, 
                                             @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                                             @Shared("fromLong") @Cached TruffleString.FromLongNode fromLongNode,
                                             @Shared("concat") @Cached TruffleString.ConcatNode concatNode) {
        // Note: For literals, Graal will eventually inline this. 
        // For dynamic strings, we convert only what's necessary.
        TruffleString s2 = fromLongNode.execute(l2, TruffleString.Encoding.UTF_16, true);
        return concatNode.execute(fromJavaStringNode.execute(s1, TruffleString.Encoding.UTF_16), s2, TruffleString.Encoding.UTF_16, true);
    }

    @Specialization(guards = "isPlus()")
    protected TruffleString doObjectString(VirtualFrame frame, Object left, TruffleString right,
                                          @Shared("dispatch") @Cached JolkDispatchNode dispatchNode,
                                          @Shared("concat") @Cached TruffleString.ConcatNode concatNode) {
        // Identity Congruence: String concatenation triggers #toString dispatch on the non-string operand.
        Object stringified = dispatchNode.execute0(frame, left, "toString");
        TruffleString s1 = (stringified instanceof TruffleString ts) ? ts : 
            TruffleString.fromJavaStringUncached(String.valueOf(stringified), TruffleString.Encoding.UTF_16);
        return concatNode.execute(s1, right, TruffleString.Encoding.UTF_16, true);
    }

    @Specialization(guards = "isPlus()")
    protected TruffleString doStringObject(VirtualFrame frame, TruffleString left, Object right,
                                          @Shared("dispatch") @Cached JolkDispatchNode dispatchNode,
                                          @Shared("concat") @Cached TruffleString.ConcatNode concatNode) {
        Object stringified = dispatchNode.execute0(frame, right, "toString");
        TruffleString s2 = (stringified instanceof TruffleString ts) ? ts : 
            TruffleString.fromJavaStringUncached(String.valueOf(stringified), TruffleString.Encoding.UTF_16);
        return concatNode.execute(left, s2, TruffleString.Encoding.UTF_16, true);
    }

    @Idempotent
    protected boolean isPlus() {
        return getOperator() == OP_PLUS;
    }

    @Idempotent
    protected boolean isMinus() {
        return getOperator() == OP_MINUS;
    }

    @Idempotent
    protected boolean isMultiply() {
        return getOperator() == OP_MUL;
    }

    @Idempotent
    protected boolean isDivide() {
        return getOperator() == OP_DIV;
    }

    @Idempotent
    protected boolean isModulo() {
        return getOperator() == OP_MOD;
    }

    @Idempotent
    protected boolean isPower() {
        return getOperator() == OP_POW;
    }

    /**
     * Unified Messaging Fallback: Use the central dispatcher for non-numeric types 
     * (e.g. String concatenation or custom operator overloading).
     */
    @Fallback
    protected Object doFallback(VirtualFrame frame, Object leftNode, Object rightNode,
                                @Shared("dispatch") @Cached JolkDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, leftNode, getOperator(), new Object[]{rightNode});
    }
}