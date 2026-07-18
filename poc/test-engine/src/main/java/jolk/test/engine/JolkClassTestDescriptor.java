package jolk.test.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import tolk.runtime.JolkMetaClass;

public class JolkClassTestDescriptor extends AbstractTestDescriptor {
    private final JolkMetaClass guestTestClass; // Reference to the guest-space MetaClass object

    public JolkClassTestDescriptor(UniqueId uniqueId, String displayName, JolkMetaClass guestTestClass) {
        super(uniqueId, displayName);
        this.guestTestClass = guestTestClass;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public JolkMetaClass getGuestTestClass() {
        return guestTestClass;
    }
}
