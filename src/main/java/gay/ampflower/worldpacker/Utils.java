package gay.ampflower.worldpacker;// Created 2022-28-09T05:10:24

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ampflower
 **/
public final class Utils {
    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final Logger logger = logger();

    public static final long KiB = 1024, MiB = KiB * KiB, GiB = KiB * MiB, TiB = KiB * GiB, PiB = KiB * TiB, EiB = KiB * PiB;
    private static final int iKB = 1000, iKiB = 1024;

    public static String displaySize(long bytes) {
        if (bytes >= EiB) return buildDisplaySize(bytes / PiB, " EiB");
        if (bytes >= PiB) return buildDisplaySize(bytes / TiB, " PiB");
        if (bytes >= TiB) return buildDisplaySize(bytes / GiB, " TiB");
        if (bytes >= GiB) return buildDisplaySize(bytes / MiB, " GiB");
        if (bytes >= MiB) return buildDisplaySize(bytes / KiB, " MiB");
        if (bytes >= KiB) return buildDisplaySize(bytes, " KiB");
        return bytes + " bytes";
    }

    private static String buildDisplaySize(long bytes, String scaleName) {
        int b0 = (int) bytes * iKB / iKiB;
        int i = b0 / iKB, d = b0 % iKB;
        if (d == 0) return i + ".0" + scaleName;
        while (d % 10 == 0) d /= 10;
        return i + prefixString(d) + d + scaleName;
    }

    private static String prefixString(int d) {
        if (d < 10) return ".00";
        if (d < 100) return ".0";
        return ".";
    }

    public static Logger logger() {
        return LoggerFactory.getLogger(walker.getCallerClass());
    }

    public static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException i) {
            logger.warn("Why did you interrupt me?", i);
        }
    }
}
