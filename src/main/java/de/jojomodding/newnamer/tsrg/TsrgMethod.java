package de.jojomodding.newnamer.tsrg;

import de.jojomodding.newnamer.hierarchy.rep.MethodRep;
import de.jojomodding.newnamer.type.FunctionType;

import java.io.PrintStream;
import java.util.Optional;

public class TsrgMethod implements MethodRep {
    private final String obfName, srgName;
    private final TsrgClass clazz;
    private final FunctionType type;
    private boolean isfinal, isprivate, issynthetic;

    protected TsrgMethod(TsrgClass clazz, FunctionType type, String notch, String srg){
        this.obfName = notch;
        this.srgName = srg;
        this.clazz = clazz;
        this.type = type;
    }

    public String getNotchName() {
        return obfName;
    }

    @Override
    public String getNotchianSignature() {
        return type.format($ -> Optional.empty());
    }

    @Override
    public boolean isPrivate() {
        return isprivate;
    }

    @Override
    public boolean isFinal() {
        return isfinal;
    }

    @Override
    public boolean isSynthetic() {
        return issynthetic;
    }

    public TsrgClass getParent() {
        return clazz;
    }

    public FunctionType getType(){
        return type;
    }

    @Override
    public String getSrgName() {
        return srgName;
    }

    public void supplyMoreInformation(boolean pri, boolean fin, boolean syn){
        isfinal = fin;
        isprivate = pri;
        issynthetic = syn;
    }

    public void writeToStream(PrintStream printStream) {
        printStream.println("\t"+obfName+" "+getNotchianSignature()+" "+srgName);
    }
}
