package de.jojomodding.newnamer;

import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.TsrgClass;
import de.jojomodding.newnamer.tsrg.TsrgField;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Util {

    public static <T> Comparator<T> byLengthFirst(Function<T, String> keyExtractor){
        return (k1,k2)-> byLengthFirst(keyExtractor.apply(k1), keyExtractor.apply(k2));
    }

    public static int byLengthFirst(String s1, String s2){
        if(s1.length() < s2.length())
            return -1;
        else if(s1.length() > s2.length())
            return 1;
        return s1.compareTo(s2);
    }

    public static void compareTsrg(Tsrg old, Tsrg newt, PrintStream out){
        Set<String> deleted = new HashSet<>(old.getClasses().keySet()), news = new HashSet<>(newt.getClasses().keySet()), common = new HashSet<>(newt.getClasses().keySet());
        common.retainAll(deleted); //new intersect old
        deleted.removeAll(common);
        news.removeAll(common);
        deleted.forEach(s -> {
            out.println("Deleted class "+s+" - "+old.getClasses().get(s).getDeobfuscatedName()+":");
            old.getClasses().get(s).writeToStream(out);
        });
        news.forEach(s -> {
            out.println("Added class "+s+" - "+newt.getClasses().get(s).getDeobfuscatedName()+":");
            newt.getClasses().get(s).writeToStream(out);
        });
        common.forEach(s -> printClassChanges(s, old.getClasses().get(s), newt.getClasses().get(s), out));
    }

    private static void printClassChanges(String s, TsrgClass oldt, TsrgClass newt, PrintStream out) {
        Set<String> oldMethods = oldt.getTSRGMethods().stream().map(m -> m.getNotchName()+" "+m.getNotchianSignature()).collect(Collectors.toSet());
        Set<String> newMethods = newt.getTSRGMethods().stream().map(m -> m.getNotchName()+" "+m.getNotchianSignature()).collect(Collectors.toSet());
        Set<String> oldFields = oldt.getFields().stream().map(TsrgField::getNotchName).collect(Collectors.toSet());
        Set<String> newFields = newt.getFields().stream().map(TsrgField::getNotchName).collect(Collectors.toSet());
        Set<String> deleted = new HashSet<>(oldMethods), news = new HashSet<>(newMethods), common = new HashSet<>(newMethods);
        common.retainAll(deleted); //new intersect old
        deleted.removeAll(common);
        news.removeAll(common);
        boolean namePrinted = false;
        if(deleted.size() != 0 || news.size() != 0){
            out.println("Changed class "+s+" - "+oldt.getDeobfuscatedName()+":");
            namePrinted = true;
        }
        deleted.forEach(n -> out.println("\tDeleted method "+oldt.getTSRGMethods().stream().filter(m -> (m.getNotchName()+" "+m.getNotchianSignature()).equals(n)).
                findAny().map(m -> m.getNotchName()+" "+m.getNotchianSignature()+" - "+m.getSrgName()).orElse(n+" - not mapped?!")));
        news.forEach(n -> out.println("\tAdded method "+newt.getTSRGMethods().stream().filter(m -> (m.getNotchName()+" "+m.getNotchianSignature()).equals(n)).
                findAny().map(m -> m.getNotchName()+" "+m.getNotchianSignature()+" - "+m.getSrgName()).orElse(n+" - not mapped?!")));
        deleted = new HashSet<>(oldFields); news = new HashSet<>(newFields); common = new HashSet<>(newFields);
        common.retainAll(deleted); //new intersect old
        deleted.removeAll(common);
        news.removeAll(common);
        if(deleted.size() != 0 || news.size() != 0 && !namePrinted){
            out.println("Changed class "+s+" - "+oldt.getDeobfuscatedName()+":");
        }
        deleted.forEach(n -> out.println("\tDeleted field "+n+" - "+oldt.getFields().stream().filter(m -> m.getNotchName().equals(n)).
                findAny().map(TsrgField::getSrgName).orElse("not mapped?!")));
        news.forEach(n -> out.println("\tAdded field "+n+" - "+newt.getFields().stream().filter(m -> m.getNotchName().equals(n)).
                findAny().map(TsrgField::getSrgName).orElse("not mapped?!")));
    }

    private static boolean verbose;

    public static void setVerbosity(boolean verbosity){
        Util.verbose = verbosity;
    }

    public static void printVerbose(String s){
        if(verbose)
            System.out.println(s);
    }

}
