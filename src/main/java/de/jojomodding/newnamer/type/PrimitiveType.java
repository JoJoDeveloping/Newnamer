package de.jojomodding.newnamer.type;

import de.jojomodding.newnamer.tsrg.NameLookup;

import java.util.Arrays;

public class PrimitiveType extends Type{

    public static Type fromChar(char peek) {
        return Primitive.fromChar(peek).type();
    }

    public enum Primitive{
        VOID("V"), BOOLEAN("Z"), BYTE("B"), CHAR("C"), SHORT("S"), INT("I"), LONG("J"), FLOAT("F"), DOUBLE("D");

        String s;

        Primitive(String s){
            this.s = s;
        }

        public PrimitiveType type(){
            return new PrimitiveType(this);
        }

        public static Primitive fromChar(char c){
            return Arrays.stream(values()).filter(p -> p.s.charAt(0) == c).findAny().orElseThrow(() -> new IllegalArgumentException("Could not find primitive "+c));
        }
    }

    private final Primitive p;

    public PrimitiveType(Primitive p){
        this.p = p;
    }

    @Override
    public String format(NameLookup env) {
        return p.s;
    }
}
