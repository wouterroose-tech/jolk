package tolk.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.RootNode;
import tolk.language.JolkLanguage;
import tolk.JolcTestBase;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class JolkClosureTest extends JolcTestBase {

    private JolkClosure createClosure(Function<Object[], Object> logic) {
        // Associate the node with the Jolk language to satisfy sharing layer requirements
        JolkLanguage lang = JolkLanguage.getLanguage();
        TestRootNode rootNode = new TestRootNode(lang, logic);
        CallTarget callTarget = rootNode.getCallTarget();
        return new JolkClosure(callTarget);
    }

    @Test
    void testIsExecutable() {
        eval("");
        context.enter();
        try {
            JolkClosure closure = createClosure(args -> "ok");
            assertTrue(InteropLibrary.getUncached().isExecutable(closure));
        } finally {
            context.leave();
        }
    }

    @Test
    void testExecute() throws Exception {
        eval("");
        context.enter();
        try {
            JolkClosure closure = createClosure(args -> "executed");
            Object result = InteropLibrary.getUncached().execute(closure);
            assertEquals("executed", result);
        } finally {
            context.leave();
        }
    }

    @Test
    void testMembers() throws Exception {
        eval("");
        context.enter();
        try {
            JolkClosure closure = createClosure(args -> null);
            InteropLibrary interop = InteropLibrary.getUncached();
            
            assertTrue(interop.hasMembers(closure));
            assertTrue(interop.isMemberInvocable(closure, "apply"));
            assertTrue(interop.isMemberInvocable(closure, "catch"));
            assertTrue(interop.isMemberInvocable(closure, "finally"));
            assertTrue(interop.isMemberInvocable(closure, "try"));
            
            Object members = interop.getMembers(closure);
            assertTrue(interop.hasArrayElements(members));
        } finally {
            context.leave();
        }
    }

    @Test
    void testApply() throws Exception {
        eval("");
        context.enter();
        try {
            JolkClosure closure = createClosure(args -> {
                if (args.length > 0) return args[0];
                return "default";
            });
            
            Object result = closure.invokeMember("apply", new Object[]{"hello"});
            assertEquals("hello", result);
        } finally {
            context.leave();
        }
    }

    @Test
    void testFinally() throws Exception {
        eval("");
        context.enter();
        try {
            AtomicBoolean cleanupRan = new AtomicBoolean(false);
            JolkClosure cleanup = createClosure(args -> {
                cleanupRan.set(true);
                return JolkNothing.INSTANCE;
            });

            JolkClosure action = createClosure(args -> "result");

            Object result = action.invokeMember("finally", new Object[]{cleanup});
            assertEquals("result", result);
            assertTrue(cleanupRan.get(), "Finally block should have run");
        } finally {
            context.leave();
        }
    }

    @Test
    void testFinallyRunsOnError() {
        eval("");
        context.enter();
        try {
            AtomicBoolean cleanupRan = new AtomicBoolean(false);
            JolkClosure cleanup = createClosure(args -> {
                cleanupRan.set(true);
                return JolkNothing.INSTANCE;
            });

            JolkClosure action = createClosure(args -> {
                throw new RuntimeException("boom");
            });

            assertThrows(RuntimeException.class, () -> {
                action.invokeMember("finally", new Object[]{cleanup});
            });
            assertTrue(cleanupRan.get(), "Finally block should have run even after exception");
        } finally {
            context.leave();
        }
    }

    @Test
    void testCatchMatches() throws Exception {
        eval("");
        context.enter();
        try {
            JolkClosure action = createClosure(args -> {
                throw new TestInteropException("oops");
            });

            Object errorType = IllegalArgumentException.class;
            JolkClosure handler = createClosure(args -> {
                Throwable t = (Throwable) args[0];
                return "Caught: " + t.getMessage();
            });

            Object result = action.invokeMember("catch", new Object[]{errorType, handler});
            assertEquals("Caught: oops", result);
        } finally {
            context.leave();
        }
    }

    @Test
    void testCatchNoMatch() {
        eval("");
        context.enter();
        try {
            JolkClosure action = createClosure(args -> {
                throw new TestInteropException("oops");
            });

            Object errorType = IllegalStateException.class;
            JolkClosure handler = createClosure(args -> "Should not happen");

            assertThrows(IllegalArgumentException.class, () -> {
                action.invokeMember("catch", new Object[]{errorType, handler});
            });
        } finally {
            context.leave();
        }
    }

    @Test
    void testTryWithResources() throws Exception {
        eval("");
        context.enter();
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            JolkObject resource = createMockResource(closed);
            
            // The closure acts as the resource provider
            JolkClosure provider = createClosure(args -> resource);

            JolkClosure logic = createClosure(args -> {
                assertNotNull(args[0]);
                return "success";
            });

            Object result = provider.invokeMember("try", new Object[]{logic});
            assertEquals("success", result);
            assertTrue(closed.get(), "Resource should be closed");
        } finally {
            context.leave();
        }
    }

    @Test
    void testTryWithResourcesClosesOnException() {
        eval("");
        context.enter();
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            JolkObject resource = createMockResource(closed);
            
            JolkClosure provider = createClosure(args -> resource);
            JolkClosure logic = createClosure(args -> {
                throw new RuntimeException("fail");
            });

            assertThrows(RuntimeException.class, () -> {
                provider.invokeMember("try", new Object[]{logic});
            });
            assertTrue(closed.get(), "Resource should be closed even after exception");
        } finally {
            context.leave();
        }
    }

    @Test
    //@Disabled("Activate when interop protocol is implemented")
    void testTryWithAutoCloseableHostResource() throws Exception {
        eval("");
        context.enter();
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            AutoCloseable resource = () -> closed.set(true);

            JolkClosure provider = createClosure(args -> resource);
            JolkClosure logic = createClosure(args -> {
                assertNotNull(args[0]);
                return "success";
            });

            Object result = provider.invokeMember("try", new Object[]{logic});
            assertEquals("success", result);
            assertTrue(closed.get(), "Host AutoCloseable resources should be closed");
        } finally {
            context.leave();
        }
    }

    static class TestRootNode extends RootNode {
        private final Function<Object[], Object> logic;

        protected TestRootNode(JolkLanguage language, Function<Object[], Object> logic) {
            super(language);
            this.logic = logic;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return logic.apply(frame.getArguments());
        }
    }

    private JolkObject createMockResource(AtomicBoolean closedSignal) {
        JolkClosure closeAction = createClosure(args -> {
            closedSignal.set(true);
            return JolkNothing.INSTANCE;
        });
        Map<String, Object> members = new HashMap<>();
        members.put("close", closeAction);
        JolkMetaClass resourceClass = new JolkMetaClass("MockResource", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, members);
        return new JolkObject(resourceClass);
    }
    
}
