package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

public class JolkStringTest extends JolcTestBase {

    @Test
    void testStringField() {
        String source = """
            class StringTest {
                String val;
                String foxJumpslazyDog = "The quick brown fox jumps over the lazy dog";
                
                String specialCharacterString() {
                    String s = "Testing «ταБЬℓσ»: 1<2 & 4+1>3, now 20% off!";
                    ^ s
                 }
                String alphanumericAndSymbolString() {
                    String s = "!\\\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_abcdefghijklmnopqrstuvwxyz{|}~`";
                    ^ s
                 }
                Boolean testAccessors() {
                    String s = "  Tolk String  ";
                    Boolean lengthOk = s #length == 15;
                    Boolean emptyOk = "" #isEmpty;
                    ^ lengthOk && emptyOk
                }

                Boolean testTransformations() {
                    String s = "  Tolk String  ";
                    Boolean trimOk = s #trim == "Tolk String";
                    Boolean upperOk = "jolk" #toUpperCase == "JOLK";
                    ^ trimOk && upperOk
                }

                Boolean testRegex() {
                    ^ "12345" #matches("\\d+")
                }

                Boolean testProtocols() {
                    Boolean presentOk = "identity" #isPresent;
                    String n = null;
                    Boolean coalesceOk = (n ?? "ok") == "ok";
                    ^ presentOk && coalesceOk
                }
            }
            """;
        Value meta = eval(source);

        Value instance = meta.invokeMember("new");
        assertFalse(instance.invokeMember("val").isNull(), "String fields should default to empty string.");
        assertTrue(instance.invokeMember("val").asString().isEmpty());

        // Validate specific literals and UTF-16 support in TruffleString
        assertEquals("The quick brown fox jumps over the lazy dog", instance.invokeMember("foxJumpslazyDog").asString());
        assertEquals("Testing «ταБЬℓσ»: 1<2 & 4+1>3, now 20% off!", instance.invokeMember("specialCharacterString").asString());
        assertEquals("!\\\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_abcdefghijklmnopqrstuvwxyz{|}~`", instance.invokeMember("alphanumericAndSymbolString").asString());
        
        // Verified split functionality
        assertTrue(instance.invokeMember("testAccessors").asBoolean(), "TruffleString accessors (#length, #isEmpty) failed.");
        assertTrue(instance.invokeMember("testTransformations").asBoolean(), "TruffleString transformations (#trim, #toUpperCase) failed.");
        assertTrue(instance.invokeMember("testRegex").asBoolean(), "TruffleString regex (#matches) failed.");
        assertTrue(instance.invokeMember("testProtocols").asBoolean(), "TruffleString protocol methods (#isPresent, ??) failed.");

        instance.invokeMember("val", "hello");
        assertEquals("hello", instance.invokeMember("val").asString());
        instance.invokeMember("val", "world");
        assertEquals("world", instance.invokeMember("val").asString());

        // Canonical #new
        String foxLiteral = "The quick brown fox jumps over the lazy dog";
        instance = meta.invokeMember("new", "test", foxLiteral);
        assertEquals("test", instance.invokeMember("val").asString(), "Canonical #new should initialize String fields.");
    }
    
    @Test
    void testEquivalence() {
        String source = """
            class EqualityTest {
                Boolean eq(String a, String b) { ^ a ~~ b }
                Boolean ne(String a, String b) { ^ a !~ b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("eq", "hello", "hello").asBoolean());
        assertFalse(instance.invokeMember("eq", "hello", "world").asBoolean());
        assertTrue(instance.invokeMember("ne", "hello", "world").asBoolean());
        assertFalse(instance.invokeMember("ne", "hello", "hello").asBoolean());
    }
    
    @Test
    void testMatches() {
        String source = """
            class RegexTest {
                Boolean matches(String str, String pattern) { ^ str #matches(pattern) }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("matches", "hello123", "hello\\d+").asBoolean());
        assertFalse(instance.invokeMember("matches", "hello", "hello\\d+").asBoolean());
    }
    
    @Test
    void testConcatenation() {
        String source = """
            class ConcatTest {
                String concat(String a, String b) { ^ a + b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals("hello", instance.invokeMember("concat", "hello", "").asString());
        assertEquals("world", instance.invokeMember("concat", "", "world").asString());
        assertEquals("helloworld", instance.invokeMember("concat", "hello", "world").asString());
    }
    
    @Test
    void testJavaProtocol() {
        String source = """
            class ConcatTest {
                Long length(String str) { ^ str #length() }
                Boolean contains(String a, String b) { ^ a #contains(b) }
                String toUpperCase(String str) { ^ str #toUpperCase() }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(5L, instance.invokeMember("length", "hello").asLong());
        assertEquals(0L, instance.invokeMember("length", "").asLong());
        assertTrue(instance.invokeMember("contains", "hello", "ell").asBoolean());
        assertFalse(instance.invokeMember("contains", "hello", "world").asBoolean());
        assertEquals("HELLO", instance.invokeMember("toUpperCase", "hello").asString());
    }
}
