package de.jojomodding.newnamer.hierarchy.rep;

import java.util.Set;

public interface ClassRep {
    String getNotchName();
    String superClass();
    Set<String> superInterfaces();
    Set<MethodRep> getMethods();
    boolean isFinal();
}
