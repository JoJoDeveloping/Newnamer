package de.jojomodding.newnamer.hierarchy;

import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

//Toposort
public class DependencySorter {

    private Map<String, ClassNode> nodes;
    private Set<ClassNode> visiting = new HashSet<>(), visited = new HashSet<>();


    public DependencySorter(List<ClassNode> nodes){
        this.nodes = nodes.stream().collect(Collectors.toMap(n -> n.name, n->n));
    }

    public List<ClassNode> toposort(){
        List<ClassNode> result = new ArrayList<>(nodes.size());
        nodes.values().stream().forEach(n -> toposort(n, result));
        return result;
    }

    private void toposort(ClassNode node, List<ClassNode> l){
        if(!visiting.add(node)) throw new IllegalArgumentException("Cyclic superclasses!");
        if(!visited.add(node)){
            visiting.remove(node);
            return;
        }
        ClassNode cn = nodes.get(node.superName);
        if(cn != null)
            toposort(cn, l);
        node.interfaces.forEach(n -> {
            ClassNode c = nodes.get(n);
            if(c != null)
                toposort(c, l);
        });
        l.add(node);
        visiting.remove(node);
    }

}
