package tolk.nodes;

///
/// Base class for all Jolk nodes that represent expressions.
/// This is a marker class that allows us to distinguish between different kinds of nodes in the AST.
/// Currently, it doesn't add any functionality beyond what is provided by JolkNode,
/// but it serves as a foundation for future expression-specific behavior and type hierarchies.
/// for example, we might later want to add common methods for evaluating expressions or handling side effects.
///
public abstract class JolkExpressionNode extends JolkNode {
}