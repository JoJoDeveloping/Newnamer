package de.jojomodding.newnamer.tsrg;

import joptsimple.OptionSet;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Tsrg {

    private final Map<String, TsrgClass> classes = new HashMap<>();
    private int newFieldNum = 0;

    public Optional<String> lookupClassName(String notchname) {
        return lookupClass(notchname).map(TsrgClass::getTypeString);
    }

    public Optional<TsrgClass> lookupClass(String notchian){
        return Optional.ofNullable(classes.getOrDefault(notchian, null));
    }

    public TsrgClass addClass(String notchname, String srgname){
        TsrgClass clazz = classes.get(notchname);
        if(clazz != null)
            throw new RuntimeException("Class with the same name added twice! Name: "+notchname+" to "+srgname+", already had "+clazz.getDeobfuscatedName());
        return classes.compute(notchname, ($,$$) -> new TsrgClass(this, notchname, srgname));
    }

    protected void handleReadMembernum(int i){
        newFieldNum = Math.max(i, newFieldNum);
    }

    public Map<String, TsrgClass> getClasses() {
        return classes;
    }

    public int getNameOffset() {
        return newFieldNum;
    }

    public int nextNameOffset() {
        return ++newFieldNum;
    }

    public Tsrg cloneEmpty() {
        Tsrg clone = new Tsrg();
        clone.newFieldNum = newFieldNum;
        return clone;
    }

    public void writeToStream(PrintStream printStream) {
        this.classes.values().forEach(c -> c.writeToStream(printStream));
    }
}
