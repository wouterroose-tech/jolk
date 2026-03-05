package tolk.mojo;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspileMojoTest {

    @Test
    void testExecute(@TempDir Path tempDir) throws Exception {
        // 1. Setup directories
        Path sourceDir = tempDir.resolve("src/main/jolk");
        Path outputDir = tempDir.resolve("target/generated-sources/jolk");
        Files.createDirectories(sourceDir);
        
        // 2. Create a dummy Jolk file
        Path jolkFile = sourceDir.resolve("Test.jolk");
        Files.writeString(jolkFile, "package com.example; class Test {}");

        // 3. Stub MavenProject
        StubMavenProject project = new StubMavenProject();

        // 4. Instantiate and Configure Mojo
        TranspileMojo mojo = new TranspileMojo();
        setField(mojo, "sourceDirectory", sourceDir.toFile()); // Inherited from AbstractJolkMojo
        setField(mojo, "outputDirectory", outputDir.toFile());
        setField(mojo, "project", project);

        // 5. Execute
        mojo.execute();

        // 6. Verify Output
        Path javaFile = outputDir.resolve("Test.java");
        assertTrue(Files.exists(javaFile), "Transpiled Java file should exist");
        
        String content = Files.readString(javaFile);
        assertTrue(content.contains("public class Test"), "Content should contain class definition");
        assertTrue(content.contains("import jolk.lang.*;"), "Content should contain default imports");

        // 7. Verify Maven integration
        assertTrue(project.compileSourceRoots.contains(outputDir.toFile().getAbsolutePath()));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    static class StubMavenProject extends MavenProject {
        final List<String> compileSourceRoots = new ArrayList<>();

        @Override
        public void addCompileSourceRoot(String path) {
            compileSourceRoots.add(path);
        }
    }
}