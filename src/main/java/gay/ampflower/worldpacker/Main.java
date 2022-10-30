package gay.ampflower.worldpacker;// Created 2022-11-09T22:30:59

import gay.ampflower.worldpacker.io.ChecksumStreamFactory;
import gay.ampflower.worldpacker.io.DigestStreamFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        var digestFactory = new DigestStreamFactory(() -> MessageDigest.getInstance("SHA-256"));
        var cksumFactory = new ChecksumStreamFactory(CRC32::new);

        var root = args.length >= 1 ? Path.of(args[0]) : Path.of(".");
        // Digest map
        var map = new ConcurrentHashMap<Sha256HashHolder, Holder>();
        // Counters
        AtomicInteger i1 = new AtomicInteger(), i2 = new AtomicInteger();
        AtomicLong l1 = new AtomicLong(), l2 = new AtomicLong();

        try (var stream = Files.walk(root)) {
            stream.parallel().forEach(path -> {
                if (Files.isRegularFile(path)) try {
                    long size = Files.size(path);
                    if (size == 0L) return;
                    try (var digestPair = digestFactory.of(Files.newInputStream(path));
                         var cksumPair = cksumFactory.of(digestPair.inputStream())) {
                        cksumPair.inputStream().transferTo(OutputStream.nullOutputStream());
                        var hash = new Sha256HashHolder(digestPair.digest().digest());
                        var set = map.computeIfAbsent(hash, $ -> new Holder(cksumPair.checksum().getValue(), size));
                        path = root.relativize(path);
                        if (set.paths().isEmpty()) {
                            i1.incrementAndGet();
                            l1.addAndGet(size);
                            System.out.printf("%s  %s\n", hash, path);
                        } else {
                            var crc = cksumPair.checksum().getValue();
                            if (set.crc32() != crc)
                                throw new AssertionError(path + " had mismatched CRC " + Long.toHexString(crc) + ", expected " + Long.toHexString(set.crc32()) + "; Existing entries: " + set.paths);
                            if (set.size() != size)
                                throw new AssertionError(path + " had mismatched size " + size + ", expected " + set.size() + "; Existing entries: " + set.paths);
                            i2.incrementAndGet();
                            l2.addAndGet(size);
                            System.out.printf("=== %s -> %s\n", path, set);
                        }
                        set.paths().add(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        System.out.printf("Digested %d files, %d (%s) unique, %d (%s) duplicates\n", i1.get() + i2.get(),
                i1.get(), Utils.displaySize(l1.get()),
                i2.get(), Utils.displaySize(l2.get()));

        try (final var writer = Files.newBufferedWriter(Path.of("files.sha256"), StandardOpenOption.CREATE);
             final var formatter = new Formatter(writer);
             final var stream = map.entrySet().stream()) {
            final var itr = stream.sorted(Map.Entry.comparingByKey()).iterator();
            while (itr.hasNext()) {
                final var e = itr.next();
                formatter.format("%s  %s\n", e.getKey(), e.getValue());
            }
        }
        System.out.println("SHA dump available at files.sha256");

        try (final var outputStream = Files.newOutputStream(Path.of("files.zip"), StandardOpenOption.CREATE_NEW);
             final var zip = new ZipOutputStream(outputStream);
             final var stream = map.values().stream()) {
            // We'll just wrap this in ZSTD.
            zip.setMethod(ZipOutputStream.STORED);

            final var itr = stream.sorted(Comparator.comparingLong(Holder::size)).iterator();
            while (itr.hasNext()) {
                final var holder = itr.next();
                final byte[] array;
                try (var inputStream = Files.newInputStream(root.resolve(holder.paths.iterator().next()))) {
                    array = inputStream.readAllBytes();
                }
                for (final var p : holder.paths) {
                    final var entry = new ZipEntry(p.toString());
                    entry.setCrc(holder.crc32);
                    entry.setSize(holder.size);
                    entry.setCompressedSize(holder.size);
                    zip.putNextEntry(entry);
                    zip.write(array);
                }
                zip.closeEntry();
            }
        }

        System.out.println("ZIP available at files.zip");
    }

    private record Holder(long crc32, long size, Set<Path> paths) {
        private Holder(long crc32, long size) {
            this(crc32, size, new ConcurrentSkipListSet<>());
        }
    }
}
