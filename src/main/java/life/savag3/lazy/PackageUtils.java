package life.savag3.lazy;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class PackageUtils {

    public static boolean isExempt(String package0) {
        if (Config.EXEMPT.isEmpty()) return false;
        return matchPatterns(Config.EXEMPT, package0);
    }

    public static boolean isExcluded(String package0) {
        if (Config.EXCLUDE.isEmpty()) return false;
        return matchPatterns(Config.EXCLUDE, package0);
    }

    private static boolean matchPatterns(List<String> targets, String package0) {
        top: for (String excluded : targets) {
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
