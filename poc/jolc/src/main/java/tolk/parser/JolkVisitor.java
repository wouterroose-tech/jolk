package tolk.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Arrays;
import tolk.grammar.jolkBaseVisitor;
import tolk.grammar.jolkParser;
import tolk.nodes.JolkClassDefinitionNode;
import tolk.nodes.JolkClosureNode;
import tolk.nodes.JolkEmptyNode;
import tolk.nodes.JolkFieldNode;
import tolk.nodes.JolkIdentityNode;
import tolk.nodes.JolkLiteralNode;
import tolk.nodes.JolkMethodNode;
import tolk.nodes.JolkMessageSendNode;
import tolk.nodes.JolkSelfNode;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkReadArgumentNode;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkNothing;

/// Visitor that traverses the ANTLR4 parse tree and produces the Truffle AST.
public class JolkVisitor extends jolkBaseVisitor<JolkNode> {

    private final Stack<List<String>> scopes = new Stack<>();

    /// ### visitUnit
    ///
    /// Visits the top-level rule of the grammar.
    ///
    /// @param ctx The parse tree context.
    /// @return A [JolkNode] for the entire compilation unit.
    @Override
    public JolkNode visitUnit(jolkParser.UnitContext ctx) {
        // A unit can contain either a type declaration or an extension declaration,
        // according to the grammar.
        if (ctx.type_decl() != null) {
            return visit(ctx.type_decl());
        }
        if (ctx.extension_decl() != null) {
            return visit(ctx.extension_decl());
        }
        return new JolkEmptyNode();
    }

    /// ### visitType_decl
    ///
    /// Visits a type declaration, currently handling `class`.
    ///
    /// @param ctx The parse tree context.
    /// @return A [JolkClassDefinitionNode] if it's a class, otherwise an empty node.
    @Override
    public JolkNode visitType_decl(jolkParser.Type_declContext ctx) {
        if (ctx.archetype() == null) {
            return new JolkEmptyNode();
        }

        // To robustly get the class name, we access the 'meta_id' from the parse tree,
        // which aligns with the grammar (`meta_id`, not `MetaId`). This prevents a
        // NullPointerException if the parse tree structure is not what's expected.
        if (ctx.type_bound() != null && ctx.type_bound().type() != null) {
            JolkFinality finality = JolkFinality.OPEN;
            JolkVisibility visibility = JolkVisibility.PUBLIC; // Jolk types default to PUBLIC
            // Iterate children to robustly check for 'final' keyword, handling potential
            // grammar variations (e.g. grouped modifiers vs direct finality rule).
            for (int i = 0; i < ctx.getChildCount(); i++) {
                var child = ctx.getChild(i);
                if (child == ctx.archetype()) {
                    break;
                }
                String text = child.getText();
                if (text.contains("final")) {
                    finality = JolkFinality.FINAL;
                    break;
                } else if (text.contains("abstract")) {
                    finality = JolkFinality.ABSTRACT;
                    break;
                }
                
                // Check for visibility modifiers
                if (text.contains("private")) visibility = JolkVisibility.PRIVATE;
                else if (text.contains("protected")) visibility = JolkVisibility.PROTECTED;
                else if (text.contains("package")) visibility = JolkVisibility.PACKAGE;
                else if (text.contains("public")) visibility = JolkVisibility.PUBLIC;
            }
            var typeContext = ctx.type_bound().type();
            if (typeContext.MetaId() != null) {
                String className = typeContext.MetaId().getText();

                JolkArchetype archetype = switch (ctx.archetype().getText()) {
                    case "class" -> JolkArchetype.CLASS;
                    case "value" -> JolkArchetype.VALUE;
                    case "record" -> JolkArchetype.RECORD;
                    case "enum" -> JolkArchetype.ENUM;
                    case "protocol" -> JolkArchetype.PROTOCOL;
                    default -> JolkArchetype.CLASS;
                };

                Map<String, Object> instanceMembers = new LinkedHashMap<>();
                Map<String, Object> instanceFields = new LinkedHashMap<>();
                Map<String, Object> metaMembers = new LinkedHashMap<>();

                for (var mbr : ctx.type_mbr()) {
                    if (mbr.member() != null) {
                        boolean isMeta = mbr.member().META() != null;
                        JolkNode node = visit(mbr.member());
                        if (node instanceof JolkFieldNode fieldNode) {
                            if (isMeta) {
                                metaMembers.put(fieldNode.getName(), fieldNode);
                            } else {
                                // Instance fields
                                String typeName = "Object";
                                if (mbr.member().state() != null) {
                                    if (mbr.member().state().field() != null) {
                                        typeName = mbr.member().state().field().type().getText();
                                    } else if (mbr.member().state().constant() != null) {
                                        typeName = mbr.member().state().constant().type().getText();
                                    }
                                }
                                instanceFields.put(fieldNode.getName(), "Int".equals(typeName) ? 0 : null);
                                // TODO: Handle instance field initializers here or in JolkClassDefinitionNode logic
                            }
                        } else if (node instanceof JolkMethodNode methodNode) {
                            if (isMeta) {
                                metaMembers.put(methodNode.getName(), methodNode);
                            } else {
                                // Instance methods
                                instanceMembers.put(methodNode.getName(), methodNode);
                            }
                        }
                    }
                }

                return new JolkClassDefinitionNode(className, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers);
            }
        }
        return new JolkEmptyNode();
    }

    ///
    /// Visits an extension declaration. This is currently a placeholder.
    ///
    /// @param ctx The parse tree context.
    /// @return An [JolkEmptyNode] as this feature is not yet fully implemented.
    @Override
    public JolkNode visitExtension_decl(jolkParser.Extension_declContext ctx) {
        // Not implemented yet, return empty node to avoid null pointers.
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitMember(jolkParser.MemberContext ctx) {
        if (ctx.method() != null) {
            return visit(ctx.method());
        } else if (ctx.state() != null) {
            if (ctx.state().field() != null) {
                return visit(ctx.state().field());
            } else if (ctx.state().constant() != null) {
                return visit(ctx.state().constant());
            }
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitField(jolkParser.FieldContext ctx) {
        String name = ctx.identifier().getText();
        JolkNode initializer = new JolkEmptyNode();
        if (ctx.assignment() != null) {
            initializer = visit(ctx.assignment().expression());
        }
        return new JolkFieldNode(name, initializer);
    }

    @Override
    public JolkNode visitConstant(jolkParser.ConstantContext ctx) {
        String name = ctx.binding().identifier().getText();
        JolkNode initializer = visit(ctx.binding().assignment().expression());
        return new JolkFieldNode(name, initializer);
    }

    @Override
    public JolkNode visitMethod(jolkParser.MethodContext ctx) {
        String name = ctx.selector_id().getText();
        String[] params = new String[0];
        boolean isVariadic = false;
        if (ctx.typed_params() != null) {
            ParameterSpec spec = parseTypedParams(ctx.typed_params());
            params = spec.names;
            isVariadic = spec.isVariadic;
        }

        scopes.push(Arrays.asList(params));
        JolkNode body = new JolkEmptyNode();
        try {
            if (ctx.block() != null) {
                body = visit(ctx.block());
            }
        } finally {
            scopes.pop();
        }
        return new JolkMethodNode(name, body, params, isVariadic);
    }

    @Override
    public JolkNode visitExpression(jolkParser.ExpressionContext ctx) {
        JolkNode result = visit(ctx.logic_or());
        // Handle ternary operations: condition ? trueBranch : falseBranch
        if (!ctx.expression().isEmpty()) {
            String op = ctx.getChild(1).getText(); // "?" or "?!"
            JolkNode thenBranch = visit(ctx.expression(0));
            result = new JolkMessageSendNode(result, op, new JolkNode[]{thenBranch});
            
            if (ctx.expression().size() > 1) {
                JolkNode elseBranch = visit(ctx.expression(1));
                result = new JolkMessageSendNode(result, ":", new JolkNode[]{elseBranch});
            }
        }
        return result;
    }

    @Override
    public JolkNode visitLogic_or(jolkParser.Logic_orContext ctx) {
        JolkNode left = visit(ctx.logic_and(0));
        for (int i = 1; i < ctx.logic_and().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.logic_and(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitLogic_and(jolkParser.Logic_andContext ctx) {
        JolkNode left = visit(ctx.inclusive_or(0));
        for (int i = 1; i < ctx.inclusive_or().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.inclusive_or(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitInclusive_or(jolkParser.Inclusive_orContext ctx) {
        JolkNode left = visit(ctx.exclusive_or(0));
        for (int i = 1; i < ctx.exclusive_or().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.exclusive_or(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitExclusive_or(jolkParser.Exclusive_orContext ctx) {
        JolkNode left = visit(ctx.bitwise_and(0));
        for (int i = 1; i < ctx.bitwise_and().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.bitwise_and(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitBitwise_and(jolkParser.Bitwise_andContext ctx) {
        JolkNode left = visit(ctx.equality(0));
        for (int i = 1; i < ctx.equality().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.equality(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitEquality(jolkParser.EqualityContext ctx) {
        // Visit the first term
        JolkNode left = visit(ctx.comparison(0));

        // Iterate over the rest of the terms (if any)
        for (int i = 1; i < ctx.comparison().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText(); // Operators are at odd indices: term op term
            JolkNode right = visit(ctx.comparison(i));

            switch (op) {
                case "==" -> left = new JolkIdentityNode(left, right, false);
                case "!=" -> left = new JolkIdentityNode(left, right, true);
                default   -> left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
            }
        }
        return left;
    }

    @Override
    public JolkNode visitComparison(jolkParser.ComparisonContext ctx) {
        JolkNode left = visit(ctx.term(0));
        for (int i = 1; i < ctx.term().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.term(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitTerm(jolkParser.TermContext ctx) {
        JolkNode left = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.factor(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitFactor(jolkParser.FactorContext ctx) {
        JolkNode left = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.unary(i));
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitUnary(jolkParser.UnaryContext ctx) {
        if (ctx.power() != null) {
            return visit(ctx.power());
        }
        String op = ctx.getChild(0).getText();
        JolkNode operand = visit(ctx.unary());
        return new JolkMessageSendNode(operand, op, new JolkNode[0]);
    }

    @Override
    public JolkNode visitPower(jolkParser.PowerContext ctx) {
        JolkNode left = visit(ctx.message());
        if (ctx.powOp() != null) {
            String op = ctx.powOp().getText();
            JolkNode right = visit(ctx.unary());
            left = new JolkMessageSendNode(left, op, new JolkNode[]{right});
        }
        if (ctx.NULL_COALESCE() != null) {
            JolkNode right = visit(ctx.power());
            left = new JolkMessageSendNode(left, "??", new JolkNode[]{right});
        }
        return left;
    }

    @Override
    public JolkNode visitMessage(jolkParser.MessageContext ctx) {
        JolkNode receiver = visit(ctx.primary());
        for (int i = 0; i < ctx.selector().size(); i++) {
            String selector = ctx.selector(i).identifier().getText();
            JolkNode[] args = new JolkNode[0];
            if (ctx.payload(i) != null) {
                if (ctx.payload(i).arguments() != null) {
                    var exprs = ctx.payload(i).arguments().expression();
                    args = new JolkNode[exprs.size()];
                    for (int j = 0; j < exprs.size(); j++) {
                        args[j] = visit(exprs.get(j));
                    }
                } else if (ctx.payload(i).closure() != null) {
                    args = new JolkNode[] { visit(ctx.payload(i).closure()) };
                }
            }
            receiver = new JolkMessageSendNode(receiver, selector, args);
        }
        return receiver;
    }

    @Override
    public JolkNode visitClosure(jolkParser.ClosureContext ctx) {
        String[] params = new String[0];
        boolean isVariadic = false;
        if (ctx.stat_params() != null) {
            if (ctx.stat_params().typed_params() != null) {
                ParameterSpec spec = parseTypedParams(ctx.stat_params().typed_params());
                params = spec.names;
                isVariadic = spec.isVariadic;
            } else if (ctx.stat_params().inferred_params() != null) {
                params = parseInferredParams(ctx.stat_params().inferred_params());
            }
        }

        scopes.push(Arrays.asList(params));
        JolkNode body = new JolkEmptyNode();
        try {
            if (ctx.statements() != null) {
                body = visit(ctx.statements());
            }
        } finally {
            scopes.pop();
        }
        return new JolkClosureNode(body, params, isVariadic);
    }

    @Override
    public JolkNode visitReserved(jolkParser.ReservedContext ctx) {
        String text = ctx.getText();
        if (text.equals("null")) return new JolkLiteralNode(JolkNothing.INSTANCE);
        if (text.equals("self")) return new JolkSelfNode();
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitIdentifier(jolkParser.IdentifierContext ctx) {
        String name = ctx.getText();
        // Check if the identifier matches a parameter in the current scope
        if (!scopes.isEmpty()) {
            int index = scopes.peek().indexOf(name);
            if (index != -1) {
                return new JolkReadArgumentNode(index);
            }
        }
        return new JolkMessageSendNode(new JolkSelfNode(), name, new JolkNode[0]);
    }

    @Override
    public JolkNode visitLiteral(jolkParser.LiteralContext ctx) {
        if (ctx.NumberLiteral() != null) return new JolkLiteralNode(Integer.parseInt(ctx.NumberLiteral().getText()));
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitBlock(jolkParser.BlockContext ctx) {
        if (ctx.statements() != null) {
            return visit(ctx.statements());
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitStatement(jolkParser.StatementContext ctx) {
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitStatements(jolkParser.StatementsContext ctx) {
        return visit(ctx.statement(ctx.statement().size() - 1));
    }

    // --- Parameter Parsing Helpers ---

    private record ParameterSpec(String[] names, boolean isVariadic) {}

    private ParameterSpec parseTypedParams(jolkParser.Typed_paramsContext ctx) {
        List<String> names = new ArrayList<>();
        boolean isVariadic = false;
        if (ctx.InstanceId() != null) {
            for (var id : ctx.InstanceId()) {
                names.add(id.getText());
            }
        }
        if (ctx.vararg_id() != null) {
            names.add(ctx.vararg_id().InstanceId().getText());
            isVariadic = true;
        }
        return new ParameterSpec(names.toArray(new String[0]), isVariadic);
    }

    private String[] parseInferredParams(jolkParser.Inferred_paramsContext ctx) {
        List<String> names = new ArrayList<>();
        for (var id : ctx.instance_id()) {
            names.add(id.getText());
        }
        return names.toArray(new String[0]);
    }
}