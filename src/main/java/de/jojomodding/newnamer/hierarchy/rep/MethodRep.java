package de.jojomodding.newnamer.hierarchy.rep;

public interface MethodRep {
    String getNotchName();
    String getNotchianSignature();
    boolean isPrivate();
    boolean isFinal();
    boolean isSynthetic();
    String getSrgName();
}
