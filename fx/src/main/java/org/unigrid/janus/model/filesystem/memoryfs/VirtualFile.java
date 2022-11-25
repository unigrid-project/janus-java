package org.unigrid.janus.model.filesystem.memoryfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.SneakyThrows;
import net.fusejna.StructStat;
import net.fusejna.types.TypeMode;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class VirtualFile extends VirtualAbstractPath {
	private ByteArrayOutputStream contents = new ByteArrayOutputStream();
	private long size = 0;
	
	public VirtualFile(final String name, final VirtualDirectory parent) {
		super(name, parent);
	}

	@SneakyThrows
	public VirtualFile(final String name, final String text)  {
		super(name);
		contents.write(text.getBytes(StandardCharsets.UTF_8));
		size = text.length();
	}

	@Override
	public VirtualAbstractPath find(String path) {
		return this;
	}

	@Override
	public void getattr(final StructStat.StatWrapper stat) {
		stat.setMode(TypeMode.NodeType.FILE).size(getSize());
	}

	public long getSize() {
		return size;
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
		try {
			final byte[] out = new byte[(int) size];

			contents.toInputStream().read(out, (int) offset, (int) size);
			buffer.put(out, (int) offset, (int) size);
			return (int) size;

		} catch (IOException ex) {
			Logger.getLogger(VirtualFile.class.getName()).log(Level.SEVERE, null, ex);
		}

		return -1;
	}

	public void truncate(final long size) {		
		final byte[] truncated = Arrays.copyOfRange(contents.toByteArray(), 0, (int) size);
		contents.reset();
		contents.write(truncated, 0, (int) size);
		this.size = size;
	}

	public int write(ByteBuffer buffer, long bufSize, long writeOffset) {
		final byte[] truncated = Arrays.copyOfRange(getArray(buffer), 0, (int) bufSize);
		final byte[] output = ArrayUtils.insert((int) 0, contents.toByteArray(), truncated);

		size = bufSize;
		contents.reset();
		contents.write(output, 0, output.length);

		return (int) size;
	}

	public static VirtualFile create(String fileName, String content) {
		final VirtualFile file = new VirtualFile(fileName, content);
		return file;
	}
	
	
	public static VirtualFile create(String fileName, VirtualDirectory directory) {
		final VirtualFile file = new VirtualFile(fileName, directory);
		return file;
	}
	
	@Override
	public String toString() {
		return StringUtils.repeat('\t', getDepth()) + "File: " + hashCode() +"/   "+ getName() + "\n";
	}
}
