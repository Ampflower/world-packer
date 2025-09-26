package gay.ampflower.worldpacker.archiver;

import gay.ampflower.worldpacker.Holder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Ampflower
 **/
public interface Archiver<I extends InputStream, O extends OutputStream, E> {
	Set<E> toEntries(final Holder holder);

	O wrapOutputStream(final OutputStream output) throws IOException;

	void writeHolderAsEntries(final O output, final Path root, final Holder holder) throws IOException;

	default void archive(
			final OutputStream outputStream,
			final Path root,
			final Collection<Holder> holders
	) throws IOException {
		try (outputStream;
			 final var zip = this.wrapOutputStream(outputStream);
			 final var stream = holders.stream()) {

			final var itr = stream.sorted(Comparator.comparingLong(Holder::size)).iterator();
			while (itr.hasNext()) {
				this.writeHolderAsEntries(zip, root, itr.next());
			}
		}
	}

	interface Apache<I extends ArchiveInputStream<E>, O extends ArchiveOutputStream<E>, E extends ArchiveEntry>
			extends Archiver<I, O, E> {

		@Override
		default void writeHolderAsEntries(
				final O output,
				final Path root,
				final Holder holder
		) throws IOException {
			final byte[] array = Files.readAllBytes(root.resolve(holder.paths().iterator().next()));

			for (final var entry : this.toEntries(holder)) {
				output.putArchiveEntry(entry);
				output.write(array);
				output.closeArchiveEntry();
			}
		}
	}

	final class Ar implements Apache<ArArchiveInputStream, ArArchiveOutputStream, ArArchiveEntry> {
		@Override
		public Set<ArArchiveEntry> toEntries(final Holder holder) {
			return holder.paths()
					.stream()
					.map(path -> new ArArchiveEntry(
							path.toString(),
							holder.size()
					))
					.collect(Collectors.toSet());
		}

		@Override
		public ArArchiveOutputStream wrapOutputStream(final OutputStream output) throws IOException {
			return new ArArchiveOutputStream(output);
		}
	}

	final class Cpio implements Apache<CpioArchiveInputStream, CpioArchiveOutputStream, CpioArchiveEntry> {
		@Override
		public Set<CpioArchiveEntry> toEntries(final Holder holder) {
			return holder.paths()
					.stream()
					.map(path -> new CpioArchiveEntry(
							path.toString(),
							holder.size()
					))
					.collect(Collectors.toSet());
		}

		@Override
		public CpioArchiveOutputStream wrapOutputStream(final OutputStream output) throws IOException {
			return new CpioArchiveOutputStream(output);
		}
	}

	final class Tar implements Apache<TarArchiveInputStream, TarArchiveOutputStream, TarArchiveEntry> {
		@Override
		public Set<TarArchiveEntry> toEntries(final Holder holder) {
			return holder.paths()
					.stream()
					.map(path -> new TarArchiveEntry(
							path.toString()
					))
					.peek(tar -> {
						tar.setSize(holder.size());
					})
					.collect(Collectors.toSet());
		}

		@Override
		public TarArchiveOutputStream wrapOutputStream(final OutputStream output) throws IOException {
			final var tar = new TarArchiveOutputStream(output);
			tar.setAddPaxHeadersForNonAsciiNames(true);
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			return tar;
		}
	}

	final class Zip implements Apache<ZipArchiveInputStream, ZipArchiveOutputStream, ZipArchiveEntry> {
		@Override
		public Set<ZipArchiveEntry> toEntries(final Holder holder) {
			return holder.paths()
					.stream()
					.map(path -> new ZipArchiveEntry(
							path.toString()
					))
					.peek(zip -> {
						zip.setCrc(holder.crc32());
						zip.setSize(holder.size());
						zip.setCompressedSize(holder.size());
					})
					.collect(Collectors.toSet());
		}

		@Override
		public ZipArchiveOutputStream wrapOutputStream(final OutputStream output) throws IOException {
			final var stream = new ZipArchiveOutputStream(output);
			stream.setLevel(ZipArchiveOutputStream.STORED);
			return stream;
		}
	}

	final class JavaZip implements Archiver<ZipInputStream, ZipOutputStream, ZipEntry> {

		@Override
		public Set<ZipEntry> toEntries(final Holder holder) {
			return holder.paths()
					.stream()
					.map(path -> new ZipEntry(path.toString()))
					.peek(zip -> {
						zip.setCrc(holder.crc32());
						zip.setSize(holder.size());
						zip.setCompressedSize(holder.size());
					})
					.collect(Collectors.toSet());
		}

		@Override
		public ZipOutputStream wrapOutputStream(final OutputStream output) throws IOException {
			return new ZipOutputStream(output);
		}

		@Override
		public void writeHolderAsEntries(
				final ZipOutputStream output,
				final Path root,
				final Holder holder
		) throws IOException {
			final byte[] array = Files.readAllBytes(root.resolve(holder.paths().iterator().next()));

			for (final var entry : this.toEntries(holder)) {
				output.putNextEntry(entry);
				output.write(array);
				output.closeEntry();
			}
		}
	}
}
