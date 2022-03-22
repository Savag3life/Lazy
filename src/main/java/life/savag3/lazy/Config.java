package life.savag3.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    // Include Public Static Fields in the output
    public static boolean INCLUDE_PUB_STATIC_FIELDS = true;
    // Include Private Static Fields in the output
    public static boolean INCLUDE_PRI_STATIC_FIELDS = true;
    // Include Public Fields in the output
    public static boolean INCLUDE_PUB_NON_STATIC_FIELDS = true;
    // Include Private Fields in the output
    public static boolean INCLUDE_PRI_NON_STATIC_FIELDS = true;
    // Include Public Methods in the output
    public static boolean INCLUDE_PRIVATE_METHODS = false;
    // Include Native Methods in the output
    public static boolean INCLUDE_NATIVE_METHODS = false;
    // Include abstract classes in the output
    public static boolean INCLUDE_ABSTRACT_CLASSES = false;
    // Include enum data in the output
    public static boolean INCLUDE_ENUM_DATA = true;

    // Do advanced logging
    public static boolean VERBOSE = true;

    static {
        EXCLUDE.add("package0/package1/package2/*");
        EXCLUDE.add("package0/*/package2");

        EXEMPT.add("package0/package1/package2/*");
        EXEMPT.add("package0/*/package2");
    }

    public static void load(String path) {
        Lazy.instance.getPersist().loadOrSaveDefault(instance, Config.class, new File(path));
    }

    public static void load() {
        Lazy.instance.getPersist().loadOrSaveDefault(instance, Config.class, "config");
    }

    public static void save() {
        Lazy.instance.getPersist().save(instance);
    }

}
