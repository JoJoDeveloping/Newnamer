package de.jojomodding.newnamer.type;

import de.jojomodding.newnamer.tsrg.NameLookup;

public class ClassType extends Type{

    private String fqn;

    public ClassType(String fqn){
        this.fqn = fqn;
    }

    /**
     * Gets the name in Lpackage/subpackage/ClassName; format
     * @return name in aforementioned format
     */

    public String fullyQualifiedName(){
        return "L"+fqn+";";
    }

    @Override
    public String format(NameLookup env) {
        return env.lookup(fqn).orElse(fullyQualifiedName());
    }
}
