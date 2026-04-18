package tolk.parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkLiteralNode;
import tolk.language.JolkLanguage;

/**
 * # JolkASTDecoder
 *
 * Responsible for the high-speed reification of the Jolk AST from a binary
 * substrate. It bypasses the lexical and syntactic analysis phases to achieve
 * industrial-tier loading performance.
 */
public final class JolkASTDecoder {

    private final JolkLanguage language;

    public JolkASTDecoder(JolkLanguage language) {
        this.language = language;
    }

    /**
     * Reconstructs a JolkNode hierarchy from the binary stream.
     *
     * @param in The binary input stream (typically a .jolc file).
     * @return The reified JolkNode root.
     * @throws IOException If the stream is malformed or inaccessible.
     */
    public JolkNode decode(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        // Validate Magic Number: 0x4A 0x4F 0x4C 0x4B (JOLK)
        if (dataIn.readInt() != 0x4A4F4C4B) {
            throw new IOException("Invalid binary AST: Magic number mismatch.");
        }
        return readNode(dataIn);
    }

    private JolkNode readNode(DataInputStream in) throws IOException {
        byte nodeId = in.readByte();
        return switch (nodeId) {
            case 1 -> {
                byte type = in.readByte();
                yield switch (type) {
                    case 'L' -> new JolkLiteralNode(in.readLong());
                    case 'S' -> new JolkLiteralNode(in.readUTF());
                    default -> throw new IOException("Unknown literal type: " + type);
                };
            }
            // ... recursive expansion for other node types ...
            default -> throw new IOException("Unknown Node ID: " + nodeId);
        };
    }
}
