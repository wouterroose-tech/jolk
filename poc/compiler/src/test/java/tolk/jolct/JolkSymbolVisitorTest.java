package tolk.jolct;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import tolk.grammar.jolkLexer;
import tolk.grammar.jolkParser;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JolkSymbolVisitorTest {

    @Test
    void testVisitPackage() {
        JolkContext context = new JolkContext();
        JolkSymbolVisitor visitor = new JolkSymbolVisitor(context);

        // 1. Visit Package
        String pkgSource = "package com.test;";
        jolkLexer pkgLexer = new jolkLexer(CharStreams.fromString(pkgSource));
        jolkParser pkgParser = new jolkParser(new CommonTokenStream(pkgLexer));
        visitor.visitPackage_decl(pkgParser.package_decl());

        // 2. Visit Type manually to verify the package state was preserved in the visitor
        String typeSource = "class MyClass {}";
        jolkLexer typeLexer = new jolkLexer(CharStreams.fromString(typeSource));
        jolkParser typeParser = new jolkParser(new CommonTokenStream(typeLexer));
        visitor.visitType_decl(typeParser.type_decl());

        // Verify package is set by checking if the type is registered with the FQN
        assertTrue(context.isJolkType("com.test.MyClass", "", Collections.emptyList()));
    }

    @Test
    void testVisitTypeDecl() {
        JolkContext context = new JolkContext();
        JolkSymbolVisitor visitor = new JolkSymbolVisitor(context);

        String source = "package com.test; class MyClass {}";
        jolkLexer lexer = new jolkLexer(CharStreams.fromString(source));
        jolkParser parser = new jolkParser(new CommonTokenStream(lexer));

        // Visit the full unit so it handles package and type in order
        visitor.visit(parser.unit());

        // Verify
        assertTrue(context.isJolkType("com.test.MyClass", "", Collections.emptyList()));
    }
}