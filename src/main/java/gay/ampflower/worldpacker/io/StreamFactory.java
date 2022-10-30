package gay.ampflower.worldpacker.io;// Created 2022-28-09T04:04:25

import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public abstract class StreamFactory<H> {
    protected final Deque<H> freeHashers = new ConcurrentLinkedDeque<>();
    private final AlgorithmProvider<H> hasherSupplier;
    private final Consumer<H> reset;

    public StreamFactory(AlgorithmProvider<H> hasherSupplier, Consumer<H> reset) throws NoSuchAlgorithmException {
        this.hasherSupplier = hasherSupplier;
        this.reset = reset;

        // Check that the supplier is valid
        freeHashers.push(hasherSupplier.get());
    }

    protected H getHasher() {
        var digest = Objects.requireNonNullElseGet(freeHashers.poll(), hasherSupplier::getOrThrow);
        reset.accept(digest);
        return digest;
    }
}
