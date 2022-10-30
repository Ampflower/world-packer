package gay.ampflower.worldpacker;// Created 2022-26-09T08:31:03

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Sha256HashHolder(long l1, long l2, long l3, long l4) implements Comparable<Sha256HashHolder> {
    private static final VarHandle BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    public Sha256HashHolder(byte[] hash) {
        this((long) BE.get(hash, 0), (long) BE.get(hash, 8), (long) BE.get(hash, 16), (long) BE.get(hash, 24));
    }

    public byte[] hash() {
        var arr = new byte[32];
        BE.set(arr, 0, l1);
        BE.set(arr, 8, l2);
        BE.set(arr, 16, l3);
        BE.set(arr, 24, l4);
        return arr;
    }

    @Override
    public String toString() {
        return String.format("%016x%016x%016x%016x", l1, l2, l3, l4);
    }

    @Override
    public int compareTo(final Sha256HashHolder o) {
        int c = Long.compareUnsigned(l1, o.l1);
        if (c == 0) c = Long.compareUnsigned(l2, o.l2);
        if (c == 0) c = Long.compareUnsigned(l3, o.l3);
        if (c == 0) c = Long.compareUnsigned(l4, o.l4);
        return c;
    }
}
