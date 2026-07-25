package jolk.test;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectDirectories;
import org.junit.platform.suite.api.Suite;

import tolk.language.JolkLanguage;

///
/// # JolkTestSuite
/// 
/// The static Java anchor for VS Code, Surefire, and IDEs@Test
/// 
/// @author Wouter Roose
@Suite
@IncludeEngines(JolkLanguage.ID)
@SelectDirectories("src/test/jolk")
public class JolkTestSuite {

}
