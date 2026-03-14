package tolk.jolct;

import tolk.grammar.jolkBaseVisitor;
import tolk.grammar.jolkParser;

/**
 * A visitor that determines if a method's terminal statement constitutes a "void path"
 * based on the "Recursive Leaf Exhaustion" principle. Its result determines whether the
 * `JolctVisitor` should inject an implicit `return this;` for methods with a `Self` return type.
 *
 * A `true` result (Void Identified) signals that a path does not end in a guaranteed return.
 * A `false` result (Resolved/Bypass) signals that a path is conclusive.
 *
 * ### Structural Resolution Matrix:
 * - **Return Expression (`^`)**: Resolved (`false`). An explicit return always satisfies the contract.
 * - **Message Expression (`#selector`)**: Bypass (`false`). Message chains are considered inconclusive and handled by the developer. 
 *   This includes terminal selectors like `#throw`.
 * - **Literal / Identifier**: Void (`true`). A naked value does not constitute a method return.
 * - **Binding / State**: Void (`true`). An assignment or declaration is a void path.
 * - **Ternary Expression (`? :`)**: Void (`true`) if *any* branch is a void path. This includes a missing `else` branch. 
 *    The analysis is a recursive OR on its branches.
 *
 * ### Recursive Logic:
 * 1.  **Short-Circuiting**: A root-level return (`^ expression`) is immediately resolved to `false` without inspecting the expression.
 * 2.  **Block Inspection**: To correctly analyze control flow, the visitor recursively inspects the *last statement* of a block. 
 *     A block is not automatically bypassed, as its terminal statement determines its void-ness.
 * 3.  **Conservatism**: Message sends are treated as "handled" states (`false`), assuming developer intent.
 */
public class JolctVoidPathDetector extends jolkBaseVisitor<Boolean> {

    public boolean isVoid(jolkParser.StatementContext ctx) {
        if (ctx == null) {
            return true;
        }
        return visit(ctx);
    }

    @Override
    protected Boolean defaultResult() {
        return true;
    }

    @Override
    protected Boolean aggregateResult(Boolean aggregate, Boolean nextResult) {
        // Used for sequences like `returnOp? expression`. If `returnOp` is present,
        // `aggregate` is false, and `false && anything` is false, achieving short-circuiting.
        return aggregate && nextResult;
    }

    @Override
    public Boolean visitReturnOp(jolkParser.ReturnOpContext ctx) {
        return false;
    }

    @Override
    public Boolean visitStatement(jolkParser.StatementContext ctx) {
        // A statement with an explicit return is never a void path.
        // The check is made more robust by also checking the raw text, in case the
        // parser tree is not constructed as expected for a return statement.
        if (ctx.returnOp() != null || ctx.getToken(jolkParser.CARET, 0) != null || ctx.getText().startsWith("^")) {
            return false;
        }
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitConstant(jolkParser.ConstantContext ctx) {
        return true;
    }

    @Override
    public Boolean visitField(jolkParser.FieldContext ctx) {
        return true;
    }

    @Override
    public Boolean visitBinding(jolkParser.BindingContext ctx) {
        return true;
    }

    @Override
    public Boolean visitExpression(jolkParser.ExpressionContext ctx) {
        if (ctx.condOp() != null) {
            // For a ternary expression, the path is void if EITHER branch is void.
            // A missing `else` branch is an implicit void path.
            if (ctx.expression().size() > 1) {
                // is non-void if and only if both branches are non-void.
                // is void if either branch is void.
                return visit(ctx.expression(0)) || visit(ctx.expression(1));
            } else {
                // if without else is always a void path.
                return true;
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitLogic_and(jolkParser.Logic_andContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitLogic_or(jolkParser.Logic_orContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitCondOp(jolkParser.CondOpContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitEquality(jolkParser.EqualityContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitComparison(jolkParser.ComparisonContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitTerm(jolkParser.TermContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitUnary(jolkParser.UnaryContext ctx) {
        if (ctx.getText().startsWith("^")) {
            return false;
        }
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitFactor(jolkParser.FactorContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitPower(jolkParser.PowerContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Boolean visitMessage(jolkParser.MessageContext ctx) {
        // Check for terminal selectors that interrupt flow
        for (jolkParser.SelectorContext sel : ctx.selector()) {
            if ("#throw".equals(sel.getText())) {
                return false; // It is a terminal exit
            }
        }
        if (!ctx.selector().isEmpty()) {
            return true;
        }
        // If no selector, it's a naked primary (value), so we check the primary.
        return visit(ctx.primary());
    }

    @Override
    public Boolean visitPrimary(jolkParser.PrimaryContext ctx) {
        if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        if (ctx.closure() != null) {
            return visit(ctx.closure());
        }
        // A primary that is a literal, identifier, or reserved word is a naked value
        // and thus constitutes a void path for the enclosing method.
        return true;
    }

    @Override
    public Boolean visitClosure(jolkParser.ClosureContext ctx) {
        if (ctx.statements() == null || ctx.statements().statement().isEmpty()) {
            return true; // An empty block is void.
        }
        // To correctly analyze control flow (e.g., in ternaries), we must inspect the block.
        // The void-ness of the block is determined by its last statement.
        java.util.List<jolkParser.StatementContext> statements = ctx.statements().statement();
        jolkParser.StatementContext lastStatement = statements.get(statements.size() - 1);
        return visit(lastStatement);
    }

    @Override
    public Boolean visitLiteral(jolkParser.LiteralContext ctx) {
        // A literal (numeric, string, or list) is a naked value and thus
        // constitutes a void path.
        return true;
    }

    @Override
    public Boolean visitIdentifier(jolkParser.IdentifierContext ctx) {
        // An identifier used as a statement is a naked value and thus
        // constitutes a void path.
        return true;
    }

    @Override
    public Boolean visitSelf_type(jolkParser.Self_typeContext ctx) {
        // A type reference is a value, not a return path.
        return true;
    }

    @Override
    public Boolean visitSelf_instance(jolkParser.Self_instanceContext ctx) {
        // The 'self' instance is a value, not a return path.
        return true;
    }

    @Override
    public Boolean visitSelector(jolkParser.SelectorContext ctx) {
        // A selector is part of a message send, which is handled by visitMessage.
        // This should not be visited on its own as a statement.
        return false;
    }
}
