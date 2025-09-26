package gay.ampflower.worldpacker;// Created 2022-11-09T22:30:59

import gay.ampflower.worldpacker.io.ChecksumStreamFactory;
import gay.ampflower.worldpacker.io.DigestStreamFactory;
import gay.ampflower.worldpacker.io.InputWorker;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * @author Ampflower
 **/
@Command(
		name = "world-packer",
		mixinStandardHelpOptions = true,
		version = "World Packer 1.1.0",
		description = """
				Sorts then dumps an archive.
								
				By default, it'll read the current working directory, then dump it to stdout.
								
				At current, no advanced transforms that would allow data savings are applied."""
)
public final class Main implements Callable<Integer> {
	// Must be executed first.
	static {
		// avoid clobbering stdout when used with piping
		System.setOut(System.err);
	}

	private static final Logger logger = Utils.logger();

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(it -> {
		final var thread = new Thread(it);
		thread.setDaemon(true);
		return thread;
	});

	@Parameters(index = "0", description = "input", defaultValue = ".")
	private String input;

	@Parameters(index = "1", description = "output", defaultValue = "-")
	private String output;

	@Option(names = {"-a", "--archive"}, description = "Valid: ar, cpio, zip, tar", defaultValue = "zip")
	private Archive archive;

	@Option(names = {"-j", "--jobs"},
			description = "The amount of virtual threads to throw at reading.\n" +
						  "\n" +
						  "This does not affect how many writer threads are used, as writing cannot be parallelized.\n" +
						  "\n" +
						  "On a hard drive, it may be required to set this value to `1` for first read, or no file cache. " +
						  "Sequential reading may end up being far faster in that case, especially on older or laptop drives.\n" +
						  "\n" +
						  "If you'd like to witness what happens when a program doesn't properly rate limit its jobs, " +
						  "set this value to `-1`. You'll probably regret it.\n" +
						  "\n" +
						  "Defaults to the amount of threads available, multiplied by 4. (i.e., 4 -> 16, 20 -> 80)",
			defaultValue = "0")
	private int jobs;

	@Option(names = {"--sha256sum-export"}, description = "Export a sha256sum compatible file at a given location.")
	private Path sha256SumPath;

	@Option(names = {"--dry"}, description = "Dry run, don't write the file, only summarise.")
	private boolean dry;

	@Option(names = {"--i-have-the-memory-to-store-input"}, description = "Stores the files in memory to allow writing to be faster.")
	private boolean memoryHog;

	// I once did this and my computer rung for 15 minutes straight.
	// Would not recommend.
	@Option(names = {"--i-am-not-wired-into-a-bell"},
			description = "Forces writing to stdout regardless of if it is a terminal.\n" +
						  "\n" +
						  "Intended to avoid accidentally blasting potential megabytes to gigabytes of garbage to your terminal, " +
						  "which may lock up your system for a while if it spams bells."
	)
	private boolean forceStdout;

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main()).execute(args));
	}

	@Override
	public Integer call() throws NoSuchAlgorithmException, IOException {
		if (!forceStdout && !dry && "-".equals(output) && System.console() != null) {
			System.err.println("""
					It appears that I am plumbed to a terminal. Are you sure you want to do this?
					If you are really sure that you want to do this, or not actually piping to a terminal,
					pass the `--i-am-not-wired-into-a-bell` flag.
					                    
					If you are seeing this and have no idea how I work, pass `--help` instead.
					""");

			return -1;
		}

		if (this.jobs == 0) {
			this.jobs = Runtime.getRuntime().availableProcessors() * 4;
		} else if (this.jobs < 0) {
			this.jobs = Integer.MAX_VALUE;
		}

		logger.info("Using {} jobs to read in files.", this.jobs);

		final var stopwatch = StopWatch.createStarted();

		var digestFactory = new DigestStreamFactory(() -> MessageDigest.getInstance("SHA-256"));
		var cksumFactory = new ChecksumStreamFactory(CRC32::new);

		var root = Path.of(input);

		final InputWorker worker = new InputWorker(root, memoryHog, jobs, digestFactory, cksumFactory, stopwatch);

		final var workerReader = executor.scheduleAtFixedRate(
				worker::log,
				5,
				5,
				TimeUnit.SECONDS
		);

		worker.digest();

		stopwatch.stop();
		workerReader.cancel(false);

		logger.info("Done!");
		worker.log();

		if (sha256SumPath != null) {
			exportSha256Sum(sha256SumPath, worker.map);
		}

		if (dry) {
			return 0;
		}

		final OutputStream outputStream;
		if ("-".equals(this.output)) {
			outputStream = new FileOutputStream(FileDescriptor.out);
		} else {
			outputStream = Files.newOutputStream(Path.of(this.output));
		}

		final var counter = new AtomicInteger();
		final var countingStream = new CountingOutputStream(outputStream);

		final var scheduled = executor.scheduleAtFixedRate(
				() -> logger.info(
						"Committed {} files, streaming {} ({} bytes)",
						counter.get(),
						Utils.displaySize(countingStream.getByteCount()),
						countingStream.getByteCount()
				),
				5,
				5,
				TimeUnit.SECONDS
		);

		archive.toArchiver().archive(countingStream, root, worker.map.values(), counter);

		scheduled.cancel(false);

		logger.info(
				"Archive available. Written {} files, streaming {} ({} bytes)",
				counter.get(),
				Utils.displaySize(countingStream.getByteCount()),
				countingStream.getByteCount()
		);

		return 0;
	}

	private static void exportSha256Sum(final Path path, final Map<Sha256HashHolder, Holder> map) {
		try (final var writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			 final var formatter = new Formatter(writer);
			 final var stream = map.entrySet().stream()
		) {
			final var itr = stream.sorted(Map.Entry.comparingByKey()).iterator();

			while (itr.hasNext()) {
				final var e = itr.next();

				for (final var p : e.getValue().paths()) {
					formatter.format("%s  %s\n", e.getKey(), p);
				}
			}
		} catch (IOException io) {
			logger.warn("Cannot export sha256sum to {}", path, io);
			return;
		}

		logger.info("SHA dump available at {}", path);
	}
}
