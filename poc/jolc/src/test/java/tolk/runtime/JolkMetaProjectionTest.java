package tolk.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import com.oracle.truffle.api.interop.InteropLibrary;

import tolk.JolcTestBase;

public class JolkMetaProjectionTest extends JolcTestBase {

    // Uncached InteropLibrary for direct calls to JolkMetaClass
    private final InteropLibrary interop = InteropLibrary.getUncached();

    @Test
    void testMetaProjectionResolution_samePackage() {
        String source = """
            ~ test;
            class Origin {
                meta constant Long X = 42;
            }""";
        eval(source);
        source = """
            ~ test;
            & test.Origin.X;
            class Target  {
                Long x() { ^ X }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(42L, instance.invokeMember("x").asLong());
    } 

    @Test
    void testMetaProjectionResolution_differentPackages() {
        String source = """
            ~ test.a;
            class Origin {
                meta constant Long X = 42;
            }""";
        eval(source);
        source = """
            ~ test.b;
            & test.a.Origin.X;
            class Target  {
                Long x() { ^ X }
            }""";
        eval(source);
        Value instance = eval(source).invokeMember("new");
        assertEquals(42L, instance.invokeMember("x").asLong());
    } 

    /**
     * Simulates the erroneous setup where InteropLibrary methods are called on an unhydrated
     * JolkMetaClass (a stub with a null metaRegistry), which previously led to a NullPointerException.
     * This test asserts that with the necessary null guards in JolkMetaClass, these calls
     * now safely return false.
     */
    @Test
    void testMetaProjectionResolution_unhydratedMetaClassInterop() throws Exception {
        // Manually create a JolkMetaClass instance that simulates a stub.
        // The key is to pass 'null' for metaMembers (which becomes metaRegistry).
        JolkMetaClass unhydratedMetaClass = new JolkMetaClass(
            "StubClass", // name
            null,        // superclass
            JolkFinality.FINAL,                // finality
            JolkVisibility.PUBLIC,             // visibility
            JolkArchetype.CLASS,               // archetype
            java.util.Collections.emptyMap(),  // instanceMethods
            java.util.Collections.emptyMap(),  // instanceFields
            null,                              // metaMethods (registry - source of NPE)
            java.util.Collections.emptyMap(),  // metaFields
            java.util.Collections.emptySet(),  // stableFields
            null,                              // hostClass
            java.util.Collections.emptyMap(),  // getterOverrides
            java.util.Collections.emptyMap()   // setterOverrides
        );

        // Assert that isMemberInvocable and isMemberReadable return false,
        // rather than throwing a NullPointerException.
        assertFalse(interop.isMemberInvocable(unhydratedMetaClass, "SOME_MEMBER"), "isMemberInvocable on an unhydrated JolkMetaClass should return false.");
        assertFalse(interop.isMemberReadable(unhydratedMetaClass, "SOME_MEMBER"), "isMemberReadable on an unhydrated JolkMetaClass should return false.");
    }
}
