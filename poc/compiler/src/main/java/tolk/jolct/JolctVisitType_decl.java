package tolk.jolct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tolk.grammar.jolkParser;

public class JolctVisitType_decl {

    private final JolctVisitor visitor;

    JolctVisitType_decl(JolctVisitor visitor) {
        this.visitor = visitor;
    }

    String visitType_decl(jolkParser.Type_declContext ctx) {
        String typeName = ctx.type_bound().type().MetaId().getText();
        visitor.currentClass = typeName;

        return "";
    }

    void visitEnum(jolkParser.Type_declContext ctx, String visibility, String typeName, StringBuilder sb) {
        visitor.currentClassHasGenerics = false;
        sb.append(visibility).append("enum ").append(typeName).append(" {\n");
        
        // Handle Enum Constants
        List<String> constants = ctx.type_mbr().stream()
            .filter(m -> m.enum_constant() != null)
            .map(m -> visitor.visit(m.enum_constant()))
            .collect(Collectors.toList());
        
        if (!constants.isEmpty()) {
            sb.append(String.join(", ", constants)).append(";\n");
        } else {
            sb.append(";\n");
        }

        for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
            if (m.member() != null) {
                sb.append(visitor.visit(m));
            }
        }
    }
    

    void visitRecord(jolkParser.Type_declContext ctx, String visibility, String typeName, StringBuilder sb) {
        visitor.currentClassHasGenerics = (ctx.type_bound().type().type_args() != null);
        sb.append(visibility).append("record ").append(typeName);
        
        if (ctx.type_bound().type().type_args() != null) {
             sb.append("<").append(visitor.visit(ctx.type_bound().type().type_args())).append(">");
        }

        boolean oldSuppress = visitor.suppressWildcard;
        visitor.suppressWildcard = true;
        List<String> components = new ArrayList<>();
        try {
            for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
                if (m.member() != null && m.member().state() != null && m.member().state().field() != null) {
                     jolkParser.FieldContext f = m.member().state().field();
                     String fType = visitor.visit(f.type());
                     String fName = f.identifier().getText();
                     components.add(fType + " " + fName);
                }
            }
        } finally {
            visitor.suppressWildcard = oldSuppress;
        }
        
        sb.append(" (").append(String.join(", ", components)).append(") {\n");

        for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
            boolean isField = (m.member() != null && m.member().state() != null && m.member().state().field() != null);
            if (!isField) {
                sb.append(visitor.visit(m));
            }
        }
    }

    void visitProtocol(jolkParser.Type_declContext ctx, String visibility, String typeName, StringBuilder sb) {
        visitor.currentClassHasGenerics = true;
        StringBuilder typeParams = new StringBuilder();

        typeParams.append("<");
        StringBuilder selfParams = new StringBuilder();

        if (ctx.type_bound().type().type_args() != null) {
            for (jolkParser.Type_boundContext tbc : ctx.type_bound().type().type_args().type_bound()) {
                String param = tbc.type().MetaId().getText();
                typeParams.append(param).append(" extends jolk.lang.Object<").append(param).append(">").append(", ");
                selfParams.append(param).append(", ");
            }
        }
        typeParams.append("Self extends ").append(typeName).append("<").append(selfParams).append("Self>>");

        sb.append(visibility).append("interface ").append(typeName).append(typeParams);
        sb.append(" {\n");

        for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
            if (m.member() != null) {
                sb.append(visitor.visit(m));
            }
        }
    }

    void visitClass(jolkParser.Type_declContext ctx, String variability, String visibility,
            String typeName, StringBuilder sb) {
        boolean isFinal = "final ".equals(variability);
        
        // Determine superclass and if it is Jolk
        String superType = null;
        boolean isJolkSuper = true;
        
        jolkParser.Type_contractsContext contracts = ctx.type_bound().type_contracts();
        if (contracts != null && contracts.getChildCount() > 0 && "extends".equals(contracts.getChild(0).getText())) {
            superType = visitor.visit(contracts.type(0));
            String rawSuperType = superType;
            int angleIndex = rawSuperType.indexOf('<');
            if (angleIndex > 0) {
                rawSuperType = rawSuperType.substring(0, angleIndex);
            }
            isJolkSuper = visitor.context.isJolkType(rawSuperType, visitor.currentPackage, visitor.currentImports);
        }
        
        boolean isJavaExt = (superType != null && !isJolkSuper);

        boolean hasGenerics = (!isFinal && !isJavaExt) || (ctx.type_bound().type().type_args() != null);
        visitor.currentClassHasGenerics = hasGenerics;

        StringBuilder typeParams = new StringBuilder();
        String selfType = (isFinal || isJavaExt) ? typeName : "Self";

        if (!isFinal && !isJavaExt) {
            typeParams.append("<");
            StringBuilder selfParams = new StringBuilder();

            if (ctx.type_bound().type().type_args() != null) {
                for (jolkParser.Type_boundContext tbc : ctx.type_bound().type().type_args().type_bound()) {
                    String param = tbc.type().MetaId().getText();
                    typeParams.append(param).append(" extends jolk.lang.Object<").append(param).append(">").append(", ");
                    selfParams.append(param).append(", ");
                }
            }
            typeParams.append("Self extends ").append(typeName).append("<").append(selfParams).append("Self>>");
        } else if (ctx.type_bound().type().type_args() != null) {
             typeParams.append("<");
             List<String> params = new ArrayList<>();
             List<jolkParser.Type_boundContext> bounds = ctx.type_bound().type().type_args().type_bound();
             for (int i = 0; i < bounds.size(); i++) {
                 String param = bounds.get(i).type().MetaId().getText();
                 typeParams.append(param).append(" extends jolk.lang.Object<").append(param).append(">");
                 params.add(param);
                 if (i < bounds.size() - 1) typeParams.append(", ");
             }
             typeParams.append(">");

             if (isFinal || isJavaExt) {
                 selfType += "<" + String.join(", ", params) + ">";
             }
        }

        String extendsClause = "extends jolk.lang.Object<" + selfType + ">";
        
        if (superType != null) {
            if (isJolkSuper) {
                if (superType.endsWith("<?>")) {
                    extendsClause = "extends " + superType.substring(0, superType.length() - 3) + "<" + selfType + ">";
                } else if (superType.endsWith(", ?>")) {
                    extendsClause = "extends " + superType.substring(0, superType.length() - 4) + ", " + selfType + ">";
                } else if (superType.endsWith(">")) {
                    extendsClause = "extends " + superType.substring(0, superType.length() - 1) + ", " + selfType + ">";
                } else {
                    extendsClause = "extends " + superType + "<" + selfType + ">";
                }
            } else {
                extendsClause = "extends " + superType;
            }
        }

        if ("jolk.lang".equals(visitor.currentPackage) && "Object".equals(typeName)) {
            extendsClause = "";
        }

        List<String> interfaces = new ArrayList<>();
        contracts = ctx.type_bound().type_contracts();
        if (contracts != null) {
            int typeIdx = 0;
            for (int i = 0; i < contracts.getChildCount(); i++) {
                String token = contracts.getChild(i).getText();
                if ("extends".equals(token)) {
                    typeIdx++;
                } else if ("implements".equals(token)) {
                    while (typeIdx < contracts.type().size()) {
                        String ifaceName = visitor.visit(contracts.type(typeIdx));
                        String rawName = ifaceName.contains("<") ? ifaceName.substring(0, ifaceName.indexOf('<')) : ifaceName;
                        
                        if (visitor.context.isJolkType(rawName, visitor.currentPackage, visitor.currentImports)) {
                            if (ifaceName.endsWith(">")) {
                                ifaceName = ifaceName.substring(0, ifaceName.length() - 1) + ", " + selfType + ">";
                            } else {
                                ifaceName = ifaceName + "<" + selfType + ">";
                            }
                        }
                        interfaces.add(ifaceName);
                        typeIdx++;
                    }
                }
            }
        }

        sb.append(visibility).append(variability).append("class ").append(typeName).append(typeParams);
        if (!extendsClause.isEmpty()) {
            sb.append(" ").append(extendsClause);
        }
        if (!interfaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", interfaces));
        }
        sb.append(" {\n");
        
        // Collect defined method signatures to avoid duplicates
        Set<String> definedSignatures = new HashSet<>();
        for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
            if (m.member() != null && m.member().method() != null) {
                String name = m.member().method().selector_id().getText();
                int arity = 0;
                if (m.member().method().typed_params() != null) {
                    jolkParser.Typed_paramsContext tpc = m.member().method().typed_params();
                    arity = tpc.annotated_type().size();
                    if (tpc.vararg_id() != null && arity == 0) arity = 1;
                }
                definedSignatures.add(name + ":" + arity);
            }
        }
        

        // Handle Members
        for (jolkParser.Type_mbrContext m : ctx.type_mbr()) {
            if (m.member() != null) {
                sb.append(visitor.visit(m));
                
                // Generate default accessors for fields/constants
                {
                    String vMod = "private ";
                    String memberVisibility = null;
                    if (m.member().vis_mod() != null) {
                        if (m.member().vis_mod().visibility() != null) {
                            memberVisibility = m.member().vis_mod().visibility().getText();
                        } else if (m.member().vis_mod().MODIFIER() != null) {
                            String[] mods = visitor.parseModifier(m.member().vis_mod().MODIFIER().getText());
                            memberVisibility = mods[0];
                        }
                    }
                    if (memberVisibility != null) {
                        vMod = "package".equals(memberVisibility) ? "" : memberVisibility + " ";
                    }
                    boolean isMeta = m.member().META() != null;
                    String staticMod = isMeta ? "static " : "";
                    
                    if (m.member().state() != null && m.member().state().field() != null) {
                        jolkParser.FieldContext f = m.member().state().field();
                        String name = f.identifier().getText();
                        String type = visitor.visit(f.type());
                        
                        // Getter
                        if (!definedSignatures.contains(name + ":0")) {
                            sb.append(vMod).append(staticMod).append(type).append(" ").append(name).append("() {\n")
                              .append("return ").append(name).append(";\n")
                              .append("}\n");
                        }
                        
                        // Setter (Fluent)
                        if (!definedSignatures.contains(name + ":1")) {
                            if (isMeta) {
                                sb.append(vMod).append(staticMod).append("void ").append(name).append("(").append(type).append(" ").append(name).append(") {\n")
                                  .append(visitor.currentClass).append(".").append(name).append(" = ").append(name).append(";\n")
                                  .append("}\n");
                            } else {
                                sb.append(vMod).append(selfType).append(" ").append(name).append("(").append(type).append(" ").append(name).append(") {\n")
                                  .append("this.").append(name).append(" = ").append(name).append(";\n")
                                  .append("return (").append(selfType).append(") this;\n")
                                  .append("}\n");
                            }
                        }
                    } else if (m.member().state() != null && m.member().state().constant() != null) {
                        jolkParser.ConstantContext s = m.member().state().constant();
                        String name = s.binding().identifier().getText();
                        String type = visitor.visit(s.type());
                        if (!definedSignatures.contains(name + ":0")) {
                            sb.append(vMod).append(staticMod).append(type).append(" ").append(name).append("() {\n")
                              .append("return ").append(name).append(";\n")
                              .append("}\n");
                        }
                    }
                }
            }
        }
    }
}