package tolk.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import tolk.jolct.JolkContext;
import tolk.jolct.JolkTranspiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/// Mojo for the "transpile" goal of the Jolk Maven plugin.
/// This Mojo will transpile .jolk files to .java files in a generated sources directory
///  
/// @author Wouter Roose
/// 

@Mojo(name = "transpile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TranspileMojo extends AbstractJolkMojo {
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/jolk")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        if (!sourceDirectory.exists()) {
            getLog().info("No Jolk source directory found at " + sourceDirectory);
            return;
        }

        Path sourcePath = sourceDirectory.toPath();
        Path outputPath = outputDirectory.toPath();
        JolkTranspiler transpiler = new JolkTranspiler();
        JolkContext context = new JolkContext();

        List<Path> jolkFiles;
        try {
            try (Stream<Path> walk = Files.walk(sourcePath)) {
                jolkFiles = walk.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".jolk"))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning for Jolk source files", e);
        }

        // 2. Pass 1: Analyze (Populate Context)
        for (Path jolkFile : jolkFiles) {
            try {
                transpiler.analyze(jolkFile, context);
            } catch (RuntimeException | IOException e) {
                throw new MojoExecutionException("Error analyzing file: " + jolkFile, e);
            }
        }

        // 3. Pass 2: Transpile (Generate Code)
        for (Path jolkFile : jolkFiles) {
            try {
                String javaCode = transpiler.transpile(jolkFile, context);

                Path relativePath = sourcePath.relativize(jolkFile);
                String javaFileName = relativePath.toString().replace(".jolk", ".java");
                Path javaFile = outputPath.resolve(javaFileName);

                Files.createDirectories(javaFile.getParent());
                Files.write(javaFile, javaCode.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException("Error writing transpiled file for: " + jolkFile, e);
            } catch (RuntimeException e) {
                throw new MojoExecutionException("Error transpiling file: " + jolkFile, e);
            }
        }

        // 4. Add generated sources to Maven project
        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }
}
