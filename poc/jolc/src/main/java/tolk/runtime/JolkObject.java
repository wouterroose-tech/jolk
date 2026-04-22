package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.interop.InteropLibrary;
import java.util.HashMap;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.nodes.JolkDispatchNode;
import tolk.nodes.JolkNode;

/// # JolkObject (The JoMoo Kernel)
///
/// The base class for all identities in the **Jolk Message-Oriented Object (JoMoo)** model. 
/// It represents a high-density synthesis of Java’s structural discipline and Smalltalk’s 
/// dynamic philosophy.
///
/// Adhering to the principle of **Industrial Sovereignty**, this class ensures that every 
/// interaction is a formal message send. It implements **Identity Restitution**, ensuring 
/// that substrate-level `void` and `null` are transformed into valid terminal responses 
/// within a **Closed-Loop Object Heap**.
@ExportLibrary(InteropLibrary.class)
public class JolkObject extends DynamicObject {

    /// The root MetaClass for the Jolk Object-Model.
    /// This identity has no superclass and serves as the terminus
    /// for all message dispatch delegation.
    public static final JolkMetaClass OBJECT_TYPE = new JolkMetaClass(
        "Object", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, new HashMap<>(), new HashMap<>()
    );

    private final JolkMetaClass metaClass;

    public JolkObject(JolkMetaClass metaClass) {
        this(metaClass, null);
    }

    public JolkObject(JolkMetaClass metaClass, Object[] args) {
        super(metaClass.getInstanceShape()); // Initialize DynamicObject with the shape
        this.metaClass = metaClass;
        // Ensure structural layout is finalized before allocating the state substrate
        this.metaClass.initializeDefaultValues();
        
        // If the metaClass failed to hydrate (e.g. forward ref still unresolved),
        // we must fallback to an empty or placeholder layout to avoid index out of bounds.
        if (!this.metaClass.hydrated) {
            return;
        }
        
        DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
        Object[] defaultValues = metaClass.getDefaultFieldValues();
        String[] allFieldNames = metaClass.getFlattenedFieldNames();

        // Selection Logic: Populate from provided arguments (canonical #new) 
        // or fallback to the template defaults.
        Object[] sourceValues = (args != null && args.length == allFieldNames.length) ? args : defaultValues;

        for (int i = 0; i < allFieldNames.length; i++) {
            objLib.put(this, allFieldNames[i], sourceValues[i]);
        }
    }

    /// ### getJolkMetaClass
    ///
    /// Returns the {@link JolkMetaClass} that describes this instance.
    public JolkMetaClass getJolkMetaClass() {
        return metaClass;
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    /// ### getMetaObject
    /// 
    /// Returns the [JolkMetaClass] that serves as the type identity for this instance.
    /// This allows the GraalVM Polyglot API to resolve the meta-object correctly.
    @ExportMessage
    public Object getMetaObject() {
        return metaClass;
    }

    /// Bridge method for unit tests to verify state. 
    /// Maps the legacy index-based access to the modern Shape-based model.
    @TruffleBoundary
    public Object getFieldValue(int index) {
        String[] names = metaClass.getFlattenedFieldNames();
        if (index >= 0 && index < names.length) {
            // Use the uncached library for manual testing access
            return DynamicObjectLibrary.getUncached().getOrDefault(this, names[index], JolkNothing.INSTANCE);
        }
        return JolkNothing.INSTANCE;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        return metaClass.getInstanceMemberNames();
    }

    @ExportMessage
    public boolean isMemberInvocable(String member,
                                     @Exclusive @CachedLibrary(limit = "3") InteropLibrary interop) {
        // Recognise methods, intrinsics, and fields as invocable targets
        return metaClass.hasInstanceMember(member) 
            || JolkDispatchNode.isObjectIntrinsic(member)
            || metaClass.getFieldIndex(member) != -1
            || interop.isMemberInvocable(metaClass, member);
    }

    @ExportMessage
    public boolean isMemberReadable(String member,
                                    @Exclusive @CachedLibrary(limit = "3") InteropLibrary interop) {
        return metaClass.getFieldIndex(member) != -1 || interop.isMemberReadable(metaClass, member);
    }

    @ExportMessage
    public Object readMember(String member,
                             @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib,
                             @Exclusive @CachedLibrary(limit = "3") InteropLibrary interop) throws UnknownIdentifierException, UnsupportedMessageException {
        if (metaClass.getFieldIndex(member) != -1) {
            return objLib.getOrDefault(this, member, JolkNothing.INSTANCE);
        }
        if (interop.isMemberReadable(metaClass, member)) {
            return JolkNode.lift(interop.readMember(metaClass, member));
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @Exclusive @CachedLibrary(limit = "3") InteropLibrary interop,
                        @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) 
                        throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        String name = member;

        // 1. Prioritize user-defined members for overridable selectors.
        if (metaClass.hasInstanceMember(name)) {
            Object instanceMember = metaClass.lookupInstanceMember(name);
            // Prepend 'this' (receiver) to arguments as Jolk instance members expect it
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            Object result = interop.execute(instanceMember, argsWithReceiver);
            return JolkNode.lift(result);
        }

        // 2. Property Projection: Map message sends to DynamicObject slot access
        if (metaClass.getFieldIndex(name) != -1) {
            if (arguments.length == 0) {
                // Getter Pattern: #field
                return objLib.getOrDefault(this, name, JolkNothing.INSTANCE);
            } else if (arguments.length == 1) {
                // Setter Pattern: #field(val) -> Returns Self (Fluent Contract)
                objLib.put(this, name, arguments[0]);
                return this;
            }
            throw ArityException.create(0, 1, arguments.length);
        }

        // 3. Handle standard intrinsic protocol via the central dispatcher (ObjectExtension).
        Object intrinsicResult = JolkDispatchNode.dispatchObjectIntrinsic(this, name, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        // 4. Meta-Stratum Delegation (Dual-Stratum Resolution)
        // If the selector is not an instance member, check the MetaClass identity.
        if (interop.isMemberInvocable(metaClass, name)) {
            return JolkNode.lift(interop.invokeMember(metaClass, name, arguments));
        }

        throw UnknownIdentifierException.create(member);
    }

    /// ### toString
    ///
    /// Returns the Jolk-standard string representation of the object.
    /// Overriding this at the Java level ensures that the [JolkDispatchNode]
    /// intrinsic path remains consistent with the object protocol.
    @Override
    public String toString() {
        return "instance of " + metaClass.name;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @ExportMessage
    public String toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return toString();
    }
}