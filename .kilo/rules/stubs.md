# Workspace Rules for Kilo Code

## AST & Class Context
When analyzing or generating Truffle AST nodes for the Jolk engine:
- Always reference the compiled signatures in `.kilo/stubs/ast-signatures.java`.
- Treat `.kilo/stubs/ast-signatures.java` as the canonical source of truth for public and protected node APIs.
- Do not request full source file reads for classes under `tolk/nodes/` unless internal method implementation details are specifically required.