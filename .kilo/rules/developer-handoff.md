# Jolk developer handoff summary

## Project description
**Jolk** is a *Unified Messaging* and *Meta-Layer Protocol* for the Java Type System. It projects the dynamic ergonomics of **Smalltalk** onto the industrial reliability of **Java**. The application acts as a communicative engine where every interaction—including arithmetic, control flow, and instantiation—is a formal *message passing* event.

## Project structure
The project is organized into the following modules:

- `doc`: documentation, the file `doc\jolk-book.md` describes the desing and implementation of the programming language jolk
  * part one and part two are reviewed
  * part three and part four are might divert from the implementation
  * the jolk bnf grammar is for documentation purpose, not the strict technical grammar
- `grammar`: Contains the **Antlr** grammar specification (`jolk.g4`).
- `poc/tolk`: The core **Tolk Engine** containing **Truffle** AST nodes and runtime identity systems.
- `poc/demo`: Demonstrators written in the jolk, implementations of the language, such as form validation frameworks.
- `poc/bench`: **JMH** benchmarks tracking *semantic flattening* performance.
- `poc/test-engine`: the implementation of Jolk unit test framework and the JolkTestEngine Junit integration 

## Core architecture
The system leverages the **GraalVM** ecosystem:
- **Antlr**: Used for deterministic parsing.
- **Truffle Framework**: Provides the AST interpreter and specialization infrastructure via the **Truffle DSL**.
- **Graal JIT**: Performs partial evaluation to collapse high-level protocols into optimized machine code.
- **Maven**: Manages dependencies and build lifecycle.

## Domain logic
Data flows through a multi-stage transformation:
1. **Parsing**: Source code is parsed into a stratified tree.
2. **Visitation**: `JolkVisitor` maps the tree to `JolkNode` identities and manages lexical scopes.
3. **Hydration**: `JolkMetaClass` descriptors resolve inheritance and member visibility.
4. **Messaging**: Interactions are routed through `JolkDispatchNode`, which prioritizes *monomorphic fast paths* and *polymorphic inline caches*.

## Past context and rules
The following specific patterns and rules have been established:
- **BigDecimal impedance**: `java.math.BigDecimal` is the substrate identity for the `Decimal` archetype. Because **Truffle Interop** does not natively recognize it as a primitive, we use *identity-aware matching* in `JolkArityDispatchNode` to bypass interop proxying for guest meta-objects.
- **Identity restitution**: Use `JolkNode.lift()` to wrap host values into guest identities and `JolkNode.unwrap()` to recover substrate identities for internal operations.
- **Semantic casing**: Identifiers starting with **Uppercase** are meta-objects (Types/Constants); **lowercase** identifiers are instances or selectors.
- **Casting requirements**: Explicit casts like `(String) jmc.getMetaSimpleName()` are often required due to **Truffle** code generation inference limits.
- **Lexical fence**: Accessing state is strictly mediated through messages; `x = 10` is reified as `self #x(10)`.

## Current status
- The `Jolk` continuum (`Nothing`, `Boolean`, `Long`, `Decimal`, `String`, `Array`, `Map`) is integrated.
- Method overloading based on arity and signature matching is functional.
- Specialization is implemented for arithmetic, list filtering (`#filter`), and iteration (`#forEach`).
- *Shim-less integration* allows direct calls to native **Java** members.

## Established coding conventions
- **Javadoc format**: Use Markdown format with the `///` prefix.
- **Heading capitalization**: Adopt Sentence case for all headings.
- **Proper nouns**: Capitalize only true proper nouns (e.g., Jolk, Java, JVM, Smalltalk).
- **Backticks**: Use for code identifiers, methods, and types (e.g., `JolkObject`, `JolkDispatchNode`).
- **Italics**: Use for first mention of concepts (e.g., *identity restitution*).

---
*This context document is maintained to synchronize architectural understanding across workspace agents.*