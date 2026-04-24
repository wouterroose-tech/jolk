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
    protected long doNumbers(Number n1, Number n2) {
        String op = getOperator();
        if (op == OP_PLUS)  return n1.longValue() + n2.longValue();
        if (op == OP_MINUS) return n1.longValue() - n2.longValue();
        if (op == OP_MUL)   return n1.longValue() * n2.longValue();
        if (op == OP_DIV)   return n1.longValue() / n2.longValue();
        if (op == OP_MOD)   return n1.longValue() % n2.longValue();
        if (op == OP_POW)   return (long) Math.pow(n1.longValue(), n2.longValue());
        
        throw new RuntimeException("Unsupported operator: " + op);
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
                                @Cached JolkDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, leftNode, getOperator(), new Object[]{rightNode});
    }
}