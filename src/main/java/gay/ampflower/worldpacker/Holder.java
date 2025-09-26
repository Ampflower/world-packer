package gay.ampflower.worldpacker;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Ampflower
 **/
public record Holder(long crc32, long size, Data data, Set<Path> paths) {
	public Holder(long crc32, long size, Data data) {
		this(crc32, size, data, new ConcurrentSkipListSet<>());
	}
}
