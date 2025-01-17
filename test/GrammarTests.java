import norswap.autumn.AutumnTestFixture;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.*;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static norswap.sigh.ast.BinaryOperator.*;

public class GrammarTests extends AutumnTestFixture {
    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final Class<?> grammarClass = grammar.getClass();

    // ---------------------------------------------------------------------------------------------

    private static IntLiteralNode intlit (long i) {
        return new IntLiteralNode(null, i);
    }

    private static FloatLiteralNode floatlit (double d) {
        return new FloatLiteralNode(null, d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLiteralsAndUnary () {
        rule = grammar.expression;

        successExpect("42", intlit(42));
        successExpect("42.0", floatlit(42d));
        successExpect("\"hello\"", new StringLiteralNode(null, "hello"));
        successExpect("(42)", new ParenthesizedNode(null, intlit(42)));
        successExpect("[1, 2, 3]", new ArrayLiteralNode(null, asList(intlit(1), intlit(2), intlit(3))));
        successExpect("true", new ReferenceNode(null, "true"));
        successExpect("false", new ReferenceNode(null, "false"));
        successExpect("null", new ReferenceNode(null, "null"));
        successExpect("!false", new UnaryExpressionNode(null, UnaryOperator.NOT, new ReferenceNode(null, "false")));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testNumericBinary () {
        rule = grammar.expression;

        successExpect("1 + 2", new BinaryExpressionNode(null, intlit(1), ADD, intlit(2)));
        successExpect("2 - 1", new BinaryExpressionNode(null, intlit(2), SUBTRACT,  intlit(1)));
        successExpect("2 * 3", new BinaryExpressionNode(null, intlit(2), MULTIPLY, intlit(3)));
        successExpect("2 / 3", new BinaryExpressionNode(null, intlit(2), DIVIDE, intlit(3)));
        successExpect("2 % 3", new BinaryExpressionNode(null, intlit(2), REMAINDER, intlit(3)));

        successExpect("1.0 + 2.0", new BinaryExpressionNode(null, floatlit(1), ADD, floatlit(2)));
        successExpect("2.0 - 1.0", new BinaryExpressionNode(null, floatlit(2), SUBTRACT, floatlit(1)));
        successExpect("2.0 * 3.0", new BinaryExpressionNode(null, floatlit(2), MULTIPLY, floatlit(3)));
        successExpect("2.0 / 3.0", new BinaryExpressionNode(null, floatlit(2), DIVIDE, floatlit(3)));
        successExpect("2.0 % 3.0", new BinaryExpressionNode(null, floatlit(2), REMAINDER, floatlit(3)));

        successExpect("2 * (4-1) * 4.0 / 6 % (2+1)", new BinaryExpressionNode(null,
            new BinaryExpressionNode(null,
                new BinaryExpressionNode(null,
                    new BinaryExpressionNode(null,
                        intlit(2),
                        MULTIPLY,
                        new ParenthesizedNode(null, new BinaryExpressionNode(null,
                            intlit(4),
                            SUBTRACT,
                            intlit(1)))),
                    MULTIPLY,
                    floatlit(4d)),
                DIVIDE,
                intlit(6)),
            REMAINDER,
            new ParenthesizedNode(null, new BinaryExpressionNode(null,
                intlit(2),
                ADD,
                intlit(1)))));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testArrayStructAccess () {
        rule = grammar.expression;
        successExpect("[1][0]", new ArrayAccessNode(null,
            new ArrayLiteralNode(null, asList(intlit(1))), intlit(0)));
        successExpect("[1].length", new FieldAccessNode(null,
            new ArrayLiteralNode(null, asList(intlit(1))), "length"));
        successExpect("p.x", new FieldAccessNode(null, new ReferenceNode(null, "p"), "x"));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testDeclarations() {
        rule = grammar.statement;

        successExpect("var x: Int = 1", new VarDeclarationNode(null,
            "x", new SimpleTypeNode(null, "Int"), intlit(1)));

        successExpect("struct P {}", new StructDeclarationNode(null, "P", asList()));

        successExpect("struct P { var x: Int; var y: Int }",
            new StructDeclarationNode(null, "P", asList(
                new FieldDeclarationNode(null, "x", new SimpleTypeNode(null, "Int")),
                new FieldDeclarationNode(null, "y", new SimpleTypeNode(null, "Int")))));

        successExpect("fun f (x: Int): Int { return 1 }",
            new FunDeclarationNode(null, "f",
                asList(new ParameterNode(null, "x", new SimpleTypeNode(null, "Int"))),
                new SimpleTypeNode(null, "Int"),
                new BlockNode(null, asList(new ReturnNode(null, intlit(1))))));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testStatements() {
        rule = grammar.statement;

        successExpect("return", new ReturnNode(null, null));
        successExpect("return 1", new ReturnNode(null, intlit(1)));
        successExpect("print(1)", new ExpressionStatementNode(null,
            new FunCallNode(null, new ReferenceNode(null, "print"), asList(intlit(1)))));
        successExpect("{ return }", new BlockNode(null, asList(new ReturnNode(null, null))));


        successExpect("if true return 1 else return 2", new IfNode(null, new ReferenceNode(null, "true"),
            new ReturnNode(null, intlit(1)),
            new ReturnNode(null, intlit(2))));

        successExpect("if false return 1 else if true return 2 else return 3 ",
            new IfNode(null, new ReferenceNode(null, "false"),
                new ReturnNode(null, intlit(1)),
                new IfNode(null, new ReferenceNode(null, "true"),
                    new ReturnNode(null, intlit(2)),
                    new ReturnNode(null, intlit(3)))));

        successExpect("while 1 < 2 { return } ", new WhileNode(null,
            new BinaryExpressionNode(null, intlit(1), LOWER, intlit(2)),
            new BlockNode(null, asList(new ReturnNode(null, null)))));
    }

    // ---------------------------------------------------------------------------------------------

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                             *
     *                                 TESTS DONE BY GROUP 10                                      *                                                             *
     *                                                                                             *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /*
    * Simple test to shox that we can declare boxes just like we do with the structs
    * */
    @Test public void testMultipleStruct() {
        rule = grammar.root;

        String input = "struct Pair { }";
        successExpect(input, new RootNode(null, asList(
            new StructDeclarationNode(null, "Pair", asList())
        )));

        input = "struct P1 { } struct P2 { }";
        successExpect(input, new RootNode(null,
            asList(
                new StructDeclarationNode(null, "P1", asList()),
                new StructDeclarationNode(null, "P2", asList())
            )
        ));

        input = "box Car { }";
        successExpect(input, new RootNode(null, asList(
            new BoxDeclarationNode(null, "Car", asList())
        )));

        // So this does not too, but no worry it is the case only in the grammar tests
        input = "box B1 { } box B2 {  }";
        successExpect(input, new RootNode(null,
            asList(
                new BoxDeclarationNode(null, "B1", asList()),
                new BoxDeclarationNode(null, "B2", asList())
            )
        ));
    }

    /*
    * Check that we can resolve an attribute declaration inside a method of the box
    * */
    @Test public void testAttributesInMethod() {
        rule = grammar.statement;

        String input = "" +
            "box Car {\n" +
            "   attr nWheels: Int\n" +
            "   meth get_nWheels(): Int {\n" +
            "       return nWheels\n" +
            "   }\n" +
            "}\n";

        successExpect(input,
            new BoxDeclarationNode(null, "Car", asList(
                new AttributeDeclarationNode(null, "nWheels", new SimpleTypeNode(null, "Int")),
                new MethodDeclarationNode(null, "get_nWheels", asList(), new SimpleTypeNode(null, "Int"),
                    new BlockNode(null, asList(new ReturnNode(null, new ReferenceNode(null, "nWheels")))))
            )));
    }

    /*
     * Show that we can state the elements of a box in the order we want,
     * and we can mix attributes and methods together
     * */
    @Test public void testBoxShuffleElements() {
        rule = grammar.statement;

        String input = "" +
            "box Shuffle {" +
            "   meth a() {  }" +
            "   attr x: Int" +
            "   attr y: String" +
            "   meth b(): Int { return x }" +
            "}";

        successExpect(input, new BoxDeclarationNode(null, "Shuffle", asList(
            new MethodDeclarationNode(null, "a", asList(), new SimpleTypeNode(null, "Void"),
                new BlockNode(null, asList())),
            new AttributeDeclarationNode(null, "x", new SimpleTypeNode(null, "Int")),
            new AttributeDeclarationNode(null, "y", new SimpleTypeNode(null, "String")),
            new MethodDeclarationNode(null, "b", asList(), new SimpleTypeNode(null, "Int"),
                new BlockNode(null, asList(new ReturnNode(null, new ReferenceNode(null, "x")))))
        )));
    }

    /*
     * Show that we can use other boxes types inside our box and return and use its elements
     * */
    @Test public void testBoxForeignBoxes() {
        rule = grammar.statement;

        String input = "" +
            "box Mixed {" +
            "   attr foreignBox: ForeignBox" +
            "   meth getForeignBox(): ForeignBox { return foreignBox }" +
            "   meth getForeignBoxAttr(): Int { return foreignBox#attribute }" +
            "   meth useForeignBoxMeth() { foreignBox#method() }" +
            "}";

        successExpect(input, new BoxDeclarationNode(null, "Mixed", asList(
            new AttributeDeclarationNode(null, "foreignBox", new SimpleTypeNode(null, "ForeignBox")),
            new MethodDeclarationNode(null, "getForeignBox", asList(), new SimpleTypeNode(null, "ForeignBox"),
                new BlockNode(null, asList(new ReturnNode(null, new ReferenceNode(null, "foreignBox"))))),
            new MethodDeclarationNode(null, "getForeignBoxAttr", asList(), new SimpleTypeNode(null, "Int"),
                new BlockNode(null, asList(new ReturnNode(null,
                    new BoxElementAccessNode(null, new ReferenceNode(null, "foreignBox"), "attribute"))))),
            new MethodDeclarationNode(null, "useForeignBoxMeth", asList(), new SimpleTypeNode(null, "Void"),
                new BlockNode(null, asList(
                    new ExpressionStatementNode(null, new FunCallNode(null, new BoxElementAccessNode(null,
                        new ReferenceNode(null, "foreignBox"), "method"), asList())
                    ))))
        )));
    }

    /*
     * A bigger example that shows a more concrete declaration of a box
     * It uses basic attributes, arrays, other boxes
     * Access other boxes attributes and methods
     * */
    @Test public void testBox() {
        rule = grammar.statement;

        String input1 = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   attr arr: Int[]\n" +
            "   attr wheels: Wheel\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "   meth get_wheels_size(): Int {\n" +
            "       return wheels#size\n" +
            "   }\n" +
            "   meth set_max_speed(speed: Int) {\n" +
            "       max_speed = speed\n" +
            "   }\n" +
            "   meth get_wheels_type(): Int {\n" +
            "       return wheels#get_type()\n" +
            "   }\n" +
            "}\n";

        successExpect(input1,
            new BoxDeclarationNode(null, "Car",
                asList(new AttributeDeclarationNode(null, "max_speed", new SimpleTypeNode(null, "Int")),
                    new AttributeDeclarationNode(null, "arr", new ArrayTypeNode(null, new SimpleTypeNode(null, "Int"))),
                    new AttributeDeclarationNode(null, "wheels", new SimpleTypeNode(null, "Wheel")),
                    new MethodDeclarationNode(null, "get_max_speed", asList(), new SimpleTypeNode(null, "Int"),
                        new BlockNode(null, asList(new ReturnNode(null, new ReferenceNode(null, "max_speed"))))),
                    new MethodDeclarationNode(null, "get_wheels_size", asList(), new SimpleTypeNode(null, "Int"),
                        new BlockNode(null, asList(new ReturnNode(null, new BoxElementAccessNode(null, new ReferenceNode(null, "wheels"), "size"))))),
                    new MethodDeclarationNode(null, "set_max_speed",
                        asList(new ParameterNode(null, "speed", new SimpleTypeNode(null, "Int"))),
                        new SimpleTypeNode(null, "Void"),
                        new BlockNode(null, asList(new ExpressionStatementNode(null,
                            new AssignmentNode(null, new ReferenceNode(null, "max_speed"),
                                new ReferenceNode(null, "speed")))))),
                    new MethodDeclarationNode(null, "get_wheels_type", asList(), new SimpleTypeNode(null, "Int"),
                        new BlockNode(null, asList(new ReturnNode(null, new FunCallNode(null,
                            new BoxElementAccessNode(null, new ReferenceNode(null, "wheels"), "get_type"), asList())))))))
        );
    }

}
