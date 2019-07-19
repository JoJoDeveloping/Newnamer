package de.jojomodding.newnamer.hierarchy.rep;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class JavaClass implements ClassRep {

    private Class c;
    private Set<String> interfaces;
    private Set<MethodRep> methods;

    public JavaClass(Class c){
        this.c = c;
    }

    @Override
    public String superClass() {
        return c.getSuperclass() == null ? null : toNative(c.getSuperclass());
    }

    @Override
    public Set<String> superInterfaces() {
        if(interfaces == null)
            interfaces = Arrays.stream(c.getInterfaces()).map(JavaClass::toNative).collect(Collectors.toSet());
        return interfaces;
    }

    @Override
    public String getNotchName() {
        return toNative(c);
    }

    @Override
    public Set<MethodRep> getMethods() {
        if(methods == null)
            methods = Arrays.stream(c.getMethods()).map(JavaMethod::new).collect(Collectors.toSet());
        return methods;
    }

    @Override
    public boolean isFinal() {
        return (c.getModifiers() & Modifier.FINAL) != 0;
    }

    public static String toNative(Class<?> c){
        if(c == Integer.TYPE) return "I";
        if(c == Long.TYPE) return "J";
        if(c == Byte.TYPE) return "B";
        if(c == Character.TYPE) return "C";
        if(c == Short.TYPE) return "S";
        if(c == Boolean.TYPE) return "Z";
        if(c == Float.TYPE) return "F";
        if(c == Double.TYPE) return "D";
        if(c == Void.TYPE) return "V";
        if(c.getName().startsWith("["))
            return c.getName().replace('.', '/');
        return "L"+c.getName().replace('.', '/')+";";
    }

}
