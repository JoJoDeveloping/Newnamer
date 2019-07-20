package de.jojomodding.newnamer.hierarchy.rep;

import de.jojomodding.newnamer.hierarchy.rep.ClassRep;

import java.util.Set;

public class ArrayRep implements ClassRep {

    private ClassRep base;
    private static ClassRep scheme = new JavaClass(Object[].class);

    public ArrayRep(ClassRep lookup) {
    }

    @Override
    public String getNotchName() {
        return "["+base.getNotchName();
    }

    @Override
    public String superClass() {
        return scheme.superClass();
    }

    @Override
    public Set<String> superInterfaces() {
        return scheme.superInterfaces();
    }

    @Override
    public Set<MethodRep> getMethods() {
        return scheme.getMethods();
    }

    @Override
    public boolean isFinal() {
        return scheme.isFinal();
    }
}
