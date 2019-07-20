package de.jojomodding.newnamer;

import de.jojomodding.newnamer.hierarchy.DependencySorter;
import de.jojomodding.newnamer.tsrg.Tsrg;
import de.jojomodding.newnamer.tsrg.parser.Parser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Newnamer {

    public static void main(String[] args) throws IOException {
        OptionParser op = new OptionParser();
        OptionSpec<File> file = op.acceptsAll(List.of("rename", "jar", "f"), "A jar which has to-be-renamed classes").withRequiredArg().required().ofType(File.class);
        OptionSpec<File> cp = op.acceptsAll(List.of("classpath", "cp", "c"), "A jar which provides classes that might be extended by classes in renameable jars").withRequiredArg().ofType(File.class);
        OptionSpec<File> cpf = op.acceptsAll(List.of("libslist", "cf", "cpf", "l"), "A list of values that could be supplied to --cp, in the format generated by MCPCongig's fernflowerLibraries*").withRequiredArg().ofType(File.class);
        OptionSpec<File> in = op.acceptsAll(List.of("inmappings", "in", "i"), "The partial mappings for this jar").withRequiredArg().required().ofType(File.class);
        OptionSpec<File> out = op.acceptsAll(List.of("outmappings", "out", "o"), "The resulting mappings, including classes named by this").withRequiredArg().required().ofType(File.class);
        OptionSpecBuilder dumpin = op.acceptsAll(List.of("dumpinput", "z"), "Dumps the input tsrg in new reordered format. useful for diffing input and output");
        OptionSpecBuilder stats = op.acceptsAll(List.of("statistics", "stats"), "Show what changed between in and out srg");
        op.accepts("v", "Be verbose");
        op.acceptsAll(List.of("help", "h", "?"), "Print help").forHelp();
        OptionSet os = op.parse(args);

        File inm = in.value(os), outm = out.value(os);

        URL[] urls =
                Stream.concat(os.valuesOf(cp).stream().map(f -> {
                    try {
                        return f.toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }), os.valuesOf(cpf).stream().flatMap(f -> {
                    try {
                        return Files.lines(f.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(s -> s.startsWith("-e=")).map(s -> s.substring(3)).map(s -> {
                    try {
                        return new File(s).toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })).toArray(URL[]::new);
        URLClassLoader cpLoader = new URLClassLoader(urls);
        System.out.println("Set up classpath contains "+urls.length+" jars");

        System.out.println("Reading tsrg!");
        Tsrg tsrg = new Parser().parse(Files.lines(inm.toPath()));
        if(os.has(dumpin))
            try(PrintStream stream = new PrintStream(new FileOutputStream(inm.getPath()+"_sorted.tsrg"))) {
                tsrg.writeToStream(stream);
                System.out.println("Wrote input tsrg to "+inm.getPath()+"_sorted.tsrg");
            }
        RenamingVisitor ncv = new RenamingVisitor(cpLoader, tsrg, os.has("v"));
        if(ncv.verbose()) {
            System.out.println("Read " + ncv.getOldTsrg().getClasses().values().size() + " classes!");
            System.out.println("New names will start at "+(1+ncv.getOldTsrg().getNameOffset()));
        }
        List<ClassNode> classes = new DependencySorter(os.valuesOf(file).stream().flatMap(f -> {
            try {
                JarFile jf = new JarFile(f);
                return jf.stream().map(k -> Map.entry(k, jf));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(e -> {
            JarFile jf = e.getValue();
            JarEntry je = e.getKey();
            if(je.getRealName().endsWith(".class")){
                try {
                    InputStream is = jf.getInputStream(je);
                    ClassReader cr = new ClassReader(is);
                    ClassNode node = new ClassNode();
                    cr.accept(node, ClassReader.EXPAND_FRAMES);
                    is.close();
                    return Stream.of(node);
                } catch (IOException ex) {
                    System.err.println("Skipping "+je.getRealName()+":");
                    ex.printStackTrace();
                }
            }
            return Stream.empty();
        }).collect(Collectors.toList())).toposort();
        classes.forEach(n -> n.accept(ncv));
        classes.forEach(n -> n.accept(ncv.roundTwo(n)));
        System.out.println("Writing result to "+outm.getPath());
        ncv.getNewTsrg().writeToStream(new PrintStream(new FileOutputStream(outm)));
        if(os.has(stats)){
            System.out.println("Printing stats:");
            Util.compareTsrg(ncv.getOldTsrg(), ncv.getNewTsrg(), System.out);
        }
    }

}
