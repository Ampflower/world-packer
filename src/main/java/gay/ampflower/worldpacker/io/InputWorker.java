package gay.ampflower.worldpacker.io;

import gay.ampflower.worldpacker.Data;
import gay.ampflower.worldpacker.Holder;
import gay.ampflower.worldpacker.Sha256HashHolder;
import gay.ampflower.worldpacker.Utils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Ampflower
 **/
public final class InputWorker {
	private static final Logger logger = Utils.logger();

	private final Path root;

	private final boolean cacheInput;
	private final int jobs;

	private final DigestStreamFactory digestFactory;
	private final ChecksumStreamFactory cksumFactory;

	public final Map<Sha256HashHolder, Holder> map = new ConcurrentHashMap<>();

	private final StopWatch stopwatch;

	// Stats counters
	private final AtomicInteger nonRegularCount = new AtomicInteger();
	private final AtomicInteger errorCount = new AtomicInteger();

	public final AtomicInteger uniqueCount = new AtomicInteger();
	public final AtomicLong uniqueSize = new AtomicLong();

	public final AtomicInteger duplicatedCount = new AtomicInteger();
	public final AtomicLong duplicatedSize = new AtomicLong();

	public InputWorker(
			final Path root,
			final boolean cacheInput,
			final int jobs,
			final DigestStreamFactory digestFactory,
			final ChecksumStreamFactory cksumFactory,
			final StopWatch stopwatch
	) {
		this.root = root;
		this.cacheInput = cacheInput;
		this.jobs = jobs;
		this.digestFactory = digestFactory;
		this.cksumFactory = cksumFactory;
		this.stopwatch = stopwatch;
	}

	private void work0(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			this.nonRegularCount.incrementAndGet();
			return;
		}

		long size = Files.size(path);
		if (size == 0L) {
			return;
		}

		try (
				final var input = Files.newInputStream(path);
				final var digest = digestFactory.of(input);
				final var cksum = cksumFactory.of(digest.inputStream())
		) {

			final Data data;

			if (this.cacheInput) {
				data = Data.storeBytes(path, cksum.inputStream().readAllBytes());
			} else {
				data = Data.storePath(path);
				cksum.inputStream().transferTo(OutputStream.nullOutputStream());
			}

			var hash = new Sha256HashHolder(digest.digest().digest());
			var set = map.computeIfAbsent(hash, $ -> new Holder(cksum.checksum().getValue(), size, data));

			path = root.relativize(path);

			if (set.paths().isEmpty()) {
				this.uniqueCount.incrementAndGet();
				this.uniqueSize.addAndGet(size);
				logger.debug("{} {}", hash, path);
			} else {
				var crc = cksum.checksum().getValue();
				if (set.crc32() != crc)
					throw new AssertionError(path + " had mismatched CRC " + Long.toHexString(crc) + ", expected " + Long.toHexString(set.crc32()) + "; Existing entries: " + set.paths());
				if (set.size() != size)
					throw new AssertionError(path + " had mismatched size " + size + ", expected " + set.size() + "; Existing entries: " + set.paths());

				if (this.cacheInput && !data.equals(set.data())) {
					throw new AssertionError(path + " had a hash collision with " + set.paths());
				}

				this.duplicatedCount.incrementAndGet();
				this.duplicatedSize.addAndGet(size);
				logger.debug("=== {} -> {}", path, set);
			}
			set.paths().add(path);
		}
	}

	public void work(final Path path) {
		try {
			work0(path);
		} catch (final IOException e) {
			this.errorCount.incrementAndGet();
			logger.error("Cannot read {}", path, e);
		}
	}

	public InputWorker digest() {
		final var semaphore = new Semaphore(jobs);

		try (final var stream = Files.walk(root)) {
			final var list = stream.map(path -> {
				semaphore.acquireUninterruptibly();
				return Thread.startVirtualThread(new Job(this, path, semaphore));
			}).toList();

			list.forEach(Utils::join);
		} catch (IOException e) {
			logger.error("Cannot read {}", root, e);
		}

		return this;
	}

	public int totalFiles() {
		return this.uniqueCount.get() + this.duplicatedCount.get();
	}

	public long totalSize() {
		return this.uniqueSize.get() + this.duplicatedSize.get();
	}

	public void log() {
		logger.info("{} => Digested {} ({}) total, {} ({}) unique, {} ({}) duplicates",
				this.stopwatch,
				this.totalFiles(), Utils.displaySize(this.totalSize()),
				this.uniqueCount.get(), Utils.displaySize(this.uniqueSize.get()),
				this.duplicatedCount.get(), Utils.displaySize(this.duplicatedSize.get())
		);
	}

	private record Job(InputWorker worker, Path path, Semaphore semaphore) implements Runnable {
		@Override
		public void run() {
			worker.work(path);
			semaphore.release();
		}
	}
}
