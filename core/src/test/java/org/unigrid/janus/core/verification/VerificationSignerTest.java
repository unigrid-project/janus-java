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

package org.unigrid.janus.core.verification;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.unigrid.janus.core.evm.EvmWallet;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.evm.LegacyVault;
import org.unigrid.janus.core.evm.Mnemonic;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.hedgehog.AddressBalance;
import org.unigrid.janus.core.hedgehog.HedgehogStand;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.core.legacy.LegacySigner;
import org.unigrid.janus.core.legacy.WalletDump;
import org.unigrid.janus.core.legacy.WalletKeys;
import org.unigrid.janus.core.wallet.LedgerState;
import org.unigrid.janus.core.wallet.LedgerState.Phase;
import org.unigrid.janus.core.wallet.WalletLedger;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VerificationSignerTest {
	private static final String PASSPHRASE = "Unigrid tëst passphrase";
	private static final String SOMEONE_ELSE = "HVrjNTDp7PzvXmiZ1Cf4t9AFogZg5BbcAE";
	private static final Instant NOW = Instant.parse("2026-11-19T15:00:00.700Z");
	private static final SeedVault CHEAP = new SeedVault(new SecureRandom(), 16);
	private static final Mnemonic PHRASE = Mnemonic.parse("abandon abandon abandon abandon abandon abandon "
		+ "abandon abandon abandon abandon abandon about");

	private HedgehogStand stand;
	private WalletLedger ledger;
	private VerificationSigner signer;

	private static final String FIXTURES = "/org/unigrid/janus/core/legacy/";

	private static Path fixture(final String name) {
		try {
			return Path.of(VerificationSignerTest.class.getResource(FIXTURES + name).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<String> lines(final String name) throws IOException {
		return Files.readAllLines(fixture(name));
	}

	@BeforeTry
	public void open() {
		stand = HedgehogStand.start().signed();
		ledger = new WalletLedger(stand.client(), ZoneOffset.UTC);
		signer = new VerificationSigner(ledger, Clock.fixed(NOW, ZoneOffset.UTC), new SecureRandom(), CHEAP,
			new LegacyVault()
		);
	}

	@AfterTry
	public void close() {
		ledger.stop();
		stand.close();
	}

	private LedgerState read(final Path wallet) throws InterruptedException {
		final Instant deadline = Instant.now().plusSeconds(20);

		ledger.load(wallet);

		while (ledger.state().phase() == Phase.LOADING && Instant.now().isBefore(deadline)) {
			Thread.sleep(20);
		}

		assertEquals(Phase.LOADED, ledger.state().phase());
		return ledger.state();
	}

	private static List<String> snapshotAddresses(final LedgerState state) {
		return state.funds().snapshotAddresses().stream().map(AddressBalance::address).toList();
	}

	/* Every address, in the order given, with a signature that checks out under it over the request's text. */
	private static void assertSignedBy(final List<String> addresses, final SignedRequest request) {
		assertEquals(addresses, request.addresses().stream().map(SignedAddress::address).toList());

		for (final SignedAddress signed : request.addresses()) {
			assertEquals(Optional.of(signed.address()), LegacySigner.addressOf(request.text(),
				Base64.getDecoder().decode(signed.signature())), signed.address()
			);
		}
	}

	@Example
	public void shouldSignEverySnapshotAddressOfAPlainWalletFundedOrEmpty() throws Exception {
		final List<String> addresses = lines("plain-wallet.addresses");

		stand.address(addresses.get(0), "480").address(addresses.get(1), "0");

		final LedgerState state = read(fixture("plain-wallet.dat"));
		final SignedRequest request = signer.sign(Holder.PERSON, fixture("plain-wallet.dat"), new char[0]);

		assertEquals(2, request.addresses().size());
		assertSignedBy(snapshotAddresses(state), request);
		assertEquals(Holder.PERSON, request.holder());
		assertEquals(NOW.truncatedTo(ChronoUnit.SECONDS), request.signedAt());
		assertEquals("verify " + request.id() + " " + NOW.getEpochSecond(), request.text());
	}

	@Example
	public void shouldSignWithTheKeysOfAnEncryptedWalletAndWipeThePassphrase() throws Exception {
		final Path wallet = fixture("encrypted-wallet.dat");
		final List<String> addresses = lines("encrypted-wallet.addresses");
		final char[] passphrase = PASSPHRASE.toCharArray();

		stand.address(addresses.get(0), "9").address(addresses.get(3), "0");

		final LedgerState state = read(wallet);

		assertSignedBy(snapshotAddresses(state), signer.sign(Holder.PERSON, wallet, passphrase));
		assertArrayEquals(new char[passphrase.length], passphrase);
	}

	@Example
	public void shouldSignWithTheKeysOfADumpImportedIntoAnEvmWallet() throws Exception {
		final Path folder = Files.createTempDirectory("wallets");
		final List<String> dumped = lines("wallet.dump.addresses");
		final EvmWallet evm = EvmWallet.create(PHRASE, "pw", CHEAP, new LegacyVault(),
			WalletDump.keys(fixture("wallet.dump"))
		);

		stand.address(dumped.get(0), "30").address(dumped.get(5), "0");

		try {
			final Path wallet = new EvmWalletStore(folder).save(evm);
			final LedgerState state = read(wallet);
			final char[] password = "pw".toCharArray();
			final SignedRequest request = signer.sign(Holder.COMPANY, wallet, password);

			assertSignedBy(snapshotAddresses(state), request);
			assertEquals(2, request.addresses().size());
			assertEquals(Holder.COMPANY, request.holder());
			assertArrayEquals(new char[password.length], password);
		} finally {
			try (Stream<Path> files = Files.walk(folder)) {
				for (final Path path : files.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
		}
	}

	@Example
	public void shouldRefuseUntilTheLedgerHasBeenReadAndStillWipeTheSecret() {
		final char[] passphrase = PASSPHRASE.toCharArray();

		assertThrows(IllegalStateException.class,
			() -> signer.sign(Holder.PERSON, fixture("encrypted-wallet.dat"), passphrase)
		);
		assertArrayEquals(new char[passphrase.length], passphrase);
	}

	@Example
	public void shouldRefuseAWalletWithNoAddressInTheSnapshot() throws Exception {
		read(fixture("plain-wallet.dat"));

		final IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> signer.sign(Holder.PERSON, fixture("plain-wallet.dat"), new char[0])
		);

		assertEquals(VerificationSigner.NOT_IN_SNAPSHOT, thrown.getMessage());
	}

	@Example
	public void shouldRefuseTheWholeRequestWhenAnAddressHasNoKeyAndWipeTheOthers() throws IOException {
		final String own = lines("plain-wallet.addresses").get(0);
		final List<LegacyKey> handedOut = new ArrayList<>();
		final KeySource source = addresses -> {
			handedOut.addAll(WalletKeys.keys(fixture("plain-wallet.dat"), Set.of(own), new char[0]));
			return handedOut;
		};
		final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> signer.sign(Holder.PERSON, List.of(own, SOMEONE_ELSE), source)
		);

		assertEquals(VerificationSigner.NO_KEY.formatted(SOMEONE_ELSE), thrown.getMessage());
		assertArrayEquals(new byte[32], handedOut.get(0).secret());
	}

	@Example
	public void shouldWipeTheKeysOnceTheyHaveSigned() throws IOException {
		final List<String> own = lines("plain-wallet.addresses").subList(0, 3);
		final List<LegacyKey> handedOut = new ArrayList<>();
		final KeySource source = addresses -> {
			handedOut.addAll(WalletKeys.keys(fixture("plain-wallet.dat"), addresses, new char[0]));
			return handedOut;
		};

		assertSignedBy(own, signer.sign(Holder.PERSON, own, source));
		assertEquals(3, handedOut.size());
		handedOut.forEach(key -> assertArrayEquals(new byte[32], key.secret()));
	}

	@Example
	public void shouldMakeAFreshIdForEveryRequestThatNoOtherTextVerifies() throws IOException {
		final List<String> own = lines("plain-wallet.addresses").subList(0, 1);
		final KeySource source = KeySource.walletDat(fixture("plain-wallet.dat"), new char[0]);
		final SignedRequest first = signer.sign(Holder.PERSON, own, source);
		final SignedRequest second = signer.sign(Holder.PERSON, own,
			KeySource.walletDat(fixture("plain-wallet.dat"), new char[0])
		);
		final byte[] signature = Base64.getDecoder().decode(first.addresses().get(0).signature());

		assertNotEquals(first.id(), second.id());
		assertNotEquals(Optional.of(own.get(0)), LegacySigner.addressOf(second.text(), signature));
		assertNotEquals(Optional.of(own.get(0)),
			LegacySigner.addressOf(SignedRequest.text(first.id(), first.signedAt().plusSeconds(1)), signature)
		);
	}
}
