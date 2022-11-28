package org.unigrid.janus.model.filesystem.memoryfs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.fusejna.StructStat;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VirtualAbstractPath<T> {
	@NonNull
	private String name;
	private VirtualDirectory parent;
	private T userData;
	
	protected VirtualAbstractPath(String name, VirtualDirectory parent, T userData) {
		this(name, parent);
		this.userData = userData;
	}

	protected VirtualAbstractPath(String name, VirtualDirectory parent) {
		this(name);
		this.parent = parent;
	}

	public void delete() {
		if (parent != null) {
			parent.deleteChild(this);
			parent = null;
		}
		
	}

	public Path getPath() {
		final String systemUser = System.getProperty("user.name");
		final List<String> mount = List.of(systemUser, "unigrid");
		final List<String> paths = new ArrayList<>();
		VirtualAbstractPath p = this;
		while (p != null) {
			paths.add(0, p.getName());
			p = p.getParent();
		}
		final String absolutePath = Path.of("home", mount.toArray(new String[0])).toString();
		return Path.of(absolutePath, paths.toArray(new String[0]));

	}

	public abstract VirtualAbstractPath find(String path);
	public abstract void getattr(StructStat.StatWrapper stat);

	public void rename(String newName) {
		while (newName.startsWith("/")) {
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
}
