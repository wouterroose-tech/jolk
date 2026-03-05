package tolk.jolct;

import tolk.grammar.jolkBaseVisitor;
import tolk.grammar.jolkParser;

public class JolkSymbolVisitor extends jolkBaseVisitor<Void> {
    private final JolkContext context;
    private String currentPackage = "";

    public JolkSymbolVisitor(JolkContext context) {
        this.context = context;
    }

    @Override
    public Void visitPackage_decl(jolkParser.Package_declContext ctx) {
        currentPackage = ctx.namespace().getText();
        return null;
    }

    @Override
    public Void visitType_decl(jolkParser.Type_declContext ctx) {
        String name = ctx.type_bound().type().MetaId().getText();
        String fqn = currentPackage.isEmpty() ? name : currentPackage + "." + name;
        String archetype = ctx.archetype().getText();
        String variability = ctx.variability() != null ? ctx.variability().getText() : "";
        boolean isProtocol = "protocol".equals(archetype);
        boolean isNonFinalClass = "class".equals(archetype) && !"final".equals(variability);

        if (isNonFinalClass) {
             context.addJolkClass(fqn);
        } else {
             context.addJolkType(fqn);
             if (isProtocol) {
                 context.addCrtpType(fqn);
             }
        }
        return null;
    }
}