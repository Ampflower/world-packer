package gay.ampflower.worldpacker;

import gay.ampflower.worldpacker.archiver.Archiver;

/**
 * @author Ampflower
 **/
public enum Archive {
	ar {
		@Override
		public Archiver<?, ?, ?> toArchiver() {
			return new Archiver.Ar();
		}
	},
	cpio {
		@Override
		public Archiver<?, ?, ?> toArchiver() {
			return new Archiver.Cpio();
		}
	},
	tar {
		@Override
		public Archiver<?, ?, ?> toArchiver() {
			return new Archiver.Tar();
		}
	},
	zip {
		@Override
		public Archiver<?, ?, ?> toArchiver() {
			return new Archiver.Zip();
		}
	},

	java_zip {
		@Override
		public Archiver<?, ?, ?> toArchiver() {
			return new Archiver.JavaZip();
		}
	};

	public abstract Archiver<?, ?, ?> toArchiver();
}
