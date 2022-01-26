package life.savag3.lazy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Config {

    public static transient Config instance = new Config();

    public static List<String> BLACKLISTED_PACKAGES = new ArrayList<>();

    public static boolean INCLUDE_PUB_STATIC_FIELDS = true;
    public static boolean INCLUDE_PRI_STATIC_FIELDS = true;
    public static boolean INCLUDE_PUB_NON_STATIC_FIELDS = true;
    public static boolean INCLUDE_PRI_NON_STATIC_FIELDS = true;
    public static boolean INCLUDE_PRIVATE_METHODS = false;
    public static boolean INCLUDE_ABSTRACT_CLASSES = false;
    public static boolean INCLUDE_ENUM_DATA = true;

    public static List<Pattern> EXEMPT_STRING_PATTERNS = new ArrayList<>();

    public static boolean VERBOSE = true;

    static {
        BLACKLISTED_PACKAGES.add("me/savag3/*/supreme/auth");
        BLACKLISTED_PACKAGES.add("life/savag3/*/supreme/auth");
        BLACKLISTED_PACKAGES.add("com/massivecraft/factions/supreme/auth/*");
        BLACKLISTED_PACKAGES.add("com/google/*");
        BLACKLISTED_PACKAGES.add("org/*");
        BLACKLISTED_PACKAGES.add("javax/*");

        // ASM is fat & can't retain values at load time so its `null` or `` always.
        EXEMPT_STRING_PATTERNS.add(Pattern.compile("%%__.*([A-Za-z1-9])__%%"));
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
