package gay.ampflower.worldpacker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author Ampflower
 **/
public interface Data {
	byte[] toArray() throws IOException;

	static Data storePath(final Path path) throws IOException {
		return new PathEntry(path);
	}

	static Data storeBytes(final Path path) throws IOException {
		return new RawEntry(path, Files.readAllBytes(path));
	}

	static Data storeBytes(final Object source, final byte[] bytes) {
		return new RawEntry(source, bytes);
	}

	record PathEntry(Path path) implements Data {
		@Override
		public byte[] toArray() throws IOException {
			return Files.readAllBytes(path);
		}
	}

	record RawEntry(Object source, @Override byte[] toArray) implements Data {

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof RawEntry raw)) {
				return false;
			}
			return Arrays.equals(toArray(), raw.toArray());
		}

		@Override
		public String toString() {
			return "Raw[source=" + source + ", storing " + Utils.displaySize(toArray.length) + "]";
		}
	}
}
