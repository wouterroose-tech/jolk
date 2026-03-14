package tolk.jolct;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class JolkContextTest {

    @Test
    void testIsJolkType_FQN() {
        JolkContext context = new JolkContext();
        context.addJolkType("com.example.MyClass");

        assertTrue(context.isJolkType("com.example.MyClass", "other.pkg", Collections.emptyList()));
        assertFalse(context.isJolkType("com.example.OtherClass", "other.pkg", Collections.emptyList()));
    }

    @Test
    void testIsJolkType_SamePackage() {
        JolkContext context = new JolkContext();
        context.addJolkType("com.example.MyClass");

        assertTrue(context.isJolkType("MyClass", "com.example", Collections.emptyList()));
        assertFalse(context.isJolkType("MyClass", "other.pkg", Collections.emptyList()));
    }

    @Test
    void testIsJolkType_ExplicitImport() {
        JolkContext context = new JolkContext();
        context.addJolkType("com.example.MyClass");

        assertTrue(context.isJolkType("MyClass", "other.pkg", Arrays.asList("com.example.MyClass")));
        assertFalse(context.isJolkType("MyClass", "other.pkg", Arrays.asList("com.example.OtherClass")));
    }

    @Test
    void testIsJolkType_WildcardImport() {
        JolkContext context = new JolkContext();
        context.addJolkType("com.example.MyClass");

        assertTrue(context.isJolkType("MyClass", "other.pkg", Arrays.asList("com.example.*")));
        assertFalse(context.isJolkType("OtherClass", "other.pkg", Arrays.asList("com.example.*")));
    }
}