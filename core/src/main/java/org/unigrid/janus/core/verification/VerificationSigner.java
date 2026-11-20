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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.evm.LegacyVault;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.hedgehog.AddressBalance;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.core.legacy.LegacySigner;
import org.unigrid.janus.core.wallet.LedgerState;
import org.unigrid.janus.core.wallet.WalletLedger;

/**
 * Signs a verification request: every snapshot address of the open wallet, funded or empty, signs the same text with
 * its own key, so that whoever holds the keys, and only they, can ask for the wallet to be verified. The addresses
 * are the ones the wallet's ledger found in the snapshot, so a caller cannot leave any out. Nothing is written down,
 * and the keys are wiped once they have signed.
 */
@ApplicationScoped
public class VerificationSigner {
	static final String NOT_READ = "The wallet's addresses have not been read from the snapshot yet";
	static final String NOT_IN_SNAPSHOT = "The wallet has no address in the snapshot";
	static final String NO_KEY = "The wallet holds no key for %s";
	static final String NOT_CHECKED_OUT = "The signature of %s does not check out";
	private static final String SEPARATOR = ", ";

	private final WalletLedger ledger;
	private final Clock clock;
	private final SecureRandom random;
	private final SeedVault seedVault;
	private final LegacyVault legacyVault;

	@Inject
	public VerificationSigner(final WalletLedger ledger) {
		this(ledger, Clock.systemUTC(), new SecureRandom(), new SeedVault(), new LegacyVault());
	}

	VerificationSigner(final WalletLedger ledger, final Clock clock, final SecureRandom random,
		final SeedVault seedVault, final LegacyVault legacyVault) {

		this.ledger = ledger;
		this.clock = clock;
		this.random = random;
		this.seedVault = seedVault;
		this.legacyVault = legacyVault;
	}

	/**
	 * Signs every snapshot address of the wallet, once its ledger has been read, with the keys the secret opens: the
	 * passphrase of a wallet.dat, or the password of the EVM wallet a dump was imported into. The secret is wiped once
	 * the call ends, whatever the outcome.
	 */
	public SignedRequest sign(final Holder holder, final Path wallet, final char[] secret) {
		try {
			return sign(holder, snapshotAddresses(), keySource(wallet, secret));
		} finally {
			Arrays.fill(secret, '\0');
		}
	}

	/* The whole request is refused when any address has no key, so a request never holds part of a wallet. */
	SignedRequest sign(final Holder holder, final List<String> addresses, final KeySource source) {
		final VerificationId id = VerificationId.random(random);
		final Instant signedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		final String text = SignedRequest.text(id, signedAt);
		final Map<String, LegacyKey> byAddress = source.keys(Set.copyOf(addresses)).stream()
			.collect(Collectors.toMap(LegacyKey::address, Function.identity(), VerificationSigner::keepFirst));

		try {
			requireEvery(addresses, byAddress);

			final List<SignedAddress> signed = addresses.stream()
				.map(address -> new SignedAddress(address, signature(text, address, byAddress.get(address))))
				.toList();

			return new SignedRequest(id, holder, signedAt, signed);
		} finally {
			byAddress.values().forEach(LegacyKey::wipe);
		}
	}

	/* Two keys of one address are the same key twice, so the copy is wiped at once rather than kept; the very same
	   object handed in twice is left alone, as wiping it would wipe the one kept. */
	private static LegacyKey keepFirst(final LegacyKey first, final LegacyKey second) {
		if (second != first) {
			second.wipe();
		}

		return first;
	}

	/* Only a read ledger knows which addresses are in the snapshot; a wallet with none has nothing to verify. */
	private List<String> snapshotAddresses() {
		final LedgerState state = ledger.state();

		if (state.phase() != LedgerState.Phase.LOADED) {
			throw new IllegalStateException(NOT_READ);
		}

		final List<String> addresses = state.funds().snapshotAddresses().stream().map(AddressBalance::address)
			.toList();

		if (addresses.isEmpty()) {
			throw new IllegalStateException(NOT_IN_SNAPSHOT);
		}

		return addresses;
	}

	/* An EVM wallet holds the keys of an imported dump; any other wallet is a wallet.dat. */
	private KeySource keySource(final Path wallet, final char[] secret) {
		return EvmWalletStore.holds(wallet)
			? KeySource.dumpImport(EvmWalletStore.read(wallet), secret, seedVault, legacyVault)
			: KeySource.walletDat(wallet, secret);
	}

	private static void requireEvery(final List<String> addresses, final Map<String, LegacyKey> byAddress) {
		final List<String> missing = addresses.stream().filter(address -> !byAddress.containsKey(address))
			.sorted().toList();

		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(NO_KEY.formatted(String.join(SEPARATOR, missing)));
		}
	}

	/* Checked before it leaves Janus, so that a fault in signing is found here and not by whoever receives it. */
	private static String signature(final String text, final String address, final LegacyKey key) {
		final byte[] signature = LegacySigner.sign(key, text);

		if (!LegacySigner.addressOf(text, signature).equals(Optional.of(address))) {
			throw new IllegalStateException(NOT_CHECKED_OUT.formatted(address));
		}

		return Base64.getEncoder().encodeToString(signature);
	}
}
