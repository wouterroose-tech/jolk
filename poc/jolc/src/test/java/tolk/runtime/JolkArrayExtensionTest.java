package tolk.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.RootNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;
import org.graalvm.polyglot.Value;

/**
 * ## JolkArrayExtensionTest
 *
 * Validates the runtime implementation of the Array archetype extensions.
 * Verifies that Jolk selectors like #at, #put, and #new correctly map to 
 * java.util.List operations while adhering to the Jolk messaging protocol.
 */
public class JolkArrayExtensionTest extends JolcTestBase {

    private final InteropLibrary interop = InteropLibrary.getUncached();

    private JolkClosure createClosure(Function<Object[], Object> logic) {
        TestRootNode rootNode = new TestRootNode(logic);
        CallTarget callTarget = rootNode.getCallTarget();
        return new JolkClosure(callTarget);
    }

    /**
     * Verifies the meta-level factory method for creating native lists.
     * Jolk: ArrayList #new
     */
    @Test
    void testNew() throws Exception {
        eval("");
        context.enter();
        try {
            // Use the Polyglot context to wrap the internal Truffle result
            Object rawResult = JolkArrayExtension.ARRAY_TYPE.callMetaMember("new", new Object[]{});
            Value result = context.asValue(rawResult);
            
            assertNotNull(result);
            assertTrue(result.isHostObject(), "Factory #new should create a Host Object.");
            List<?> list = result.as(List.class);
            assertEquals(0, list.size());
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies the meta-level factory method for creating native lists.
     * Jolk: ArrayList #new(1, 2, 3)
     */
    @Test
    void testVariadicNew() throws Exception {
        eval("");
        context.enter();
        try {
            Object rawResult = JolkArrayExtension.ARRAY_TYPE.callMetaMember("new", new Object[]{1L, 2L, 3L});
            Value result = context.asValue(rawResult);
            
            assertNotNull(result);
            assertTrue(result.isHostObject(), "Factory #new should create a Host Object.");
            List<?> list = result.as(List.class);
            assertEquals(3, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(3L, list.get(2));
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies positional retrieval via #at.
     * Jolk: list #at(1)
     */
    @Test
    void testAt() throws Exception {
        eval("");
        context.enter();
        try {
            List<String> list = new ArrayList<>(List.of("first", "second"));
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("at");
            
            // Wrap the result to access standard Polyglot coercion (like asString)
            Object rawResult = interop.execute(method, guestList, 1L);
            Value result = context.asValue(rawResult);
            assertEquals("second", result.asString());
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies mutation and Receiver Retention.
     * Jolk: list #put(0, "new")
     */
    @Test
    void testPut() throws Exception {
        eval("");
        context.enter();
        try {
            List<Object> list = new ArrayList<>(List.of("old"));
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("put");
            
            // Jolk Protocol: execute(receiver, index, value). Returns receiver.
            Object rawResult = interop.execute(method, guestList, 0L, "new");
            Value result = context.asValue(rawResult);
            
            assertEquals("new", list.get(0));
            assertEquals(context.asValue(guestList), result, "#put should return the receiver for fluent chaining.");
        } finally {
            context.leave();
        }
    }

    @Test
    void testAtNullRestitution() throws Exception {
        eval("");
        context.enter();
        try {
            List<Object> list = new ArrayList<>();
            list.add(null);
            // Identity Restitution: Wrap raw Java object as a Guest-visible HostObject.
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("at");
            
            Object rawResult = interop.execute(method, guestList, 0L);
            assertEquals(JolkNothing.INSTANCE, rawResult, "Raw null from list should be lifted to JolkNothing.");
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies the anyMatch method with closure predicate.
     * Jolk: list #anyMatch({ |x| x > 5 })
     */
    @Test
    void testAnyMatch() throws Exception {
        eval("");
        context.enter();
        try {
            // Create test data: list with elements where some satisfy the condition
            List<Long> list = new ArrayList<>(List.of(1L, 3L, 7L, 2L));
            Object guestList = JolkLanguage.getContext().env.asGuestValue(list);
            
            // Create a closure that tests if element > 5
            JolkClosure predicate = createClosure(args -> {
                Long element = (Long) args[0];
                return element > 5L;
            });
            Object guestPredicate = JolkLanguage.getContext().env.asGuestValue(predicate);
            
            Object method = JolkArrayExtension.ARRAY_TYPE.lookupInstanceMember("anyMatch");
            
            // Execute anyMatch with the closure
            Object rawResult = interop.execute(method, guestList, guestPredicate);
            Value result = context.asValue(rawResult);
            
            // Should return true since 7 > 5
            assertTrue(result.asBoolean());
            
            // Test with a list where no elements satisfy the condition
            List<Long> noMatchList = new ArrayList<>(List.of(1L, 2L, 3L, 4L));
            Object guestNoMatchList = JolkLanguage.getContext().env.asGuestValue(noMatchList);
            
            Object rawNoMatchResult = interop.execute(method, guestNoMatchList, predicate);
            Value noMatchResult = context.asValue(rawNoMatchResult);
            
            // Should return false since no element > 5
            assertFalse(noMatchResult.asBoolean());
        } finally {
            context.leave();
        }
    }

    static class TestRootNode extends RootNode {
        private final Function<Object[], Object> logic;

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
    void testNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new; 
                ArrayList<Long> run() { ^ ArrayList<Long> #new }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value longList = instance.invokeMember("longList");
        assertNotNull(longList, "Field 'longList' should be initialized.");
        assertNotEquals(JolkNothing.INSTANCE, longList);
        assertTrue(longList.isHostObject(), "Result of ArrayList #new should be a Host Object.");
        assertEquals(ArrayList.class, longList.asHostObject().getClass());
        
        Value runValue = instance.invokeMember("run");
        assertNotNull(runValue, "Method 'run' should return a value.");
        // Verify the result is a native Java ArrayList (Shim-less Integration)
        assertTrue(runValue.isHostObject(), "Result of ArrayList #new should be a Host Object.");
        assertEquals(ArrayList.class, runValue.asHostObject().getClass());
    }

    @Test
    void testVariadicNewArray() {
        String source = """
            class MyClass {
                ArrayList<Long> longList = ArrayList #new(1, 2, 3);
                Long run(Int key) { ^ self #longList #at(key) }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value longList = instance.invokeMember("longList");
        assertEquals(3, ((List<?>) longList.asHostObject()).size());
        assertNotNull(longList);
        assertEquals(ArrayList.class, longList.asHostObject().getClass());
        assertEquals(1, instance.invokeMember("run", 0).asLong());
    }

    @Test
    void testArrayLiteral() {
        String source = """
            class ArrayLiteralTest {
                ArrayList<Long> emptyList = #[];
                ArrayList<Long> longList = #[1, 2, 3];
                ArrayList<String> colors = #["red", "green", "blue"];
                Long run(Int key) { ^ self #longList #at(key) }  
                Long run() { ^ self #longList #put(1, 42) #at(1) } 
                String run(Int key, String color) { ^ self #colors #put(key, color) #at(key) }                
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value emptyList = instance.invokeMember("emptyList");
        assertEquals(0, ((List<?>) emptyList.asHostObject()).size());
        Value longList = instance.invokeMember("longList");
        assertEquals(3, ((List<?>) longList.asHostObject()).size());
        assertEquals(1L, instance.invokeMember("run", 0).asLong()); 
        assertEquals(42, instance.invokeMember("run").asLong()); 
        Value colors = instance.invokeMember("colors");
        assertEquals(3, ((List<?>) colors.asHostObject()).size());
        assertEquals("red", ((List<?>) colors.asHostObject()).get(0).toString());
        assertEquals("RED", instance.invokeMember("run", 0, "RED").toString()); 
    } 

    @Test
    void testAnyMatch_2() {
        String source = """
            class AnyMatchTest {
                ArrayList<Long> elements = #[1, 2, 3];
                Boolean run(Long x) { ^ self #elements #anyMatch [s -> s == x] }          
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertFalse(instance.invokeMember("run", 0).asBoolean()); 
        assertTrue(instance.invokeMember("run", 2).asBoolean()); 
    }     

    @Test
    void testFindFirst() {
        String source = """
            class FindFirstTest {
                ArrayList<Long> elements = #[1, 2, 3];
                Long run(Long x) { ^ self #elements #findFirst [s -> s == x] }          
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(2, instance.invokeMember("run", 2).asLong()); 
        assertEquals("null", instance.invokeMember("run", 0).toString(), "Should return Nothing when match is not found"); 
    }   

    @Test
    @Disabled
    void testFilter() {
        String source = """
            class FindFirstTest {
                ArrayList<Long> elements = #[1, 2, 3];
                Boolean test(Long x) { ^ self #elements #filter [s -> s == x] #size == 1}          
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.invokeMember("test", 2).asBoolean()); 
        assertTrue(instance.invokeMember("test", 0).asBoolean()); 
    }   

    @Test
    void testStream() {
        String source = """
            + java.util.ArrayList;
            class Test {
                String run() {
                    ArrayList<String> colors = Array #new("red", "green", "blue");
                    ^ colors #stream #reduce("", [ reduced, reducing -> reduced + reducing ])
                }
                String reduce(String reduced, String reducing) {
                    ^ reduced + reducing
                }          
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals("redgreenblue", instance.invokeMember("run").toString()); 
    }  

}
