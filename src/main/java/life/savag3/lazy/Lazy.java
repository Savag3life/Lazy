package life.savag3.lazy;

import com.google.gson.GsonBuilder;
import life.savag3.lazy.asm.ClassExplorer;
import life.savag3.lazy.gson.adaptors.PatternAdaptor;
import life.savag3.lazy.utils.DiskUtils;
import life.savag3.lazy.utils.Persist;
import lombok.Getter;
import lombok.SneakyThrows;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

public class Lazy {

    private final HashMap<String, byte[]> results = new HashMap<>();
    private final JarFile jar;
    private final File output, original;

    @Getter private final Persist persist;
    @Getter private final DiskUtils disk;

    @Getter private final ExecutorService async = Executors.newFixedThreadPool(4);

    @Getter private final AtomicInteger fieldCount = new AtomicInteger(0);
    @Getter private final AtomicInteger methodCount = new AtomicInteger(0);
    @Getter private final AtomicInteger classCount = new AtomicInteger(0);
    @Getter private final Instant start;

    public static Lazy instance;

    @SneakyThrows
    public Lazy(String[] args) {
        instance = this;
        start = Instant.now();

        this.disk = new DiskUtils();
        this.persist = new Persist(
                new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .registerTypeAdapter(Pattern.class, new PatternAdaptor())
                        .serializeNulls()
                        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
                        .create(),
                this.disk
        );

        // Debug Paths
        System.out.println("Canonical Path - " + new java.io.File(".").getCanonicalPath());
        System.out.println("Working Dir - " + new File("").getAbsolutePath());
        System.out.println(" ");

        if (args.length < 2) {
            System.out.println("Invalid argument counts. Found " + args.length + ", Required 2");
            System.out.println("Proper usage <> req, [] opt: java -jar <Path/To/Jar.jar> [Path/To/Config.txt] ");
            System.exit(1);
        }

        if (args.length == 3) {
            System.out.println("Reading Config... (" + args[2] + ")");
            Config.load(args[2]);
        } else Config.load();

        for (String package0 : Config.BLACKLISTED_PACKAGES) {
            System.out.println("Skipping packages matching: " + package0);
        }
        System.out.println(" ");

        for (Pattern pattern : Config.EXEMPT_STRING_PATTERNS) {
            System.out.println("Skipping strings matching: " + pattern.pattern());
        }

        this.original = new File(args[0]);
        System.out.println("Reading Jar... (" + original.getAbsolutePath() + ")");
        this.jar = new JarFile(original);
        this.output = new File(args[1]);

        if (!this.output.createNewFile()) {
            System.out.println("Couldn't create output file... exiting");
            System.exit(1);
        }

        for (Enumeration<JarEntry> list = jar.entries(); list.hasMoreElements(); ) {
            JarEntry clazz = list.nextElement();
            if (clazz.isDirectory()) continue;
            if (!clazz.getName().endsWith(".class")) continue;

            ClassExplorer navigator = new ClassExplorer();

            ClassReader reader = new ClassReader(jar.getInputStream(clazz));
            reader.accept(navigator, 0);
            navigator.visitEnd();
        }

        pack();
    }

    /**
     * Add byte that represent a class to the results map to be written to final jar.
     * @param pack The package & class name `life/savag3/example/Core.class`
     * @param bytes The bytes that are contained in the cleaned class.
     */
    public void add(String pack, byte[] bytes) {
        this.results.put(pack, bytes);
    }

    /**
     * Pack the results of the classes stored in results map into a new jar file.
     */
    @SneakyThrows
    public void pack() {
        System.out.println(" ");
        System.out.println(" Writing new Jar (" + this.output.getAbsolutePath() + ")");
        JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(this.output.getAbsolutePath())), this.jar.getManifest());

        for (Map.Entry<String, byte[]> pack : this.results.entrySet()) {
            if (Config.VERBOSE)  System.out.print(" .. Writing " + pack.getKey());
            JarEntry j = new JarEntry(pack.getKey());
            j.setSize(pack.getValue().length);
            jarOutputStream.putNextEntry(j);
            jarOutputStream.write(pack.getValue());
            jarOutputStream.closeEntry();
            if (Config.VERBOSE)  System.out.print(" ... Done\n");
        }
        jarOutputStream.flush();
        jarOutputStream.close();
        jar.close();

        System.out.println(" ");
        System.out.println("Jar saved to " + this.output.getAbsolutePath() + " in " + Duration.between(start, Instant.now()).toMillis() + "ms");
        System.out.println("Cleaned " + classCount.get() + " classes containing " + methodCount.get() + " methods & " + fieldCount + " static fields.");
        System.out.println("Original Size: " + this.original.length() + " bytes, New size: " + this.output.length() + " bytes; Size reduced by " + (Math.abs((1.0f - ((float) this.output.length() / (float) this.original.length()))) * 100.0f) + "%");
    }
}
