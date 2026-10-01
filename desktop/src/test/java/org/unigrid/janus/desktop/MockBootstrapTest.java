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

package org.unigrid.janus.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.unigrid.janus.core.Release;
import org.unigrid.janus.core.legacy.LegacyWallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MockBootstrapTest {
	private static final int TRANSACTION_COUNT_OFFSET = 64;
	private static final int TRANSACTION_TABLE_OFFSET = 104;
	private static final int TRANSACTION_ID = 32;
	private static final int SIGNATURE_HEADER = 12;
	private static final long SECONDS = 120;

	@TempDir
	private Path folder;

	@Test
	public void shouldPrintThePublicKeyTheWayHedgehogReadsIt() throws Exception {
		assertTrue(Pattern.matches("[0-9a-f]{262}", MockBootstrap.publicKey(MockBootstrap.keys())));
	}

	@Test
	public void shouldSignItsContentWithTheKeyItWasGiven() throws Exception {
		final KeyPair keys = MockBootstrap.keys();
		final Path file = folder.resolve("bootstrap.dat");

		MockBootstrap.write(file, address(), MockBootstrap.HISTORY, keys);

		final byte[] bytes = Files.readAllBytes(file);
		final ByteBuffer header = ByteBuffer.wrap(bytes);
		final int content = (int) (header.getLong(TRANSACTION_TABLE_OFFSET)
			+ header.getLong(TRANSACTION_COUNT_OFFSET) * TRANSACTION_ID);
		final Signature verifier = Signature.getInstance("SHA512withECDSA");

		assertEquals("UGDSIGN1", new String(bytes, content, "UGDSIGN1".length(), StandardCharsets.US_ASCII));
		verifier.initVerify(keys.getPublic());
		verifier.update(MessageDigest.getInstance("SHA-512").digest(Arrays.copyOf(bytes, content)));
		assertTrue(verifier.verify(Arrays.copyOfRange(bytes, content + SIGNATURE_HEADER, bytes.length)));
	}

	/* Hedgehog itself is the judge of the format; it is on a developer's computer once Janus has run there. */
	@Test
	public void shouldBeReadAsASignedLedgerByHedgehog() throws Exception {
		final Path hedgehog = Path.of(System.getProperty("user.home"), ".janus", "hedgehog",
			new Release().hedgehogVersion().orElseThrow(), "hedgehog");

		Assumptions.assumeTrue(Files.isExecutable(hedgehog), "No Hedgehog at " + hedgehog);

		final KeyPair keys = MockBootstrap.keys();
		final Path file = folder.resolve("bootstrap.dat");
		final String address = address();

		MockBootstrap.write(file, address, MockBootstrap.HISTORY, keys);

		final List<String> options = List.of(hedgehog.toString(), "bootstrap", "--snapshot=" + file,
			"--network-keys=" + MockBootstrap.publicKey(keys));

		assertTrue(Pattern.compile("Signature:\\s+SIGNED\\b").matcher(run(options, "info")).find());
		assertTrue(run(options, "balance", address).contains("480.00000000 in 2 transactions"));
	}

	private String address() throws IOException {
		final Path wallet = folder.resolve("wallet.dat");

		try (InputStream in = getClass().getResourceAsStream("wallet.dat")) {
			Files.copy(in, wallet);
		}

		return LegacyWallet.addresses(wallet).first();
	}

	private static String run(final List<String> options, final String... command) throws Exception {
		final List<String> line = new ArrayList<>(options);

		line.addAll(List.of(command));

		final Process process = new ProcessBuilder(line).redirectErrorStream(true).start();
		final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(process.waitFor(SECONDS, TimeUnit.SECONDS), "Hedgehog did not finish");
		assertEquals(0, process.exitValue(), output);
		return output;
	}
}
