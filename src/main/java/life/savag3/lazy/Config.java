package life.savag3.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Config {

    public static transient Config instance = new Config();

    // Packages exempt from being stripped by Lazy
    public static List<String> BLACKLISTED_PACKAGES = new ArrayList<>();

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
    // Include abstract classes in the output
    public static boolean INCLUDE_ABSTRACT_CLASSES = false;
    // Include enum data in the output
    public static boolean INCLUDE_ENUM_DATA = true;

    // Do logging
    public static boolean VERBOSE = true;

    static {
        BLACKLISTED_PACKAGES.add("me/savag3/*/supreme/auth");
        BLACKLISTED_PACKAGES.add("life/savag3/*/supreme/auth");
        BLACKLISTED_PACKAGES.add("com/massivecraft/factions/supreme/auth/*");
        BLACKLISTED_PACKAGES.add("com/google/*");
        BLACKLISTED_PACKAGES.add("org/*");
        BLACKLISTED_PACKAGES.add("javax/*");
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
