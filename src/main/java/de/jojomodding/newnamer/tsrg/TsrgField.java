package de.jojomodding.newnamer.tsrg;

import java.io.PrintStream;

public class TsrgField {

    private final String obfName, srgName;
    private final TsrgClass clazz;

    protected TsrgField(TsrgClass clazz, String notch, String srg){
        this.obfName = notch;
        this.srgName = srg;
        this.clazz = clazz;
    }

    public String getNotchName() {
        return obfName;
    }

    public String getSrgName() {
        return srgName;
    }

    public TsrgClass getParent() {
        return clazz;
    }

    public void writeToStream(PrintStream printStream) {
        printStream.println("\t"+obfName+" "+srgName);
    }
}
