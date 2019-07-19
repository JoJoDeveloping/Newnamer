package de.jojomodding.newnamer.hierarchy.rep;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JavaMethod implements MethodRep {

    private Method m;

    public JavaMethod(Method m) {
        this.m = m;
    }

    @Override
    public String getNotchName() {
        return m.getName();
    }

    @Override
    public String getSrgName() {
        return m.getName();
    }

    @Override
    public String getNotchianSignature() {
        return "("+ Arrays.stream(m.getParameterTypes()).map(JavaClass::toNative).collect(Collectors.joining())+")"+JavaClass.toNative(m.getReturnType());
    }

    @Override
    public boolean isPrivate() {
        return (m.getModifiers() & Modifier.PRIVATE) != 0;
    }

    @Override
    public boolean isFinal() {
        return (m.getModifiers() & Modifier.FINAL) != 0;
    }

    @Override
    public boolean isSynthetic() {
        return m.isSynthetic();
    }
}
