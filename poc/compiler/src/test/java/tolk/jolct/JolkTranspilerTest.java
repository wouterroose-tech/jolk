package tolk.jolct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JolkTranspilerTest {

    @Test
    void testTranspileFlow(@TempDir Path tempDir) throws IOException {
        // 1. Setup
        Path sourceFile = tempDir.resolve("Test.jolk");
        String sourceCode = "package com.example; class Test {}";
        Files.writeString(sourceFile, sourceCode);

        JolkContext context = new JolkContext();
        JolkTranspiler transpiler = new JolkTranspiler();

        // 2. Analyze (Phase 1)
        transpiler.analyze(sourceFile, context);

        // Verify context has the type
        assertTrue(context.isJolkType("com.example.Test", "", Collections.emptyList()));

        // 3. Transpile (Phase 2)
        String javaCode = transpiler.transpile(sourceFile, context);

        // 4. Verify Output
        assertNotNull(javaCode);
        assertTrue(javaCode.contains("package com.example;"));
        assertTrue(javaCode.contains("class Test"));
    }
}