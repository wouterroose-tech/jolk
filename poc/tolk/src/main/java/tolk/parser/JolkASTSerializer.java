package tolk.parser;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import tolk.nodes.JolkNode;

/**
 * # JolkASTSerializer
 * 
 * Provides a naive, deterministic serialization of the Jolk AST.
 * It traverses the node hierarchy and writes a compact binary 
 * representation to a stream.
 */
public final class JolkASTSerializer {

    /**
     * Serializes the given node and its children to the output stream.
     * 
     * @param node The root of the AST subtree to serialize.
     * @param out The destination stream.
     * @throws IOException If writing fails.
     */
    public void serialize(JolkNode node, OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(out);
        writeNode(node, dataOut);
        dataOut.flush();
    }

    private void writeNode(JolkNode node, DataOutputStream out) throws IOException {
        // Naive ID mapping (This should eventually be moved to a registry)
        if (node instanceof tolk.nodes.JolkLiteralNode) {
            out.writeByte(1); // ID for Literal
            Object val = ((tolk.nodes.JolkLiteralNode) node).executeGeneric(null);
            writeLiteral(val, out);
        } 
        // ... handle other node types ...
        else {
            out.writeByte(0); // Unknown/Generic
        }
    }

    private void writeLiteral(Object val, DataOutputStream out) throws IOException {
        if (val instanceof Long) {
            out.writeByte('L');
            out.writeLong((Long) val);
        } else if (val instanceof String) {
            out.writeByte('S');
            out.writeUTF((String) val);
        }
        // ...
    }
}
