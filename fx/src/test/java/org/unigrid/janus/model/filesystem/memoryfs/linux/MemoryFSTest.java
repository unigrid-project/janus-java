/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
*/

package org.unigrid.janus.model.filesystem.memoryfs.linux;

import jakarta.enterprise.event.Event;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import mockit.Mocked;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.model.filesystem.memoryfs.VirtualFile;
import org.unigrid.janus.model.signal.UsedSpace;

public class MemoryFSTest extends BaseMockedWeldTest {
	@Mocked public Event<UsedSpace> usedSpaceEvent;
	@Mocked ModeWrapper mode;
	@Mocked FileInfoWrapper info;

//	@Example
//	public boolean shouldRetainCapacityOnRenames() {
//		final MemoryFS fs = new MemoryFS(usedSpaceEvent);
//		final long sizeBefore = ((VirtualFile) fs.getMemoryPath("/Directory with files/hello.txt")).getSize();
//
//		fs.rename("/Directory with files/hello.txt", "/Directory with files/New name.txt");
//		final long sizeAfter = ((VirtualFile) fs.getMemoryPath("/Directory with files/New name.txt")).getSize();
//
//		return sizeBefore == sizeAfter;
//	}

	@Provide
	public Arbitrary<ByteBuffer> provideByteBuffer(@ForAll @AlphaChars @StringLength(min = 500, max = 1000) String content) {
		return Arbitraries.of(Charset.defaultCharset().encode(content));
	}

	@Property
	public void shouldBeABleToEditContent(@ForAll("provideByteBuffer") ByteBuffer startingContent,
		@ForAll("provideByteBuffer") ByteBuffer endingContent) {
		
		final MemoryFS fs = new MemoryFS(usedSpaceEvent);
		fs.create("/Directory with files/hello2.txt",  mode ,info);

//		fs.write("/Directory with files/hello2.txt", startingContent, startingContent.capacity(), 0, null);
//		final long sizeBefore = ((VirtualFile) fs.getMemoryPath("/Directory with files/hello2.txt")).getSize();
//		assertThat(sizeBefore, equalTo((long) startingContent.capacity()));
//		
//		fs.write("/Directory with files/hello2.txt", endingContent, endingContent.capacity(), 0, null);
//		final long sizeAfter = ((VirtualFile) fs.getMemoryPath("/Directory with files/hello2.txt")).getSize();
//		assertThat(sizeAfter, equalTo((long) endingContent.capacity()));
		
	}
}
