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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.nodes.JolkReturnException;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass (Meta-Object Descriptor)
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
///  @author Wouter Roose

@ExportLibrary(InteropLibrary.class)
public final class JolkMetaClass implements TruffleObject {

    public static final Set<String> INTRINSIC_MEMBERS = Set.of(
        "==", "!=", "~~", "!~", "??", "hash", "toString", "class", 
        "instanceOf", "isPresent", "isEmpty", "ifPresent", "ifEmpty", 
        "throw", "?", "? :", "?!", "?! :"
    );

    public final String name;
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
    // Meta members (methods and field accessors).
    private final Map<String, Object> metaMembers;
    private final Map<String, Object> metaFields;
    // Meta-level field storage
    private final Map<String, Integer> metaFieldIndices;
    private final Object[] metaFieldValues;
    private final Map<String, JolkMetaFieldAccessor> metaAccessorCache;

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>());
    }

    // Convenience constructor for unit tests providing instance field templates
    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers, new HashMap<>());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers, Map<String, Object> metaFields) {
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
        this.metaFields = metaFields; // Maintain reference to allow deferred hydration
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
        this.metaMembers = metaMembers; // Maintain reference to allow deferred hydration
        this.metaFieldIndices = new HashMap<>();
        this.metaAccessorCache = new HashMap<>();
        
        this.metaFieldValues = new Object[metaFields.size()];
        int mIdx = 0;
        for (String fieldName : metaFields.keySet()) {
            metaFieldIndices.put(fieldName, mIdx++);
            // Auto-register meta field accessors in the meta-member map
            if (!this.metaMembers.containsKey(fieldName)) {
                this.metaMembers.put(fieldName, getMetaAccessor(fieldName, false));
            }
        }

        initializeDefaultValues();
    }

    /**
     * ### registerInstanceMethod
     * 
     * Registers a Java-implemented method for instances of this type.
     */
    public void registerInstanceMethod(String name, JolkBuiltinMethod method) {
        this.instanceMembers.put(name, method);
    }

    /**
     * ### registerMetaMethod
     * 
     * Registers a Java-implemented method for the type itself (the MetaClass).
     */
    public void registerMetaMethod(String name, JolkBuiltinMethod method) {
        this.metaMembers.put(name, method);
    }

    /**
     * ### getJolkMetaClass
     * 
     * Returns the meta-identity of this object. In the Jolk Meta-Object Protocol,
     * classes (MetaClasses) act as their own meta-identities.
     * 
     * @return This MetaClass instance.
     */
    public JolkMetaClass getJolkMetaClass() {
        return this; // In the PoC, classes are their own meta-identities.
    }

    public Object getMetaFieldValue(int index) {
        return metaFieldValues[index];
    }

    public void setMetaFieldValue(int index, Object value) {
        metaFieldValues[index] = value;
    }

    public void setMetaFieldValue(String name, Object value) {
        Integer idx = metaFieldIndices.get(name);
        if (idx != null) metaFieldValues[idx] = value;
    }

    public Object getMetaAccessor(String name, boolean isStable) {
        JolkMetaFieldAccessor accessor = metaAccessorCache.get(name);
        if (accessor == null || accessor.isStable != isStable) {
            accessor = new JolkMetaFieldAccessor(name, metaFieldIndices.get(name), isStable);
            metaAccessorCache.put(name, accessor);
        }
        return accessor;
    }

    /**
     * ### initializeDefaultValues
     * 
     * Re-scans the instanceFields map to establish the template values for new 
     * instances. This is called after the hydration phase in the definition node.
     */
    public void initializeDefaultValues() {
        if (superclass != null) {
            System.arraycopy(superclass.defaultFieldValues, 0, this.defaultFieldValues, 0, superclass.totalFieldCount);
        }
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Integer idx = fieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();
                // Robust Hint Detection: Resolve the type name from MetaClasses, Strings, or Host Classes.
                String hint = resolveTypeHint(val);

                if (isLongHint(hint) || (val == null && entry.getKey().equalsIgnoreCase("id"))) {
                    this.defaultFieldValues[idx] = 0L;
                } else if (isBooleanHint(hint)) {
                    this.defaultFieldValues[idx] = false;
                } else if (isStringHint(hint)) {
                    this.defaultFieldValues[idx] = "";
                } else if (val instanceof JolkMetaClass || val == null || val instanceof String) {
                    // Other classes (including sentinels for non-intrinsics) or explicit nulls default to Nothing.
                    this.defaultFieldValues[idx] = JolkNothing.INSTANCE;
                } else {
                    this.defaultFieldValues[idx] = val;
                }
            }
        }

        // Meta-Level Primary Initializer Expansion
        for (Map.Entry<String, Object> entry : metaFields.entrySet()) {
            Integer idx = metaFieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();

                // If the value is a JolkMetaClass or a String, it's a type hint,
                // so we apply default initialization based on the hint.
                // Otherwise, it's a concrete initializer value.
                if (val instanceof JolkMetaClass || val instanceof String) {
                    String hint = resolveTypeHint(val);
                    if (isLongHint(hint) || (val == null && entry.getKey().equalsIgnoreCase("id"))) {
                        this.metaFieldValues[idx] = 0L;
                    } else if (isBooleanHint(hint)) {
                        this.metaFieldValues[idx] = false;
                    } else if (isStringHint(hint)) {
                        this.metaFieldValues[idx] = "";
                    } else {
                        this.metaFieldValues[idx] = JolkNothing.INSTANCE;
                    }
                } else {
                    this.metaFieldValues[idx] = val == null ? JolkNothing.INSTANCE : val; // Use the provided concrete value
                }
            }
        }
    }

    private static String resolveTypeHint(Object val) {
        if (val instanceof JolkMetaClass mc) return mc.name;
        if (val instanceof String s) return s;
        if (val instanceof Class<?> c) return c.getSimpleName();
        return null;
    }

    public static boolean isLongHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Long") 
            || hint.equalsIgnoreCase("Int")
            || hint.equalsIgnoreCase("jolk.lang.Long");
    }

    public static boolean isBooleanHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Boolean") 
            || hint.equalsIgnoreCase("jolk.lang.Boolean");
    }

    public static boolean isStringHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("String") || hint.equalsIgnoreCase("jolk.lang.String");
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaObject() {
        return this;
    }

    @ExportMessage
    public boolean isMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaQualifiedName() {
        return name;
    }

    @ExportMessage
    public Object getMetaSimpleName() {
        return name;
    }

    @ExportMessage
    public boolean isMetaInstance(Object instance) {
        InteropLibrary interop = InteropLibrary.getUncached();

        // 1. Handle Jolk's reified null (`Nothing`)
        if (instance == JolkNothing.INSTANCE) {
            // Nothing is an instance of its own type and also of Object.
            if (this == JolkNothing.NOTHING_TYPE) return true;
        }

        // 2. Handle Jolk's intrinsic numeric archetypes.
        // We use InteropLibrary to handle both raw primitives and polyglot-wrapped values.
        if (interop.isNumber(instance)) {
            if (isLongHint(this.name)) return true;
        }

        // 3. Handle Jolk's intrinsic `Boolean` archetype.
        if (interop.isBoolean(instance)) {
            // Primitives match their specialized archetypes.
            if (isBooleanHint(this.name)) return true;
        }

        // 4. Handle Jolk's intrinsic `String` archetype.
        if (instance instanceof String) {
            if (isStringHint(this.name)) return true;
        }

        // 5. Handle standard JolkObjects by walking their class hierarchy.
        if (instance instanceof JolkObject jolkObject) {
            JolkMetaClass current = jolkObject.getJolkMetaClass();
            while (current != null) {
                if (current == this) return true;
                current = current.superclass;
            }
        }

        // 6. Root Object Fallback: JolkObjects, Nothing, and primitives are instances of Object.
        // Note: During message dispatch, specialized MetaClasses (Boolean/Long) must be 
        // registered before the root Object archetype to avoid shadowing specialized members.
        if (this.superclass == null && "Object".equals(this.name)) {
            return instance instanceof JolkObject || instance == JolkNothing.INSTANCE || 
                   interop.isNumber(instance) || interop.isBoolean(instance) || instance instanceof String;
        }

        // 7. For the PoC, other host objects are not considered instances of Jolk types.
        return false;
    }

    /**
     * ### isMemberReadable
     * 
     * Returns true if the identifier exists in the meta-member map.
     */
    @ExportMessage
    public boolean isMemberReadable(String member) {
        return metaMembers.containsKey(member);
    }

    /**
     * ### readMember
     * 
     * Retrieves the internal substrate object (Closure or Accessor) for a meta-member.
     */
    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        Object val = metaMembers.get(member);
        if (val == null) throw UnknownIdentifierException.create(member);
        return val;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        // This returns the members of the META-OBJECT, not the instance.
        Set<String> keys = new HashSet<>(metaMembers.keySet());
        keys.add("new");
        keys.add("name");
        keys.add("superclass");
        keys.add("isInstance");
        // Consistent with JolkObject: Classes are polite JoMoos
        keys.addAll(INTRINSIC_MEMBERS);
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        InteropLibrary interop = InteropLibrary.getUncached();
        // 1. Local Meta-Members (Methods and Field Accessors)
        if (metaMembers.containsKey(member)) return true;
        
        // 2. Local Meta-Intrinsics (Identity properties should not delegate)
        if (switch (member) {
            case "new", "name", "superclass", "isInstance" -> true;
            default -> false;
        }) return true;

        if (isObjectIntrinsic(member)) return true;

        // 3. Meta-Inheritance check
        return superclass != null && interop.isMemberInvocable(superclass, member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        InteropLibrary interop = InteropLibrary.getUncached();
        if (metaMembers.containsKey(member)) {
            Object memberObj = metaMembers.get(member);
            // Jolk Protocol: Every method call must include the receiver as the first argument.
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return interop.execute(memberObj, metaArguments);
            } catch (JolkReturnException e) {
                throw e; // Preserve signal for non-local returns
            }
        }

        // 2. Local Meta-Intrinsics: Identity properties specific to this class
        switch (member) {
            case "new":
                // Canonical #new logic: Only applies if no explicit meta-method named 'new' exists.
                // This allows the variadic 'meta Map new(Object...)' to take precedence.
                if (arguments.length == 0) return new JolkObject(this);
                
                if (arguments.length == totalFieldCount) {
                    return new JolkObject(this, arguments);
                }
                throw ArityException.create(totalFieldCount, totalFieldCount, arguments.length);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return superclass != null ? superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return isMetaInstance(arguments[0]);
        }

        // 3. Meta-Inheritance: Delegate to superclass meta-stratum
        if (superclass != null) {
            try {
                return interop.invokeMember(superclass, member, arguments);
            } catch (UnknownIdentifierException e) {
                // fall through to generic protocol
            }
        }

        // 2. Jolk Object Protocol: Classes are polite JoMoos
        Object intrinsicResult = dispatchObjectIntrinsic(this, member, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        throw UnknownIdentifierException.create(member);
    }

    public static boolean isObjectIntrinsic(String member) {
        // Using a Set.contains with interned strings allows Graal 
        // to optimize this check into a bit-mask or a high-speed 
        // jump table if the set is small and the string is a constant.
        return member != null && INTRINSIC_MEMBERS.contains(member);
    }

    @TruffleBoundary
    public static Object dispatchObjectIntrinsic(Object receiver, String name, Object[] arguments, InteropLibrary interop) {
        InteropLibrary genericInterop = InteropLibrary.getUncached();
        try {
            switch (name) {
                case "==" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver == other) return true;
                    if (receiver instanceof Number n1 && other instanceof Number n2) return n1.longValue() == n2.longValue();
                    if (receiver instanceof Boolean b1 && other instanceof Boolean b2) return b1.booleanValue() == b2.booleanValue();
                    if (receiver instanceof String s1 && other instanceof String s2) return s1.equals(s2);
                    if (receiver instanceof TruffleObject || other instanceof TruffleObject || isBoxed(receiver) || isBoxed(other)) {
                        return interop.isIdentical(receiver, other, genericInterop);
                    }
                    return false;
                }
                case "!=" -> {
                    Object eq = dispatchObjectIntrinsic(receiver, "==", arguments, interop);
                    return (eq instanceof Boolean b) ? !b : true;
                }
                case "~~" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object other = arguments[0];
                    if (receiver instanceof Number n1 && other instanceof Number n2) return n1.longValue() == n2.longValue();
                    return receiver.equals(other);
                }
                case "!~" -> {
                    Object eq = dispatchObjectIntrinsic(receiver, "~~", arguments, interop);
                    return (eq instanceof Boolean b) ? !b : true;
                }
                case "??" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "hash" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return (receiver == null || receiver == JolkNothing.INSTANCE) ? 0L : (long) receiver.hashCode();
                }
                case "toString" -> { 
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return (receiver == null || receiver == JolkNothing.INSTANCE) ? "null" : receiver.toString();
                }
                case "isPresent" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof JolkMatch match) return match.isPresent();
                    return receiver != null && receiver != JolkNothing.INSTANCE && !interop.isNull(receiver);
                }
                case "isEmpty" -> {
                    return !((Boolean) dispatchObjectIntrinsic(receiver, "isPresent", arguments, interop));
                }
                case "ifPresent" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver == null || receiver == JolkNothing.INSTANCE || interop.isNull(receiver)) return JolkNothing.INSTANCE;
                    Object val = (receiver instanceof JolkMatch match) ? match.getValue() : receiver;
                    if (val == null) return JolkNothing.INSTANCE;
                    Object result = genericInterop.execute(arguments[0], val);
                    return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                }
                case "ifEmpty" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if ((Boolean) dispatchObjectIntrinsic(receiver, "isEmpty", new Object[0], interop)) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "throw" -> {
                    if (receiver instanceof Throwable t) JolkExceptionExtension.throwException(t);
                    throw new RuntimeException("The #throw selector can only be invoked on Throwable identities.");
                }
                case "class" -> {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    if (receiver instanceof Long || receiver instanceof Integer) return JolkLongExtension.LONG_TYPE;
                    if (receiver instanceof Boolean) return JolkBooleanExtension.BOOLEAN_TYPE;
                    if (receiver instanceof String) return JolkStringExtension.STRING_TYPE;
                    if (receiver instanceof JolkMetaClass) return receiver;
                    if (receiver instanceof JolkObject jo) return jo.getJolkMetaClass();
                    return interop.hasMetaObject(receiver) ? interop.getMetaObject(receiver) : JolkNothing.NOTHING_TYPE;
                }
                case "instanceOf" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    Object type = arguments[0];
                    return (genericInterop.isMetaObject(type) && genericInterop.isMetaInstance(type, receiver)) ? JolkMatch.with(receiver) : JolkMatch.empty();
                }
                case "?" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "?!" -> {
                    if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                    if (receiver instanceof Boolean b && !b) {
                        Object result = genericInterop.execute(arguments[0]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return receiver;
                }
                case "? :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
                case "?! :" -> {
                    if (arguments.length != 2) throw ArityException.create(2, 2, arguments.length);
                    if (receiver instanceof Boolean b) {
                        Object result = !b ? genericInterop.execute(arguments[0]) : genericInterop.execute(arguments[1]);
                        return (result == null || genericInterop.isNull(result)) ? JolkNothing.INSTANCE : result;
                    }
                    return JolkNothing.INSTANCE;
                }
            }
        } catch (JolkReturnException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Intrinsic dispatch failed: #" + name, e);
        }
        return null;
    }

    private static boolean isBoxed(Object obj) {
        return obj instanceof Boolean || obj instanceof Byte || obj instanceof Short ||
               obj instanceof Integer || obj instanceof Long || obj instanceof Float ||
               obj instanceof Double || obj instanceof Character || obj instanceof String;
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
        String sanitized = name;
        return instanceMembers.containsKey(sanitized) || instanceFields.containsKey(sanitized);
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (e.g., a `JolkClosure`) by name.
     */
    public Object lookupInstanceMember(String name) {
        String sanitized = name;
        Object member = instanceMembers.get(sanitized);
        if (member != null) {
            // If the parser erroneously puts the field definition in instanceMembers,
            // we detect the duplicate and prefer the synthetic accessor.
            if (instanceFields.containsKey(sanitized) && instanceFields.get(sanitized) == member) {
                return accessorCache.get(sanitized);
            }
            return member;
        }
        // Virtual Field Strategy: Hydrate a specialized node if the field exists.
        if (instanceFields.containsKey(sanitized)) {
            return accessorCache.get(sanitized);
        }
        // Recurse up the hierarchy
        if (superclass != null) {
            return superclass.lookupInstanceMember(sanitized);
        }
        return null;
    }

    /**
     * ## lookupMetaMember
     *
     * Looks up a meta-level member (e.g., a built-in factory method like #new) 
     * by name. This is used by the dispatch system to resolve messages 
     * sent to the MetaClass itself.
     */
    public Object lookupMetaMember(String name) {
        return metaMembers.get(name);
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
        public boolean isExecutable() {
            return true;
        }
    
        @ExportMessage
        public Object execute(Object[] arguments,
                       @Cached(value = "create()", inline = false) ConditionProfile lengthProfile) throws ArityException, UnsupportedTypeException {
            
            if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
            
            // Argument 0 is the receiver (JolkObject)
            if (!(arguments[0] instanceof JolkObject receiver)) {
                throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkObject");
            }

            if (lengthProfile.profile(arguments.length == 1)) {
                // Getter: #x -> returns value
                Object result = receiver.getFieldValue(index);
                // Identity Restitution Protocol: Ensure no raw JVM null or Interop null leaks.
                return (result == null || (result != JolkNothing.INSTANCE && InteropLibrary.getUncached().isNull(result))) ? JolkNothing.INSTANCE : result;
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
