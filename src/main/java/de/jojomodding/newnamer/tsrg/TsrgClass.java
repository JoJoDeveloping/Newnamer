package de.jojomodding.newnamer.tsrg;

import de.jojomodding.newnamer.hierarchy.rep.ClassRep;
import de.jojomodding.newnamer.hierarchy.rep.MethodRep;
import de.jojomodding.newnamer.type.ClassType;
import de.jojomodding.newnamer.type.FunctionType;
import de.jojomodding.newnamer.type.Type;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TsrgClass extends ClassType implements ClassRep {

    private final String deobfName, obfName;
    private final Set<TsrgMethod> methods;
    private final Set<TsrgField> fields;
    private String superclass=null;
    private boolean isfinal = false;
    private Set<String> interfaces = new HashSet<>();
    private final Tsrg file;

    public TsrgClass(Tsrg file, String obf, String deobf) {
        super(obf);
        this.file = file;
        this.deobfName = deobf;
        this.obfName = obf;
        methods = new TreeSet<>((m1,m2) -> {
            int i = m1.getNotchName().compareTo(m2.getNotchName());
            if(i==0) return m1.getNotchianSignature().compareTo(m2.getNotchianSignature());
            return i;
        });
        fields = new TreeSet<>(Comparator.comparing(TsrgField::getNotchName));
    }

    public String getDeobfuscatedName() {
        return deobfName;
    }

    public String getNotchName() {
        return obfName;
    }

    @Override
    public String superClass() {
        return superclass;
    }

    @Override
    public Set<String> superInterfaces() {
        return interfaces;
    }

    public String getTypeString(){
        return "L"+deobfName+";";
    }

    public Set<MethodRep> getMethods() {
        return methods.stream().map(k -> (MethodRep)k).collect(Collectors.toSet());
    }

    public Set<TsrgMethod> getTSRGMethods() {
        return methods;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    public Set<TsrgField> getFields() {
        return fields;
    }

    public TsrgField addField(String notch, String srg) {
        if(srg.startsWith("field_"))
            file.handleReadMembernum(readInt(6, srg));
        TsrgField f = new TsrgField(this, notch, srg);
        this.fields.add(f);
        return f;
    }

    public TsrgMethod addMethod(FunctionType type, String notch, String srg) {
        if(srg.startsWith("func_"))
            file.handleReadMembernum(readInt(5, srg));
        TsrgMethod m = new TsrgMethod(this, type, notch, srg);
        this.methods.add(m);
        return m;
    }

    int readInt(int index, String s){
        if(index >= s.length()) throw new IllegalArgumentException("Name with number in it does not actually contain a number!");
        int i;
        char c = s.charAt(index);
        if(c >= '0' && c <= '9')
            i = c - '0';
        else throw new IllegalArgumentException("Name with number in it does not actually contain a number!");
        while (true){
            index++;
            if(index >= s.length())
                return i;
            c = s.charAt(index);
            if(c >= '0' && c <= '9')
                i = 10*i+(c - '0');
            else return i;
        }
    }

    public void supplyMoreInformation(String superclass, boolean isfinal, Set<String> interfaces){
        this.superclass = superclass;
        this.isfinal = isfinal;
        this.interfaces = interfaces;
    }

    public void writeToStream(PrintStream printStream) {
        printStream.println(obfName+" "+deobfName);
        this.fields.forEach(f -> f.writeToStream(printStream));
        this.methods.forEach(m -> m.writeToStream(printStream));
    }
}
