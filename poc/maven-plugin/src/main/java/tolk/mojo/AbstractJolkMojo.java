package tolk.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;

/// <p>Abstract base class for Jolk Maven plugin Mojos.</p>
/// 
/// @author Wouter Roose
/// 

public abstract class AbstractJolkMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.basedir}/src/main/jolk")
    protected File sourceDirectory;
    
    // Common parsing logic here...
}

