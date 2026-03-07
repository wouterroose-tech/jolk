
package tolk.jolct;

import java.util.List;
import tolk.grammar.jolkParser.*;

public class JolctVisitMethod {
    

    final private JolctVisitor visitor;

    JolctVisitMethod(JolctVisitor visitor) {
        this.visitor = visitor;
    }

    String visitMethod(MethodContext ctx) {
        StringBuilder sb = new StringBuilder();
    
        if (ctx.type_args() != null) {
            appendJolkObject(ctx, sb);
        }
    
        String name = ctx.selector_id().getText();
        boolean isEqualsOverride = "~~".equals(name);
        boolean originalInEqualsOverride = visitor.inEqualsOverride;
        visitor.inEqualsOverride = isEqualsOverride;

        try {
            boolean isMeta = isMeta(ctx);
            boolean isConstructor = isMeta && "new".equals(name);
        
            String returnType = "";
            if (!isConstructor) {
                if (isEqualsOverride) {
                    returnType = "boolean";
                } else {
                    returnType = getReturnType(ctx);
                }
                sb.append(returnType).append(" ");
                if (isEqualsOverride) {
                    name = "equals";
                } else if ("new".equals(name))
                    name = "new_" + visitor.visit(ctx.type());
                sb.append(name);
            } else {
                sb.append(visitor.currentClass);
            }
        
            sb.append("(");
            if (ctx.typed_params() != null) {
                sb.append(visitor.visit(ctx.typed_params()));
            }
            sb.append(")");
        
            if (ctx.block() != null) {
                appendBlock(ctx, sb, isConstructor, returnType);
            } else {
                sb.append(";\n");
            }
        } finally {
            visitor.inEqualsOverride = originalInEqualsOverride;
        }
        return sb.toString();
    }

    private void appendBlock(MethodContext ctx, StringBuilder sb, boolean isConstructor, String returnType) {
        sb.append(" {\n");
        String previousReturnType = visitor.currentMethodReturnType;
        visitor.currentMethodReturnType = returnType;
        if (isConstructor)
            visitor.inConstructor = true;
        String body = "";
        if (ctx.block().statements() != null) {
            body = visitor.visit(ctx.block().statements());
        }
        if (isConstructor)
            visitor.inConstructor = false;
        visitor.currentMethodReturnType = previousReturnType;
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }
        boolean explicitReturn = isLastStatementVoid(ctx.block());
        // If the method has an explicit return type and the last statement is void,
        // add a return statement. If not, it is not conclusive and will generate
        // a compile error forcing the developer to add an explicit return.
        if (explicitReturn && !isConstructor) {
            if ("Self".equals(ctx.type().getText())) {
                Type_declContext typeDecl = (Type_declContext) ctx.getParent().getParent()
                        .getParent();
                boolean isFinal = typeDecl.variability() != null
                        && "final".equals(typeDecl.variability().getText());
   
                sb.append("return ");
                if (isFinal) {
                    sb.append("this;\n");
                } else {
                    sb.append("(Self) this;\n");
                }
            } else {
                sb.append("return null;\n");
            }
        }
        sb.append("}\n");
    }

    private String getReturnType(MethodContext ctx) {
        String returnType = visitor.visit(ctx.type());
        if ("Self".equals(returnType)) {
            if (ctx.getParent().getParent().getParent() instanceof Type_declContext) {
                Type_declContext typeDecl = (Type_declContext) ctx.getParent().getParent()
                        .getParent();
                if (typeDecl.variability() != null && "final".equals(typeDecl.variability().getText())) {
                    String typeName = typeDecl.type_bound().type().MetaId().getText();
                    if (typeDecl.type_bound().type().type_args() != null) {
                        typeName += "<" + visitor.visit(typeDecl.type_bound().type().type_args()) + ">";
                    }
                    returnType = typeName;
                }
            }
        }
        return returnType;
    }

    private boolean isMeta(MethodContext ctx) {
        boolean isMeta = false;
        if (ctx.getParent() instanceof MemberContext) {
            MemberContext mbr = (MemberContext) ctx.getParent();
            isMeta = mbr.META() != null;
        }
        return isMeta;
    }

    private void appendJolkObject(MethodContext ctx, StringBuilder sb) {
        sb.append("<");
        List<Type_boundContext> bounds = ctx.type_args().type_bound();
        for (int i = 0; i < bounds.size(); i++) {
            String param = bounds.get(i).type().MetaId().getText();
            sb.append(param).append(" extends jolk.lang.Object<").append(param).append(">");
            if (i < bounds.size() - 1)
                sb.append(", ");
        }
        sb.append("> ");
    }

    boolean isLastStatementVoid(BlockContext ctx) {
        if (ctx == null || ctx.statements() == null || ctx.statements().statement().isEmpty()) {
            return true;
        }
        List<StatementContext> statements = ctx.statements().statement();
        StatementContext last = statements.get(statements.size() - 1);
        return visitor.voidPathDetector.isVoid(last);
    }
}
