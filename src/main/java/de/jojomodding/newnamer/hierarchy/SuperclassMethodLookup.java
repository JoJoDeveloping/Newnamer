package de.jojomodding.newnamer.hierarchy;

import de.jojomodding.newnamer.hierarchy.rep.ClassRep;
import de.jojomodding.newnamer.hierarchy.rep.JavaClass;
import de.jojomodding.newnamer.hierarchy.rep.MethodRep;
import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.TsrgClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SuperclassMethodLookup {

    private Tsrg renameables;
    private ClassLoader cp;
    private Map<String, ClassRep> reps = new HashMap<>();

    public SuperclassMethodLookup(Tsrg tsrg, ClassLoader cp){
        this.renameables = tsrg;
        this.cp = cp;
    }

    public ClassRep lookup(String n){
        return reps.computeIfAbsent(n, $ -> {
            TsrgClass t = renameables.getClasses().get(n);
            if(t == null) {
                switch (n.charAt(0)){
                    case 'B': return new JavaClass(Byte.TYPE);
                    case 'C': return new JavaClass(Character.TYPE);
                    case 'D': return new JavaClass(Double.TYPE);
                    case 'F': return new JavaClass(Float.TYPE);
                    case 'I': return new JavaClass(Integer.TYPE);
                    case 'J': return new JavaClass(Long.TYPE);
                    case 'S': return new JavaClass(Short.TYPE);
                    case 'V': return new JavaClass(Void.TYPE);
                    case 'Z': return new JavaClass(Boolean.TYPE);
                    case 'L':
                        try {
                            return new JavaClass(Class.forName(n.substring(1, n.length()-1).replace('/', '.'), false, cp));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not find class "+n+" for parent lookup!", e);
                        }
                    case '[':
                        try {
                            return new JavaClass(Class.forName(n.replace('/', '.'), false, cp));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Could not find class "+n+" for parent lookup!", e);
                        }
                }
            }
            return t;
        });
    }

    public Optional<String> parentName(String className, String methodName, String methodSignature){
        ClassRep cp = lookup(className);
        Optional<String> result = Optional.empty();
        if(cp.superClass() != null)
            result = walkDependencyTree(cp.superClass(), methodName, methodSignature);
        return result.or(
                ()->cp.superInterfaces().stream().flatMap(p -> walkDependencyTree(p, methodName, methodSignature).stream()).findAny());
    }

    public Optional<String> walkDependencyTree(String c, String name, String sig){
        ClassRep cp = lookup(c);
        if(cp.isFinal()) return Optional.empty();
        for(MethodRep m : cp.getMethods()){
            if(m.getNotchName().equals(name) && m.getNotchianSignature().equals(sig)){
                if(m.isPrivate())
                    continue;
                if(m.isFinal())
                    throw new RuntimeException("Method extends final method!");
                return Optional.of(m.getSrgName());
            }
        }
        Optional<String> result=Optional.empty();
        if(cp.superClass() != null)
            result = walkDependencyTree(cp.superClass(), name, sig);
        return result.or(
                ()->cp.superInterfaces().stream().flatMap(p -> walkDependencyTree(p, name, sig).stream()).findAny());
    }

}
