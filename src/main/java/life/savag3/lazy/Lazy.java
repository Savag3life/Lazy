package life.savag3.lazy;

import com.google.gson.GsonBuilder;
import life.savag3.lazy.asm.LazyClassTransformer;
import life.savag3.lazy.utils.PackageUtils;
import life.savag3.lazy.utils.Persist;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Lazy {

    public static Lazy instance;

    // Results Map to produce output jar <FullClassNameAndPackage, ClassContentAsByteArray>
    private final HashMap<String, byte[]> results = new HashMap<>();
    // JarFile to read classes from
    private final JarFile jar;
    // Output jar to create & populate, jar to read from.
    private final File output, original;
    @Getter private final Persist persist;
    @Getter private final Instant start;

    @SneakyThrows
    public Lazy(String[] args) {
        instance = this;
        start = Instant.now();

        this.persist = new Persist(
                new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .serializeNulls()
                        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
                        .create()
        );

        // Print working paths for debugging and testing
        System.out.println("Canonical Path - " + new java.io.File(".").getCanonicalPath());
        System.out.println("Working Dir - " + new File("").getAbsolutePath());
        System.out.println(" ");

        // Parse args
        if (args.length < 2) {
            System.out.println("Invalid argument counts. Found " + args.length + ", Required 2");
            System.out.println("Usage: java -jar <Path/To/Input.jar> <Path/To/Output.jar> [Path/To/Config.json]");
            System.out.println("Map: `<>` fields are required, `[]` fields are optional");
            System.exit(1);
        }

        if (args.length == 3) {
            // Read the config file from disk if it exists
            System.out.println("Reading Config... (" + args[2] + ")");
            Config.load(args[2]);
        } else {
            // Create a new config file if one doesn't exist
            Config.load();
        }

        this.original = new File(args[0]);
        System.out.println("Reading Jar... (" + original.getAbsolutePath() + ")");
        this.jar = new JarFile(original);
        this.output = new File(args[1]);

        if (!this.output.createNewFile()) {
            System.out.println("Couldn't create output file... exiting");
            System.exit(1);
        }

        // Enumerate over jarfile entries
        for (Enumeration<JarEntry> list = jar.entries(); list.hasMoreElements(); ) {
            JarEntry clazz = list.nextElement();
            if (clazz.isDirectory()) continue;
            // We only care about class files.
            if (!clazz.getName().endsWith(".class")) continue;
            try {
                // Check if a class is excluded | true ? skip : process
                if (PackageUtils.isExcluded(clazz.getName())) continue;
                // Check if a class is exempt | true ? write whole class to output : write stripped class to output
                System.out.println("Processing " + clazz.getName());
                if (PackageUtils.isExempt(clazz.getName())) {
                    add(clazz.getName(), jar.getInputStream(clazz).readAllBytes());
                } else {
                    LazyClassTransformer transformer = new LazyClassTransformer(jar.getInputStream(clazz).readAllBytes());
                    add(clazz.getName(), transformer.transform());
                }
            } catch (Exception e) {
                System.out.println("Failed to read class: " + clazz.getName() + " - Class is compiled on a unsupported version of Java");
                if (Config.VERBOSE) e.printStackTrace();
                System.out.println("Skipping class...");
            }
        }

        // Pack & write the output jar
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
        System.out.println("Original Size: " + this.original.length() + " bytes, New size: " + this.output.length() + " bytes; Size reduced by " + (Math.abs((1.0f - ((float) this.output.length() / (float) this.original.length()))) * 100.0f) + "%");
    }
}
