package tolk.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import java.util.HashSet;
import java.util.HashMap;
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
    // Instance fields (Sovereign Coordinate Map).
    private final Map<String, Object> instanceFields;
    // Map field names to their array index for O(1) access
    private final Map<String, Integer> fieldIndices;
    // Cache synthetic accessors to prevent allocation on every lookup
    private final Map<String, JolkSyntheticAccessor> accessorCache;
    // Total number of fields including hierarchy
    private final int totalFieldCount;
    // Default field values
    private final Object[] defaultFieldValues;
    // Meta members (e.g. user-defined meta methods).
    private final Map<String, Object> metaMembers;

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), Collections.emptyMap());
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), metaMembers);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), metaMembers);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
        this.fieldIndices = new HashMap<>();
        this.accessorCache = new HashMap<>();
        
        // Calculate field layout: Start after superclass fields to support inheritance
        int currentIndex = (superclass != null) ? superclass.getFieldCount() : 0;
        for (String fieldName : instanceFields.keySet()) {
            fieldIndices.put(fieldName, currentIndex);
            accessorCache.put(fieldName, new JolkSyntheticAccessor(fieldName, currentIndex));
            currentIndex++;
        }
        this.totalFieldCount = currentIndex;
        
        this.defaultFieldValues = new Object[totalFieldCount];
        if (superclass != null) {
            System.arraycopy(superclass.defaultFieldValues, 0, this.defaultFieldValues, 0, superclass.totalFieldCount);
        }
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Integer idx = fieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();
                if (val == null) {
                    // Heuristic for the PoC: if the field name or context implies a Long
                    // (often signaled by passing the Long MetaClass as a sentinel in visitors),
                    // we default to 0L. Otherwise, we fall back to the Nothing identity.
                    this.defaultFieldValues[idx] = JolkNothing.INSTANCE;
                } else if (val instanceof JolkMetaClass mc && "Long".equals(mc.getMetaSimpleName())) {
                    this.defaultFieldValues[idx] = 0L;
                } else {
                    this.defaultFieldValues[idx] = val;
                }
            }
        }
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
        // 1. Handle Jolk's reified null (`Nothing`)
        if (instance == JolkNothing.INSTANCE) {
            // Nothing is an instance of its own type and also of Object.
            return this == JolkNothing.NOTHING_TYPE || "Object".equals(this.name);
        }

        // 2. Handle Jolk's intrinsic `Long` (represented by java.lang.Long or java.lang.Integer)
        if (instance instanceof Long || instance instanceof Integer) {
            // Both Long and Integer are treated as instances of Long and also of Object.
            return "Long".equals(this.name) || "Object".equals(this.name);
        }

        // 3. Handle standard JolkObjects by walking their class hierarchy.
        if (instance instanceof JolkObject jolkObject) {
            JolkMetaClass current = jolkObject.getJolkMetaClass();
            while (current != null) {
                if (current == this) return true;
                current = current.superclass;
            }
        }

        // 4. For the PoC, other Java objects (e.g., String) are not considered
        // instances of any Jolk type to ensure runtime safety.
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
                    // Support Canonical #new if arguments match field count
                    if (arguments.length == totalFieldCount) {
                        return new JolkObject(this, arguments);
                    }
                    throw ArityException.create(totalFieldCount, totalFieldCount, arguments.length);
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
        return instanceMembers.containsKey(name) || instanceFields.containsKey(name);
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (e.g., a `JolkClosure`) by name.
     */
    public Object lookupInstanceMember(String name) {
        Object member = instanceMembers.get(name);
        if (member != null) {
            // If the parser erroneously puts the field definition in instanceMembers,
            // we detect the duplicate and prefer the synthetic accessor.
            if (instanceFields.containsKey(name) && instanceFields.get(name) == member) {
                return accessorCache.get(name);
            }
            return member;
        }
        // Virtual Field Strategy: Hydrate a specialized node if the field exists.
        if (instanceFields.containsKey(name)) {
            return accessorCache.get(name);
        }
        // Recurse up the hierarchy
        if (superclass != null) {
            return superclass.lookupInstanceMember(name);
        }
        return null;
    }

    /**
     * ## getInstanceMemberKeys
     *
     * @return A set of all instance member names.
     */
    public Set<String> getInstanceMemberKeys() {
        Set<String> keys = new HashSet<>(instanceMembers.keySet());
        keys.addAll(instanceFields.keySet());
        return keys;
    }

    public int getFieldCount() {
        return totalFieldCount;
    }

    public int getFieldIndex(String name) {
        Integer index = fieldIndices.get(name);
        if (index != null) return index;
        if (superclass != null) return superclass.getFieldIndex(name);
        return -1;
    }

    Object[] getDefaultFieldValues() {
        return defaultFieldValues;
    }

    ///
    /// ## JolkSyntheticAccessor
    ///
    /// Implements the "Virtual Field Strategy" by acting as a dynamic accessor node.
    /// It handles both read (getter) and write (setter) operations based on the
    /// number of arguments provided, supporting the Jolk fluent setter pattern.
    ///
    @ExportLibrary(InteropLibrary.class)
    public static final class JolkSyntheticAccessor implements TruffleObject {
        private final String fieldName;
        private final int index;

        public JolkSyntheticAccessor(String fieldName, int index) {
            this.fieldName = fieldName;
            this.index = index;
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @ExportMessage
        Object execute(Object[] arguments,
                       @Cached("createBinaryProfile()") ConditionProfile lengthProfile) throws ArityException, UnsupportedTypeException {
            
            if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
            
            // Argument 0 is the receiver (JolkObject)
            if (!(arguments[0] instanceof JolkObject receiver)) {
                throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkObject");
            }

            if (lengthProfile.profile(arguments.length == 1)) {
                // Getter: #x -> returns value
                return receiver.getFieldValue(index);
            } else if (lengthProfile.profile(arguments.length >= 2)) {
                // Setter: #x(value) -> returns Self (for fluent chaining)
                Object value = arguments[1];
                receiver.setFieldValue(index, value);
                return receiver;
            }
            throw ArityException.create(1, 2, arguments.length);
        }
    }
}
