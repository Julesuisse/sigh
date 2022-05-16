import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

/**
 * NOTE(norswap): These tests were derived from the {@link InterpreterTests} and don't test anything
 * more, but show how to idiomatically test semantic analysis. using {@link UraniumTestFixture}.
 */
public final class SemanticAnalysisTests extends UraniumTestFixture
{
    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.rule = grammar.root();
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    private String input;

    @Override protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }

    @Override protected String astNodeToString (Object ast) {
        LineMapString map = new LineMapString("<test>", input);
        return ast.toString() + " (" + ((SighNode) ast).span.startString(map) + ")";
    }

    // ---------------------------------------------------------------------------------------------

    @Override protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((SighNode) ast));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testLiteralsAndUnary() {
        successInput("return 42");
        successInput("return 42.0");
        successInput("return \"hello\"");
        successInput("return (42)");
        successInput("return [1, 2, 3]");
        successInput("return true");
        successInput("return false");
        successInput("return null");
        successInput("return !false");
        successInput("return !true");
        successInput("return !!true");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testNumericBinary() {
        successInput("return 1 + 2");
        successInput("return 2 - 1");
        successInput("return 2 * 3");
        successInput("return 2 / 3");
        successInput("return 3 / 2");
        successInput("return 2 % 3");
        successInput("return 3 % 2");

        successInput("return 1.0 + 2.0");
        successInput("return 2.0 - 1.0");
        successInput("return 2.0 * 3.0");
        successInput("return 2.0 / 3.0");
        successInput("return 3.0 / 2.0");
        successInput("return 2.0 % 3.0");
        successInput("return 3.0 % 2.0");

        successInput("return 1 + 2.0");
        successInput("return 2 - 1.0");
        successInput("return 2 * 3.0");
        successInput("return 2 / 3.0");
        successInput("return 3 / 2.0");
        successInput("return 2 % 3.0");
        successInput("return 3 % 2.0");

        successInput("return 1.0 + 2");
        successInput("return 2.0 - 1");
        successInput("return 2.0 * 3");
        successInput("return 2.0 / 3");
        successInput("return 3.0 / 2");
        successInput("return 2.0 % 3");
        successInput("return 3.0 % 2");

        failureInputWith("return 2 + true", "Trying to add Int with Bool");
        failureInputWith("return true + 2", "Trying to add Bool with Int");
        failureInputWith("return 2 + [1]", "Trying to add Int with Int[]");
        failureInputWith("return [1] + 2", "Trying to add Int[] with Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testOtherBinary() {
        successInput("return true && false");
        successInput("return false && true");
        successInput("return true && true");
        successInput("return true || false");
        successInput("return false || true");
        successInput("return false || false");

        failureInputWith("return false || 1",
            "Attempting to perform binary logic on non-boolean type: Int");
        failureInputWith("return 2 || true",
            "Attempting to perform binary logic on non-boolean type: Int");

        successInput("return 1 + \"a\"");
        successInput("return \"a\" + 1");
        successInput("return \"a\" + true");

        successInput("return 1 == 1");
        successInput("return 1 == 2");
        successInput("return 1.0 == 1.0");
        successInput("return 1.0 == 2.0");
        successInput("return true == true");
        successInput("return false == false");
        successInput("return true == false");
        successInput("return 1 == 1.0");

        failureInputWith("return true == 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 == false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" == \"hi\"");
        successInput("return [1] == [1]");

        successInput("return 1 != 1");
        successInput("return 1 != 2");
        successInput("return 1.0 != 1.0");
        successInput("return 1.0 != 2.0");
        successInput("return true != true");
        successInput("return false != false");
        successInput("return true != false");
        successInput("return 1 != 1.0");

        failureInputWith("return true != 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 != false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" != \"hi\"");
        successInput("return [1] != [1]");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testVarDecl() {
        successInput("var x: Int = 1; return x");
        successInput("var x: Float = 2.0; return x");

        successInput("var x: Int = 0; return x = 3");
        successInput("var x: String = \"0\"; return x = \"S\"");

        failureInputWith("var x: Int = true", "expected Int but got Bool");
        failureInputWith("return x + 1", "Could not resolve: x");
        failureInputWith("return x + 1; var x: Int = 2", "Variable used before declaration: x");

        // implicit conversions
        successInput("var x: Float = 1 ; x = 2");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testRootAndBlock () {
        successInput("return");
        successInput("return 1");
        successInput("return 1; return 2");

        successInput("print(\"a\")");
        successInput("print(\"a\" + 1)");
        successInput("print(\"a\"); print(\"b\")");

        successInput("{ print(\"a\"); print(\"b\") }");

        successInput(
            "var x: Int = 1;" +
                "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
                "print(\"\" + x)");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testCalls() {
        successInput(
            "fun add (a: Int, b: Int): Int { return a + b } " +
                "return add(4, 7)");

        successInput(
            "struct Point { var x: Int; var y: Int }" +
                "return $Point(1, 2)");

        successInput("var str: String = null; return print(str + 1)");

        failureInputWith("return print(1)", "argument 0: expected String but got Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testArrayStructAccess() {
        successInput("return [1][0]");
        successInput("return [1.0][0]");
        successInput("return [1, 2][1]");

        failureInputWith("return [1][true]", "Indexing an array using a non-Int-valued expression");

        // TODO make this legal?
        // successInput("[].length", 0L);

        successInput("return [1].length");
        successInput("return [1, 2].length");

        successInput("var array: Int[] = null; return array[0]");
        successInput("var array: Int[] = null; return array.length");

        successInput("var x: Int[] = [0, 1]; x[0] = 3; return x[0]");
        successInput("var x: Int[] = []; x[0] = 3; return x[0]");
        successInput("var x: Int[] = null; x[0] = 3");

        successInput(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = $P(1, 2);" +
                "p.y = 42;" +
                "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "p.y = 42");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, true)",
            "argument 1: expected Int but got Bool");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).z",
            "Trying to access missing field z on struct P");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        successInput("if (true) return 1 else return 2");
        successInput("if (false) return 1 else return 2");
        successInput("if (false) return 1 else if (true) return 2 else return 3 ");
        successInput("if (false) return 1 else if (false) return 2 else return 3 ");

        successInput("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ");

        failureInputWith("if 1 return 1",
            "If statement with a non-boolean condition of type: Int");
        failureInputWith("while 1 return 1",
            "While statement with a non-boolean condition of type: Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testInference() {
        successInput("var array: Int[] = []");
        successInput("var array: String[] = []");
        successInput("fun use_array (array: Int[]) {} ; use_array([])");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testTypeAsValues() {
        successInput("struct S{} ; return \"\"+ S");
        successInput("struct S{} ; var type: Type = S ; return \"\"+ type");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        successInput("fun f(): Int { if (true) return 1 else return 2 } ; return f()");

        // TODO: would be nice if this pinpointed the if-statement as missing the return,
        //   not the whole function declaration
        failureInputWith("fun f(): Int { if (true) return 1 } ; return f()",
            "Missing return in function");
    }

    // ---------------------------------------------------------------------------------------------

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *                                                                                             *
     *                                 TESTS DONE BY GROUP 10                                      *                                                             *
     *                                                                                             *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Test public void testBoxSimpleCase()
    {
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
        successInput(input);
    }

    @Test public void testBoxDoubleCase()
    {
        String input = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "}\n" +
            "box Bus {\n" +
            "   attr max_speed: Int\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "}\n";
        successInput(input);
    }

    @Test public void testForeignBox()
    {
        String input = "" +
            "box Wheel { \n" +
            "   meth get_size(): Int {\n" +
            "       return size\n" +
            "   }\n" +
            "   attr size: Int\n" +
            "}\n" +
            "box Car {\n" +
            "   attr wheels: Wheel\n" +
            "   meth get_wheels_size_attr(): Int {\n" +
            "       return wheels#size\n" +
            "   }\n" +
            "   meth get_wheels_size_meth(): Int {\n" +
            "       return wheels#get_size()\n" +
            "   }\n" +
            "}\n";
        successInput(input);
    }

    // Show that we are still valid for functions
    @Test public void testFunctionReturnType()
    {
        failureInputWith("fun f(): String { return false }",
            "Incompatible return type, expected String but got Bool");
    }

    // And that methods work too now, even with attributes of the box!
    @Test public void testMethodReturnType()
    {
        String input = "" +
            "box Car {" +
            "   attr nWheels: Int\n" +
            "   meth get_nWheels(): String {\n" +
            "       return nWheels\n" +
            "   }\n" +
            "}\n";
        failureInputWith(input, "Incompatible return type, expected String but got Int");
    }

    @Test public void testForeignAttributeTypes()
    {
        String input = "" +
            "box Car {\n" +
            "   attr max_speed: Int\n" +
            "   attr arr: Int[]\n" +
            "   meth get_max_speed(): Int {\n" +
            "       return max_speed\n" +
            "   }\n" +
            "   meth get_arr_i(i: Int): Int {\n" +
            "       return arr[i]\n" +
            "   }\n" +
            "   meth set_max_speed(speed: Int) {\n" +
            "       max_speed = speed\n" +
            "   }\n" +
            "}\n";
        successInput(input);
    }


    @Test public void testBoxSemantic()
    {
        String input3 = "" +
            "box Wheel { \n" +
            "   meth get_size(): Int {\n" +
            "       return size\n" +
            "   }\n" +
            "   attr size: Int\n" +
            "}\n" +
            "box Car {\n" +
            "   attr wheels: Wheel\n" +
            "   meth get_wheels_size(): Int {\n" +
            "       return wheels#get_size()\n" +
            "   }\n" +
            "   meth set_wheels_size_failure(size: Int) {\n" +
            // The following line try to assign a value to an attribute (ok)
            "       wheels#size = size\n" +
            "   }\n" +
            "   meth set_wheels_size_failure(size: Int) {\n" +
            // The following line try to assign a value to a method (not ok!)
            "       wheels#get_size = size\n" +
            "   }\n" +
            "}\n";

        failureInputWith(input3, "Trying to assign a value to a non-compatible lvalue.");
    }

    @Test public void testBoxAttributeOperation() {
        String input = "" +
            "box MyBox {\n" +
            "   attr height: Int\n" +
            "   attr width: Int\n" +
            "   attr depth: Int\n" +
            "   meth assignSizes(h: Int, w: Int, d: Int) {\n" +
            "       height = h\n" +
            "       width  = w\n" +
            "       depth  = d\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#assignSizes(2, 2, 2)\n" +
            "return myBox#height * myBox#width * myBox#depth";

        successInput(input);

        input = "" +
            "box MyBox {\n" +
            "   attr height: String\n" +
            "   attr width: Int\n" +
            "   meth assignSizes(h: String, w: Int) {\n" +
            "       height = h\n" +
            "       width  = w\n" +
            "   }\n" +
            "}\n" +
            "var myBox: MyBox = create MyBox()\n" +
            "myBox#assignSizes(\"Two\", 2)\n" +
            "return myBox#height * myBox#width";

        failureInputWith(input, "Trying to multiply String with Int");
    }

    @Test public void testAttributesScope() {
        String input = "" +
            "box Car {\n" +
            "   attr nWheels: Int\n" +
            "}\n" +
            "var copy: Int = nWheels";

        failureInputWith(input, "Could not resolve: nWheels");

        input = "" +
            "box Car {\n" +
            "   attr nWheels: Int\n" +
            "   meth getNWheels(): Int {\n" +
            "       return nWheels\n" +
            "   }\n" +
            "}\n";
        successInput(input);
    }

    /*
    * We cannot access a box attribute with the notation of a struct
    * It must be a '#' instead of a '.'
    * */
    @Test public void testAttributeAccess()
    {
        String input = "" +
            "box BadBox {\n" +
            "   attr youWontGetMe: Int\n" +
            "}\n" +
            "var badBox: BadBox = create BadBox()\n" +
            "var _: Int = badBox.youWontGetMe";

        failureInputWith(input, "Trying to access a field on an expression of type BadBox");

        input = "" +
            "box BadBox {\n" +
            "   attr youWillGetMe: Int\n" +
            "}\n" +
            "var badBox: BadBox = create BadBox()\n" +
            "var _: Int = badBox#youWillGetMe";

        successInput(input);
    }

    /*
    * The box type must match an existing type
    * */
    @Test public void testDeclareUnknownBox() {
        String input = "" +
            "box GoodBox {  }\n" +
            "var goodBox: BadBox = create GoodBox()";

        failureInputWith(input, "could not resolve: BadBox");
    }

    /*
    * The box type must match the correct type
    * */
    @Test public void testAssignWrongBoxType() {
        String input = "" +
            "box GoodBox {  }\n" +
            "box BadBox {  }\n" +
            "var goodBox: BadBox = create GoodBox()";

        failureInputWith(input, "incompatible initializer type provided for variable `goodBox`: expected BadBox but got GoodBox");
    }

    /*
    * We can pass boxes as arguments of a function and use its elements
    * */
    @Test public void testBoxAsArgument() {
        String input = "" +
            "box Box {\n" +
            "   attr volume: Int\n" +
            "   meth getVolume(): Int {\n" +
            "       return volume\n" +
            "   }\n" +
            "}\n" +
            // Get the attribute directly
            "fun f(wineBox: Box): Int {\n" +
            "   return wineBox#volume\n" +
            "}\n" +
            // Ask the getter function to return the attribute
            "fun g(wineBox: Box): Int {\n" +
            "   return wineBox#getVolume()\n" +
            "}\n" +
            "var wineBox: Box = create Box()\n" +
            "wineBox#volume = 5\n" +
            "f(wineBox)\n" +
            "g(wineBox)\n";

        successInput(input);
    }

    // ---------------------------------------------------------------------------------------------
}
