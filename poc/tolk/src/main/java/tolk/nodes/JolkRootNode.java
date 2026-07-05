package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.language.JolkLanguage;

///
/// The root node of a Jolk execution tree.
///
/// This node wraps the top-level [JolkNode] (typically the result of parsing a compilation unit)
/// and serves as the entry point for the Truffle runtime to execute the code. It is responsible
/// for bridging the language context with the AST execution.
///
public final class JolkRootNode extends RootNode {

    @Child
    private JolkNode bodyNode;
    private final String name;
    private final boolean canReturnNonLocally;

    public JolkRootNode(JolkLanguage language, FrameDescriptor frameDescriptor, JolkNode bodyNode, String name, boolean canReturnNonLocally) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
        this.name = name;
        this.canReturnNonLocally = canReturnNonLocally;
    }

    /**
     * Convenience constructor for cases where a FrameDescriptor is not explicitly provided,
     * defaulting to an empty FrameDescriptor.
     */
    public JolkRootNode(JolkLanguage language, JolkNode bodyNode, String name, boolean isMethod) {
        this(language, new FrameDescriptor(), bodyNode, name, isMethod);
    }

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode, String name) {
        this(language, new FrameDescriptor(), bodyNode, name, true);
    }

    public JolkRootNode(JolkLanguage language, JolkNode bodyNode) {
        this(language, new FrameDescriptor(), bodyNode, "root", true);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result;
        try {
            // Safety Guard: Return Nothing if the body is null (synthesized stubs)
            result = (bodyNode != null) ? bodyNode.executeGeneric(frame) : tolk.runtime.JolkNothing.INSTANCE;
        } catch (JolkReturnException e) {
            // Non-Local Return Protocol: Verify if this activation is the 'Lexical Home'.
            // The target identity is the arguments array of the home method activation.
            if (canReturnNonLocally && e.getTarget() == frame.getArguments()) {
                result = e.getResult();
            } else if (e.getTarget() == null && e.getResult() instanceof Throwable t) {
                // Global Exception Protocol: If the target is null, it's a guest error 
                // (e.g., from Exception #throw). We unwrap and sneaky-throw the 
                // original cause to ensure it crosses the interop boundary as 
                // a standard Java exception for host assertions.
                throw sneakyThrow(t);
            } else {
                // Propagate non-local returns to outer scopes or global exceptions 
                // if this is not a method boundary.
                throw e;
            }
        }
        return JolkNode.interopLift(result);
    }

    @TruffleBoundary
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public String getName() {
        return name;
    }
}