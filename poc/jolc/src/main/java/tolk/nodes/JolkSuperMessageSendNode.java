package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.NodeInfo;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;

/**
 * ### JolkSuperMessageSendNode
 * 
 * Implements the `super #message` dispatch protocol.
 * 
 * In Jolk, `super` is not a pseudo-variable but a message dispatch modifier. 
 * It instructs the runtime to resolve the selector starting from the superclass 
 * of the class that defines the calling method, while keeping the current 
 * `self` as the receiver.
 */
@NodeInfo(shortName = "super")
public class JolkSuperMessageSendNode extends JolkExpressionNode {

    @Children private final JolkNode[] argumentNodes;
    private final String selector;
    private JolkMetaClass holderClass; // The class where this code is physically defined

    public JolkSuperMessageSendNode(String selector, JolkNode[] argumentNodes) {
        this.selector = selector;
        this.argumentNodes = argumentNodes;
    }

    /**
     * Binds this node to the class identity that contains it. 
     * This is called during the class hydration phase.
     */
    public void setHolderClass(JolkMetaClass holderClass) {
        this.holderClass = holderClass;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // 1. Evaluate 'self' (Receiver is always the first argument in Jolk methods)
        Object self = (frame.getArguments().length > 0) ? frame.getArguments()[0] : JolkNothing.INSTANCE;
        
        if (holderClass == null) {
            throw new RuntimeException("Unbound super call: the definition node failed to hydrate the super context.");
        }
        
        // 2. Resolve the starting point for lookup (the superclass of the definition context)
        JolkMetaClass startClass = holderClass.getSuperclass();
        if (startClass == null) {
            throw new RuntimeException("Message not understood: #" + selector + " on super of root Object");
        }

        // 3. Perform Member Lookup bypassing the receiver's class overrides
        // We distinguish between instance-level super and meta-level super calls.
        Object member = (self instanceof JolkMetaClass) 
            ? (JolkMetaClass.isObjectIntrinsic(selector) ? null : startClass.lookupMetaMember(selector))
            : startClass.lookupInstanceMember(selector);

        Object[] args = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            args[i] = argumentNodes[i].executeGeneric(frame);
        }

        try {
            if (member != null) {
                Object[] executeArgs = new Object[args.length + 1];
                executeArgs[0] = self; // Preserve the receiver identity
                System.arraycopy(args, 0, executeArgs, 1, args.length);
                return lift(InteropLibrary.getUncached().execute(member, executeArgs));
            }
            if (self instanceof JolkMetaClass metaSelf) {
                // Meta-level super dispatch uses the superclass lookup start point but preserves
                // the original class receiver so inherited intrinsics like #new behave correctly.
                return lift(JolkMetaClass.invokeSuperMember(metaSelf, startClass, selector, args, InteropLibrary.getUncached()));
            }
            throw new RuntimeException("Message not understood: #" + selector + " in super hierarchy of " + holderClass.name);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException | UnknownIdentifierException e) {
            throw new RuntimeException("Super message dispatch failed: #" + selector + " in " + holderClass.name, e);
        }
    }
}
