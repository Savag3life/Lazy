package life.savag3.lazy;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Utility class for package related methods like checking if a package is exempt or excluded.
 *
 * @author Jacob C (Savag3life)
 * @since 2023-09-04
 */
@UtilityClass
public final class PackageUtils {

    /**
     * Checks if a package is exempt from the final output jar
     *
     * @param package0 The package to check
     * @return True if the package is exempt, false otherwise
     */
    public static boolean isExempt(String package0) {
        if (Config.EXEMPT.isEmpty()) return false;
        return matchPatterns(Config.EXEMPT, package0);
    }

    /**
     * Checks if a package is excluded from the final output jar
     *
     * @param package0 The package to check
     * @return True if the package is excluded, false otherwise
     */
    public static boolean isExcluded(String package0) {
        if (Config.EXCLUDE.isEmpty()) return false;
        return matchPatterns(Config.EXCLUDE, package0);
    }

    /**
     * Checks if a package matches any of the patterns
     *
     * @param targets The patterns to match
     * @param package0 The package to check
     * @return True if the package matches any of the patterns, false otherwise
     */
    private static boolean matchPatterns(List<String> targets, String package0) {
        top:
        for (String excluded : targets) {
            if (excluded.equals(package0)) return true;
            if (excluded.contains("*")) {
                if (excluded.endsWith("*")) {
                    if (package0.startsWith(excluded.substring(0, excluded.length() - 1))) return true;
                } else {
                    final String[] parts = excluded.split("\\*");
                    int currentIdx = 0;
                    for (String part : parts) {
                        int idx = package0.indexOf(part, currentIdx);
                        if (idx == -1) continue top;
                        currentIdx = idx + part.length();
                    }
                }
            }
        }
        return false;
    }
}
