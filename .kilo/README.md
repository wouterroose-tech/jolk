# Kilo Code Agent Configuration

This directory contains configuration files, context rules, and auto-generated API stubs for the Kilo Code workspace extension.

## Directory Overview

* **`rules`**: Context directives and architectural rules that instruct Kilo Code on how to process the codebase.
* **`stubs/`**: Target directory for auto-generated API signatures.
  * **`stubs/ast-signatures.java`**: Consolidated `javap` public and protected API signatures for all AST nodes (`tolk.nodes.*`).

## Model & Provider Configuration

The workspace employs a multi-model routing strategy leveraging **Continue**, **Google Gemini** and **Mistral AI** to optimize performance, latency, and context efficiency across distinct development tasks.

# Kilo Code Agent Configuration

This directory contains configuration files, context rules, and auto-generated API stubs for the Kilo Code workspace extension.

## Model & Provider Configuration

The workspace employs a multi-model routing strategy leveraging **Continue**, **Google Gemini**, and **Mistral AI** to optimize performance, latency, and context efficiency across distinct development tasks.

### Model Routing Matrix

| Task / Domain | Provider | Selected Model | Role / Rationale |
| :--- | :--- | :--- | :--- |
| **Code Completion** | Continue | Continue Extension Integration | Handles real-time inline completions (temporary setup). |
| **Code Generation & Edit** | Google | Gemini Flash / Gemini Pro | High-reasoning code edits, refactoring, and Truffle AST generation. |
| **Interactive Chat** | Google | Gemini Flash-Mini | Low-latency conversational responses and technical Q&A. |
| **Workspace Indexing** | Mistral AI | Mistral Embeddings / Models | Fast structural code analysis, indexing, and vector retrieval. |

### Model Task Allocation

* **Continue for Code Completion (Temporary)**: Employed temporarily while free inference tiers and local tab-completion remain accessible, and until a permanent, low-latency Fill-In-The-Middle (FIM) workflow is integrated directly within Kilo Code.
* **Mistral for Indexing**: Indexing requires processing large codebases into dense vector representations. Mistral offers high-throughput embedding models with low API cost per million tokens, making it optimal for continuous background indexing and fast semantic search retrieval.
* **Gemini for Coding & Chat**: Active code generation and refactoring require deep structural reasoning and long-range context handling. Gemini’s million-token context window and strict instruction following allow it to analyze entire Truffle AST hierarchies and generate accurate code edits without dropping context.
  * **Gemini Flash-Lite**: Assigned to mechanics and boilerplate, including Truffle node boilerplate generation, standard AST node factory tests, or JUnit discovery loops for `.jolk` files.
  * **Gemini Flash & Gemini Pro**: Reserved for Graal compilation boundaries and complex Truffle AST logic (e.g., `@Specialization` guards, handling lazy meta-constant lens projections in JoMoo instances, or debugging partial evaluation escapes flagged by `-Dtruffle.gis.dump=...`).

### Task Allocation Matrix

| Model Class | Ideal Technical Tasks | When to Avoid |
| :--- | :--- | :--- |
| **Flash-Lite** | • Small bug fixes<br>• Boilerplate class generation<br>• Single-file refactoring<br>• Simple parsing logic & grammar updates | • Truffle partial evaluation analysis<br>• Architectural AST redesign<br>• Deep, non-obvious compiler bugs |
| **Flash / Pro** | • **Truffle DSL Specialization**: Trimming `@Specialization` guards and handling node rewrites.<br>• **Graal PE Boundaries**: Identifying boundary leaks (e.g., unexpected object allocation across PE nodes).<br>• **Complex AST Interactions**: Designing message-dispatch nodes involving JoMoo object instances or lazy meta-constant lens projections.<br>• **Multi-File Context**: Solving cross-cutting issues involving `jolk.g4`, the parser listener, and Truffle execution nodes simultaneously. | • Daily code completions (inefficient token usage)<br>• Simple unit test scaffolding<br>• Basic file discovery loops |

### Workflow Decision Rules

1. **Use Flash as the Primary Driver**: Keep Gemini Flash as the default model in Kilo Code for fast, low-cost modifications, file generation, and incremental refactoring.
2. **Switch to Pro for Deep Execution Analysis**: Escalate to Gemini Pro when Truffle fails during runtime optimization (e.g., failure to inline, polyglot boundary issues, or complex ANTLR listener hierarchy designs).
3. **Attach Context Explicitly**: When invoking Pro for high-level language design, explicitly attach `@jolk-book.md` and `@jolk.g4` to ensure it enforces Jolk execution invariants (such as no `Void`/`val` keywords and strict message-oriented dispatch).

### Configuration Reference

```json
{
  "kilocode.codeGeneration.provider": "google",
  "kilocode.codeGeneration.model": "gemini-2.5-pro",
  "kilocode.chat.provider": "google",
  "kilocode.chat.model": "gemini-2.5-flash-mini",
  "kilocode.indexing.provider": "mistral",
  "kilocode.completion.engine": "continue"
}
```

---

##  AST Signature Synchronization

To prevent context window saturation from large Java AST implementations, class signatures are generated automatically during the Maven build lifecycle.

* **Source**: Bytecode compiled from `poc/tolk/src/main/java/tolk/nodes/`
* **Trigger Phase**: Maven `process-classes` (during `test`) lifecycle phase via `maven-antrun-plugin` in `poc/tolk/pom.xml`
* **Output Target**: `.kilo/stubs/ast-signatures.java`

### How to Apply in LLM Context

* **Architecture & Planning**: Prompt the LLM referencing `ast-signatures.java` for structural node analysis, method lookup, or pipeline design. Do not attach full node implementation files.
* **Targeted Refactoring**: Include specific source files from `poc/tolk/src/main/java/tolk/nodes/` only when internal method implementation details or Truffle node logic modifications are explicitly required.
* **Regeneration**: Update the file after modifying AST signatures:

```powershell
mvn process-classes -f poc/tolk/pom.xml
```