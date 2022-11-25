package org.unigrid.janus.memoryfs.win;

import com.github.jnrwinfspteam.jnrwinfsp.api.FileAttributes;
import com.github.jnrwinfspteam.jnrwinfsp.api.FileInfo;
import com.github.jnrwinfspteam.jnrwinfsp.api.ReparsePoint;
import com.github.jnrwinfspteam.jnrwinfsp.api.WinSysTime;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public abstract class MemoryObj {

	@Getter
	private final MemoryObj parent;
	@Getter @Setter
	private Path path;
	@Getter
	private final Set<FileAttributes> fileAttributes;
	@Getter @Setter
	private byte[] securityDescriptor;
	@Getter @Setter
	private byte[] reparseData;
	@Getter @Setter
	private int reparseTag;
	@Setter
	private WinSysTime creationTime;
	@Setter
	private WinSysTime lastAccessTime;
	@Setter
	private WinSysTime lastWriteTime;
	@Setter
	private WinSysTime changeTime;
	@Setter
	private long indexNumber;

	public MemoryObj(MemoryObj parent, Path path, byte[] securityDescriptor, ReparsePoint reparsePoint) {
		this.parent = parent;
		this.path = Objects.requireNonNull(path);
		this.fileAttributes = EnumSet.noneOf(FileAttributes.class);
		this.securityDescriptor = Objects.requireNonNull(securityDescriptor);
		this.reparseData = null;
		this.reparseTag = 0;
		WinSysTime now = WinSysTime.now();
		this.creationTime = now;
		this.lastAccessTime = now;
		this.lastWriteTime = now;
		this.changeTime = now;
		this.indexNumber = 0;

		if (reparsePoint != null) {
			this.reparseData = reparsePoint.getData();
			this.reparseTag = reparsePoint.getTag();
			fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT);
		}
	}

	public final String getName() {
		return path.getNameCount() > 0 ? path.getFileName().toString() : null;
	}

	public abstract int getAllocationSize();

	public abstract int getFileSize();

	public final FileInfo generateFileInfo() {
		return generateFileInfo(getPath().toString());
	}

	public final FileInfo generateFileInfo(String filePath) {
		FileInfo res = new FileInfo(filePath);
		res.getFileAttributes().addAll(fileAttributes);
		res.setAllocationSize(getAllocationSize());
		res.setFileSize(getFileSize());
		res.setCreationTime(creationTime);
		res.setLastAccessTime(lastAccessTime);
		res.setLastWriteTime(lastWriteTime);
		res.setChangeTime(changeTime);
		res.setReparseTag(reparseTag);
		res.setIndexNumber(indexNumber);
		return res;
	}

	public final void touch() {
		WinSysTime now = WinSysTime.now();
		setLastAccessTime(now);
		setLastWriteTime(now);
		setChangeTime(now);
	}

	public final void touchParent() {
		MemoryObj parent = getParent();
		if (parent != null) {
			parent.touch();
		}
	}
}
