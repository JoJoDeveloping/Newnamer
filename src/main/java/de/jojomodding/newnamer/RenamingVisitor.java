package de.jojomodding.newnamer;

import de.jojomodding.newnamer.hierarchy.SuperclassMethodLookup;
import de.jojomodding.newnamer.hierarchy.rep.MethodRep;
import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.TsrgClass;
import de.jojomodding.newnamer.tsrg.TsrgField;
import de.jojomodding.newnamer.tsrg.TsrgMethod;
import de.jojomodding.newnamer.tsrg.parser.Parser;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RenamingVisitor extends ClassVisitor {

    private Tsrg old, newt;
    private final boolean verbose;
    private SuperclassMethodLookup overrideChecker;

    public RenamingVisitor(URLClassLoader cpLoader, Tsrg tsrg, boolean verbose) {
        super(Opcodes.ASM6);
        this.newt = tsrg.cloneEmpty();
        overrideChecker = new SuperclassMethodLookup(newt, cpLoader);
        this.old = tsrg;
        this.verbose = verbose;
    }
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if(verbose)System.out.println("Visiting class "+name);
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
            System.out.println("Mapping new class "+name+" to "+clazz.getDeobfuscatedName());
        }else newt.addClass(name, name);
        clazz.supplyMoreInformation("L"+superName+";", (access&Opcodes.ACC_FINAL)!=0, interfaces == null ? Set.of() : Set.of(interfaces));
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public Tsrg getOldTsrg() {
        return old;
    }

    public boolean verbose() {
        return verbose;
    }

    public ClassVisitor roundTwo(ClassNode n) {
        return new ClassVisitor(Opcodes.ASM6) {
            private TsrgClass currentClass = null, currentClassOld = null;
            Map<String, String> syntheticBridgeTargetNames = new HashMap<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                currentClass = newt.getClasses().get(name);
                currentClassOld = old.getClasses().get(name);
                if(currentClass == null) throw new IllegalStateException("Class appeared in phase 2 only: "+name);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if(verbose)System.out.println("Visiting field "+name);
                if((access & Opcodes.ACC_SYNTHETIC) == 0 && (access & Opcodes.ACC_BRIDGE) == 0) {
                    (currentClassOld != null ?
                        currentClassOld.getFields().stream().
                                filter(f -> f.getNotchName().equals(name)).
                                findAny().map(TsrgField::getSrgName) : Optional.<String>empty()).
                            or(() -> {
                                String s ="field_" + newt.nextNameOffset() + "_" + name;
                                System.out.println("Found new field "+name+" in "+currentClass.getDeobfuscatedName()+", srg: "+s);
                                return Optional.of(s);
                            }).
                            ifPresent(srg -> {
                                if(verbose)
                                    System.out.println("Renaming field "+name+" in class "+currentClass.getDeobfuscatedName()+" to "+srg);
                                currentClass.addField(name, srg);
                            });
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            private Optional<String> getMethodName(String name, String sig, int access){
                if(name.equals("<init>")||name.equals("<clinit>")) return Optional.empty();
                if((access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) != 0 && (name+sig).equals("main([Ljava/lang/String;)V"))
                    return Optional.of(name);
                if((access & Opcodes.ACC_SYNTHETIC) != 0 && (access & Opcodes.ACC_SYNTHETIC) == 0) {
                    if(verbose)
                        System.out.println("Ignoring synthetic method "+name+sig);
                    return Optional.empty();
                }
                String tgt = syntheticBridgeTargetNames.get(name+"@"+sig);
                if(tgt != null)
                    return Optional.of(tgt);
                return overrideChecker.parentName(currentClass.getNotchName(), name, sig).
                        or(()->
                                (currentClassOld != null ?
                                 currentClassOld.getMethods().stream().
                                         filter(mr -> mr.getNotchName().equals(name) && mr.getNotchianSignature().equals(sig)).
                                         findAny().
                                         map(MethodRep::getSrgName) : Optional.<String>empty()).
                                    or(()->{
                                        String s = "func_"+newt.nextNameOffset()+"_"+name;
                                        System.out.println("Found new method "+s+sig+" in class "+currentClass.getDeobfuscatedName()+" srg: "+s);
                                        return Optional.of(s);
                                    })
                        );
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if(verbose)System.out.println("Visiting method "+name);
                getMethodName(name, descriptor, access).ifPresent(srg -> {
                    if(verbose)
                        System.out.println("Renaming method "+name+descriptor+" in class "+currentClass.getDeobfuscatedName()+" to "+srg);
                    currentClass.addMethod(Parser.parseFunctionTypeStatic(descriptor), name, srg)
                            .supplyMoreInformation((access & Opcodes.ACC_PRIVATE) != 0, (access & Opcodes.ACC_FINAL) != 0, (access & Opcodes.ACC_SYNTHETIC) != 0);


                if((access & Opcodes.ACC_BRIDGE) != 0) {
                    n.methods.stream().filter(n -> n.name.equals(name) && n.desc.equals(descriptor) && n.access == access).findAny().ifPresentOrElse(m -> {
                        AbstractInsnNode[] nodes = m.instructions.toArray();
                        Optional<MethodInsnNode> delegated = Optional.empty();
                        for(AbstractInsnNode n : nodes){
                            if(n instanceof MethodInsnNode){
                                if(delegated.isPresent())
                                    throw new IllegalStateException("Synthetic bridge calling more than one method!");
                                else delegated = Optional.of((MethodInsnNode)n);
                            }
                        }
                        if(!delegated.isPresent()) throw new IllegalStateException("Synthetic bridge does not call a method!");
                        MethodInsnNode min = delegated.get();
                        if(min.name.equals(name) && Parser.parseFunctionTypeStatic(min.desc).arguments().size() == Parser.parseFunctionTypeStatic(descriptor).arguments().size() && min.owner.equals(n.name)){
                            System.out.println("Found bridge method bridging "+name+descriptor+" to "+name+min.desc+" in class "+currentClass.getDeobfuscatedName());
                            Set<TsrgMethod> mrs = currentClass.getTSRGMethods().stream().filter(mr -> mr.getNotchName().equals(name) && mr.getNotchianSignature().equals(min.desc)).collect(Collectors.toSet());
                            if(mrs.size() == 0) //the target has not yet been resolved
                                syntheticBridgeTargetNames.put(name+"@"+min.desc, name);
                            else if(mrs.size() == 1)
                                mrs.forEach(mr2 -> {
                                    currentClass.getTSRGMethods().remove(mr2);
                                    currentClass.addMethod(mr2.getType(), mr2.getNotchName(), srg);
                                });
                            else throw new IllegalArgumentException("Trying to rename bridge endpoint failed because we found more than two targets");

                        }
                    }, () -> {throw new IllegalStateException("Class does not have method we're trying to visit!");});
                }


                });
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

        };

    }

    public Tsrg getNewTsrg() {
        return newt;
    }
}
