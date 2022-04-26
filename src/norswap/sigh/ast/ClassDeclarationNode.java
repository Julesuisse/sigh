package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;
import java.util.List;

public class ClassDeclarationNode extends DeclarationNode
{
    public final String name;
    public final List<FieldDeclarationNode> attributes;
    public final List<ClassFunDeclarationNode> functions;

    @SuppressWarnings("unchecked")
    public ClassDeclarationNode (Span span, Object name, Object attributes, Object functions) {
        super(span);
        this.name = Util.cast(name, String.class);
        this.attributes = Util.cast(attributes, List.class);
        this.functions = Util.cast(functions, List.class);
    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return "class " + name;
    }

    @Override public String declaredThing () {
        return "class";
    }
}
