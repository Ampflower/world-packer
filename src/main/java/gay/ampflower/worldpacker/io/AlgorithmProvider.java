package gay.ampflower.worldpacker.io;

import java.security.NoSuchAlgorithmException;

@FunctionalInterface
public interface AlgorithmProvider<T> {
    T get() throws NoSuchAlgorithmException;

    default T getOrThrow() {
        try {
            return get();
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError("Provider now returns errors", nsae);
        }
    }
}