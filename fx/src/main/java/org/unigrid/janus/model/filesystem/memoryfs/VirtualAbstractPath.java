package org.unigrid.janus.model.filesystem.memoryfs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.fusejna.StructStat;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VirtualAbstractPath<T> {
	private final String separator;
	@NonNull private String name;
	private VirtualTimeInfo timeInfo = VirtualTimeInfo.builder().build();
	private VirtualDirectory parent;
	private T userData;
	private final Supplier<String> rootPathSupplier;

	protected VirtualAbstractPath(String separator, String name, VirtualDirectory parent, T userData, Supplier<String> rootPathSupplier) {
		this(separator, name, parent, rootPathSupplier);
		this.userData = userData;
	}

	protected VirtualAbstractPath(String separator, String name, VirtualDirectory parent, Supplier<String> rootPathSupplier) {
		this(separator, name, rootPathSupplier);
		this.parent = parent;
	}

	public void delete() {
		if (parent != null) {
			parent.deleteChild(this);
			parent = null;
		}
		
	}

	public Path getPath() {
		final List<String> paths = new ArrayList<>();
		VirtualAbstractPath p = this;
		while (p != null) {
			paths.add(0, p.getName());
			p = p.getParent();
		}
		return Path.of(rootPathSupplier.get(), paths.toArray(new String[0]));
	}

	public abstract Optional<VirtualAbstractPath<T>> find(String path);
	public abstract void getattr(StructStat.StatWrapper stat);

	public void rename(String newName) {
		while (newName.startsWith(separator)) {
			newName = newName.substring(1);
		}
		name = newName;
	}

	public int getDepth() {
		VirtualAbstractPath path = this;
		int depth = 0;

		while(path.getParent() != null) {
			depth++;
			path = path.getParent();
		}

		return depth;
	}

	public long getFolderSize() {
		int size = 0;
		
		if (this instanceof VirtualDirectory<T> memoryDirectory) {
			for (VirtualAbstractPath<T> mp : memoryDirectory.getChildren()) {
				size += mp.getFolderSize();
			}
		} else if (this instanceof VirtualFile<T> memoryFile) {
			size += memoryFile.getSize();
		}
		return size;
	}
	
	public static String getLastComponent(String separator, String path) {
		while (path.substring(path.length() - 1).equals(separator)) {
			path = path.substring(0, path.length() - 1);
		}

		if (path.isEmpty()) {
			return "";
		}
		return path.substring(path.lastIndexOf(separator) + 1);
	}
}
