package org.unigrid.janus.model.filesystem.memoryfs.linux;

import org.unigrid.janus.model.filesystem.memoryfs.VirtualDirectory;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualFile;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualAbstractPath;
import jakarta.enterprise.event.Event;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;
import org.unigrid.janus.model.service.api.MountFailureException;
import org.unigrid.janus.model.service.api.Mountable;
import org.unigrid.janus.model.signal.UsedSpace;

public class MemoryFS extends FuseFilesystemAdapterAssumeImplemented implements Mountable
{
	private final static String SEPARATOR = "/";

	private final Supplier<String> rootPathSupplier = () -> {
		final String systemUser = System.getProperty("user.name");
		final List<String> mount = List.of(systemUser, "unigrid");

		return Path.of("home", mount.toArray(new String[0])).toString();
	};

	private final VirtualDirectory rootDirectory = new VirtualDirectory(SEPARATOR, "", rootPathSupplier);
	private final Event<UsedSpace> usedSpaceEvent;

	public MemoryFS(Event<UsedSpace> usedSpaceEvent)
	{
		this.usedSpaceEvent = usedSpaceEvent;

		// Sprinkle some files around
		rootDirectory.addChild(VirtualFile.create(SEPARATOR, "Sample file.txt", "Hello there, feel free to look around.\n", rootPathSupplier));
		rootDirectory.addChild(VirtualDirectory.create(SEPARATOR, "Sample directory", rootPathSupplier));
		final VirtualDirectory dirWithFiles = VirtualDirectory.create(SEPARATOR, "Directory with files", rootPathSupplier);
		rootDirectory.addChild(dirWithFiles);
		dirWithFiles.addChild(VirtualFile.create(SEPARATOR, "hello.txt", "This is some sample text.\n", rootPathSupplier));
		dirWithFiles.addChild(VirtualFile.create(SEPARATOR, "hello again.txt", "This another file with text in it! Oh my!\n", rootPathSupplier));
		final VirtualDirectory nestedDirectory = VirtualDirectory.create(SEPARATOR, "Sample nested directory", rootPathSupplier);
		dirWithFiles.addChild(nestedDirectory);
		nestedDirectory.addChild(VirtualFile.create(SEPARATOR, "So deep.txt", "Man, I'm like, so deep in this here file structure.\n", rootPathSupplier));
	}

	@Override
	public int access(final String path, final int access)
	{
		return 0;
	}

	@Override
	public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
		if (getMemoryPath(path) != null) {
			return -ErrorCodes.EEXIST();
		}

		final Optional<VirtualAbstractPath<?>> parent = getParentPath(path);

		if (parent.get() instanceof VirtualDirectory directory) {
			directory.mkfile(VirtualAbstractPath.getLastComponent(SEPARATOR, path));
			return 0;
		}

		return -ErrorCodes.ENOENT();
	}

	@Override
	public int getattr(final String path, final StatWrapper stat) {
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);

		if (p.isPresent()) {
			p.get().getattr(stat);
			return 0;
		}

		return -ErrorCodes.ENOENT();
	}

	private Optional<VirtualAbstractPath<?>> getParentPath(final String path) {
		return rootDirectory.find(path.substring(0, path.lastIndexOf(SEPARATOR)));
	}

	public Optional<VirtualAbstractPath<?>> getMemoryPath(String path) {
		return rootDirectory.find(path);
	}
	
	@Override
	public int mkdir(final String path, final ModeWrapper mode) {
		if (getMemoryPath(path).isPresent()) {
			return -ErrorCodes.EEXIST();
		}

		final Optional<VirtualAbstractPath<?>> parent = getParentPath(path);

		if (parent.get() instanceof VirtualDirectory directory) {
			directory.mkdir(VirtualAbstractPath.getLastComponent(SEPARATOR, path));
			return 0;
		}

		return -ErrorCodes.ENOENT();
	}

	@Override
	public void mount() throws MountFailureException
	{
		try {
			String systemUser = System.getProperty("user.name");
			Files.createDirectories(Paths.get("/home/"+systemUser+"/unigrid"));

			String path = "/home/"+systemUser+"/unigrid";
			log(true).mount(path);

		} catch (IOException | FuseException ex) {
			throw new MountFailureException(ex);
		}
	}

	@Override
	public int open(final String path, final FileInfoWrapper info)
	{
		return 0;
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);

		if (p.isEmpty()) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p.get() instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}

		return ((VirtualFile<?>) p.get()).read(buffer, size, offset);
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p.get() instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}
		((VirtualDirectory) p.get()).read(filler);
		return 0;
	}

	@Override
	public int rename(final String path, final String newName)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);

		if (p == null) {
			return -ErrorCodes.ENOENT();
		}

		final Optional<VirtualAbstractPath<?>> newParent = getParentPath(newName);

		if (newParent == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(newParent.get() instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}

		p.get().delete();
		((VirtualDirectory) newParent.get()).deleteChild(getMemoryPath(newName).get());
		p.get().rename(newName.substring(newName.lastIndexOf(SEPARATOR)));
		((VirtualDirectory) newParent.get()).addChild(p.get());
		usedSpaceEvent.fire(UsedSpace.builder().size(rootDirectory.getFolderSize()).build());
		return 0;
	}

	@Override
	public int rmdir(final String path)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);

		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p.get() instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}
		p.get().delete();
		usedSpaceEvent.fire(UsedSpace.builder().size(rootDirectory.getFolderSize()).build());
		return 0;
	}

	@Override
	public int truncate(final String path, final long offset)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p.get() instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}
		((VirtualFile) p.get()).truncate(offset);
		return 0;
	}

	@Override
	public int unlink(final String path)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		p.get().delete();
		
		usedSpaceEvent.fire(UsedSpace.builder().size(rootDirectory.getFolderSize()).build());
		return 0;
	}

	@Override
	public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
			final FileInfoWrapper wrapper)
	{
		final Optional<VirtualAbstractPath<?>> p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p.get() instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}
		int ret = ((VirtualFile) p.get()).write(buf, bufSize, writeOffset);
		usedSpaceEvent.fire(UsedSpace.builder().size(rootDirectory.getFolderSize()).build());
		return ret;
	}

	@Override
	public int statfs(String path, StructStatvfs.StatvfsWrapper wrapper) {
		final int i = super.statfs(path, wrapper);
		
		wrapper.blocks(58*1024*1024).bsize(8192).bfree(58*1024*1024);
		usedSpaceEvent.fire(UsedSpace.builder().size(rootDirectory.getFolderSize()).build());
                return i;
	}

	@Override
	public String toString() {
		return rootDirectory.toString();
	}
}
