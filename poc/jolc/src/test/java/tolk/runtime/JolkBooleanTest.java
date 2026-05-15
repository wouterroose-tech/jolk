package tolk.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.RootNode;
import tolk.JolcTestBase;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkBooleanTest
 *
 * Verifies the intrinsic behavior of the Jolk `Boolean` type.
 * This includes logical operations, branching messages used for control flow,
 * and the standard object protocol.
 */
public class JolkBooleanTest extends JolcTestBase {

    private Object getOperation(String opName) {
        Object op = JolkBooleanExtension.BOOLEAN_TYPE.lookupInstanceMember(opName);
        assertNotNull(op, "Operation " + opName + " should be defined in JolkBoolean.BOOLEAN_TYPE");
        return op;
    }

    private Object execute(Object op, Object... args) throws Exception {
        return InteropLibrary.getUncached().execute(op, args);
    }

    private Object createExecutable(Runnable runnable) {
        TestRootNode rootNode = new TestRootNode(args -> {
            runnable.run();
            return JolkNothing.INSTANCE;
        });
        return new JolkClosure(rootNode.getCallTarget());
    }

    static class TestRootNode extends RootNode {
        private final Function<Object[], Object> logic;

        // By passing null to super, we avoid the requirement for an entered polyglot 
        // context during node instantiation in unit tests.
        protected TestRootNode(Function<Object[], Object> logic) {
            super(null);
            this.logic = logic;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return logic.apply(frame.getArguments());
        }
    }

    @Test
    void testLogicAnd() throws Exception {
        Object op = getOperation("&&");
        assertEquals(true, execute(op, true, true));
        assertEquals(false, execute(op, true, false));
        assertEquals(false, execute(op, false, true));
        assertEquals(false, execute(op, false, false));
    }

    @Test
    void testIdentityAndEquivalence() throws Exception {
        assertTrue((Boolean) execute(getOperation("=="), true, true));
        assertFalse((Boolean) execute(getOperation("=="), true, false));
        assertTrue((Boolean) execute(getOperation("!="), true, false));

        assertTrue((Boolean) execute(getOperation("~~"), true, true));
        assertTrue((Boolean) execute(getOperation("!~"), true, false));
    }

    @Test
    void testLogicOr() throws Exception {
        Object op = getOperation("||");
        assertEquals(true, execute(op, true, true));
        assertEquals(true, execute(op, true, false));
        assertEquals(true, execute(op, false, true));
        assertEquals(false, execute(op, false, false));
    }

    @Test
    void testLogicNot() throws Exception {
        Object op = getOperation("!");
        assertEquals(false, execute(op, true));
        assertEquals(true, execute(op, false));
    }

    @Test
    void testIfTrue() throws Exception {
        Object op = getOperation("?");
        AtomicBoolean executed = new AtomicBoolean(false);
        Object action = createExecutable(() -> executed.set(true));

        // true ? [ action ]
        Object resTrue = execute(op, true, action);
        assertTrue(executed.get(), "Action should execute for true");
        assertEquals(true, resTrue);

        executed.set(false);
        // false ? [ action ]
        Object resFalse = execute(op, false, action);
        assertFalse(executed.get(), "Action should NOT execute for false");
        assertEquals(false, resFalse);
    }

    @Test
    void testIfFalse() throws Exception {
        Object op = getOperation("?!");
        AtomicBoolean executed = new AtomicBoolean(false);
        Object action = createExecutable(() -> executed.set(true));

        // false ?! [ action ]
        Object resFalse = execute(op, false, action);
        assertTrue(executed.get(), "Action should execute for false");
        assertEquals(false, resFalse);

        executed.set(false);
        // true ?! [ action ]
        Object resTrue = execute(op, true, action);
        assertFalse(executed.get(), "Action should NOT execute for true");
        assertEquals(true, resTrue);
    }

    @Test
    void testCombinedTernary() throws Exception {
        Object op = getOperation("? :");
        AtomicBoolean thenExecuted = new AtomicBoolean(false);
        AtomicBoolean elseExecuted = new AtomicBoolean(false);
        
        Object thenAction = createExecutable(() -> thenExecuted.set(true));
        Object elseAction = createExecutable(() -> elseExecuted.set(true));

        // true ? [ then ] : [ else ]
        execute(op, true, thenAction, elseAction);
        assertTrue(thenExecuted.get());
        assertFalse(elseExecuted.get());
        // Note: In Jolk, ternary returns the result of the executed block.
    }

    @Test
    void testElse() throws Exception {
        Object op = getOperation(":");
        AtomicBoolean executed = new AtomicBoolean(false);
        Object action = createExecutable(() -> executed.set(true));

        // false : [ action ]
        Object resFalse = execute(op, false, action);
        assertTrue(executed.get(), "Action should execute for false (else branch)");
        assertEquals(false, resFalse);

        executed.set(false);
        // true : [ action ]
        Object resTrue = execute(op, true, action);
        assertFalse(executed.get(), "Action should NOT execute for true (else branch)");
        assertEquals(true, resTrue);
    }

    @Test
    void testObjectProtocol() throws Exception {
        // toString
        assertEquals("true", execute(getOperation("toString"), true));
        assertEquals("false", execute(getOperation("toString"), false));

        // Hash
        assertEquals((long) Boolean.TRUE.hashCode(), execute(getOperation("hash"), true));

        // Presence
        assertTrue((Boolean) execute(getOperation("isPresent"), true));
        assertFalse((Boolean) execute(getOperation("isEmpty"), false));

        // Class
        assertEquals(JolkBooleanExtension.BOOLEAN_TYPE, execute(getOperation("class"), true));

        // Applied
        String source = """
            class ProtoTest {
                String str(Boolean b) { ^ b #toString }
                Long h(Boolean b) { ^ b #hash }
                Boolean eq(Boolean a, Boolean b) { ^ a ~~ b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals("true", instance.invokeMember("str", true).asString());
        assertEquals((long)Boolean.TRUE.hashCode(), instance.invokeMember("h", true).asLong());
        assertTrue(instance.invokeMember("eq", true, true).asBoolean());
    }

    @Test
    void testPresenceLogic() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Object action = createExecutable(() -> executed.set(true));

        // ifPresent: Booleans are present identities, so action should execute
        Object resPresent = execute(getOperation("ifPresent"), true, action);
        assertTrue(executed.get(), "ifPresent should execute for Boolean");
        // JolkObject.ifPresent returns the result of the action
        assertEquals(JolkNothing.INSTANCE, resPresent);

        executed.set(false);

        // ifEmpty: Booleans are NOT empty, so action should NOT execute
        Object resEmpty = execute(getOperation("ifEmpty"), true, action);
        assertFalse(executed.get(), "ifEmpty should NOT execute for Boolean");
        // JolkObject.ifEmpty returns self (the receiver)
        assertEquals(true, resEmpty);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object op = getOperation("instanceOf");
        Object match = execute(op, true, JolkBooleanExtension.BOOLEAN_TYPE);
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(match, "isPresent"));
    }

    @Test
    void testBooleanField() {
        String source = "class Container { Boolean val; }";
        Value meta = eval(source);

        Value instance = meta.invokeMember("new");
        assertFalse(instance.invokeMember("val").isNull(), "Boolean fields should default to false.");
        assertFalse(instance.invokeMember("val").asBoolean());

        instance.invokeMember("val", true);
        assertTrue(instance.invokeMember("val").asBoolean());
        instance.invokeMember("val", false);
        assertFalse(instance.invokeMember("val").asBoolean());

        // Canonical #new
        instance = meta.invokeMember("new", true);
        assertTrue(instance.invokeMember("val").asBoolean(), "Canonical #new should initialize Boolean fields.");
    }

    @Test
    void testLogicExpression() {
        String source = "class ExprTest { Boolean run() { ^ true && false || !false } }";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        assertTrue(result.asBoolean(), "The logical expression should evaluate correctly.");
    }

    @Test
    void testLogicOperations() {
        String source = """
            class LogicTest {
                Boolean and(Boolean a, Boolean b) { ^ a && b }
                Boolean or(Boolean a, Boolean b) { ^ a || b }
                Boolean not(Boolean a) { ^ !a }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("and", true, true).asBoolean());
        assertFalse(instance.invokeMember("and", true, false).asBoolean());
        assertTrue(instance.invokeMember("or", false, true).asBoolean());
        assertFalse(instance.invokeMember("or", false, false).asBoolean());
        assertFalse(instance.invokeMember("not", true).asBoolean());
        assertTrue(instance.invokeMember("not", false).asBoolean());
    }

    @Test
    void testEquality() {
        String source = """
            class EqualityTest {
                Boolean eq(Boolean a, Boolean b) { ^ a == b }
                Boolean ne(Boolean a, Boolean b) { ^ a != b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("eq", true, true).asBoolean());
        assertFalse(instance.invokeMember("eq", true, false).asBoolean());
        assertTrue(instance.invokeMember("ne", true, false).asBoolean());
    }
}
