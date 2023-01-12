package org.unigrid.janus.model.filesystem.memoryfs;

import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.fusejna.StructStat;
import net.fusejna.types.TypeMode;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;

public class VirtualFile<T> extends VirtualAbstractPath<T> {
	
	private FastByteArrayOutputStream contents = new FastByteArrayOutputStream();	
	@Getter @Setter private long size = 0;

	public VirtualFile(String separator, String name, VirtualDirectory<T> parent, Supplier<String> rootPathSupplier) {
		super(separator, name, parent, rootPathSupplier);
	}

	@SneakyThrows
	public VirtualFile(String separator, final String name, final String text, Supplier<String> rootPathSupplier)  {
		super(separator, name, rootPathSupplier);
		contents.write(text.getBytes(StandardCharsets.UTF_8));
		size = text.length();
	}

	@Override
	public Optional<VirtualAbstractPath<T>> find(String path) {
		return Optional.of(this);
	}

	@Override
	public void getattr(final StructStat.StatWrapper stat) {
		stat.setMode(TypeMode.NodeType.FILE).size(getSize());
	}

	private byte[] getArray(ByteBuffer buffer) {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			while(true) {
				output.write(buffer.get());
			}
		} catch(Exception ex) {
			/* Ignore it */
		}

		return output.toByteArray();
	}

	public int read(ByteBuffer buffer, long size, long offset) {
		long bytesToRead = size;

		if (offset >= getSize()) {
			return 0;
		}

		if (getSize() < offset + size) {
			bytesToRead = getSize() - offset;
		}

		buffer.put(contents.array, (int) offset, (int) bytesToRead);
		return (int) bytesToRead;
	}

	@SneakyThrows
	public void truncate(final long size) {		
		final byte[] truncated = Arrays.copyOfRange(contents.array, 0, (int) size);
		contents.reset();
		contents.write(truncated, 0, (int) size);
		this.size = size;
	}

	@SneakyThrows
	public int write(ByteBuffer buffer, long bufSize, long writeOffset) {
		final byte[] data = Arrays.copyOfRange(getArray(buffer), 0, (int) bufSize);

		contents.position(writeOffset);
		contents.write(data, 0, (int) bufSize);

		this.size = contents.length;
		return (int) bufSize;
	}

	public static <T> VirtualFile<T> create(String separator, String fileName, String content,
		Supplier<String> rootPathSupplier) {

		final VirtualFile<T> file = new VirtualFile(separator, fileName, content, rootPathSupplier);
		return file;
	}
	
	
	public static <T> VirtualFile<T> create(String separator, String fileName, VirtualDirectory<T> directory,
		Supplier<String> rootPathSupplier) {

		final VirtualFile<T> file = new VirtualFile(separator, fileName, directory, rootPathSupplier);
		return file;
	}
	
	@Override
	public String toString() {
		return StringUtils.repeat('\t', getDepth()) + "File: " + hashCode() + getSeparator() + "   "+ getName() + "\n";
	}
}
