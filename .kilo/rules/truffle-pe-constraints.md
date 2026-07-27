# TRUFFLE PARTIAL EVALUATION & COMPILATION BOUNDARY CONSTRAINTS

When generating or refactoring Truffle AST nodes, you must strictly adhere to Graal partial evaluation (PE) invariants. Every method inside `@NodeChild` or `@Specialization` classes must compile down to allocation-free machine code after PE.

1. Specialization Guard Invariants (`@Specialization(guards = ...)`)
- **Zero Allocations**: Guard expressions must NEVER instantiate objects, allocate arrays, or invoke methods that perform heap allocation.
- **Side-Effect Free**: Guard expressions must evaluate purely using identity checks (`==`), primitive comparisons, or `@CompilationFinal` state.
- **Compilation Constant Asserts**: Always insert `CompilerAsserts.compilationConstant()` for cached parameters evaluated in guards.
- **FORBIDDEN IN GUARDS**: `new`, string concatenations, Stream API, collection creation, or non-inlined utility calls.

2. Jolk Meta-Constant & Lens State (`@CompilationFinal` & `@Cached`)
- **Lazy Projection Folding**: Any immutable field, lens offset, or Jolk meta-constant projection must be annotated with `@CompilationFinal` or `@CompilationFinal(dimensions = N)` for arrays.
- **State Invalidation**: Any mutation to a `@CompilationFinal` field must be immediately preceded by `CompilerDirectives.transferToInterpreterAndInvalidate()`.
- **AST Caching**: Use `@Cached` or `@CachedLibrary` to capture stable dispatch targets or meta-constant lenses. Ensure cached variables are placed as the *last* parameters in `@Specialization` signatures.
- **Compilation Constant Assertion**: Annotate method bodies with `CompilerAsserts.neverPartOfCompilation()` for slow-path interpreter setups, and `CompilerAsserts.partialEvaluationConstant()` on constant-folded values.

3. Strict Reflection & Dynamic Loading Prohibition
- **No JVM Reflection**: Do NOT use `Class.forName()`, `Method.invoke()`, `Field.get()`, or `ClassLoader` lookups inside nodes or `@Specialization` methods.
- **No Dynamic Type Checks**: Do NOT use `instanceof` against dynamic execution targets when type specialization can be handled via `@Specialization` signature types or Truffle `Node'` child dispatch.
- **Truffle Interop**: Use `@CachedLibrary(limit = "...")` and `InteropLibrary` for meta-object or polymorphic object inspectability instead of reflection.

## REQUIRED CODE FORMAT TEMPLATE FOR AST NODES

When generating nodes, follow this exact structure:

```java
@NodeInfo(shortName = "JolkMessageDispatch")
public abstract class JolkDispatchNode extends Node {

    // 1. Meta-constant lens state folded by Graal PE
    @CompilationFinal private MetaConstantLens cachedLens;

    public abstract Object executeDispatch(VirtualFrame frame, JolkObject receiver, Object[] args);

    // 2. Pure guard, zero allocations, cached parameters placed last
    @Specialization(guards = "receiver.getStructureSchema() == cachedSchema")
    protected Object doDirectDispatch(
            VirtualFrame frame,
            JolkObject receiver,
            Object[] args,
            @Cached("receiver.getStructureSchema()") Schema cachedSchema,
            @Cached("lookupDispatchTarget(cachedSchema)") DirectCallNode callNode) {
        
        // Assert PE constant boundaries
        CompilerAsserts.partialEvaluationConstant(cachedSchema);
        
        return callNode.call(args);
    }

    // 3. Slow path explicitly removed from PE execution trace
    @TruffleBoundary
    protected Object fallbackSlowPath(JolkObject receiver, Object[] args) {
        // Slow path or complex runtime lookup
        return ...;
    }
}

```

## Key Architectural Safeguards Embedded Above

1. **Boundary Marker (`@TruffleBoundary`)**: Forces the LLM to route any unavoidable runtime setup (like complex exception creation or string formatting) into a method explicitly excluded from compilation.
2. **Order Violation Guard**: Enforces the Truffle DSL constraint requiring `@Cached` parameters to strictly appear *after* dynamic execution operands in `@Specialization` method arguments.
3. **PE Constant Verification**: Directs the LLM to insert Graal `CompilerAsserts` directly into generated Java code, causing instant compilation failures in tests if a value leaks out of partial evaluation.
