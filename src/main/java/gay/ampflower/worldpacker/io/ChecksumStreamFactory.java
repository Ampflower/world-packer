package gay.ampflower.worldpacker.io;// Created 2022-28-09T04:10:19

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class ChecksumStreamFactory extends StreamFactory<Checksum> {

    public ChecksumStreamFactory(final AlgorithmProvider<Checksum> hasherSupplier) throws NoSuchAlgorithmException {
        super(hasherSupplier, Checksum::reset);
    }

    public StreamChecksumPair of(InputStream stream) {
        var checksum = getHasher();
        return new StreamChecksumPair(new CheckedInputStream(stream, checksum), checksum);
    }

    public record StreamChecksumPair(CheckedInputStream inputStream, Checksum checksum) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}
