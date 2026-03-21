package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass
/// 
/// Represents a Jolk Type (a meta-object) at runtime.
///
/// This is the Java implementation of the `MetaClass` concept defined in Jolk.
/// It acts as the first-class identity for Classes, Records, and Enums. As a
/// meta-object, it is responsible for handling meta-level messages such as
/// `#new` (instance creation) and type introspection (`#name`, `#superclass`).
/// 
/// It also serves as a container for the definitions of members (fields and methods)
/// that belong to instances of this type. However, it does not execute instance-level
/// messages itself; that is the responsibility of {@link JolkObject}.
///
@ExportLibrary(InteropLibrary.class)
public final class JolkMetaClass implements TruffleObject {

    private final String name;
    private final JolkMetaClass superclass;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    // Instance members (fields and methods) for instances of this class.
    private final Map<String, Object> instanceMembers;
    // Meta members (e.g. user-defined meta methods).
    private final Map<String, Object> metaMembers;

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, Collections.emptyMap());
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, metaMembers);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.metaMembers = metaMembers;
    }

    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaQualifiedName() {
        return name;
    }

    @ExportMessage
    Object getMetaSimpleName() {
        return name;
    }

    @ExportMessage
    boolean isMetaInstance(Object instance) {
        // In Jolk, everything is an Object, including Nothing.
        if ("Object".equals(this.name)) {
            return instance instanceof JolkObject || instance == JolkNothing.INSTANCE;
        }
        if (instance == JolkNothing.INSTANCE) {
            return this == JolkNothing.NOTHING_TYPE;
        }
        if (instance instanceof JolkObject jolkObject) {
            JolkMetaClass current = jolkObject.getJolkMetaClass();
            while (current != null) {
                if (current == this) return true;
                current = current.superclass;
            }
            return false;
        }
        return false;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        // This returns the members of the META-OBJECT, not the instance.
        Set<String> keys = new HashSet<>(metaMembers.keySet());
        keys.add("new");
        keys.add("name");
        keys.add("superclass");
        keys.add("isInstance");
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return metaMembers.containsKey(member);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        // This checks if a message can be sent TO THE META-OBJECT itself.
        return metaMembers.containsKey(member) || switch (member) {
            case "new", "name", "superclass", "isInstance" -> true;
            default -> false;
        };
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (metaMembers.containsKey(member)) {
            Object memberObj = metaMembers.get(member);
            return InteropLibrary.getUncached().execute(memberObj, arguments);
        }
        switch (member) {
            case "new":
                if (arguments.length != 0) {
                    throw ArityException.create(0, 0, arguments.length);
                }
                return new JolkObject(this);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return superclass != null ? superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return isMetaInstance(arguments[0]);
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        if (metaMembers.containsKey(member)) {
            return metaMembers.get(member);
        }
        throw UnknownIdentifierException.create(member);
    }

    // --- Instance member lookup (for JolkObject) ---

    /**
     * ## hasInstanceMember
     *
     * Checks if a member with the given name exists on instances of this class.
     * This is used by `JolkObject` to determine if it can respond to an
     * instance-level message.
     *
     * @param name The name of the member.
     * @return `true` if an instance member with that name exists.
     */
    public boolean hasInstanceMember(String name) {
        return instanceMembers.containsKey(name);
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (e.g., a `JolkClosure`) by name.
     */
    public Object lookupInstanceMember(String name) {
        return instanceMembers.get(name);
    }

    /**
     * ## getInstanceMemberKeys
     *
     * @return A set of all instance member names.
     */
    public Set<String> getInstanceMemberKeys() {
        return instanceMembers.keySet();
    }
}
