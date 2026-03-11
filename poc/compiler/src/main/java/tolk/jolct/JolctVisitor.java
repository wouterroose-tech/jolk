package tolk.jolct;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tolk.grammar.jolkBaseVisitor;
import tolk.grammar.jolkParser;
import tolk.grammar.jolkParser.Method_referenceContext;

public class JolctVisitor extends jolkBaseVisitor<String> {

    // global indentation state for readability of generated code
    final JolkContext context;
    String currentPackage = "";
    String currentClass = "";
    List<String> currentImports = new ArrayList<>();
    String currentMethodReturnType = null;
    boolean inConstructor = false;
    boolean inEqualsOverride = false;
    boolean suppressWildcard = false;
    boolean currentClassHasGenerics = true;
    private JolctVisitType_decl visitType_decl = new JolctVisitType_decl(this);
    private JolctVisitMethod visitMethod = new JolctVisitMethod(this);
    private JolctVisitorExpression visitorExpression = new JolctVisitorExpression(this);
    private JolctVisitorMessage visitorMessage = new JolctVisitorMessage(this);
    JolctVoidPathDetector voidPathDetector = new JolctVoidPathDetector();

    public JolctVisitor(JolkContext context) {
        this.context = context;
    }

    @Override
    public String visitUnit(jolkParser.UnitContext ctx) {
        StringBuilder sb = new StringBuilder();
        this.currentImports.clear(); // Clear at the start of a new unit

        boolean isJolkLangPackage = false;
        if (ctx.package_decl() != null) {
            sb.append(visit(ctx.package_decl()));
            if (ctx.package_decl().namespace() != null && "jolk.lang".equals(ctx.package_decl().namespace().getText())) {
                isJolkLangPackage = true;
            }
        }
        // The visit methods will add to currentImports
        for (jolkParser.ExpansionContext ec : ctx.expansion()) {
            sb.append(visit(ec));
        }
        for (jolkParser.ProjectionContext pc : ctx.projection()) {
            sb.append(visit(pc));
        }

        if (ctx.type_decl() != null) {
            sb.append(visit(ctx.type_decl()));
        } else if (ctx.extension_decl() != null) {
            sb.append(visit(ctx.extension_decl()));
        }
        return sb.toString();
    }

    @Override
    public String visitPackage_decl(jolkParser.Package_declContext ctx) {
        this.currentPackage = ctx.namespace().getText();
        return "package " + this.currentPackage + ";\n";
    }

    @Override
    public String visitExpansion(jolkParser.ExpansionContext ctx) {
        jolkParser.InclusionContext inclusion = ctx.inclusion();
        String namespace = inclusion.namespace().getText();
        boolean isWildcard = inclusion.MUL() != null;
        String imp = "import " + namespace + (isWildcard ? ".*" : "") + ";\n";
        this.currentImports.add(namespace + (isWildcard ? ".*" : ""));
        return imp;
    }

    @Override
    public String visitProjection(jolkParser.ProjectionContext ctx) {
        jolkParser.InclusionContext inclusion = ctx.inclusion();
        String namespace = inclusion.namespace().getText();
        boolean isWildcard = inclusion.MUL() != null;
        // Lensing will be translated to import static.
        String imp = "import static " + namespace + (isWildcard ? ".*" : "") + ";\n";
        this.currentImports.add("static " + namespace + (isWildcard ? ".*" : ""));
        return imp;
    }

    @Override
    public String visitType_decl(jolkParser.Type_declContext ctx) {

        String finality = "";
        String visibility = "public ";

        if (ctx.modifiers() != null) {
            jolkParser.ModifiersContext modsCtx = ctx.modifiers();
            if (modsCtx.finality() != null) {
                finality = modsCtx.finality().getText() + " ";
            }
            if (modsCtx.vis_mod() != null) {
                if (modsCtx.vis_mod().visibility() != null) {
                    String v = modsCtx.vis_mod().visibility().getText();
                    visibility = "package".equals(v) ? "" : v + " ";
                }
                if (modsCtx.vis_mod().MODIFIER() != null) {
                    String[] mods = parseModifier(modsCtx.vis_mod().MODIFIER().getText());
                    if (mods[0] != null) visibility = "package".equals(mods[0]) ? "" : mods[0] + " ";
                    if (mods[1] != null) finality = mods[1] + " ";
                }
            }
        }

        String typeName = ctx.type_bound().type().MetaId().getText();
        this.currentClass = typeName;
        String archetype = ctx.archetype().getText();

        StringBuilder sb = new StringBuilder();
        sb.append(visitType_decl.visitType_decl(ctx));

        if ("enum".equals(archetype)) {
            visitType_decl.visitEnum(ctx, visibility, typeName, sb);
        } else if ("record".equals(archetype)) {
            visitType_decl.visitRecord(ctx, visibility, typeName, sb);
        } else if ("protocol".equals(archetype)) {
            visitType_decl.visitProtocol(ctx, visibility, typeName, sb);
        } else {
            visitType_decl.visitClass(ctx, finality, visibility, typeName, sb);
        }

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String visitType(jolkParser.TypeContext ctx) {
        if (ctx.MetaId() != null) {
            String name = ctx.MetaId().getText();

            StringBuilder namespace = new StringBuilder();
            for (jolkParser.IdentifierContext id : ctx.identifier()) {
                namespace.append(id.getText()).append(".");
            }
            String ns = namespace.toString();

            boolean isObject = "Object".equals(name) && ns.isEmpty();
            if (inEqualsOverride && isObject) {
                return "Object";
            }

            boolean isCore = ns.isEmpty() && Set.of("Object", "Array", "Set", "Map", "Nothing", "Closure").contains(name);

            String checkName = ns + name;
            boolean isCrtp = context.isCrtpType(checkName, currentPackage, currentImports);

            // Object is the root Jolk class and always has the Self generic
            if (isObject)
                isCrtp = true;

            boolean injectWildcard = isCrtp;
            if (injectWildcard) {
                String fullCurrentName = currentPackage.isEmpty() ? currentClass : currentPackage + "." + currentClass;
                if (checkName.equals(fullCurrentName) || (ns.isEmpty() && name.equals(currentClass))) {
                    if (!currentClassHasGenerics) {
                        injectWildcard = false;
                    }
                }
            }
            if (suppressWildcard) {
                injectWildcard = false;
            }
            if (ctx.getParent() instanceof jolkParser.PrimaryContext) {
                injectWildcard = false;
            }
            if (ctx.getParent() instanceof jolkParser.Type_contractsContext) {
                injectWildcard = false;
            }

            StringBuilder sb = new StringBuilder();
            if (isCore) {
                sb.append("jolk.lang.").append(name);
            } else {
                sb.append(ns);
                sb.append(name);
            }

            if (ctx.type_args() != null) {
                sb.append("<").append(visit(ctx.type_args()));
                if (injectWildcard)
                    sb.append(", ?");
                sb.append(">");
            } else if (injectWildcard) {
                sb.append("<?>");
            }
            return sb.toString();
        }
        return ctx.getText().replace("[", "<").replace("]", ">");
    }

    @Override
    public String visitEnum_constant(jolkParser.Enum_constantContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.MetaId().getText());
        if (ctx.arguments() != null) {
            sb.append("(").append(visit(ctx.arguments())).append(")");
        }
        return sb.toString();
    }

    @Override
    public String visitType_mbr(jolkParser.Type_mbrContext ctx) {
        if (ctx.member() == null)
            return "";

        String memberCode = visit(ctx.member());

        if (memberCode == null || memberCode.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(memberCode);
        if (!memberCode.endsWith("\n")) {
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String visitMember(jolkParser.MemberContext ctx) {
        StringBuilder sb = new StringBuilder();

        String visibility = null;
        String finality = ctx.finality() != null ? ctx.finality().getText() : null;

        if (ctx.vis_mod() != null) {
            if (ctx.vis_mod().visibility() != null) {
                visibility = ctx.vis_mod().visibility().getText();
            }
            if (ctx.vis_mod().MODIFIER() != null) {
                String[] mods = parseModifier(ctx.vis_mod().MODIFIER().getText());
                if (mods[0] != null) visibility = mods[0];
                if (mods[1] != null) finality = mods[1];
            }
        }

        if (visibility != null) {
            if (!"package".equals(visibility))
                sb.append(visibility).append(" ");
        } else {
            if (ctx.method() != null) {
                sb.append("public ");
            } else {
                sb.append("private ");
            }
        }

        boolean isMeta = ctx.META() != null;
        boolean isConstructor = false;
        if (ctx.method() != null) {
            String name = ctx.method().selector_id().getText();
            if (isMeta && "new".equals(name))
                isConstructor = true;
        }

        if (isMeta && !isConstructor) {
            sb.append("static ");
        }

        if (finality != null) {
            sb.append(finality).append(" ");
        }

        boolean prevInConstructor = this.inConstructor;
        this.inConstructor = isConstructor;
        if (ctx.state() != null) sb.append(visit(ctx.state()));
        else sb.append(visit(ctx.method()));
        this.inConstructor = prevInConstructor;

        return sb.toString();
    }

    @Override
    public String visitMethod(jolkParser.MethodContext ctx) {
        return visitMethod.visitMethod(ctx);
    }

    @Override
    public String visitTyped_params(jolkParser.Typed_paramsContext ctx) {
        List<String> params = new ArrayList<>();
        List<org.antlr.v4.runtime.tree.TerminalNode> ids = ctx.InstanceId();
        for (int i = 0; i < ctx.annotated_type().size(); i++) {
            String type = visit(ctx.annotated_type(i));
            String name = (i < ids.size())
                    ? ids.get(i).getText()
                    : (ctx.vararg_id() != null ? ctx.vararg_id().getText() : "arg" + i);
            params.add(type + " " + name);
        }
        return String.join(", ", params);
    }
	@Override
    public String visitInferred_params(jolkParser.Inferred_paramsContext ctx) {
        return ctx.InstanceId().stream()
                .map(org.antlr.v4.runtime.tree.ParseTree::getText)
                .collect(Collectors.joining(", "));
    }

	@Override
    public String visitStat_params(jolkParser.Stat_paramsContext ctx) {
        if (ctx.typed_params() != null) {
            return visit(ctx.typed_params());
        }
        if (ctx.inferred_params() != null) {
            return visit(ctx.inferred_params());
        }
        return "";
    }

    @Override
    public String visitAnnotated_type(jolkParser.Annotated_typeContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (jolkParser.AnnotationContext a : ctx.annotation()) {
            sb.append(visit(a)).append(" ");
        }
        sb.append(visit(ctx.type()));
        return sb.toString();
    }

    @Override
    public String visitAnnotation(jolkParser.AnnotationContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitClosure(jolkParser.ClosureContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 1. Parameters
        boolean needsParens = true;
        if (ctx.stat_params() != null) {
            if (ctx.stat_params().inferred_params() != null &&
                ctx.stat_params().inferred_params().InstanceId().size() == 1 &&
                ctx.stat_params().typed_params() == null) {
                needsParens = false;
            }
        } else {
            // No params, needs empty parens
            sb.append("()");
        }

        if (ctx.stat_params() != null) {
            if (needsParens) sb.append("(");
            sb.append(visit(ctx.stat_params()));
            if (needsParens) sb.append(")");
        }

        sb.append(" -> ");

        // 2. Body
        boolean needsBraces = true;
        if (ctx.statements() != null && ctx.statements().statement().size() == 1) {
            jolkParser.StatementContext stmt = ctx.statements().statement(0);
            if (stmt.expression() != null && stmt.returnOp() == null && stmt.constant() == null && stmt.field() == null && stmt.binding() == null) {
                needsBraces = false;
            }
        }

        if (needsBraces) {
            sb.append("{\n");
            sb.append(visitClosureBody(ctx));
            sb.append("}");
        } else {
            String stmtStr = visit(ctx.statements().statement(0));
            if (stmtStr.endsWith(";")) {
                stmtStr = stmtStr.substring(0, stmtStr.length() - 1);
            }
            sb.append(stmtStr);
        }

        return sb.toString();
    }

    public String visitClosureBody(jolkParser.ClosureContext ctx) {
        if (ctx.statements() == null) {
            return "";
        }
        return visit(ctx.statements());
    }
    
    @Override
    public String visitStatements(jolkParser.StatementsContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (jolkParser.StatementContext stmt : ctx.statement()) {
            String s = visit(stmt);
            if (!s.isEmpty()) {
                sb.append(s).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String visitStatement(jolkParser.StatementContext ctx) {
        if (ctx.constant() != null)
            return visit(ctx.constant()) + ";";
        if (ctx.field() != null)
            return visit(ctx.field()) + ";";
        if (ctx.binding() != null)
            return visit(ctx.binding()) + ";";
        if (ctx.expression() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String expr = visit(ctx.expression());

        if (ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("^")) {
            if (!expr.trim().startsWith("return") && !expr.trim().startsWith("if") && !inConstructor) {
                sb.append("return ");
            }
        }

        sb.append(expr);
        if (!expr.trim().endsWith("}")) {
            sb.append(";");
        }
        return sb.toString();
    }

    @Override
    public String visitExpression(jolkParser.ExpressionContext ctx) {
        return visitorExpression.visitExpression(ctx);
    }

    @Override
    public String visitLogic_or(jolkParser.Logic_orContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitLogic_and(jolkParser.Logic_andContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitInclusive_or(jolkParser.Inclusive_orContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitExclusive_or(jolkParser.Exclusive_orContext ctx) {
        if (ctx.bitwise_and().size() == 1) {
            return visit(ctx.bitwise_and(0));
        }
        String result = visit(ctx.bitwise_and(0));
        for (int i = 1; i < ctx.bitwise_and().size(); i++) {
            String op = ctx.getChild(i * 2 - 1).getText();
            String right = visit(ctx.bitwise_and(i));
            if ("|!".equals(op)) {
                op = "^";
            }
            result = result + " " + op + " " + right;
        }
        return result;
    }

    @Override
    public String visitBitwise_and(jolkParser.Bitwise_andContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitEquality(jolkParser.EqualityContext ctx) {
        if (ctx.comparison().size() == 1) {
            return visit(ctx.comparison(0));
        }

        String result = visit(ctx.comparison(0));
        for (int i = 1; i < ctx.comparison().size(); i++) {
            String op = ctx.getChild(i * 2 - 1).getText();
            String right = visit(ctx.comparison(i));
            switch (op) {
                case "~~":
                    result = "java.util.Objects.equals(" + result + ", " + right + ")";
                    break;
                case "!~":
                    result = "(!java.util.Objects.equals(" + result + ", " + right + "))";
                    break;
                default: // ==, !=
                    result = result + " " + op + " " + right;
                    break;
            }
        }
        return result;
    }

    @Override
    public String visitComparison(jolkParser.ComparisonContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitTerm(jolkParser.TermContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitFactor(jolkParser.FactorContext ctx) {
        return visitBinary(ctx);
    }

    @Override
    public String visitUnary(jolkParser.UnaryContext ctx) {
        // Determine if a prefix operator (!) or (-) is present
        if (ctx.getChildCount() > 1) {
            String operator = ctx.getChild(0).getText();
            // Recursive descent to handle nested unary operators (e.g., !!x)
            String operand = visitUnary(ctx.unary());
            return operator + operand;
        }
        return visitPower(ctx.power());
    }

    @Override
    public String visitPower(jolkParser.PowerContext ctx) {
        String left = visit(ctx.message());
        if (ctx.powOp() != null) {
            left = left + visit(ctx.powOp()) + visit(ctx.unary());
        }
        if (ctx.NULL_COALESCE() != null) {
            String right = visit(ctx.power());
            return "(" + left + " != null ? " + left + " : " + right + ")";
        }
        return left;
    }

    @Override
    public String visitAddOp(jolkParser.AddOpContext ctx) {
        return " " + ctx.getText() + " ";
    }

    @Override
    public String visitMulOp(jolkParser.MulOpContext ctx) {
        return " " + ctx.getText() + " ";
    }

    @Override
    public String visitRelOp(jolkParser.RelOpContext ctx) {
        return " " + ctx.getText() + " ";
    }

    @Override
    public String visitPowOp(jolkParser.PowOpContext ctx) {
        return " " + ctx.getText() + " ";
    }

    private String visitBinary(org.antlr.v4.runtime.ParserRuleContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            org.antlr.v4.runtime.tree.ParseTree child = ctx.getChild(i);
            if (child instanceof org.antlr.v4.runtime.tree.TerminalNode) {
                sb.append(" ").append(child.getText()).append(" ");
            } else {
                sb.append(visit(child));
            }
        }
        return sb.toString().trim();
    }

    @Override
    public String visitField(jolkParser.FieldContext ctx) {
        String type = visit(ctx.type());
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ").append(ctx.identifier().getText());
        if (ctx.assignment() != null) {
            sb.append(" ").append(visit(ctx.assignment()));
        }
        return sb.toString();
    }

    @Override
    public String visitState(jolkParser.StateContext ctx) {
        if (ctx.constant() != null) return visit(ctx.constant()) + ";";
        if (ctx.field() != null) return visit(ctx.field()) + ";";
        return "";
    }

    @Override
    public String visitConstant(jolkParser.ConstantContext ctx) {
        StringBuilder sb = new StringBuilder("final ");
        sb.append(visit(ctx.type())).append(" ");
        sb.append(ctx.binding().identifier().getText());

        if (ctx.binding().expression() != null) {
            sb.append(" = ").append(visit(ctx.binding().expression()));
        }
        return sb.toString();
    }

    @Override
    public String visitBinding(jolkParser.BindingContext ctx) {
        return ctx.identifier().getText() + " = " + visit(ctx.expression());
    }

    @Override
    public String visitAssignment(jolkParser.AssignmentContext ctx) {
        return "= " + visit(ctx.expression());
    }

    @Override
    public String visitType_args(jolkParser.Type_argsContext ctx) {
        return ctx.type_bound().stream().map(this::visit).collect(Collectors.joining(", "));
    }

    @Override
    public String visitType_bound(jolkParser.Type_boundContext ctx) {
        return visit(ctx.type());
    }

    @Override
    public String visitMessage(jolkParser.MessageContext ctx) {
        return visitorMessage.visitMessage(ctx);
    }

    @Override
    public String visitSelector(jolkParser.SelectorContext ctx) {
        return ctx.identifier().getText();
    }

    @Override
    public String visitArguments(jolkParser.ArgumentsContext ctx) {
        return ctx.expression().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitPrimary(jolkParser.PrimaryContext ctx) {
        if (ctx.reserved() != null) {
            if (ctx.reserved().self_instance() != null) {
                return "this";
            }
            return ctx.reserved().getText();
        }
        if (ctx.type() != null) {
            return visit(ctx.type());
        }
        if (ctx.identifier() != null) {
            if (ctx.identifier().MetaId() != null) {
                String name = ctx.identifier().MetaId().getText();
                if (Set.of("Object", "Array", "Set", "Map", "Nothing", "Closure").contains(name)) {
                    return "jolk.lang." + name;
                }
                return name;
            }
            return ctx.identifier().getText();
        }
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        if (ctx.list_literal() != null) {
            return visit(ctx.list_literal());
        }
        if (ctx.expression() != null) {
            return "(" + visit(ctx.expression()) + ")";
        }
        if (ctx.closure() != null) {
            boolean prev = this.inConstructor;
            this.inConstructor = false;
            String res = visit(ctx.closure());
            this.inConstructor = prev;
            return res;
        }
        if (ctx.method_reference() != null) {
            return visit(ctx.method_reference());
        }
        return ctx.getText();
    }

    @Override
    public String visitLiteral(jolkParser.LiteralContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitMethod_reference(Method_referenceContext ctx) {
        String receiver;
        if (ctx.reserved() != null) {
            receiver = ctx.reserved().getText();
            if ("self".equals(receiver)) {
                receiver = "this";
            }
        } else {
            receiver = ctx.identifier(0).getText();
        }
        String method = ctx.identifier(ctx.identifier().size() - 1).getText();
        return receiver + "::" + method;
    }

    @Override
    public String visitList_literal(jolkParser.List_literalContext ctx) {
        if (ctx.array_literal() != null) return visit(ctx.array_literal());
        if (ctx.set_literal() != null) return visit(ctx.set_literal());
        if (ctx.map_literal() != null) return visit(ctx.map_literal());
        return "";
    }

    @Override
    public String visitArray_literal(jolkParser.Array_literalContext ctx) {
        StringBuilder sb = new StringBuilder("jolk.lang.Array.of(");
        if (ctx.literal_list() != null) {
            sb.append(visit(ctx.literal_list()));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visitSet_literal(jolkParser.Set_literalContext ctx) {
        StringBuilder sb = new StringBuilder("jolk.lang.Set.of(");
        if (ctx.literal_list() != null) {
            sb.append(visit(ctx.literal_list()));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visitMap_literal(jolkParser.Map_literalContext ctx) {
        StringBuilder sb = new StringBuilder("jolk.lang.Map.of(");
        if (ctx.map_list() != null) {
            sb.append(visit(ctx.map_list()));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String visitLiteral_list(jolkParser.Literal_listContext ctx) {
        return ctx.expression().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitMap_list(jolkParser.Map_listContext ctx) {
        return ctx.map_entry().stream()
                .map(this::visit)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String visitMap_entry(jolkParser.Map_entryContext ctx) {
        // Transpiles 'key -> value' to 'key, value' for the Map constructor
        return visit(ctx.expression(0)) + ", " + visit(ctx.expression(1));
    }

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    protected String aggregateResult(String aggregate, String nextResult) {
        if (aggregate == null)
            return nextResult;
        if (nextResult == null)
            return aggregate;
        return aggregate + nextResult;
    }

    String[] parseModifier(String text) {
        String vis = null;
        String var = null;
        int idx = 1; // skip #
        if (idx < text.length()) {
            char c = text.charAt(idx);
            if (c == '<') { vis = "public"; idx++; }
            else if (c == '~') { vis = "package"; idx++; }
            else if (c == ':') { vis = "protected"; idx++; }
            else if (c == '>') { vis = "private"; idx++; }
        }
        if (idx < text.length()) {
            char c = text.charAt(idx);
            if (c == '?') var = "abstract";
            else if (c == '!') var = "final";
        }
        return new String[]{vis, var};
    }
}