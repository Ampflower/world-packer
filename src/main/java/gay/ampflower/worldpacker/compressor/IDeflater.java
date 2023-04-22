package gay.ampflower.worldpacker.compressor;// Created 2022-11-09T22:36:56

/**
 * @author Ampflower
 * @since ${version}
 **/
public abstract class IDeflater {
    public static final long libdeflateLimit = 1024 * 1024 * 32;

    public static IDeflater ofBlock() {
        return new LibDeflate();
    }

    public static StreamingDeflater ofStream() {
        return new ZipDeflate();
    }

    public interface StreamingDeflater {

    }

    private static final class LibDeflate extends IDeflater {
    }

    private static final class ZipDeflate extends IDeflater implements StreamingDeflater {

    }
}
