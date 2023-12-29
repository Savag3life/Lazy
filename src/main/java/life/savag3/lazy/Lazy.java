package life.savag3.lazy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Main class for the program. Handles checking arguments, config loading, and acts as a central storage
 * class for the program to pass data around. Also handles the actual processing of the input jar & writing the output jar.
 *
 * @author Jacob C (Savag3life)
 * @since 2023-09-04
 */
public class Lazy {

    private final File output; // Output jar

    private File originalFile; // Original input jar
    private JarFile originalJarFile; // The opened input jar

    private final HashMap<String, byte[]> resultClassMap = new HashMap<>();

    @Getter private final Gson gson;
    @Getter private final Instant start;

    public Lazy(String input, String output, String config) {
        start = Instant.now();

        this.gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .serializeNulls()
                        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
                        .create();

        // Print working paths for debugging and testing
        System.out.println("Working Dir - " + new File("").getAbsolutePath());

        // Load config
        loadOrCreateConfig(config);

        // Load input jar
        attemptLoadInput(input);

        this.output = new File(output);

        try {
            this.output.createNewFile();
        } catch (IOException er) {
            System.out.println("Couldn't create output file... exiting");
            er.printStackTrace();
            System.exit(1);
            return;
        }

        System.out.println(" ");

        // Enumerate over jarfile entries
        for (Enumeration<JarEntry> list = originalJarFile.entries(); list.hasMoreElements(); ) {
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
                    add(clazz.getName(), originalJarFile.getInputStream(clazz).readAllBytes());
                } else {
                    LazyClassTransformer transformer = new LazyClassTransformer(originalJarFile.getInputStream(clazz).readAllBytes());
                    add(clazz.getName(), transformer.transform());
                }
            } catch (Exception e) {
                System.out.println("Failed while processing class: " + clazz.getName());
                if (Config.VERBOSE) e.printStackTrace();
                System.out.println("Skipping class...");
            }
        }

        // Pack & write the output jar
        pack();
    }

    /**
     * Attempt to load a config file if provided via the command line.
     * If the file doesn't exist, create a new config file.
     *
     * @param config The path to the config file.
     */
    private void loadOrCreateConfig(String config) {
        if (config != null) {
            // Read the config file from disk if it exists
            System.out.println("Reading Config... (" +  config + ")");
            Config.load(this, config);
        } else {
            // Create a new config file if one doesn't exist
            Config.load(this);
        }
    }

    /**
     * Attempt to load the input jar provided via the command line.
     * If the file doesn't exist, exit the program.
     *
     * @param inputFile The path to the input jar file.
     */
    private void attemptLoadInput(String inputFile) {
        this.originalFile = new File(inputFile);

        if (!this.originalFile.exists()) {
            System.out.println("Input file doesn't exist... exiting");
            System.exit(1);
            return;
        }

        System.out.println("Reading Jar... (" + originalFile.getAbsolutePath() + ")");
        try {
            this.originalJarFile = new JarFile(originalFile);
        } catch (IOException e) {
            System.out.println("Failed to read jar file. (" + originalFile.getAbsolutePath() + ")");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Add byte that represent a class to the results map to be written to final jar.
     * @param pack The package & class name `life/savag3/example/Core.class`
     * @param bytes The bytes that are contained in the cleaned class.
     */
    public void add(String pack, byte[] bytes) {
        this.resultClassMap.put(pack, bytes);
    }


    /**
     * Pack the results of the classes stored in results map into a new jar file.
     */
    public void pack() {
        System.out.println(" ");
        System.out.println("Writing new Jar (" + this.output.getAbsolutePath() + ")");

        JarOutputStream jarOutputStream;
        try {
            jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(this.output.getAbsolutePath())), this.originalJarFile.getManifest());
        } catch (IOException e) {
            System.out.println("Failed to write jar file. (" + this.output.getAbsolutePath() + ")");
            System.out.println("Failed to create JarOutputStream object.");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        try {
            for (Map.Entry<String, byte[]> pack : this.resultClassMap.entrySet()) {
                if (Config.VERBOSE) System.out.print(" .. Writing " + pack.getKey());
                JarEntry j = new JarEntry(pack.getKey());
                j.setSize(pack.getValue().length);
                jarOutputStream.putNextEntry(j);
                jarOutputStream.write(pack.getValue());
                jarOutputStream.closeEntry();
                if (Config.VERBOSE) System.out.print(" ... Done\n");
            }
        } catch (IOException e) {
            System.out.println("Failed to write jar file. (" + this.output.getAbsolutePath() + ")");
            System.out.println("Failed to write entry to jar file.");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        try {
            jarOutputStream.flush();
            jarOutputStream.close();
            originalJarFile.close();
        } catch (IOException e) {
            System.out.println("Failed to write jar file. (" + this.output.getAbsolutePath() + ")");
            System.out.println("Failed to close JarOutputStream object.");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        System.out.println(" ");
        System.out.println("Jar saved to " + this.output.getAbsolutePath() + " in " + Duration.between(start, Instant.now()).toMillis() + "ms");
        System.out.printf(
                "Original Size: %d bytes, New size: %d bytes; Size reduced by %.2f%%\n",
                this.originalFile.length(),
                this.output.length(),
                Math.abs((1.0D - ((double) this.output.length() / (double) this.originalFile.length()))) * 100.0D
        );
    }
}
