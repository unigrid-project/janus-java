package org.unigrid.janus.model.filesystem.memoryfs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.Getter;
import net.fusejna.DirectoryFiller;
import net.fusejna.StructStat;
import net.fusejna.types.TypeMode;
import org.apache.commons.lang3.StringUtils;

public class VirtualDirectory<T> extends VirtualAbstractPath<T> implements Cloneable {
	@Getter
	private final List<VirtualAbstractPath<T>> children = Collections.synchronizedList(new ArrayList<>());

	public VirtualDirectory(String separator, final String name, Supplier<String> rootPathSupplier) {
		super(separator, name, rootPathSupplier);
	}

	public VirtualDirectory(String separator, final String name, final VirtualDirectory<T> parent, Supplier<String> rootPathSupplier) {
		super(separator, name, parent, rootPathSupplier);
	}

	public void addChild(final VirtualAbstractPath<T> p) {
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

	public void deleteChild(final VirtualAbstractPath<T> child) {
		children.remove(child);
	}

	public Optional<VirtualAbstractPath<T>> find(String path, VirtualDirectory<T> directory) {

		final String[] names = path.split(Pattern.quote(File.separator));
		
		if (names.length <= 1) {
			return Optional.of(this);
		} else {
			for (VirtualAbstractPath<T> mp : children) {
				if (mp.getName().equals(names[1])) {
					final String newPath = String.join(getSeparator(), Arrays.copyOfRange(names, 1, names.length));
					return mp.find(newPath);
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<VirtualAbstractPath<T>> find(String path) {
		return find(path, this);
	}

	@Override
	public void getattr(final StructStat.StatWrapper stat) {
		stat.setMode(TypeMode.NodeType.DIRECTORY);
	}

	public void mkdir(final String lastComponent) {
		children.add(new VirtualDirectory(getSeparator(), lastComponent, this, getRootPathSupplier()));
	}

	public void mkfile(String lastComponent) {
		children.add(VirtualFile.create(getSeparator(), lastComponent, this, getRootPathSupplier()));
	}

	public void read(final DirectoryFiller filler) {
		for (final VirtualAbstractPath<T> p : children) {
			filler.add(p.getName());
		}
	}

	public static <T> VirtualDirectory<T> create(String separator, String fileName, Supplier<String> rootPathSupplier) {
		return new VirtualDirectory(separator, fileName, rootPathSupplier);
	}

	@Override
	public String toString() {
		String string = StringUtils.repeat('\t', getDepth()) + "Directory: " + getName() + "\n";

		for (VirtualAbstractPath<T> path : children) {
			string += path.toString();
		}
		return string;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new VirtualDirectory(getSeparator(), getName(), getRootPathSupplier());
	}
	
	public boolean contains(String name){
		for (VirtualAbstractPath<T> child : children) {
			if (child.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
