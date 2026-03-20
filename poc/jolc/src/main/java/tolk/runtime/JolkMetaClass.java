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
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass
/// 
/// Represents a Jolk Type (meta-object) at runtime.
///
/// This is the Java implementation of the `MetaClass` concept defined in Jolk.
/// It acts as the first-class identity for Classes, Records, and Enums, enabling
/// meta-level messaging such as `#new` and type introspection.
/// 
@ExportLibrary(InteropLibrary.class)
public final class JolkMetaClass implements TruffleObject {

    private final String name;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    private final Map<String, Object> members;

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> members) {
        this.name = name;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.members = members;
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
        if (instance instanceof JolkObject jolkObject) {
            return jolkObject.getJolkMetaClass() == this;
        }
        return false;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        Set<String> keys = new HashSet<>(members.keySet());
        keys.add("new");
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return members.containsKey(member);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        return "new".equals(member) || members.containsKey(member);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (members.containsKey(member)) {
            Object memberObj = members.get(member);
            return InteropLibrary.getUncached().execute(memberObj, arguments);
        }
        if ("new".equals(member)) {
            if (arguments.length != 0) {
                throw ArityException.create(0, 0, arguments.length);
            }
            return new JolkObject(this);
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        if (members.containsKey(member)) {
            return members.get(member);
        }
        throw UnknownIdentifierException.create(member);
    }

}
