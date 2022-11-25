package org.unigrid.janus.model.filesystem.memoryfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import net.fusejna.DirectoryFiller;
import net.fusejna.StructStat;
import net.fusejna.types.TypeMode;
import org.apache.commons.lang3.StringUtils;

public class VirtualDirectory extends VirtualAbstractPath {
	@Getter
	private final List<VirtualAbstractPath> children = Collections.synchronizedList(new ArrayList<VirtualAbstractPath>());

	public VirtualDirectory(final String name) {
		super(name);
	}

	public VirtualDirectory(final String name, final VirtualDirectory parent) {
		super(name, parent);
	}

	public void addChild(final VirtualAbstractPath p) {
		children.add(p);
		p.setParent(this);
	}

	public void deleteChild(final String name) {
		for (VirtualAbstractPath mp : children) {
			if (mp.getName().equals(name)) {
				children.remove(mp);
			}
		}
	}

	public void deleteChild(final VirtualAbstractPath child) {
		children.remove(child);
	}

	public VirtualAbstractPath find(String path, VirtualDirectory directory) {
		final String[] names = path.split("/");

		if (names.length <= 1) {
			return this;
		} else {
			for (VirtualAbstractPath mp : children) {
				if (mp.getName().equals(names[1])) {
					final String newPath = String.join("/", Arrays.copyOfRange(names, 1, names.length));
					return mp.find(newPath);
				}
			}
		}

		return null;
	}

	@Override
	public VirtualAbstractPath find(String path) {
		return find(path, this);
	}

	@Override
	public void getattr(final StructStat.StatWrapper stat) {
		stat.setMode(TypeMode.NodeType.DIRECTORY);
	}

	public void mkdir(final String lastComponent) {
		children.add(new VirtualDirectory(lastComponent, this));
	}

	public void mkfile(final String lastComponent) {
		children.add(VirtualFile.create(lastComponent, this));
	}

	public void read(final DirectoryFiller filler) {
		for (final VirtualAbstractPath p : children) {
			filler.add(p.getName());
		}
	}
	
	public static VirtualDirectory create(String fileName) {
		return new VirtualDirectory(fileName);
	}

	@Override
	public String toString() {
		String string = StringUtils.repeat('\t', getDepth()) + "Directory: " + getName() + "\n";

		for (VirtualAbstractPath path : children) {
			string += path.toString();
		}
		return string;
	}
	
	
}
