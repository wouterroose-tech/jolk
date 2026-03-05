package tolk.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;

/// Placeholder for the compile goal of the Jolk Maven plugin.
/// This Mojo will compile .jolk files directly to .class files in the output directory.
/// 
/// @author Wouter Roose
/// 

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends AbstractJolkMojo {
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory; // usually target/classes

    public void execute() {
        // 1. Compile .jolk -> .class directly
    }
}

