package de.jojomodding.newnamer.type;

import de.jojomodding.newnamer.tsrg.NameLookup;

import java.util.Optional;

public abstract class Type {

    @Override
    public String toString() {
        return format(fqn -> Optional.empty());
    }

    public abstract String format(NameLookup env);
}
