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
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class MemberRenamer extends ClassVisitor {

    private Tsrg oldt, newt;
    private SuperclassMethodLookup overrideChecker;
    private final String enumValuesName;
    private final ClassNode n;
    private TsrgClass currentClass, currentClassOld;
    private Map<String, String> syntheticBridgeTargetNames;

    public MemberRenamer(ClassNode n, Tsrg oldt, Tsrg newt, SuperclassMethodLookup overrideChecker) {
        super(Opcodes.ASM6);
        this.n = n;
        this.oldt = oldt;
        this.newt = newt;
        this.overrideChecker = overrideChecker;
        currentClass = null;
        currentClassOld = null;
        syntheticBridgeTargetNames = new HashMap<>();


        if((n.access & Opcodes.ACC_ENUM) != 0) {
            if (n.superName != null && n.superName.equals("java/lang/Enum")) {
                Optional<MethodNode> mn = n.methods.stream().
                        filter(m -> m.name.equals("values") && m.desc.equals("()[L" + n.name + ";") && (m.access & Opcodes.ACC_STATIC) != 0).
                        findAny();
                if (mn.isEmpty())
                    throw new IllegalArgumentException("enum without values() method!");
                FieldInsnNode getter = null;
                for (AbstractInsnNode in : mn.get().instructions.toArray()) {
                    if (in instanceof FieldInsnNode && in.getOpcode() == Opcodes.GETSTATIC)
                        getter = (FieldInsnNode) in;
                }
                if (getter == null)
                    throw new IllegalArgumentException("values() method in enum " + n.name + " does not load $VALUES field!");
                if (getter.owner.equals(n.name) && getter.desc.equals("[L" + n.name + ";"))
                    enumValuesName = getter.name;
                else
                    throw new IllegalArgumentException("values() method in enum " + n.name + " does not load $VALUES field!");
            }else{
                enumValuesName = null; //if you give an enum member it's own anonymous classes these classes are marked as enum yet aren't actually. you end up here.
            }
        }else enumValuesName = null;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentClass = newt.getClasses().get(name);
        currentClassOld = oldt.getClasses().get(name);
        if(currentClass == null) throw new IllegalStateException("Class appeared in phase 2 only: "+name);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Util.printVerbose("Visiting field "+name);
        Optional<String> newName = Optional.empty();
        if((access & Opcodes.ACC_SYNTHETIC) != 0) {
            if(name.equals(enumValuesName))
                newName = Optional.of("$VALUES");

            if(newName.isEmpty()){
                Util.printVerbose("Ignoring synthetic field " + name + " in class " + currentClass.getNotchName());
                return super.visitField(access, name, descriptor, signature, value);
            }
        }
        newName.or(()->currentClassOld != null ?
                    currentClassOld.getFields().stream().
                    filter(f -> f.getNotchName().equals(name)).
                    findAny().map(TsrgField::getSrgName) : Optional.<String>empty()).
                or(() -> {
                    String s ="field_" + newt.nextNameOffset() + "_" + name;
                    Util.printVerbose("Found new field "+name+" in "+currentClass.getDeobfuscatedName()+", srg: "+s);
                    return Optional.of(s);
                }).
                ifPresent(srg -> {
                    Util.printVerbose("Renaming field "+name+" in class "+currentClass.getDeobfuscatedName()+" to "+srg);
                    currentClass.addField(name, srg);
                });
        return super.visitField(access, name, descriptor, signature, value);
    }

    private Optional<String> getMethodName(String name, String sig, int access){
        if(name.equals("<init>")||name.equals("<clinit>")) return Optional.empty();
        if((access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) != 0 && (name+sig).equals("main([Ljava/lang/String;)V"))
            return Optional.of(name);
        if((access & Opcodes.ACC_SYNTHETIC) != 0 && (access & Opcodes.ACC_SYNTHETIC) == 0) {
            Util.printVerbose("Ignoring synthetic method "+name+sig);
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
                                String s = "func_"+ newt.nextNameOffset()+"_"+name;
                                Util.printVerbose("Found new method "+name+sig+" in class "+currentClass.getDeobfuscatedName()+" srg: "+s);
                                return Optional.of(s);
                            })
                );
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Util.printVerbose("Visiting method "+name);
        getMethodName(name, descriptor, access).ifPresent(srg -> {
            Util.printVerbose("Renaming method " + name + descriptor + " in class " + currentClass.getDeobfuscatedName() + " to " + srg);
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
                    Util.printVerbose("Found bridge method bridging "+name+descriptor+" to "+name+min.desc+" in class "+currentClass.getDeobfuscatedName());
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

}
