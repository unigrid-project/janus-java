package org.unigrid.janus.model.filesystem.memoryfs.linux;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Example;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import java.util.List;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualDirectory;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VirtualFileTest {
	
	@Provide
	public Arbitrary<String[]> providePlatformSpecifics(){
		return Arbitraries.of(new String[]{"/", "","/"}, new String[]{"\\", "\\", "J:\\"});
	}
	
	@Provide
	public Arbitrary<VirtualFile> provideVirtualFile(@ForAll @AlphaChars @StringLength(min = 10, max = 20) String fileName,
		@ForAll @AlphaChars @StringLength(min = 5000, max = 1_000_000) String content,
		@ForAll("providePlatformSpecifics") String[] platform) {

		final VirtualDirectory parent = new VirtualDirectory(platform[0], platform[1], () -> platform[2]); // original
		final VirtualDirectory level1 = new VirtualDirectory(platform[0], "level-1", () -> platform[2]);
		final VirtualDirectory level2 = new VirtualDirectory(platform[0], "level-2", () -> platform[2]);
		
		final VirtualFile file = new VirtualFile(platform[0], fileName, content, () -> platform[2]); // original
		
		parent.addChild(level1);
		level1.addChild(level2);
		level2.addChild(file);
		
		//parent.addChild(file); // original
		return Arbitraries.of(file);
	}
	
	@Property(tries=100)
	public void shouldBeABleToRead(@ForAll("provideVirtualFile") VirtualFile file,
		@ForAll @Size(10) List<@IntRange(min=500, max=50_000) Integer> sizes, 
		@ForAll @Size(10) List<@IntRange(min=500, max=50_000) Integer> offsets){

		for (int i = 0; i < 10; i++) {
			ByteBuffer bf = ByteBuffer.allocate(sizes.get(i));
			final int readBytes = file.read(bf, sizes.get(i), offsets.get(i));

			assertThat(readBytes, lessThanOrEqualTo((int) file.getSize()));
		}
	}

	@Property(tries=100)
	public void shouldBeABleToWrite(@ForAll("provideVirtualFile") VirtualFile file,
		@ForAll @Size(10) List<@AlphaChars @StringLength(min=100, max=100_000) String> strings,
		@ForAll @Size(10) List<@IntRange(min=500, max=50_000) Integer> offsets){

		for (int i = 0; i < 10; i++) {
			ByteBuffer bf = ByteBuffer.allocate(strings.get(i).length());
			final int writeBytes = file.write(bf, strings.get(i).length(), offsets.get(i));

			assertThat(writeBytes, lessThanOrEqualTo((int) file.getSize()));
		}
	}	
		
	@Property(tries=100)
	public void shouldBeABleToRename(@ForAll("provideVirtualFile") VirtualFile file,
		@ForAll @Size(10) List<@AlphaChars @StringLength(min=2, max=8) String> strings) {
		
		for (int i = 0; i < 10; i++) {
			file.rename(strings.get(i));
		}
	}

	@Example
	public void shouldMaintainIntegrityOnRead(@ForAll @AlphaChars @StringLength(min = 10, max = 20) String fileName,
		@ForAll("providePlatformSpecifics") String[] platform) {

		final VirtualFile file = new VirtualFile(platform[0], fileName,
			"THERE WAS A CHRISTMASTREE WITH NO PINE ON IT", () -> platform[2]
		);

		final ByteBuffer buffer = ByteBuffer.allocate(3);

		file.read(buffer, 3, 6);
		assertThat(new String(buffer.array()), equalTo("WAS"));

		buffer.rewind();
		file.read(buffer, 3, 12);
		assertThat(new String(buffer.array()), equalTo("CHR"));
	}

	@Example
	public void shouldMaintainIntegrityOnWrite(@ForAll @AlphaChars @StringLength(min = 10, max = 20) String fileName,
		@ForAll("providePlatformSpecifics") String[] platform) {
		
		final String xmas = "THERE WAS A CHRISTMASTREE WITH NO PINE ON IT";
		final VirtualFile file = new VirtualFile(platform[0], fileName, xmas, () -> platform[2]);
		final ByteBuffer inBuffer = ByteBuffer.allocate(100);

		ByteBuffer outBuffer = ByteBuffer.wrap("LAY".getBytes());
		file.write(outBuffer, 3, 6);
		file.read(inBuffer, xmas.length(), 0);
		assertThat(new String(inBuffer.array(), 0, xmas.length()), equalTo("THERE LAY A CHRISTMASTREE WITH NO PINE ON IT"));

		inBuffer.rewind();
		outBuffer = ByteBuffer.wrap("ON TOP OF IT".getBytes());
		file.write(outBuffer, 12, 39);
		file.read(inBuffer, 39 + 12, 0);
		assertThat(new String(inBuffer.array(), 0, 39 + 12), equalTo("THERE LAY A CHRISTMASTREE WITH NO PINE ON TOP OF IT"));
	}
}
