/*
    The Janus Wallet
    Copyright © 2021-2026 Stiftelsen The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;

public class ReleaseKeyTest {
	private static final String FINGERPRINT = "A1CB0037B3B92D595FA1536C95A98E888B0BA5D9";
	private static final byte[] DATA = "janus-1.0.12.jar\n".getBytes(StandardCharsets.US_ASCII);

	private Path folder;
	private Path file;

	@BeforeTry
	public void makeAFile() throws IOException {
		folder = Files.createTempDirectory("release-key");
		file = Files.write(folder.resolve("download"), DATA);
	}

	@AfterTry
	public void removeIt() throws IOException {
		try (Stream<Path> paths = Files.walk(folder)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Example
	public void shouldBundleTheFoundationReleaseKey() {
		final byte[] fingerprint = ReleaseKey.getPublicKeyRing().iterator().next().getPublicKey().getFingerprint();

		assertEquals(FINGERPRINT, HexFormat.of().withUpperCase().formatHex(fingerprint));
	}

	@Example
	public void shouldAcceptASignatureByTheTrustedKey() {
		final SigningKey trusted = new SigningKey();

		assertTrue(ReleaseKey.verify(file, trusted.sign(DATA), trusted.ring()));
	}

	@Example
	public void shouldRefuseASignatureByAnotherKey() {
		assertFalse(ReleaseKey.verify(file, new SigningKey().sign(DATA), new SigningKey().ring()));
	}

	@Example
	public void shouldRefuseASignatureOverOtherData() throws IOException {
		final SigningKey trusted = new SigningKey();

		Files.writeString(file, "tampered");
		assertFalse(ReleaseKey.verify(file, trusted.sign(DATA), trusted.ring()));
	}

	@Example
	public void shouldRefuseSomethingThatIsNoSignature() {
		assertFalse(ReleaseKey.verify(file, DATA));
	}
}
