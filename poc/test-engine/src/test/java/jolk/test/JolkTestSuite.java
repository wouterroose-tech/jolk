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


    /*
     * ## Architectural Disparity Between Dynamic Nodes and Suite Protocol
     * 
     * `@TestFactory` succeeds because `convertToDynamicNode` translates
     * `JolkMethodTestDescriptor` instances into `org.junit.jupiter.api.DynamicTest`
     * objects. JUnit Jupiter constructs an internal identifier graph
     * (`[engine:junit-jupiter]/.../[dynamic-test:#1]`). The VS Code Java Test
     * Extension possesses built-in handlers for Jupiter test streams.
     * 
     * `@Suite` relies directly on the JUnit Platform test protocol. The platform
     * serialises `TestDescriptor` instances into `TestIdentifier` transfers. The VS
     * Code extension receives raw `UniqueId` paths from `JolkTestEngine` without
     * Jupiter intermediate translation.
     * 
     * ## UniqueId Segment Type Requirements
     * 
     * The VS Code Java Test Extension parses `UniqueId` segments during suite tree
     * construction. The extension expects segment types matching `[class:...]` and
     * `[method:...]`.
     * 
     * When `JolkMethodTestDescriptor` uses segment key `"test"`
     * (`uniqueId.append("test", name)`), the extension fails to parse the
     * identifier as an executable method node. The parser merges unrecognized
     * segment types into the parent node. Standardising segment keys to `"method"`
     * restores tree node parsing.
     * 
     * ## TestSource Resolution Mechanics
     * 
     * `DynamicTest` objects pass URI references to Jupiter handlers. Native engine
     * descriptors rely on `TestSource` implementations.
     * 
     * VS Code maps test identifiers using `ClassSource` or `MethodSource`
     * constructs. Passing `FileSource` referencing non-Java files causes the VS
     * Code Java language server to discard line positioning. The extension groups
     * child descriptors sharing identical source file paths under a single class
     * entry.
     * 
     * ## Engine UniqueId Hierarchy Alignment
     * 
     * During `@Suite` discovery, `junit-platform-suite-engine` passes a composite
     * root `UniqueId` into `discover()`:
     * 
     * `[engine:junit-platform-suite]/[suite:jolk.test.JolkTestSuite]/[engine:jolk]`
     * 
     * Constructing child descriptors requires appending segments directly to the
     * supplied parent `UniqueId`. Descriptors created using
     * `UniqueId.forEngine("jolk")` construct an independent tree root. The suite
     * launcher discards descriptors that diverge from the parent suite hierarchy.
     * 
     * 
     * Pass the `uniqueId` argument supplied to `TestEngine.discover()` directly to
     * `JolkEngineDescriptor`. Derive all child descriptor identifiers
     * hierarchically from parent instances using standard segment types.
     * 
     * ---
     * 
     * ## Identifier Construction Sequence
     * 
     * ```
     * @Override public TestDescriptor discover(EngineDiscoveryRequest
     * discoveryRequest, UniqueId uniqueId) { // 1. Construct root descriptor from
     * launcher uniqueId JolkEngineDescriptor engineDescriptor = new
     * JolkEngineDescriptor(uniqueId, "Jolk Engine");
     * 
     * // 2. Append class segment to engine uniqueId String className =
     * "JolkTestCase"; UniqueId classId =
     * engineDescriptor.getUniqueId().append("class", className);
     * 
     * JolkClassTestDescriptor classDescriptor = new JolkClassTestDescriptor(
     * classId, className, FileSource.from(filePath.toFile()) );
     * engineDescriptor.addChild(classDescriptor);
     * 
     * // 3. Append method segment to class uniqueId String selectorName =
     * "testAssertTrue"; UniqueId methodId =
     * classDescriptor.getUniqueId().append("method", selectorName);
     * 
     * JolkMethodTestDescriptor methodDescriptor = new JolkMethodTestDescriptor(
     * methodId, selectorName, FileSource.from(filePath.toFile()) );
     * classDescriptor.addChild(methodDescriptor);
     * 
     * return engineDescriptor; }
     * 
     * ```
     * 
     * ### 1. Root Descriptor Propagation
     * 
     * The JUnit suite orchestrator prepends suite identifiers to `uniqueId`.
     * `JolkEngineDescriptor` must consume this parameter to preserve the parent
     * tree context.
     * 
     * ### 2. Segment Naming Standardisation
     * 
     * The VS Code Java Test Extension parses segment keys to build editor trees.
     * Use segment key `"class"` for containers and segment key `"method"` for
     * executable test nodes.
     */
}
