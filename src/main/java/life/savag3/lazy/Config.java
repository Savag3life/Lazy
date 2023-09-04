package life.savag3.lazy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class used to store the configuration for Lazy
 * This class is serialized to JSON and saved to the output jar as `config.json`
 *
 * @author Jacob C (Savag3life)
 * @since 2023-09-04
 */
public class Config {

    public static transient Config instance = new Config();

    // Packages exempt from being stripped by Lazy (Essentially Skipping & adding to output jar)
    // Format: package0/package1/package2/* - Exempt anything inside this package
    // Format: package0/*/package2 - Exempt anything with the root path `package0` and a sub package `package2`
    public static List<String> EXEMPT = new ArrayList<>();

    // Packages to be excluded from the output (These packages / classes are not included in the output jar)
    // Format: package0/package1/package2/* - Exclude anything inside this package
    // Format: package0/*/package2 - Exclude anything with the root path `package0` and a sub package `package2`
    public static List<String> EXCLUDE = new ArrayList<>();

    // Should Lazy include fields marked as `public static`
    public static boolean INCLUDE_PUB_STATIC_FIELDS = true;
    // Should Lazy include fields marked as `private static`
    public static boolean INCLUDE_PRI_STATIC_FIELDS = true;

    // Should Lazy include fields marked as non-static `public`
    public static boolean INCLUDE_PUB_NON_STATIC_FIELDS = true;
    // Should Lazy include fields marked as non-static `private`
    public static boolean INCLUDE_PRI_NON_STATIC_FIELDS = true;

    // Should Lazy include emptied methods which are marked as `private`
    public static boolean INCLUDE_PRIVATE_METHODS = false;
    // Should Lazy include emptied methods which are marked as `native`
    // This is not recommended unless you want to include a native method class as an API method.
    // Native methods are very slow and shouldn't be called unless you know what you're doing.
    public static boolean INCLUDE_NATIVE_METHODS = false;

    // Methods annotated with any of these annotations will be included in the output jar
    // no matter the other configuration options listed.
    public static List<String> RETENTION_ANNOTATIONS = Arrays.asList(
            "me.savag3.gucci.common.annotations.RetainAPI"
    );

    // Should Lazy print verbose output to the console
    public static boolean VERBOSE = true;

    static {
        EXCLUDE.add("org/reflections/*");
        EXCLUDE.add("org/sfl4j/*");

        EXEMPT.add("org/spongepowered/*");
    }

    public static void load(Lazy lazy, String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {

                System.out.println("Failed to load config file: " + path);
                System.out.println("Using & writing default config file.");
                file.createNewFile();
                Files.writeString(file.toPath(), lazy.getGson().toJson(instance));

            } else {

                String content = Files.readString(new File(path).toPath());
                if (content == null || content.isEmpty()) {
                    System.out.println("Found config but it was empty: " + path);
                    System.out.println("Overwriting with default config.");
                    Files.writeString(file.toPath(), lazy.getGson().toJson(instance));
                    return;
                }

                instance = lazy.getGson().fromJson(content, Config.class);
            }
        } catch (IOException er) {
            System.out.println("Failed to load config file: " + path);
            er.printStackTrace();
        }
    }

    public static void load(Lazy lazy) {
        load(lazy, "config.json");
    }
}
