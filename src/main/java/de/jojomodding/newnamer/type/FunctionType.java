package de.jojomodding.newnamer.type;

import de.jojomodding.newnamer.tsrg.NameLookup;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionType extends Type{

    private final List<Type> args;
    private final Type result;

    public FunctionType(Type result, List<Type> args){
        this.args = args;
        this.result = result;
    }

    @Override
    public String format(NameLookup env) {
        return "("+args.stream().map(t -> t.format(env)).collect(Collectors.joining()) + ")"+result.format(env);
    }

    public List<Type> arguments() {
        return args;
    }
}
