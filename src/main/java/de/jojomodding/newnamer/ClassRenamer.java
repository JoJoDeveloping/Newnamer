package de.jojomodding.newnamer;

import de.jojomodding.newnamer.hierarchy.SuperclassMethodLookup;
import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.TsrgClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class ClassRenamer extends ClassVisitor {

    private Tsrg old, newt;
    private SuperclassMethodLookup lookup;

    public ClassRenamer(URLClassLoader cpLoader, Tsrg tsrg) {
        super(Opcodes.ASM6);
        this.newt = tsrg.cloneEmpty();
        this.old = tsrg;
        lookup = new SuperclassMethodLookup(newt, cpLoader);
    }
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Util.printVerbose("Visiting class "+name);
        if(name == null) {
            super.visit(version, access, name, signature, superName, interfaces);
            return;
        }
        TsrgClass clazz = old.getClasses().get(name);
        if(clazz == null){
            if(name.contains("/"))
                clazz = newt.addClass(name, name);
            else
                clazz = newt.addClass(name, "C_"+newt.nextNameOffset()+"_"+name);
            Util.printVerbose("Mapping new class "+name+" to "+clazz.getDeobfuscatedName());
        }else newt.addClass(name, clazz.getDeobfuscatedName());
        clazz.supplyMoreInformation("L"+superName+";", (access&Opcodes.ACC_FINAL)!=0, interfaces == null ? Set.of() : Arrays.stream(interfaces).map(s -> "L"+s+";").collect(Collectors.toSet()));
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public Tsrg getOldTsrg() {
        return old;
    }

    public ClassVisitor memberRenamer(ClassNode n) {
        return new MemberRenamer(n, old, newt, lookup);

    }

    public Tsrg getNewTsrg() {
        return newt;
    }

}
