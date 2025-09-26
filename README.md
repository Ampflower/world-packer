# World Packer

A basic sorting archiver.

*Obsoleted by [World Bundler](https://github.com/Ampflower/world-bundler)*

## Wait, but you updated this recently? Why?

Evidently, World Packer still has its uses with its naive, yet clever, approach.

## So, how do I use it then?

`java -jar world-packer.jar [-hV] [--dry] [-a=<archive>] [-j=<jobs>] <input> <output>`

- `-h` or `--help` for the full CLI arguments.
- `-V` or `--version` a probably neglected version string.
- `--dry` to not actually write anything. Implies `--i-am-not-wired-into-a-bell`.
- `-a` or `--archive` to set the archive, with valid options of `ar`, `cpio`, `tar`, `zip`
  - Defaults to `zip`
- `-j` or `--jobs` to limit reader thread count.
  - Defaults to thread count * 4.
- `<input>` is the input directory.
  - Defaults to `.`
- `<output>` is the output archive.
  - Defaults to standard output for convenience, as you'll likely want to chain a compressor right after.

### Wait, `--i-am-not-wired-into-a-bell`??

The flag allows you to dump directly into a terminal, or a pipe that behaves like a terminal.

There's also `--i-have-the-memory-to-store-input`, which as you might guess by the name,
will store all your input files in memory until it's ready to be written back to disk.
