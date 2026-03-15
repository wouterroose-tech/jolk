package tolk.parser;

import tolk.grammar.jolkBaseVisitor;
import tolk.grammar.jolkParser;
import tolk.nodes.JolkClassDefinitionNode;
import tolk.nodes.JolkEmptyNode;
import tolk.nodes.JolkNode;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;

/// Visitor that traverses the ANTLR4 parse tree and produces the Truffle AST.
public class JolkVisitor extends jolkBaseVisitor<JolkNode> {

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
        // We check for the token type directly rather than comparing strings for
        // efficiency and robustness.
        if (ctx.archetype() == null || ctx.archetype().getStart().getType() != jolkParser.CLASS) {
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
                return new JolkClassDefinitionNode(className, finality, visibility);
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
}