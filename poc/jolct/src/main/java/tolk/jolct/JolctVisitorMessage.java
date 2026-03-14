package tolk.jolct;

import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.ParseTree;
import tolk.grammar.jolkParser;

public class JolctVisitorMessage {

    private static final Set<String> INTRINSIC_SELECTORS = Set.of(
        "#case",
        "#default",
        "#catch",
        "#throw",
        "#ifPresent", 
        "#ifEmpty",
        "#isPresent", 
        "#isEmpty",
        "#instanceOf");

    private static final Set<String> CORE_COLLECTIONS = Set.of(
        "Array",
        "Set",
        "Map",
        "jolk.lang.Array",
        "jolk.lang.Set",
        "jolk.lang.Map"
    );

    final private JolctVisitor visitor;
    private int tempVarCount = 0;

    JolctVisitorMessage(JolctVisitor visitor) {
        this.visitor = visitor;
    }

    private boolean isSystemSelector(String selector) {
        return INTRINSIC_SELECTORS.contains(selector);
    }

    public String visitMessage(jolkParser.MessageContext ctx) {
        int terminalIndex = findSystemPivot(ctx);

        // Resolve the primary subject through standard selectors only
        String subject = visitor.visit(ctx.primary());
        subject = projectMessageChain(ctx, terminalIndex, subject);
        // Deterministic Binary Path Exit
        if (terminalIndex == -1) {
            return subject;
        }
        //  Identify intrinsic selector
        String intrinsicSelector = ctx.selector(terminalIndex).getText();
        if ("#instanceOf".equals(intrinsicSelector)) {
            return projectInstanceOf(subject, ctx, terminalIndex);
        }
        if (isIsPresenceSelector(intrinsicSelector)) {
            return projectPresence(subject, intrinsicSelector);
        }
        if (isIfPresenceSelector(intrinsicSelector)) {
            return projectPresenceBlock(subject, intrinsicSelector, ctx, terminalIndex);
        }
        // TODO CASE
        if ("#catch".equals(intrinsicSelector)) {
            return projectTryCatch(subject, ctx, terminalIndex);
        }
        if ("#throw".equals(intrinsicSelector)) {
            return projecThrow(subject);
        }
        throw new IllegalStateException("incorrect message chain: " + ctx.getText());
    }

    private String projecThrow(String subject) {
        // receiver #throw -> throw receiver
        return "throw " + subject;
    }

    private boolean isIfPresenceSelector(String intrinsicSelector) {
        return intrinsicSelector.equals("#ifPresent") || intrinsicSelector.equals("#ifEmpty");
    }

    private boolean isIsPresenceSelector(String intrinsicselector) {
        return intrinsicselector.equals("#isPresent") || intrinsicselector.equals("#isEmpty");
    }

    private String projectMessageChain(jolkParser.MessageContext ctx, int terminalIndex, String current) {
        int limit = (terminalIndex != -1) ? terminalIndex : ctx.selector().size();
        int selectorCount = 0;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof jolkParser.SelectorContext) {
                if (selectorCount >= limit) {
                    break;
                }
                String selector = visitor.visit(child);
                String args = "()";
                boolean explicitArgs = false;

                jolkParser.PayloadContext payloadCtx = null;
                if (i + 1 < ctx.getChildCount() && ctx.getChild(i + 1) instanceof jolkParser.PayloadContext) {
                    payloadCtx = (jolkParser.PayloadContext) ctx.getChild(i + 1);
                }

                if (payloadCtx != null) {
                    if (payloadCtx.arguments() != null) {
                        jolkParser.ArgumentsContext argumentsCtx = payloadCtx.arguments();
                        args = argumentsCtx.expression().isEmpty()
                                ? "()"
                                : "(" + argumentsCtx.expression().stream().map(visitor::visit).collect(Collectors.joining(", ")) + ")";
                        explicitArgs = true;
                    } else if (payloadCtx.closure() != null) {
                        args = "(" + visitor.visit(payloadCtx.closure()) + ")";
                        explicitArgs = true;
                    }
                }

            if ("new".equals(selector)) {
                if ("super".equals(current) && visitor.inConstructor) {
                    current = "super" + args;
                    if (selectorCount + 1 < limit)
                        current += "; this";
                } else if (CORE_COLLECTIONS.contains(current)) {
                    current = current + ".of" + args;
                } else {
                    current = "new " + current + args;
                }
            } else if ("as".equals(selector)) {
                String type = args.substring(1, args.length() - 1);
                current = "((" + type + ") " + current + ")";
            } else if ("isInstance".equals(selector)) {
                current = current + ".class.isInstance" + args;
            } else {
                if (!explicitArgs && !selector.isEmpty() && Character.isUpperCase(selector.charAt(0))) {
                    args = "";
                }
                current = current + "." + selector + args;
            }
            selectorCount++;
            }
        }
        return current;
    }

    /**
     * Locates the terminal boundary between iterative message dispatches and system directives.
     * * <p>This method identifies the pivot point where a value-yielding chain terminates 
     * to initiate a structural projection (e.g., {@code #case}, {@code #catch}, or {@code #ifPresent}). 
     * It ensures the subject is fully resolved before the control-flow transformation occurs.</p>
     * * @param ctx The message context containing the primary subject and selector sequence.
     * @return The index of the first system selector, or -1 if the chain is a standard expression.
     */
    private int findSystemPivot(jolkParser.MessageContext ctx) {
        for (int i = 0; i < ctx.selector().size(); i++) {
            String selectorText = ctx.selector(i).getText();
            // Identification of Structural or Identity Pivots
            if (isSystemSelector(selectorText)) {
                return i;
            }
        }
        // No pivot detected; message is a simple iterative expression
        return -1;
    }

    private String getIfPresentParamName(jolkParser.ClosureContext blockCtx) {
        if (blockCtx != null && blockCtx.stat_params() != null) {
            if (blockCtx.stat_params().inferred_params() != null && !blockCtx.stat_params().inferred_params().InstanceId().isEmpty()) {
                return blockCtx.stat_params().inferred_params().InstanceId(0).getText();
            } else if (blockCtx.stat_params().typed_params() != null && !blockCtx.stat_params().typed_params().InstanceId().isEmpty()) {
                return blockCtx.stat_params().typed_params().InstanceId(0).getText();
            }
        }
        return null;
    }

    private String projectPresenceBlock(String subject, String selector, jolkParser.MessageContext ctx,
            int selectorIndex) {
        jolkParser.PayloadContext payload = getPayloadForSelector(ctx, selectorIndex);
        jolkParser.ClosureContext blockCtx = (payload != null) ? payload.closure() : null;

        String tempVar = "_subj" + (tempVarCount++);
        String operator = selector.equals("#ifPresent") ? " != null" : " == null";

        StringBuilder sb = new StringBuilder();
        sb.append("final var ").append(tempVar).append(" = ").append(subject).append(";\n");
        sb.append("if (").append(tempVar).append(operator).append(") {\n");

        if (blockCtx != null && "#ifPresent".equals(selector) && blockCtx.stat_params() != null) {
            String paramName = getIfPresentParamName(blockCtx);
            if (paramName != null) {
                sb.append("final var ").append(paramName).append(" = ").append(tempVar).append(";\n");
            }
        }
        if (blockCtx != null) {
            sb.append(visitor.visitClosureBody(blockCtx));
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String projectInstanceOf(String subject, jolkParser.MessageContext ctx, int startIndex) {
        jolkParser.PayloadContext instanceOfPayload = getPayloadForSelector(ctx, startIndex);
        String type = visitor.visit(instanceOfPayload.arguments());

        if (startIndex + 1 < ctx.selector().size()) {
            String nextSelector = ctx.selector(startIndex + 1).getText();
            if ("#ifPresent".equals(nextSelector)) {
                jolkParser.PayloadContext ifPresentPayload = getPayloadForSelector(ctx, startIndex + 1);
                jolkParser.ClosureContext blockCtx = ifPresentPayload.closure();

                StringBuilder sb = new StringBuilder();
                sb.append("if (").append(subject).append(" instanceof ").append(type).append(") {\n");

                if (blockCtx != null) {
                    String paramName = getIfPresentParamName(blockCtx);
                    if (paramName != null) {
                        sb.append("final var ").append(paramName).append(" = (").append(type).append(") ").append(subject).append(";\n");
                    }
                    sb.append(visitor.visitClosureBody(blockCtx));
                }
                sb.append("}");
                return sb.toString();
            } else if ("#ifEmpty".equals(nextSelector)) {
                jolkParser.PayloadContext ifEmptyPayload = getPayloadForSelector(ctx, startIndex + 1);
                jolkParser.ClosureContext blockCtx = ifEmptyPayload.closure();

                StringBuilder sb = new StringBuilder();
                sb.append("if (!(").append(subject).append(" instanceof ").append(type).append(")) {\n");
                if (blockCtx != null) {
                    sb.append(visitor.visitClosureBody(blockCtx));
                }
                sb.append("}");
                return sb.toString();
            } else if ("#isPresent".equals(nextSelector)) {
                return "(" + subject + " instanceof " + type + ")";
            } else if ("#isEmpty".equals(nextSelector)) {
                return "(!(" + subject + " instanceof " + type + "))";
            }
        }
        return "(" + subject + " instanceof " + type + ")";
    }

    private String projectPresence(String subject, String selector) {
        // Direct reification of identity state into Java boolean expressions
        switch (selector) {
            case "#isPresent":
                return "(" + subject + " != null)";
            case "#isEmpty":
                return "(" + subject + " == null)";
            default:
                throw new IllegalStateException("Unexpected identity selector: " + selector);
        }
    }

    private String projectSwitch(String subject, jolkParser.MessageContext ctx, int startIndex) {
        StringBuilder sb = new StringBuilder("switch (" + subject + ") {\n");
        
        for (int i = startIndex; i < ctx.selector().size(); i++) {
            String selector = ctx.selector(i).getText();
            jolkParser.PayloadContext payload = getPayloadForSelector(ctx, i);
            String block = (payload != null) ? visitor.visit(payload) : "";
            if ("#case".equals(selector)) {
                // Extract argument for the case predicate
                String value = (payload != null && payload.arguments() != null) ? visitor.visit(payload.arguments()) : "";
                sb.append("  case ").append(value).append(": ").append(block).append("\n");
            } else if (selector.equals("#default")) {
                sb.append("  default: ").append(block).append("\n");
            }
        }
        return sb.append("}").toString();
    }

    private String projectTryCatch(String subject, jolkParser.MessageContext ctx, int index) {
        String tryBody = subject.trim();
        
        // If the primary is a closure, we use its content directly for the try-body.
        // If it is an expression (e.g. variable), we must execute the closure.
        if (ctx.primary().closure() != null) { // subject is a closure
             if (index == 0) { // is #catch the first selector?
                 tryBody = visitor.visitClosureBody(ctx.primary().closure()).trim();
            }
        } else {
            tryBody += ".apply()";
        }
        // Ensure semicolon termination for the try block content
        if (!tryBody.endsWith(";") && !tryBody.endsWith("}")) {
            tryBody += ";";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("try {\n").append(tryBody).append("\n}");

        for (int i = index; i < ctx.selector().size(); i++) {
            String selector = ctx.selector(i).getText();
            jolkParser.PayloadContext payload = getPayloadForSelector(ctx, i);

            if (payload != null && payload.closure() != null) {
                jolkParser.ClosureContext blockCtx = payload.closure();

                if ("#catch".equals(selector)) {    
                    String params = "Exception e";
                    if (blockCtx.stat_params() != null) {
                        params = visitor.visit(blockCtx.stat_params());
                    }
                    sb.append(" catch (").append(params).append(") {\n");
                    sb.append(visitor.visitClosureBody(blockCtx));
                    sb.append("}");
                } 
                else if ("#finally".equals(selector)) {
                    if (payload.closure() != null) {
                        blockCtx = payload.closure();
                        sb.append(" finally {\n");
                        sb.append(visitor.visitClosureBody(blockCtx));
                        sb.append("}");
                        i++; // Skip the block we just processed
                    }
                }
            }
        }
        return sb.toString();
    }

    private jolkParser.PayloadContext getPayloadForSelector(jolkParser.MessageContext ctx, int selectorIndex) {
        int currentSelectorIndex = 0;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof jolkParser.SelectorContext) {
                if (currentSelectorIndex == selectorIndex) {
                    if (i + 1 < ctx.getChildCount() && ctx.getChild(i + 1) instanceof jolkParser.PayloadContext) {
                        return (jolkParser.PayloadContext) ctx.getChild(i + 1);
                    }
                    return null;
                }
                currentSelectorIndex++;
            }
        }
        return null;
    }
}
