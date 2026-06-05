package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import tolk.language.JolkContext;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkNothing;

/**
 * ### JolkReadTypeNode
 * 
 * Resolves a Meta-Object (Type) identity at runtime.
 * This node defer the lookup to the execution phase, allowing for forward
 * references and cross-unit resolution that cannot be performed at parse-time.
 */
public final class JolkReadTypeNode extends JolkExpressionNode {
    private final String typeName;
    private final String packageName;
    @Child private JolkNode metaReceiver;

    public JolkReadTypeNode(String typeName) {
        this(null, typeName, "", null);
    }

    public JolkReadTypeNode(String typeName, JolkNode metaReceiver) {
        this(null, typeName, "", metaReceiver);
    }

    public JolkReadTypeNode(JolkLanguage language, String typeName, String packageName, JolkNode metaReceiver) {
        this.typeName = typeName;
        this.packageName = packageName;
        this.metaReceiver = metaReceiver;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkContext context = JolkLanguage.getLanguage(this).getContextReference().get(this);

        // Priority 1: Qualified Identifier Resolution
        if (typeName.contains(".")) {
            Object qualified = context.getDefinedClass(typeName);
            if (qualified != null) return qualified;
        }

        // Priority 0: Package-Local Resolution
        // Enables classes within the same package to refer to each other by their short names.
        if (!packageName.isEmpty() && !typeName.contains(".")) {
            Object localType = context.getDefinedClass(packageName + "." + typeName);
            if (localType != null) return localType;
        }

        // Priority 1: Meta-Projection Resolution (Expansion/Lenses)
        // Ensures that Guest expansions (e.g. + demo.Validation) shadow Global Host classes.
        Object projected = context.lookupProjection(typeName);
        if (projected != null) {
            if (projected instanceof String path) {
                // Resolve the FQN path into a Solid meta-object to prevent Sending #new to a String.
                return JolkNode.lift(context.getOrCreateClass(path));
            }
            return JolkNode.lift(projected);
        }

        // Priority 2: Global Type / Host Resolution
        Object type = context.getDefinedClass(typeName);
        if (type != null) return type;

        // Priority 3: Meta-Constant/Field Fallback (Message to Self's class)
        if (metaReceiver != null) {
            Object metaObj = metaReceiver.executeGeneric(frame);
            if (metaObj != null && metaObj != JolkNothing.INSTANCE) {
                try {
                    InteropLibrary interop = InteropLibrary.getUncached(metaObj);
                    // Identity Resolution: Prioritize readable fields/constants over invocables.
                    if (interop.isMemberReadable(metaObj, typeName)) {
                        return JolkNode.lift(interop.readMember(metaObj, typeName));
                    } else if (interop.isMemberInvocable(metaObj, typeName)) {
                        // Returning the member itself for potential later invocation or reference.
                        return JolkNode.lift(interop.invokeMember(metaObj, typeName));
                    }
                } catch (UnsupportedMessageException | UnknownIdentifierException | ArityException | UnsupportedTypeException e) {
                    // Fall through to Nothing
                }
            }
        }

        return JolkNothing.INSTANCE;
    }
}