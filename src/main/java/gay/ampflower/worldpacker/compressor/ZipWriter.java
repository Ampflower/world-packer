package gay.ampflower.worldpacker.compressor;// Created 2022-11-09T22:33:07

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Class dedicated to writing a deduplicated zip.
 * <p>
 * Works in a similar manner as a zip-bomb with overlapping the records for the files,
 * but without any side effects with Deflate directly.
 *
 * @author Ampflower
 * @see <a href="https://pkware.cachefly.net/webdocs/APPNOTE/APPNOTE-6.3.9.TXT">PKZip Format Specification</a>
 * @since ${version}
 **/
public class ZipWriter implements AutoCloseable {
    private static final int
            localFileHeaderSignature = 0x04034b50,
            centralFileHeaderSignature = 0x02014b50,
            digitalSignatureSignature = 0x05054b50;
    private static final short
            pkZipHeaderSignature = 0x4b50,
            versionNeededToExtract = 0x0;

    // Necessary VarHandles for writing a valid PKZip file.
    private static final VarHandle leIntHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle leShortHandle = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle beIntHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle beShortHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

    private static final ArrayList<ZipEntry> entries = new ArrayList<>();

    private final byte[] buffer = new byte[8192];
    private int index;

    private final OutputStream stream;
    private final IDeflater deflater;

    public ZipWriter(OutputStream stream, IDeflater deflater) {
        this.stream = stream;
        this.deflater = deflater;
    }

    @Override
    public void close() throws IOException {
    }

    private record ZipEntry(
            short gpBitFlag,
            short compressionMethod,
            short lastModFileTime,
            short lastModFileDate,
            // The following three are reused for the data descriptor
            int crc32,
            int compressedSize,
            int uncompressedSize,
            byte[] name,
            // Not for central directory
            int dataOffset,
            int locationOnDisc
    ) {
    }
}
