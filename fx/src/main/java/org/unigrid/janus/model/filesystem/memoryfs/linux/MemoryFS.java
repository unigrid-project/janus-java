package org.unigrid.janus.model.filesystem.memoryfs.linux;

import org.unigrid.janus.model.filesystem.memoryfs.VirtualDirectory;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualFile;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualAbstractPath;
import jakarta.enterprise.event.Event;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

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
	private final VirtualDirectory rootDirectory = new VirtualDirectory("");
	private final Event<UsedSpace> usedSpaceEvent;

	public MemoryFS(Event<UsedSpace> usedSpaceEvent)
	{
		this.usedSpaceEvent = usedSpaceEvent;

		// Sprinkle some files around
		rootDirectory.addChild(VirtualFile.create("Sample file.txt", "Hello there, feel free to look around.\n"));
		rootDirectory.addChild(VirtualDirectory.create("Sample directory"));
		final VirtualDirectory dirWithFiles = VirtualDirectory.create("Directory with files");
		rootDirectory.addChild(dirWithFiles);
		dirWithFiles.addChild(VirtualFile.create("hello.txt", "This is some sample text.\n"));
		dirWithFiles.addChild(VirtualFile.create("hello again.txt", "This another file with text in it! Oh my!\n"));
		final VirtualDirectory nestedDirectory = VirtualDirectory.create("Sample nested directory");
		dirWithFiles.addChild(nestedDirectory);
		nestedDirectory.addChild(VirtualFile.create("So deep.txt", "Man, I'm like, so deep in this here file structure.\n"));
	}

	@Override
	public int access(final String path, final int access)
	{
		return 0;
	}

	@Override
	public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info)
	{
		if (getMemoryPath(path) != null) {
			return -ErrorCodes.EEXIST();
		}
		final VirtualAbstractPath parent = getParentPath(path);
		if (parent instanceof VirtualDirectory) {
			((VirtualDirectory) parent).mkfile(getLastComponent(path));
			return 0;
		}
		return -ErrorCodes.ENOENT();
	}

	@Override
	public int getattr(final String path, final StatWrapper stat)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p != null) {
			p.getattr(stat);
			return 0;
		}
		return -ErrorCodes.ENOENT();
	}

	private String getLastComponent(String path)
	{
		while (path.substring(path.length() - 1).equals("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (path.isEmpty()) {
			return "";
		}
		return path.substring(path.lastIndexOf("/") + 1);
	}

	private VirtualAbstractPath getParentPath(final String path)
	{
		return rootDirectory.find(path.substring(0, path.lastIndexOf("/")));
	}

	public VirtualAbstractPath getMemoryPath(final String path)
	{
		return rootDirectory.find(path);
	}

	
	public static long getFolderSize(VirtualAbstractPath memoryPath) {
		int size = 0;

		if (memoryPath instanceof VirtualDirectory memoryDirectory) {
			for (VirtualAbstractPath mp : memoryDirectory.getChildren()) {
				size += getFolderSize(mp);
			}
			
		} else if (memoryPath instanceof VirtualFile memoryFile) {
			size += memoryFile.getSize();
		}

		return size;
	}
	
	@Override
	public int mkdir(final String path, final ModeWrapper mode)
	{
		if (getMemoryPath(path) != null) {
			return -ErrorCodes.EEXIST();
		}
		final VirtualAbstractPath parent = getParentPath(path);
		if (parent instanceof VirtualDirectory) {
			((VirtualDirectory) parent).mkdir(getLastComponent(path));
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
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}
		return ((VirtualFile) p).read(buffer, size, offset);
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}
		((VirtualDirectory) p).read(filler);
		return 0;
	}

	@Override
	public int rename(final String path, final String newName)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		final VirtualAbstractPath newParent = getParentPath(newName);
		if (newParent == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(newParent instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}
		p.delete();
		((VirtualDirectory) newParent).deleteChild(getMemoryPath(newName));
		p.rename(newName.substring(newName.lastIndexOf("/")));
		((VirtualDirectory) newParent).addChild(p);
		usedSpaceEvent.fire(UsedSpace.builder().size(MemoryFS.getFolderSize(rootDirectory)).build());
		return 0;
	}

	@Override
	public int rmdir(final String path)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p instanceof VirtualDirectory)) {
			return -ErrorCodes.ENOTDIR();
		}
		p.delete();
		usedSpaceEvent.fire(UsedSpace.builder().size(MemoryFS.getFolderSize(rootDirectory)).build());
		return 0;
	}

	@Override
	public int truncate(final String path, final long offset)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}
		((VirtualFile) p).truncate(offset);
		return 0;
	}

	@Override
	public int unlink(final String path)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		p.delete();
		
		usedSpaceEvent.fire(UsedSpace.builder().size(MemoryFS.getFolderSize(rootDirectory)).build());
		return 0;
	}

	@Override
	public int write(final String path, final ByteBuffer buf, final long bufSize, final long writeOffset,
			final FileInfoWrapper wrapper)
	{
		final VirtualAbstractPath p = getMemoryPath(path);
		if (p == null) {
			return -ErrorCodes.ENOENT();
		}
		if (!(p instanceof VirtualFile)) {
			return -ErrorCodes.EISDIR();
		}
		int ret = ((VirtualFile) p).write(buf, bufSize, writeOffset);
		usedSpaceEvent.fire(UsedSpace.builder().size(MemoryFS.getFolderSize(rootDirectory)).build());
		return ret;
	}

	@Override
	public int statfs(String path, StructStatvfs.StatvfsWrapper wrapper) {
		final int i = super.statfs(path, wrapper);
		wrapper.bsize(8192);
		wrapper.blocks(4096 * 50);
		wrapper.bavail(4096 * 50);
		wrapper.bfree(4096 * 50);
		//wrapper.blocks(58*1024*1024).bsize(8192).bfree(58*1024*1024);
		usedSpaceEvent.fire(UsedSpace.builder().size(MemoryFS.getFolderSize(rootDirectory)).build());
                return i;
	}

	@Override
	public String toString() {
		return rootDirectory.toString();
	}
		

	
}
