package tolk.jolct;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import tolk.grammar.jolkParser;

public class JolctVisitorExpression {

    final private JolctVisitor visitor;

    JolctVisitorExpression(JolctVisitor visitor) {
        this.visitor = visitor;
    }

    /**
     * Projects Jolk ternary ({@code ?} and {@code ?!}) expressions.
     * <p>The projection strategy is governed by context-dependent reification:</p>
     * <ul>
     * <li><b>Value-Yielding:</b> Assignments and returns are projected as Java ternary expressions. 
     * <b>PoC Limitation:</b> Branch blocks are prohibited; only simple expressions are supported.</li>
     * <li><b>Standalone Statement:</b> Unbound expressions undergo <b>Statement Lifting</b> into 
     * Java {@code if-else} blocks, supporting full block-branch transpilation.</li>
     * </ul>
     * @param ctx The {@code ExpressionContext} originating from the Tolk grammar.
     * @return Java source string as either a terminal expression or a lifted statement block.
     */
    public String visitExpression(jolkParser.ExpressionContext ctx) {

        RuleContext parent = ctx.getParent();
        boolean isReturn = isReturn(parent) ;
        String returnStr = (isReturn && !visitor.inConstructor) ? "return " : "";
        String logic_or = addSelfOnReturn(isReturn, visitor.visit(ctx.logic_or()));
        // Directly check for the returnOp context
        if (ctx.condOp() == null) {
            return returnStr + logic_or;
        }
        // Logical Inversion check
        if ("?!".equals(ctx.condOp().getText())) {
            logic_or = "!(" + logic_or + ")";
        }
        if (isStatement(parent)) {
            // This is an if-statement, not a ternary. Generate the full block.
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(logic_or).append(")");
            
            String trueBody = getBlockBody(ctx.expression(0));
            appendBranch(isReturn, returnStr, trueBody, sb);
            
            if (ctx.expression().size() > 1) {
                sb.append(" else");
                String falseBody = getBlockBody(ctx.expression(1));
                appendBranch(isReturn, returnStr, falseBody, sb);
            }
            return sb.toString();
        }
        String trueExpr = visitor.visit(ctx.expression(0));
        String falseExpr = (ctx.expression().size() > 1) ? visitor.visit(ctx.expression(1)) : null;
        
        // handle Ternary
        return (isReturn ? returnStr : "") + "(" + logic_or + ")"
                + " ? " + trueExpr
                + " : " + (falseExpr != null ? falseExpr : "null");
    }

    private void appendBranch(boolean isReturn, String returnStr, String falseExpr, StringBuilder sb) {
        sb.append(" {\n");
        falseExpr = addSelfOnReturn(isReturn, falseExpr);
        falseExpr = (falseExpr != null && !falseExpr.equals("null")) ? formatStatementBody(falseExpr) : "null;\n";
        sb.append((isReturn ? returnStr : "") + falseExpr);
        sb.append("}");
    }

    private boolean isReturn(RuleContext parent) {
        if (parent instanceof jolkParser.StatementContext) {
            jolkParser.StatementContext ctx = (jolkParser.StatementContext) parent;
            return ctx.returnOp() != null || ctx.getToken(jolkParser.CARET, 0) != null;
        }
        return false;
    }

    private String addSelfOnReturn(Boolean isReturn, String expr) {
        if (isReturn && "Self".equals(visitor.currentMethodReturnType) && "this".equals(expr)) {
            return "(Self) " + expr;
        }
        return expr;
    }

    private String formatStatementBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(body);
        // If it was a simple expression, it needs a semicolon.
        if (!body.trim().endsWith(";") && !body.trim().endsWith("}")) {
            sb.append(";");
        }
        if (!body.endsWith("\n")) {
            sb.append("\n");
        }
        return sb.toString();
    }

    private boolean isStatement(RuleContext ctx) {
        return ctx instanceof jolkParser.StatementContext;
    }

    private String getBlockBody(jolkParser.ExpressionContext ctx) {
        jolkParser.ClosureContext closure = getClosure(ctx);
        if (closure != null) {
            return visitor.visitClosureBody(closure);
        }
        return visitor.visit(ctx);
    }

    private jolkParser.ClosureContext getClosure(ParseTree node) {
        while (true) {
            if (node instanceof jolkParser.ClosureContext) {
                return (jolkParser.ClosureContext) node;
            }
            if (node.getChildCount() == 1) {
                node = node.getChild(0);
                continue;
            }
            if (node instanceof jolkParser.PrimaryContext) {
                jolkParser.PrimaryContext primary = (jolkParser.PrimaryContext) node;
                if (primary.expression() != null) {
                    node = primary.expression();
                    continue;
                }
            }
            break;
        }
        return null;
    }
}
