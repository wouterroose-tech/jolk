package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.library.CachedLibrary;

/// # JolkEnumConstant
///
/// Represents an enum constant in the Jolk runtime.
/// Enum constants are singleton objects that support the standard Jolk protocol
/// plus a `name()` method that returns the enum constant's name.
///
@ExportLibrary(InteropLibrary.class)
public class JolkEnumConstant implements TruffleObject {

    private final JolkMetaClass enumClass;
    private final String name;
    private final int ordinal;

    public JolkEnumConstant(JolkMetaClass enumClass, String name, int ordinal) {
        this.enumClass = enumClass;
        this.name = name;
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaObject() {
        return enumClass;
    }

    @com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
    static Object doLookupEnumInstance(JolkEnumConstant receiver, String member) {
        return receiver.enumClass.lookupInstanceMember(member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                              @CachedLibrary(limit = "3") InteropLibrary interop,
                              @Cached(value = "member", allowUncached = true, neverDefault = false) String cachedMember,
                              @Cached(value = "doLookupEnumInstance(this, member)", allowUncached = true, neverDefault = false) Object cachedMemberObj)
            throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {

        // Handle enum-specific methods
        switch (member) {
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "ordinal":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return (long) ordinal; // Consistent with Jolk numeric standard
        }

        // Delegate to the enum class for other methods
        Object instanceMember = null;
        if (member.equals(cachedMember)) {
            instanceMember = cachedMemberObj;
        } else if (enumClass.hasInstanceMember(member)) {
            instanceMember = enumClass.lookupInstanceMember(member);
        }

        if (instanceMember != null) {
            // Prepend 'this' (receiver) to arguments as Jolk instance members expect it
            Object[] argsWithReceiver = new Object[arguments.length + 1];
            argsWithReceiver[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, argsWithReceiver, 1, arguments.length);
            try {
                Object result = interop.execute(instanceMember, argsWithReceiver);
                return result == null ? JolkNothing.INSTANCE : result;
            } catch (UnsupportedMessageException e) {
                 // Fallback if the member object is not executable (e.g. a field accessor that failed)
                 throw e;
            }
        }

        // Fallback to the Jolk Object Protocol (e.g. #class, #isPresent, #~~)
        Object intrinsicResult = JolkIntrinsicProtocol.dispatchObjectIntrinsic(this, member, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        return switch (member) {
            case "name", "ordinal" -> true;
            default -> enumClass.hasInstanceMember(member) || JolkIntrinsicProtocol.isObjectIntrinsic(member);
        };
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) {
        // Merge enum class members with constant-specific metadata selectors
        Object membersObj = enumClass.getInstanceMemberNames();
        if (membersObj instanceof JolkMemberNames jmn) {
            // In a full implementation, we would return a union of arrays.
            // For the PoC, delegating to the class is acceptable but omits name/ordinal from the list.
        }
        return membersObj;
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return switch (member) {
            case "name", "ordinal" -> true;
            default -> enumClass.hasInstanceMember(member);
        };
    }

    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        if (member.equals("name")) return name;
        if (member.equals("ordinal")) return (long) ordinal;
        if (enumClass.hasInstanceMember(member)) {
            return enumClass.lookupInstanceMember(member);
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    public String toDisplayString(boolean allowSideEffects) {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}