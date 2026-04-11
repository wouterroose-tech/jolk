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
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.nodes.JolkDispatchNode;

/// # JolkObject
/// 
/// The base class for all objects in the Jolk Object Model (JoMoo).
/// This includes instances of classes defined in the language, as well as any built-in objects.
/// Currently, this is just a placeholder to establish the object model structure.
/// In the future, this class will be expanded to include fields, methods, and other features of the object model.
/// 
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

    /**
     * Bridge method for unit tests to verify state. 
     * Maps the legacy index-based access to the modern Shape-based model.
     */
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
    public boolean isMemberInvocable(String member) {
        // Recognise methods, intrinsics, and fields as invocable targets
        return metaClass.hasInstanceMember(member) 
            || JolkDispatchNode.isObjectIntrinsic(member)
            || metaClass.getFieldIndex(member) != -1;
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return metaClass.getFieldIndex(member) != -1;
    }

    @ExportMessage
    public Object readMember(String member,
                             @CachedLibrary("this") DynamicObjectLibrary objLib) throws UnknownIdentifierException {
        if (metaClass.getFieldIndex(member) != -1) {
            return objLib.getOrDefault(this, member, JolkNothing.INSTANCE);
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @CachedLibrary(limit = "3") InteropLibrary interop,
                        @CachedLibrary("this") DynamicObjectLibrary objLib) 
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
            return result == null ? JolkNothing.INSTANCE : result;
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

        // 2. Handle standard intrinsic protocol via the central dispatcher (ObjectExtension).
        Object intrinsicResult = JolkDispatchNode.dispatchObjectIntrinsic(this, name, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
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