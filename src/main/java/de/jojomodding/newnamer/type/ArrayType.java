package de.jojomodding.newnamer.type;


import de.jojomodding.newnamer.tsrg.NameLookup;

public class ArrayType extends Type{

    private final Type subtype;

    public ArrayType(Type subtype){
        this.subtype = subtype;
    }

    @Override
    public String format(NameLookup env) {
        return "["+subtype.format(env);
    }
}
