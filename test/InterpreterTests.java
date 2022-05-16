import norswap.autumn.AutumnTestFixture;
import norswap.autumn.Grammar;
import norswap.autumn.Grammar.rule;
import norswap.autumn.ParseResult;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.AttributeDeclarationNode;
import norswap.sigh.ast.BoxDeclarationNode;
import norswap.sigh.ast.SighNode;
import norswap.sigh.ast.SimpleTypeNode;
import norswap.sigh.interpreter.Interpreter;
import norswap.sigh.interpreter.InterpreterException;
import norswap.sigh.interpreter.Null;
import norswap.sigh.types.BoxType;
import norswap.uranium.Reactor;
import norswap.uranium.SemanticError;
import norswap.utils.IO;
import norswap.utils.TestFixture;
import norswap.utils.data.wrappers.Pair;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Set;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public final class InterpreterTests extends TestFixture {

    // TODO peeling

    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    // ---------------------------------------------------------------------------------------------

    private Grammar.rule rule;

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, null);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn, String expectedOutput) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (rule rule, String input, Object expectedReturn, String expectedOutput) {
        // TODO
        // (1) write proper parsing tests
        // (2) write some kind of automated runner, and use it here

        autumnFixture.rule = rule;
        ParseResult parseResult = autumnFixture.success(input);
        SighNode root = parseResult.topValue();

        Reactor reactor = new Reactor();
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        Interpreter interpreter = new Interpreter(reactor);
        walker.walk(root);
        reactor.run();
        Set<SemanticError> errors = reactor.errors();

        if (!errors.isEmpty()) {
            LineMapString map = new LineMapString("<test>", input);
            String report = reactor.reportErrors(it ->
                it.toString() + " (" + ((SighNode) it).span.startString(map) + ")");
            //            String tree = AttributeTreeFormatter.format(root, reactor,
            //                    new ReflectiveFieldWalker<>(SighNode.class, PRE_VISIT, POST_VISIT));
            //            System.err.println(tree);
            throw new AssertionError(report);
        }

        Pair<String, Object> result = IO.captureStdout(() -> interpreter.interpret(root));
        assertEquals(result.b, expectedReturn);
        if (expectedOutput != null) assertEquals(result.a, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn, String expectedOutput) {
        rule = grammar.root;
        check("return " + input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn) {
        rule = grammar.root;
        check("return " + input, expectedReturn);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkThrows (String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> check(input, null));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLiteralsAndUnary () {
        checkExpr("42", 42L);
        checkExpr("42.0", 42.0d);
        checkExpr("\"hello\"", "hello");
        checkExpr("(42)", 42L);
        checkExpr("[1, 2, 3]", new Object[]{1L, 2L, 3L});
        checkExpr("true", true);
        checkExpr("false", false);
        checkExpr("null", Null.INSTANCE);
        checkExpr("!false", true);
        checkExpr("!true", false);
        checkExpr("!!true", true);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testNumericBinary () {
        checkExpr("1 + 2", 3L);
        checkExpr("2 - 1", 1L);
        checkExpr("2 * 3", 6L);
        checkExpr("2 / 3", 0L);
        checkExpr("3 / 2", 1L);
        checkExpr("2 % 3", 2L);
        checkExpr("3 % 2", 1L);

        checkExpr("1.0 + 2.0", 3.0d);
        checkExpr("2.0 - 1.0", 1.0d);
        checkExpr("2.0 * 3.0", 6.0d);
        checkExpr("2.0 / 3.0", 2d / 3d);
        checkExpr("3.0 / 2.0", 3d / 2d);
        checkExpr("2.0 % 3.0", 2.0d);
        checkExpr("3.0 % 2.0", 1.0d);

        checkExpr("1 + 2.0", 3.0d);
        checkExpr("2 - 1.0", 1.0d);
        checkExpr("2 * 3.0", 6.0d);
        checkExpr("2 / 3.0", 2d / 3d);
        checkExpr("3 / 2.0", 3d / 2d);
        checkExpr("2 % 3.0", 2.0d);
        checkExpr("3 % 2.0", 1.0d);

        checkExpr("1.0 + 2", 3.0d);
        checkExpr("2.0 - 1", 1.0d);
        checkExpr("2.0 * 3", 6.0d);
        checkExpr("2.0 / 3", 2d / 3d);
        checkExpr("3.0 / 2", 3d / 2d);
        checkExpr("2.0 % 3", 2.0d);
        checkExpr("3.0 % 2", 1.0d);

        checkExpr("2 * (4-1) * 4.0 / 6 % (2+1)", 1.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testOtherBinary () {
        checkExpr("true  && true",  true);
        checkExpr("true  || true",  true);
        checkExpr("true  || false", true);
        checkExpr("false || true",  true);
        checkExpr("false && true",  false);
        checkExpr("true  && false", false);
        checkExpr("false && false", false);
        checkExpr("false || false", false);

        checkExpr("1 + \"a\"", "1a");
        checkExpr("\"a\" + 1", "a1");
        checkExpr("\"a\" + true", "atrue");

        checkExpr("1 == 1", true);
        checkExpr("1 == 2", false);
        checkExpr("1.0 == 1.0", true);
        checkExpr("1.0 == 2.0", false);
        checkExpr("true == true", true);
        checkExpr("false == false", true);
        checkExpr("true == false", false);
        checkExpr("1 == 1.0", true);
        checkExpr("[1] == [1]", false);

        checkExpr("1 != 1", false);
        checkExpr("1 != 2", true);
        checkExpr("1.0 != 1.0", false);
        checkExpr("1.0 != 2.0", true);
        checkExpr("true != true", false);
        checkExpr("false != false", false);
        checkExpr("true != false", true);
        checkExpr("1 != 1.0", false);

        checkExpr("\"hi\" != \"hi2\"", true);
        checkExpr("[1] != [1]", true);

        // test short circuit
        checkExpr("true || print(\"x\") == \"y\"", true, "");
        checkExpr("false && print(\"x\") == \"y\"", false, "");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testVarDecl () {
        check("var x: Int = 1; return x", 1L);
        check("var x: Float = 2.0; return x", 2d);

        check("var x: Int = 0; return x = 3", 3L);
        check("var x: String = \"0\"; return x = \"S\"", "S");

        // implicit conversions
        check("var x: Float = 1; x = 2; return x", 2.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRootAndBlock () {
        rule = grammar.root;
        check("return", null);
        check("return 1", 1L);
        check("return 1; return 2", 1L);

        check("print(\"a\")", null, "a\n");
        check("print(\"a\" + 1)", null, "a1\n");
        check("print(\"a\"); print(\"b\")", null, "a\nb\n");

        check("{ print(\"a\"); print(\"b\") }", null, "a\nb\n");

        check(
            "var x: Int = 1;" +
                "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
                "print(\"\" + x)",
            null, "1\n2\n1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCalls () {
        check(
            "fun add (a: Int, b: Int): Int { return a + b } " +
                "return add(4, 7)",
            11L);

        HashMap<String, Object> point = new HashMap<>();
        point.put("x", 1L);
        point.put("y", 2L);

        check(
            "struct Point { var x: Int; var y: Int }" +
                "return $Point(1, 2)",
            point);

        check("var str: String = null; return print(str + 1)", "null1", "null1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testArrayStructAccess () {
        checkExpr("[1][0]", 1L);
        checkExpr("[1.0][0]", 1d);
        checkExpr("[1, 2][1]", 2L);

        // TODO check that this fails (& maybe improve so that it generates a better message?)
        // or change to make it legal (introduce a top type, and make it a top type array if thre
        // is no inference context available)
        // checkExpr("[].length", 0L);
        checkExpr("[1].length", 1L);
        checkExpr("[1, 2].length", 2L);

        checkThrows("var array: Int[] = null; return array[0]", NullPointerException.class);
        checkThrows("var array: Int[] = null; return array.length", NullPointerException.class);

        check("var x: Int[] = [0, 1]; x[0] = 3; return x[0]", 3L);
        checkThrows("var x: Int[] = []; x[0] = 3; return x[0]",
            ArrayIndexOutOfBoundsException.class);
        checkThrows("var x: Int[] = null; x[0] = 3",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).y",
            2L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "return p.y",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = $P(1, 2);" +
                "p.y = 42;" +
                "return p.y",
            42L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "p.y = 42",
            NullPointerException.class);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        check("if (true) return 1 else return 2", 1L);
        check("if (false) return 1 else return 2", 2L);
        check("if (false) return 1 else if (true) return 2 else return 3 ", 2L);
        check("if (false) return 1 else if (false) return 2 else return 3 ", 3L);

        check("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ", null, "0\n1\n2\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testInference () {
        check("var array: Int[] = []", null);
        check("var array: String[] = []", null);
        check("fun use_array (array: Int[]) {} ; use_array([])", null);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testTypeAsValues () {
        check("struct S{} ; return \"\"+ S", "S");
        check("struct S{} ; var type: Type = S ; return \"\"+ type", "S");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        check("fun f(): Int { if (true) return 1 else return 2 } ; return f()", 1L);
    }

    // ---------------------------------------------------------------------------------------------

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                             *
     *                                 TESTS DONE BY GROUP 10                                      *                                                             *
     *                                                                                             *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


    /*
    * The attributes can be assigned and thier value retrieved in all kind of combinations
    * */
    @Test public void testBoxAttributes()
    {
        rule = grammar.root;

        String input = "" +
            "box MyBox {\n" +
            "   attr name: String\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#name = \"papers\"\n" +
            "return myBox#name";
        check(input, "papers");

        input = "" +
            "box MyBox {\n" +
            "   attr height: Int\n" +
            "   attr width: Int\n" +
            "   attr depth: Int\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#height = 2\n" +
            "myBox#width  = 2\n" +
            "myBox#depth  = 2\n" +
            "return myBox#height * myBox#width * myBox#depth";
        check(input, 8L);
    }

    /*
     * The methods of a class work properly
     * */
    @Test public void testBoxMethods()
    {
        rule = grammar.root;

        String input = "" +
            "box MyBox {\n" +
            "   meth getMaterial(): String {\n" +
            "       return \"craft\"\n" +
            "   }\n" +
            "   meth getSize(): Int {\n" +
            "       return 8\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "return myBox#getMaterial()\n";
        check(input, "craft");

        input = "" +
            "box MyBox {\n" +
            "   meth getMaterial(): String {\n" +
            "       return \"craft\"\n" +
            "   }\n" +
            "   meth getSize(): Int {\n" +
            "       return 8\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "return myBox#getSize()\n";
        check(input, 8L);
    }

    /*
    * Some tests showing the limitations of our scope handling
    * */
    @Test public void testBoxAssignValues()
    {
        rule = grammar.root;
        /* We have problems with the scopes inside a box
        * In fact the first test will throw a null pointer excepetion during the interpretation
        * as there is problem with the way we deal with scopes.
        * However, the second test will succeed as we are able to change correctly the attribute
        * values from outside the object.
        * */

        String input = "" +
            "struct Pair { var a: Int var b: Int }\n" +
            "var pair: Pair = $Pair(2, 2)\n" +
            "var v: Int = pair.a * pair.b\n" +
            "box MyBox {\n" +
            "   attr height: Int\n" +
            "   attr width: Int\n" +
            "   meth assignSizes(h: Int, w: Int) {\n" +
            "       height = h\n" +
            "       width  = w\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#assignSizes(2, 2)\n" +
            "return myBox#height * myBox#width";
        checkThrows(input, InterpreterException.class);

        input = "" +
            "box MyBox {\n" +
            "   attr height: Int\n" +
            "   attr width: Int\n" +
            "   meth assignSizes(h: Int, w: Int) {\n" +
            "       height = h\n" +
            "       width  = w\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#height = 2\n" +
            "myBox#width  = 2\n " +
            "return myBox#height * myBox#width";
        check(input, 4L);
    }

    /*
    * Same tests as the one of the structs but on box types
    * */
    @Test public void testTypeAsValuesForBoxes () {
        rule = grammar.root;

        check("box S{} ; return \"\"+ S", "S");
        check("box S{} ; var type: Type = S ; return \"\"+ type", "S");
    }

    // ---------------------------------------------------------------------------------------------
    // The following tests are the one that passes the grammar

    @Test public void testBoxSimpleCase()
    {
        rule = grammar.root;

        String input = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "   meth set_max_speed(speed: Int) {\n" +
            "       max_speed = speed\n" +
            "   }\n" +
            "}" +
            "var car: Car = create Car()\n";
        // The following test succeed, but actually this is not what we want!
        // The returned value should obviously be an Int and not an AttributeDeclarationNode.
        check(input + "return car#max_speed", new AttributeDeclarationNode(null, "max_speed", new SimpleTypeNode(null, "Int")));
        check(input + "return car#get_max_speed()", new AttributeDeclarationNode(null, "max_speed", new SimpleTypeNode(null, "Int")));
    }

    /*
    * The first version won't work because of the scope again, however the second one does exactly
    * the same and returns properly.
    * */
    @Test public void testMixTwoBoxes()
    {
        rule = grammar.root;

        String input = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "}\n" +
            "box Bus {\n" +
            "   attr max_speed: Int\n" +
            "   meth set_max_speed(speed: Int) {\n" +
            "       max_speed = speed\n" +
            "   }\n" +
            "}\n" +
            "var bus: Bus = create Bus()\n" +
            "var car: Car = create Car()\n" +
            "car#max_speed = 150" +
            "bus#set_max_speed(car#get_max_speed()/2)" +
            "return bus#max_speed";

        checkThrows(input, InterpreterException.class);

        input = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "}\n" +
            "box Bus {\n" +
            // We chose here to put Float type, it would also succeed with Int
            "   attr max_speed: Float\n" +
            "   meth set_max_speed(speed: Float) {\n" +
            "       max_speed = speed\n" +
            "   }\n" +
            "}\n" +
            "var bus: Bus = create Bus()\n" +
            "var car: Car = create Car()\n" +
            "car#max_speed = 150" +
            "bus#max_speed = car#max_speed/2" +
            "return bus#max_speed";

        check(input, 75L);
    }

    @Test public void testBoxAsArrays()
    {
        rule = grammar.root;

        String input = "" +
            "box Box {\n" +
            "   attr value: Int\n" +
            "}\n" +
            "var b1: Box = create Box()\n" +
            "var b2: Box = create Box()\n" +
            "var b3: Box = create Box()\n" +
            "var boxes: Box[] = [b1, b2, b3]\n" +
            "boxes[1]#value = 10\n" +
            "boxes[2]#value = 54\n" +
            "return boxes[1]#value";

        check(input, 10L);
    }

    // ---------------------------------------------------------------------------------------------

    // NOTE(norswap): Not incredibly complete, but should cover the basics.
}
