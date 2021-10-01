package life.savag3.lazy.utils;

import life.savag3.lazy.Config;

public class PackageUtils {
    public static boolean isBlacklistedPackage(String package0) {
        for (String blackPack : Config.BLACKLISTED_PACKAGES) {
            if (blackPack.contains("*")) {

                if (blackPack.endsWith("*")) {
                    if (package0.startsWith(blackPack.substring(0, blackPack.length() - 1))) return true;
                    else continue;
                }

                String[] parts = blackPack.split("\\*");

                boolean all = true;
                for (String part : parts) {
                    all = package0.contains(part);
                    if (!all) break;
                }

                if (all) return true;
            }
        }
        return false;
    }
}
