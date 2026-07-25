package jolk.test.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * JolkFolderTestDescriptor
 */
public class JolkFolderTestDescriptor extends AbstractTestDescriptor {


    protected JolkFolderTestDescriptor(UniqueId uniqueId, String displayName) {
		super(uniqueId, displayName);
	}

	@Override
    public Type getType() {
        return Type.CONTAINER;
    }

}
