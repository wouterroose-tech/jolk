package tolk.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Arrays;
import org.antlr.v4.runtime.tree.ParseTree;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import tolk.grammar.jolkBaseVisitor;
import tolk.language.JolkLanguage;
import com.oracle.truffle.api.nodes.RootNode;
import tolk.grammar.jolkParser;
import tolk.nodes.JolkArithmeticNodeGen;
import tolk.nodes.JolkComparisonNode;
import tolk.nodes.JolkClassDefinitionNode;
import tolk.nodes.JolkBlockNode;
import tolk.nodes.JolkReadEnvironmentNode;
import tolk.nodes.JolkLogicalNodeGen;
import tolk.nodes.JolkUnaryNodeGen;
import tolk.nodes.JolkReadTypeNode;
import tolk.nodes.JolkReturnNode;
import tolk.nodes.JolkClosureNode;
import tolk.nodes.JolkEmptyNode;
import tolk.nodes.JolkFieldNode;
import tolk.nodes.JolkIdentityNode;
import tolk.nodes.JolkLiteralNode;
import tolk.nodes.JolkMethodNode;
import tolk.nodes.JolkMessageSendNode;
import tolk.nodes.JolkSelfNode;
import tolk.nodes.JolkSuperNode;
import tolk.nodes.JolkSuperMessageSendNode;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkRootNode;
import tolk.nodes.JolkReadArgumentNode;
import tolk.nodes.JolkReadLocalVariableNode;
import tolk.nodes.JolkWriteLocalVariableNode;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkNothing;

/// Visitor that traverses the ANTLR4 parse tree and produces the Truffle AST.
public class JolkVisitor extends jolkBaseVisitor<JolkNode> {

    private final JolkLanguage language;

    private final Stack<List<String>> scopes = new Stack<>();
    private final Stack<Integer> parameterThresholds = new Stack<>();
    private final Stack<Integer> methodDepths = new Stack<>();
    private String currentClassName;
    private boolean isInMetaScope = false;

    public JolkVisitor(JolkLanguage language) {
        this.language = language;
    }

    /// ### visitUnit
    ///
    /// Visits the top-level rule of the grammar.
    ///
    /// @param ctx The parse tree context.
    /// @return A [JolkNode] for the entire compilation unit.
    @Override
    public JolkNode visitUnit(jolkParser.UnitContext ctx) {
        // Visit preamble directives for their side-effects (context registration)
        if (ctx.package_decl() != null) {
            visit(ctx.package_decl());
        }

        for (jolkParser.ExpansionContext expansionCtx : ctx.expansion()) {
            visit(expansionCtx);
        }

        for (jolkParser.ProjectionContext projectionCtx : ctx.projection()) {
            visit(projectionCtx);
        }

        // TODO: Handle unit-level annotations.

        // The unit's primary executable node is either a type or an extension.
        if (ctx.type_decl() != null) {
            return visit(ctx.type_decl());
        }
        if (ctx.extension_decl() != null) {
            return visit(ctx.extension_decl());
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitPackage_decl(jolkParser.Package_declContext ctx) {
        // TODO: Handle package declaration (namespace and alias)
        return super.visitPackage_decl(ctx);
    }

    @Override
    public JolkNode visitExpansion(jolkParser.ExpansionContext ctx) {
        // Resolves the namespace path (e.g. java.lang.RuntimeException) 
        // and registers it as a Host Class in the current context.
        String path = ctx.inclusion().namespace().getText();
        language.getContextReference().get(null).registerHostClass(path);
        // Currently, we return an empty node as the expansion itself does not produce executable code.
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitProjection(jolkParser.ProjectionContext ctx) {
        // TODO: Handle projection lens (using meta/&)
        return super.visitProjection(ctx);
    }

    @Override
    public JolkNode visitInclusion(jolkParser.InclusionContext ctx) {
        // TODO: Handle namespace inclusion
        return super.visitInclusion(ctx);
    }

    @Override
    public JolkNode visitAlias(jolkParser.AliasContext ctx) {
        // TODO: Handle meta_id aliasing
        return super.visitAlias(ctx);
    }

    @Override
    public JolkNode visitNamespace(jolkParser.NamespaceContext ctx) {
        // TODO: Handle fully qualified namespace resolution
        return super.visitNamespace(ctx);
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

            if (ctx.modifiers() != null) {
                String modsText = ctx.modifiers().getText();
                if (modsText.contains("final")) {
                    finality = JolkFinality.FINAL;
                } else if (modsText.contains("abstract")) {
                    finality = JolkFinality.ABSTRACT;
                }

                if (modsText.contains("private")) visibility = JolkVisibility.PRIVATE;
                else if (modsText.contains("protected")) visibility = JolkVisibility.PROTECTED;
                else if (modsText.contains("package")) visibility = JolkVisibility.PACKAGE;
                else if (modsText.contains("public")) visibility = JolkVisibility.PUBLIC;
            }
            var typeContext = ctx.type_bound().type();
            if (typeContext.MetaId() != null) {
                String className = typeContext.MetaId().getText().intern();
                // Track the current class for 'Self' resolution
                String oldClassName = this.currentClassName;
                this.currentClassName = className;

                JolkArchetype archetype = switch (ctx.archetype().getText()) {
                    case "class" -> JolkArchetype.CLASS;
                    case "value" -> JolkArchetype.VALUE;
                    case "record" -> JolkArchetype.RECORD;
                    case "enum" -> JolkArchetype.ENUM;
                    case "protocol" -> JolkArchetype.PROTOCOL;
                    default -> JolkArchetype.CLASS;
                };

                Map<String, List<JolkMethodNode>> instanceMethods = new LinkedHashMap<>(); // Instance methods
                Map<String, JolkFieldNode> instanceFields = new LinkedHashMap<>(); // Instance fields
                Map<String, List<JolkNode>> metaMembers = new LinkedHashMap<>(); // Can contain JolkFieldNode or JolkMethodNode
                List<String> enumConstants = new ArrayList<>(); // Enum constant names for ENUM archetype

                for (var mbr : ctx.type_mbr()) {
                    if (mbr.member() != null) {
                        boolean isMeta = mbr.member().META() != null;
                        boolean oldMetaScope = this.isInMetaScope;
                        this.isInMetaScope = isMeta;
                        try {
                            // TODO: Collect member annotations for the class definition metadata
                            JolkNode node = visit(mbr.member());
                            
                            if (node instanceof JolkFieldNode fieldNode) {
                                if (isMeta) {
                                    metaMembers.computeIfAbsent(fieldNode.getName(), k -> new ArrayList<>()).add(fieldNode);
                                } else {
                                    // Instance fields
                                    instanceFields.put(fieldNode.getName(), fieldNode);
                                }
                            } else if (node instanceof JolkMethodNode methodNode) {
                                if (isMeta) {
                                    metaMembers.computeIfAbsent(methodNode.getName(), k -> new ArrayList<>()).add(methodNode);
                                } else {
                                    // Instance methods
                                    instanceMethods.computeIfAbsent(methodNode.getName(), k -> new ArrayList<>()).add(methodNode);
                                }
                            }
                        } finally {
                            this.isInMetaScope = oldMetaScope;
                        }
                    } else if (mbr.enum_constant() != null && archetype == JolkArchetype.ENUM) {
                        // Handle enum constants
                        String constantName = mbr.enum_constant().MetaId().getText().intern();
                        enumConstants.add(constantName);
                    }
                }

                String superclassName = null;
                if (ctx.type_bound().type_contracts() != null) {
                    jolkParser.Type_contractsContext contracts = ctx.type_bound().type_contracts();
                    if (contracts.EXTENDS() != null) {
                        superclassName = contracts.type(0).getText().intern();
                    }
                }

                this.currentClassName = oldClassName;
                // Support for single inheritance via 'extends'.
                return new JolkClassDefinitionNode(className, superclassName, finality, visibility, archetype, instanceMethods, instanceFields, metaMembers, enumConstants);
            }
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitModifiers(jolkParser.ModifiersContext ctx) {
        // TODO: Extract visibility and finality modifiers
        return super.visitModifiers(ctx);
    }

    @Override
    public JolkNode visitVis_mod(jolkParser.Vis_modContext ctx) {
        // TODO: Handle symbolic modifiers or keyword visibility
        return super.visitVis_mod(ctx);
    }

    @Override
    public JolkNode visitVisibility(jolkParser.VisibilityContext ctx) {
        // TODO: Map visibility to JolkVisibility enum
        return super.visitVisibility(ctx);
    }

    @Override
    public JolkNode visitFinality(jolkParser.FinalityContext ctx) {
        // TODO: Map finality to JolkFinality enum
        return super.visitFinality(ctx);
    }

    @Override
    public JolkNode visitArchetype(jolkParser.ArchetypeContext ctx) {
        // TODO: Map keyword to JolkArchetype
        return super.visitArchetype(ctx);
    }

    @Override
    public JolkNode visitType_bound(jolkParser.Type_boundContext ctx) {
        // TODO: Resolve type and contract conjunctions
        return super.visitType_bound(ctx);
    }

    @Override
    public JolkNode visitType_args(jolkParser.Type_argsContext ctx) {
        // TODO: Resolve generic type arguments
        return super.visitType_args(ctx);
    }

    @Override
    public JolkNode visitType_contracts(jolkParser.Type_contractsContext ctx) {
        // TODO: Handle extends/implements hierarchy
        return super.visitType_contracts(ctx);
    }

    @Override
    public JolkNode visitType_mbr(jolkParser.Type_mbrContext ctx) {
        // TODO: Handle member annotations and member declaration
        return super.visitType_mbr(ctx);
    }

    @Override
    public JolkNode visitExtension_decl(jolkParser.Extension_declContext ctx) {
        // For @Intrinsic extensions, we must register the target host class 
        // (e.g. java.lang.Throwable) in the JolkContext. This side-effect ensures 
        // that the type identity is resolved early, allowing the Truffle Interop 
        // protocol to correctly identify the receiver and dispatch Jolk messages 
        // to the appropriate runtime extension (like JolkExceptionExtension).
        if (ctx.type() != null) {
            String targetTypePath = ctx.type().getText();
            language.getContextReference().get(null).registerHostClass(targetTypePath);
        }

        // TODO: Handle extension method definitions for non-intrinsic extensions.
        // Currently, we return an empty node because intrinsic logic is 
        // provided by the Truffle runtime libraries.
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitExtension_mbr(jolkParser.Extension_mbrContext ctx) {
        // TODO: Handle extension method/member
        return super.visitExtension_mbr(ctx);
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
    public JolkNode visitState(jolkParser.StateContext ctx) {
        // TODO: Handle state (field or constant)
        return super.visitState(ctx);
    }

    @Override
    public JolkNode visitField(jolkParser.FieldContext ctx) {
        // The 'stable' keyword signals instance-level immutability.
        // We check for the terminal presence to set the stability flag.
        boolean isStable = ctx.STABLE() != null;
        String name = ctx.identifier().getText().intern();
        String typeName = ctx.type().getText().intern();
        JolkNode initializer = new JolkEmptyNode();
        if (ctx.assignment() != null) {
            initializer = visit(ctx.assignment());
        }

        // If inside a method/closure, this is a local variable declaration
        if (!scopes.isEmpty()) {
            List<String> currentScope = scopes.peek();
            currentScope.add(name);
            LocalInfo info = resolveLocal(name);
            return new JolkWriteLocalVariableNode(info.index, info.depth, initializer);
        }

        return new JolkFieldNode(name, typeName, initializer, isStable);
    }

    @Override
    public JolkNode visitConstant(jolkParser.ConstantContext ctx) {
        // Constants are implicitly stable (immutable) identities.
        String name = ctx.binding().identifier().getText().intern();
        String typeName = ctx.type().getText().intern();
        JolkNode initializer = visit(ctx.binding().expression());

        if (!scopes.isEmpty()) {
            List<String> currentScope = scopes.peek();
            currentScope.add(name);
            LocalInfo info = resolveLocal(name);
            // Note: PoC assumes write capability for initialization;
            // full implementation would enforce immutability post-init.
            return new JolkWriteLocalVariableNode(info.index, info.depth, initializer);
        }

        return new JolkFieldNode(name, typeName, initializer, true);
    }

    @Override
    public JolkNode visitBinding(jolkParser.BindingContext ctx) {
        String name = ctx.identifier().getText().intern();
        JolkNode expression = visit(ctx.expression());

        // Resolve lexical scope
        LocalInfo info = resolveLocal(name);
        if (info != null) {
            // Parameters are immutable in Jolk; only locals can be reassigned.
            if (info.isParameter) {
                throw new RuntimeException("Jolk Error: Cannot reassign immutable parameter '" + name + "'");
            }
            return new JolkWriteLocalVariableNode(info.index, info.depth, expression);
        }

        // Fallback: Jolk treats assignments as message sends to 'self' (synthesized setters)
        return new JolkMessageSendNode(visitReservedSelf(), name, new JolkNode[]{expression});
    }

    @Override
    public JolkNode visitAssignment(jolkParser.AssignmentContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public JolkNode visitEnum_constant(jolkParser.Enum_constantContext ctx) {
        // TODO: Handle enum constant with optional arguments
        return super.visitEnum_constant(ctx);
    }

    @Override
    public JolkNode visitMethod(jolkParser.MethodContext ctx) {
        String name = ctx.selector_id().getText().intern();

        // Signature Fence: Identify if this is a Command method (implicit self return).
        // If the return type is 'Self' or the class name, it follows the Self-Return Contract.
        String returnType = ctx.type() != null ? ctx.type().getText() : null;
        boolean isCommand = returnType == null || "Self".equals(returnType) || returnType.equals(currentClassName);

        String[] params = new String[0];
        boolean isVariadic = false;
        if (ctx.typed_params() != null) {
            ParameterSpec spec = parseTypedParams(ctx.typed_params());
            params = spec.names;
            isVariadic = spec.isVariadic;
        }

        // Establish the method scope: 'self' is always at index 0.
        List<String> methodScope = new ArrayList<>();
        methodScope.add("self");
        methodScope.addAll(Arrays.asList(params));
        
        // Store the number of parameters (including 'self') for this scope.
        // This threshold is used to distinguish parameters from local variables.
        int currentParameterThreshold = methodScope.size();
        parameterThresholds.push(methodScope.size());
        int baseDepth = scopes.size();
        scopes.push(methodScope);
        methodDepths.push(baseDepth);
        JolkNode body = new JolkEmptyNode();
        int frameSlots = 0;
        try {
            if (ctx.block() != null) {
                body = visit(ctx.block());
                if (isCommand) {
                    // Synthesized Return: Append 'self' to the block. If an explicit '^' 
                    // was hit, the JolkReturnException will bypass this node.
                    body = new JolkBlockNode(new JolkNode[]{body, visitReservedSelf()});
                }
            }
            frameSlots = methodScope.size();
        } finally {
            scopes.pop();
            parameterThresholds.pop();
            methodDepths.pop();
        }
        return new JolkMethodNode(name, body, params, isVariadic, frameSlots);
    }

    @Override
    public JolkNode visitAnnotated_type(jolkParser.Annotated_typeContext ctx) {
        // TODO: Collect type annotations and resolve type
        return super.visitAnnotated_type(ctx);
    }

    @Override
    public JolkNode visitVararg_id(jolkParser.Vararg_idContext ctx) {
        // TODO: Handle vararg spread
        return super.visitVararg_id(ctx);
    }

    @Override
    public JolkNode visitExpression(jolkParser.ExpressionContext ctx) {
        JolkNode result = visit(ctx.logic_or());
        // Handle ternary operations: condition ? trueBranch : falseBranch
        if (!ctx.expression().isEmpty()) {
            String op = ctx.getChild(1).getText(); // "?" or "?!"
            // We pass the raw ParseTree (the expression context) to ensureClosure.
            // This allows the visitor to decide whether to wrap it in an implicit closure
            // or visit it directly if it's already a closure literal.
            JolkNode thenBranch = ensureClosure(ctx.expression(0));

            if (ctx.expression().size() > 1) {
                // Atomic ternary: if both branches are present, we dispatch a single message
                // to ensure the branch result is returned instead of the Boolean receiver.
                String selector = op + " :"; // results in "? :" or "?! :"
                JolkNode elseBranch = ensureClosure(ctx.expression(1));
                result = new JolkMessageSendNode(result, selector, new JolkNode[]{thenBranch, elseBranch});
            } else {
                // Binary branching: behaves as a control-flow message returning the receiver.
                result = new JolkMessageSendNode(result, op, new JolkNode[]{thenBranch});
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
            left = JolkLogicalNodeGen.create(op, right, left);
        }
        return left;
    }

    @Override
    public JolkNode visitLogic_and(jolkParser.Logic_andContext ctx) {
        JolkNode left = visit(ctx.inclusive_or(0));
        for (int i = 1; i < ctx.inclusive_or().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.inclusive_or(i));
            left = JolkLogicalNodeGen.create(op, right, left);
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
            left = new JolkComparisonNode(left, op, right);
        }
        return left;
    }

    @Override
    public JolkNode visitTerm(jolkParser.TermContext ctx) {
        JolkNode left = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.factor(i));
            left = JolkArithmeticNodeGen.create(op, left, right);
        }
        return left;
    }

    @Override
    public JolkNode visitFactor(jolkParser.FactorContext ctx) {
        JolkNode left = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            JolkNode right = visit(ctx.unary(i));
            left = JolkArithmeticNodeGen.create(op, left, right);
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
        return JolkUnaryNodeGen.create(op, operand);
    }

    @Override
    public JolkNode visitPower(jolkParser.PowerContext ctx) {
        JolkNode left = visit(ctx.message());
        if (ctx.powOp() != null) {
            String op = ctx.powOp().getText();
            JolkNode right = visit(ctx.unary());
            left = JolkArithmeticNodeGen.create(op, left, right);
        }
        if (ctx.NULL_COALESCE() != null) {
            // Lazy Evaluation: The fallback expression must be wrapped in a closure.
            JolkNode rightClosure = ensureClosure(ctx.power());
            left = new JolkMessageSendNode(left, "??", new JolkNode[]{rightClosure});
        }
        return left;
    }

    @Override
    public JolkNode visitMessage(jolkParser.MessageContext ctx) {
        JolkNode receiver = ctx.primary() != null ? visit(ctx.primary()) : visitReservedSelf();
        if (receiver == null) {
            receiver = new JolkEmptyNode();
        }

        // Aligned Child Traversal: In ANTLR4, optional children (like payload?) are not padded 
        // in their respective lists. We must traverse the actual child sequence to correctly 
        // pair selectors with their immediate payloads.
        for (int i = 0; i < ctx.getChildCount(); i++) {
            var child = ctx.getChild(i);
            if (child instanceof jolkParser.SelectorContext selectorCtx) {
                String selector = selectorCtx.identifier().getText();
                JolkNode[] args = new JolkNode[0];

                // Check if the immediately following child is a payload
                if (i + 1 < ctx.getChildCount() && ctx.getChild(i + 1) instanceof jolkParser.PayloadContext payloadCtx) {
                    if (payloadCtx.arguments() != null) {
                        var exprs = payloadCtx.arguments().expression();
                        args = new JolkNode[exprs.size()];
                        for (int j = 0; j < exprs.size(); j++) args[j] = visit(exprs.get(j));
                    } else if (payloadCtx.closure() != null) {
                        args = new JolkNode[] { visit(payloadCtx.closure()) };
                    }
                    i++; // Consume the payload child
                }

                receiver = (receiver instanceof JolkSuperNode)
                    ? new JolkSuperMessageSendNode(selector, args)
                    : new JolkMessageSendNode(receiver, selector, args);
            }
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

        // For closures, index 0 is reserved for the captured lexical environment.
        List<String> closureScope = new ArrayList<>();
        closureScope.add("<env>"); 
        closureScope.addAll(Arrays.asList(params));
        
        parameterThresholds.push(closureScope.size());
        scopes.push(closureScope);
        JolkNode body = new JolkEmptyNode();
        int frameSlots = 0;
        try {
            if (ctx.statements() != null) {
                body = visit(ctx.statements());
            }
            frameSlots = closureScope.size();
        } finally {
            scopes.pop();
            parameterThresholds.pop();
        }

        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlots(frameSlots, FrameSlotKind.Object);
        
        // Closures are not method boundaries; isMethod must be false to
        // allow Non-Local Returns (^) to propagate to the defining method.
        JolkRootNode jolkRootNode = new JolkRootNode(language, builder.build(), body, "closure", false);
        return new JolkClosureNode(jolkRootNode.getCallTarget(), params, isVariadic);
    }

    @Override
    public JolkNode visitAnnotation(jolkParser.AnnotationContext ctx) {
        // TODO: Resolve annotation and its arguments
        return super.visitAnnotation(ctx);
    }

    @Override
    public JolkNode visitAnnotation_args(jolkParser.Annotation_argsContext ctx) {
        // TODO: Handle annotation argument list
        return super.visitAnnotation_args(ctx);
    }

    @Override
    public JolkNode visitAnnotation_arg(jolkParser.Annotation_argContext ctx) {
        // TODO: Handle key-value annotation pair
        return super.visitAnnotation_arg(ctx);
    }

    @Override
    public JolkNode visitAnnotation_val(jolkParser.Annotation_valContext ctx) {
        // TODO: Handle nested annotation values
        return super.visitAnnotation_val(ctx);
    }

    @Override
    public JolkNode visitPrimary(jolkParser.PrimaryContext ctx) {
        if (ctx.reserved() != null) return visit(ctx.reserved());
        // Jolk matches InstanceId as 'identifier' and MetaId as 'type' in the primary rule.
        // We must check both to ensure both instance and meta identifiers are resolved.
        if (ctx.identifier() != null) return visit(ctx.identifier());
        if (ctx.type() != null) return visit(ctx.type());

        if (ctx.literal() != null) return visit(ctx.literal());
        if (ctx.list_literal() != null) return visit(ctx.list_literal());
        if (ctx.expression() != null) return visit(ctx.expression());
        if (ctx.closure() != null) return visit(ctx.closure());
        if (ctx.method_reference() != null) return visit(ctx.method_reference());
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitReserved(jolkParser.ReservedContext ctx) {
        String text = ctx.getText();
        return switch (text) {
            case "null" -> new JolkLiteralNode(JolkNothing.INSTANCE);
            case "self" -> visitReservedSelf();
            case "super" -> new JolkSuperNode();
            case "Self" -> new JolkReadTypeNode(language, currentClassName, null); // Directly resolve the current class's MetaClass
            case "true" -> new JolkLiteralNode(true);
            case "false" -> new JolkLiteralNode(false);
            default -> new JolkEmptyNode();
        };
    }

    @Override
    public JolkNode visitIdentifier(jolkParser.IdentifierContext ctx) {
        return createIdentifierNode(ctx.getText().intern());
    }

    @Override
    public JolkNode visitMethod_reference(jolkParser.Method_referenceContext ctx) {
        // TODO: Handle method reference (##)
        return super.visitMethod_reference(ctx);
    }

    /**
     * ### visitType
     * 
     * Resolves a Type reference. In Jolk, Types are first-class Meta-Objects.
     */
    @Override
    public JolkNode visitType(jolkParser.TypeContext ctx) {
        // We extract the base identifier (MetaId), as generics are erased at runtime in the PoC.
        // This ensures that 'ArrayList<Long>' is resolved as 'ArrayList'.
        String name = (ctx.MetaId() != null) ? ctx.MetaId().getText().intern() : ctx.getText().intern();

        if ("Self".equals(name)) {
            return new JolkReadTypeNode(language, currentClassName, null);
        }
        return createIdentifierNode(name);
    }

    /**
     * ### createIdentifierNode
     * 
     * Centralized logic for resolving identifiers into AST nodes based on
     * lexical scope and semantic casing.
     */
    private JolkNode createIdentifierNode(String name) {

        // 1. Check if it's a Parameter in the current or outer scope (Recursive Search)
        JolkNode argNode = lookupIdentifier(name);
        if (argNode != null) {
            return argNode;
        }

        // 2. Semantic Casing: Uppercase names are Meta-Objects (Types/Constants)
        if (Character.isUpperCase(name.charAt(0))) {
            // Priority 1: Intrinsic Types (Resolved as static literals for performance)
            if ("Long".equals(name) || "Int".equals(name)) return new JolkLiteralNode(tolk.runtime.JolkLongExtension.LONG_TYPE);
            if ("Boolean".equals(name)) return new JolkLiteralNode(tolk.runtime.JolkBooleanExtension.BOOLEAN_TYPE);
            if ("Nothing".equals(name)) return new JolkLiteralNode(tolk.runtime.JolkNothing.NOTHING_TYPE);
            if ("String".equals(name)) return new JolkLiteralNode(tolk.runtime.JolkStringExtension.STRING_TYPE);
            if ("Exception".equals(name)) return new JolkLiteralNode(tolk.runtime.JolkExceptionExtension.EXCEPTION_TYPE);
            
            // Support for the Array archetype via common Java collection aliases.
            if ("Array".equals(name) || "List".equals(name) || "ArrayList".equals(name)) 
                return new JolkLiteralNode(tolk.runtime.JolkArrayExtension.ARRAY_TYPE);

            // Priority 2: Current Class Reference
            if (name.equals(currentClassName)) return new JolkReadTypeNode(language, name, null);
            
            // Priority 3: User-Defined Meta-Objects (Resolved via dynamic lookup with Meta-fallback)
            // This handles both external class references (ClassA) and internal constants (FORTY_TWO).
            JolkNode metaSelf = isInMetaScope ? visitReservedSelf() : new JolkMessageSendNode(visitReservedSelf(), "class", new JolkNode[0]);
            return new JolkReadTypeNode(language, name, metaSelf);
        }

        // 3. Default: Treat as a message send to 'self' (e.g. field access)
        return new JolkMessageSendNode(visitReservedSelf(), name, new JolkNode[0]);
    }

    @Override
    public JolkNode visitLiteral(jolkParser.LiteralContext ctx) {
        if (ctx.NumberLiteral() != null) 
            // Use parseUnsignedLong to handle magnitudes up to 2^64-1 (bit-pattern representation).
            // This correctly handles 9223372036854775808 (2^63) which is then Negated 
            // by the unary operator in the runtime to result in Long.MIN_VALUE.
            return new JolkLiteralNode(Long.parseUnsignedLong(ctx.NumberLiteral().getText()));
            
        if (ctx.StringLiteral() != null) {
            String text = ctx.StringLiteral().getText();
            // Remove the surrounding quotes
            String val = text.substring(1, text.length() - 1);
            return new JolkLiteralNode(val);
        }

        if (ctx.CharLiteral() != null) {
            // TODO: Implementation for CharLiteral
        }

        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitList_literal(jolkParser.List_literalContext ctx) {
        if (ctx.array_literal() != null) return visit(ctx.array_literal());
        if (ctx.set_literal() != null) return visit(ctx.set_literal());
        if (ctx.map_literal() != null) return visit(ctx.map_literal());
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitArray_literal(jolkParser.Array_literalContext ctx) {
        // Collection Literal Synthesis: #[1, 2, 3] -> Array #new(1, 2, 3)
        // This converts the bracketed literal into a standard variadic message 
        // send to the Array meta-object, supporting Unified Messaging.
        JolkNode receiver = new JolkLiteralNode(tolk.runtime.JolkArrayExtension.ARRAY_TYPE);
        JolkNode[] args = extractLiteralList(ctx.literal_list());
        return new JolkMessageSendNode(receiver, "new", args);
    }

    @Override
    public JolkNode visitSet_literal(jolkParser.Set_literalContext ctx) {
        // TODO: Handle set literal synthesis
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitMap_literal(jolkParser.Map_literalContext ctx) {
        // TODO: Handle map literal synthesis
        return new JolkEmptyNode();
    }

    /**
     * ### extractLiteralList
     * 
     * Helper to extract an array of JolkNodes from a comma-separated literal list.
     */
    private JolkNode[] extractLiteralList(jolkParser.Literal_listContext ctx) {
        if (ctx == null || ctx.expression() == null) return new JolkNode[0];
        return ctx.expression().stream()
                  .map(this::visit)
                  .toArray(JolkNode[]::new);
    }

    @Override
    public JolkNode visitMap_entry(jolkParser.Map_entryContext ctx) {
        // TODO: Handle key -> value map entry
        return super.visitMap_entry(ctx);
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
        // Handle all statement alternatives defined in the grammar.
        if (ctx.constant() != null) return visit(ctx.constant());
        if (ctx.field() != null) return visit(ctx.field());
        if (ctx.binding() != null) return visit(ctx.binding());

        if (ctx.expression() != null) {
            // Check for the explicit return symbol (CARET) and calculate target method depth
            // We check the first token of the statement specifically to avoid false positives in sub-expressions.
            boolean hasCaret = ctx.getToken(jolkParser.CARET, 0) != null || 
                              (ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("^"));
            
            // We visit the expression after checking for the caret to maintain structural context
            JolkNode exprNode = visit(ctx.expression());
            
            if (hasCaret) {
                // targetDepth calculation:
                // 1. In a method: distance from current closure scope back to the method scope.
                // 2. At top-level (script): depth needed to reach the script root activation.
                int targetDepth = methodDepths.isEmpty() ? scopes.size() : 
                                  Math.max(0, scopes.size() - methodDepths.peek() - 1);
                return new JolkReturnNode(exprNode, new JolkReadEnvironmentNode(targetDepth));
            }
            return exprNode;
        }
        return new JolkEmptyNode();
    }

    @Override
    public JolkNode visitStatements(jolkParser.StatementsContext ctx) {
        // Preserves all statements by wrapping them in a BlockNode.
        List<JolkNode> nodes = new ArrayList<>();
        List<jolkParser.StatementContext> statements = ctx.statement();
        for (int i = 0; i < statements.size(); i++) {
            JolkNode node = visit(statements.get(i));
            nodes.add(node);
            
            if (node instanceof JolkReturnNode && i < statements.size() - 1) {
                // ### Dead Code Detection
                // In alignment with Jolk's 'Engineered Integrity', unreachable code 
                // is treated as a terminal semantic error rather than being silently ignored.
                int line = statements.get(i + 1).getStart().getLine();
                throw new RuntimeException("Jolk Semantic Error [line " + line + "]: Unreachable code detected after return terminal (^).");
            }
        }
        return new JolkBlockNode(nodes.toArray(new JolkNode[0]));
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
        for (var id : ctx.InstanceId()) {
            names.add(id.getText());
        }
        return names.toArray(new String[0]);
    }

    /**
     * Ensures that a parse tree branch is treated as a closure.
     * If it is not already a closure literal (e.g., [ ... ]), it accounts for 
     * implicit closure scoping by pushing an environment scope before visitation.
     *
     * @param tree The parse tree to visit.
     * @return A [JolkNode] representing the closure.
     */
    private JolkNode ensureClosure(ParseTree tree) {
        if (tree == null) return new JolkEmptyNode();
        
        if (isClosureLiteral(tree)) {
            return visit(tree);
        }

        // Implicit closure: push an environment scope to account for frame depth.
        scopes.push(new ArrayList<>(List.of("<env>")));
        parameterThresholds.push(1); // One slot for the implicit environment capture
        try {
            JolkNode body = visit(tree);
            FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
            builder.addSlots(1, FrameSlotKind.Object);
            RootNode root = new JolkRootNode(language, builder.build(), body, "closure", false);
            return new JolkClosureNode(root.getCallTarget(), new String[0], false);
        } finally {
            scopes.pop();
            parameterThresholds.pop();
        }
    }

    private boolean isClosureLiteral(ParseTree tree) {
        if (tree instanceof jolkParser.ClosureContext) return true;
        if (tree.getChildCount() == 1) return isClosureLiteral(tree.getChild(0));
        return false;
    }

    /**
     * Searches through the lexical scope stack recursively to find an identifier.
     * Calculates the frame depth required to reach the variable.
     *
     * @param name The name of the identifier to look up.
     * @return A [JolkNode] to read the argument if found, otherwise null.
     */
    private JolkNode lookupIdentifier(String name) {
        LocalInfo info = resolveLocal(name);
        if (info != null) {
            return info.isParameter 
                ? new JolkReadArgumentNode(info.index, info.depth)
                : new JolkReadLocalVariableNode(info.index, info.depth);
        }
        return null;
    }

    private record LocalInfo(int index, int depth, boolean isParameter) {}

    private LocalInfo resolveLocal(String name) {
        int currentDepth = 0;
        for (int i = scopes.size() - 1; i >= 0; i--) {
            int index = scopes.get(i).indexOf(name);
            if (index != -1) {
                boolean isParam = index < parameterThresholds.get(i);
                return new LocalInfo(index, currentDepth, isParam);
            }
            currentDepth++;
        }
        return null;
    }

    /**
     * Resolves the 'self' reference by prioritizing the lexically captured 'self'
     * (Home context) over the dynamic receiver.
     */
    private JolkNode visitReservedSelf() {
        JolkNode selfNode = lookupIdentifier("self");
        return selfNode != null ? selfNode : new JolkSelfNode();
    }
}