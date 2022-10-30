package gay.ampflower.worldpacker.io;// Created 2022-26-09T07:19:52

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class DigestStreamFactory extends StreamFactory<MessageDigest> {
    public DigestStreamFactory(AlgorithmProvider<MessageDigest> digestSupplier) throws NoSuchAlgorithmException {
        super(digestSupplier, MessageDigest::reset);
    }

    public StreamDigestPair of(InputStream stream) {
        var digest = getHasher();
        return new StreamDigestPair(new DigestInputStream(stream, digest), digest);
    }

    public record StreamDigestPair(DigestInputStream inputStream, MessageDigest digest) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}
