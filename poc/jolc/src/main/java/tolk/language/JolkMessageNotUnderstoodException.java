package tolk.language;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.interop.InteropLibrary;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkExceptionExtension;
import tolk.runtime.JolkMemberNames;
import tolk.nodes.JolkNode;
import java.util.Collections;

/// ### MessageNotUnderstood
/// 
/// Reifies a dispatch failure as a first-class **Atomic Identity**. 
/// 
/// In the standard JVM, dispatch failures are fragmented across multiple types 
/// (e.g., `ClassCastException`, `NoSuchMethodError`, `UnknownIdentifierException`). 
/// Jolk acts as a **Semantic Harmonizer**, projecting these disparate substrate-level 
/// failures onto this singular identity.
/// 
/// This ensures that communication failures—whether they occur on native Jolk 
/// archetypes, polyglot guest objects (JS/Python), or host Java proxies—are 
/// treated as manageable guest-level events.
/// 
/// @author Wouter Roose
@ExportLibrary(InteropLibrary.class)
public class JolkMessageNotUnderstoodException extends RuntimeException implements TruffleObject {

    /// ### TYPE
    /// 
    /// The first-class meta-object for the `MessageNotUnderstood` identity. 
    /// This descriptor anchors the failure within Jolk's formal type lattice, 
    /// allowing guest-level code to perform targeted recovery logic via the `#catch` protocol.
    public static final JolkMetaClass TYPE = new JolkMetaClass(
        "MessageNotUnderstood",
        JolkExceptionExtension.EXCEPTION_TYPE,
        JolkFinality.FINAL,
        JolkVisibility.PUBLIC,
        JolkArchetype.CLASS,
        Collections.emptyMap(),
        Collections.emptyMap()
    );

    private final Object receiver;
    private final String selector;

    /// ### Constructor
    /// 
    /// Reifies a communication failure into a first-class, manageable identity.
    /// 
    /// @param receiver The object that received the message.
    /// @param selector The name of the message that was not understood.
    @TruffleBoundary
    public JolkMessageNotUnderstoodException(Object receiver, String selector) {
        super("Message not understood: #" + selector + " on " + receiver);
        this.receiver = receiver;
        this.selector = selector;
    }

    /// ### getReceiver
    /// 
    /// Returns the **Receiver** that failed to process the message.
    /// 
    /// @return the receiver object.
    public Object getReceiver() { return receiver; }

    /// ### getSelector
    /// 
    /// Returns the name (selector) of the message that was not understood.
    /// 
    /// @return the selector name.
    public String getSelector() { return selector; }

    /// ### hasMembers
    /// 
    /// Signals that this exception reifies its internal context as readable members.
    @ExportMessage
    public boolean hasMembers() { return true; }

    /// ### getMembers
    /// 
    /// Returns the collection of members available on this exception identity.
    /// This provides the **Metaboundary** bridge allowing guest Jolk logic
    /// to inspect the `receiver` and `selector` properties.
    @ExportMessage
    public final Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        return new JolkMemberNames(new String[]{"receiver", "selector"});
    }

    /// ### isMemberReadable
    /// 
    /// Validates if a specific diagnostic field is accessible via the protocol.
    @ExportMessage
    public boolean isMemberReadable(String member) {
        return "receiver".equals(member) || "selector".equals(member);
    }

    /// ### readMember
    /// 
    /// Performs **Identity Projection**, allowing Jolk code to extract 
    /// the context of the dispatch failure for diagnostic or recovery purposes.
    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        return switch (member) {
            case "receiver" -> JolkNode.lift(receiver);
            case "selector" -> JolkNode.lift(selector);
            default -> throw UnknownIdentifierException.create(member);
        };
    }

    /// ### hasMetaObject
    /// 
    /// Signals that this identity possesses a first-class meta-descriptor.
    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    /// ### getMetaObject
    /// 
    /// Returns the {@link #TYPE} identity for this exception.
    @ExportMessage
    public Object getMetaObject() {
        return TYPE;
    }
}