package de.jojomodding.newnamer;

import de.jojomodding.newnamer.hierarchy.DependencySorter;
import de.jojomodding.newnamer.tsrg.parser.Parser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
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
        OptionSpec<File> in = op.acceptsAll(List.of("inmappings", "in", "i"), "The partial mappings for this jar").withRequiredArg().required().ofType(File.class);
        OptionSpec<File> out = op.acceptsAll(List.of("outmappings", "out", "o"), "The resulting mappings, including classes named by this").withRequiredArg().required().ofType(File.class);
        op.accepts("v", "Be verbose");
        op.acceptsAll(List.of("help", "h", "?"), "Print help").forHelp();
        OptionSet os = op.parse(args);

        File inm = in.value(os), outm = out.value(os);
        System.out.println("Reading tsrg!");

        URLClassLoader cpLoader = new URLClassLoader(os.valuesOf(cp).stream().map(f -> {
            try {
                return f.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new));

        RenamingVisitor ncv = new RenamingVisitor(cpLoader, new Parser().parse(Files.lines(inm.toPath())), os.has("v"));
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
    }

}
