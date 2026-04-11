package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;


public class JolkEnumTest extends JolcTestBase {

    private final String enumSource = """
            enum Level {
                ERROR; WARNING; INFO; DEBUG;
                Boolean isError() { ^ self == ERROR }
                Boolean isWarning() { ^ self == WARNING }
                Boolean isInfo() { ^ self == INFO }
                Boolean isDebug() { ^ self == DEBUG }
            }""";  
            

    @Test
    void testSimpleEnum() {
        String enumSource = "enum Color { RED; GREEN; BLUE; }";
        Value colorClass = eval(enumSource);
    
        assertNotNull(colorClass);
        assertTrue(colorClass.hasMembers());

        Value redConst = colorClass.getMember("RED");
        assertNotNull(redConst);
        assertEquals("RED", redConst.invokeMember("name").asString());
        assertTrue(redConst.invokeMember("==", redConst).asBoolean());
        assertTrue(redConst.invokeMember("~~", redConst).asBoolean());
    }

    @Test
    public void testLevelEnumConstants() {
        // Test that Level enum constants are accessible
        Value levelClass = eval(enumSource);
        assertNotNull(levelClass);

        // Test accessing enum constants
        Value errorConst = levelClass.getMember("ERROR");
        assertNotNull(errorConst);
        assertEquals("ERROR", errorConst.invokeMember("name").asString());
        assertTrue(errorConst.invokeMember("==", errorConst).asBoolean());
        assertTrue(errorConst.invokeMember("~~", errorConst).asBoolean());
        assertTrue(errorConst.invokeMember("isError").asBoolean());

        Value warningConst = levelClass.getMember("WARNING");
        assertNotNull(warningConst);
        assertEquals("WARNING", warningConst.invokeMember("name").asString());

        Value infoConst = levelClass.getMember("INFO");
        assertNotNull(infoConst);
        assertEquals("INFO", infoConst.invokeMember("name").asString());

        Value debugConst = levelClass.getMember("DEBUG");
        assertNotNull(debugConst);
        assertEquals("DEBUG", debugConst.invokeMember("name").asString());
    }

    @Test
    public void testLevelEnumMethods() {
        Value levelClass = eval(enumSource);

        // Test ERROR constant methods
        Value errorConst = levelClass.getMember("ERROR");
        assertTrue(errorConst.invokeMember("isError").asBoolean());
        assertFalse(errorConst.invokeMember("isWarning").asBoolean());
        assertFalse(errorConst.invokeMember("isInfo").asBoolean());
        assertFalse(errorConst.invokeMember("isDebug").asBoolean());

        // Test WARNING constant methods
        Value warningConst = levelClass.getMember("WARNING");
        assertFalse(warningConst.invokeMember("isError").asBoolean());
        assertTrue(warningConst.invokeMember("isWarning").asBoolean());
        assertFalse(warningConst.invokeMember("isInfo").asBoolean());
        assertFalse(warningConst.invokeMember("isDebug").asBoolean());
    }

    @Test
    public void testLevelEnumOrdinal() {
        Value levelClass = eval(enumSource);

        assertEquals(0, levelClass.getMember("ERROR").invokeMember("ordinal").asInt());
        assertEquals(1, levelClass.getMember("WARNING").invokeMember("ordinal").asInt());
        assertEquals(2, levelClass.getMember("INFO").invokeMember("ordinal").asInt());
        assertEquals(3, levelClass.getMember("DEBUG").invokeMember("ordinal").asInt());
    }
    
    @Test
    void testFunction() {
        eval(enumSource);
        String source = """
            class MyClass {
                String run() {
                    Level level = Level #ERROR;
                    level #ifPresent [ l -> ^l #isError ? l #name ];
                    ^null;
                }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("ERROR", instance.invokeMember("run").asString()); 
    }  

}
