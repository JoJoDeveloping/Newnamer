package de.jojomodding.newnamer.tsrg;

import de.jojomodding.newnamer.tsrg.TsrgClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface NameLookup{
    Optional<String> lookup(String fqn);
}
