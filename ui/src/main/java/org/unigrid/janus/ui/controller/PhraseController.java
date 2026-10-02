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

package org.unigrid.janus.ui.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import org.unigrid.janus.core.evm.EvmWallet;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.evm.LegacyVault;
import org.unigrid.janus.core.evm.Mnemonic;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.legacy.LegacyKey;
import org.unigrid.janus.ui.view.AppView;
import org.unigrid.janus.ui.view.ImportView;
import org.unigrid.janus.ui.view.PasswordView;
import org.unigrid.janus.ui.view.RestoreView;
import org.unigrid.janus.ui.view.SeedView;
import org.unigrid.janus.ui.view.VerifyView;
import org.unigrid.janus.ui.view.WelcomeView;
import org.unigrid.janus.web.action.Action;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.View;

/**
 * Making an EVM wallet from a new recovery phrase, or from one the user already has. The phrase lives in
 * memory only until it is sealed behind a password and saved, and is forgotten the moment the flow ends.
 */
@ApplicationScoped
public class PhraseController {
	public static final int MINIMUM_PASSWORD = 8;

	private static final String PASSWORD = "password";
	private static final String REPEAT = "repeat";

	private final EvmWalletStore store;
	private final SeedVault vault;
	private final LegacyVault legacyVault = new LegacyVault();
	private final WalletController wallets;
	private final ImportController importer;
	private final SecureRandom random;

	private volatile Mnemonic pending;
	private volatile List<LegacyKey> imported = List.of();
	private volatile boolean restoring;
	private volatile List<Integer> order = List.of();
	private final List<Integer> picked = new ArrayList<>();

	@Inject
	public PhraseController(final EvmWalletStore store, final WalletController wallets,
		final ImportController importer) {

		this(store, new SeedVault(), wallets, importer, new SecureRandom());
	}

	public PhraseController(final EvmWalletStore store, final SeedVault vault, final WalletController wallets,
		final ImportController importer, final SecureRandom random) {

		this.store = store;
		this.vault = vault;
		this.wallets = wallets;
		this.importer = importer;
		this.random = random;
	}

	@Action("create")
	public SeedView onClickCreate() {
		forget();
		return begin();
	}

	/* The keys of the chosen wallet dump wait in memory beside the new phrase, which is theirs to seal. */
	@Action("import-dump")
	public SeedView onClickImportDump() {
		final List<LegacyKey> keys = importer.dumpKeys();

		forget();
		imported = keys;
		return begin();
	}

	@Action("create-back")
	public View onClickBackToWelcome() {
		final boolean importing = importing();

		forget();
		return importing ? importer.onClickImport() : new WelcomeView();
	}

	/* The same words come back, so a phrase already written down stays the one to confirm. */
	@Action("create-seed")
	public synchronized SeedView onClickBackToPhrase() {
		picked.clear();
		return new SeedView(pending().words());
	}

	@Action("create-verify")
	public VerifyView onClickSaved() {
		return verify();
	}

	@Action("create-pick")
	public synchronized VerifyView onClickWord(final Form form) {
		final int index = Integer.parseInt(form.get("index"));

		if (!picked.contains(index) && order.contains(index)) {
			picked.add(index);
		}

		return verify();
	}

	@Action("create-reset")
	public synchronized VerifyView onClickReset() {
		picked.clear();
		return verify();
	}

	@Action("create-password")
	public PasswordView onClickConfirmed() {
		if (!verify().confirmed()) {
			throw new IllegalStateException("The phrase has not been confirmed");
		}

		return password(null);
	}

	@Action("restore")
	public RestoreView onClickRestore() {
		forget();
		restoring = true;
		return new RestoreView(Collections.nCopies(Mnemonic.WORDS, ""), null);
	}

	@Action("restore-back")
	public ImportView onClickBackToImport() {
		forget();
		return importer.onClickImport();
	}

	/* The boxes are read as one text, so a whole phrase pasted into the first box counts as well. */
	@Action("restore-words")
	public View onEnterWords(final Form form) {
		final String typed = String.join(" ", IntStream.rangeClosed(1, Mnemonic.WORDS)
			.mapToObj(n -> Objects.requireNonNullElse(form.get("word" + n), "")).toList());

		try {
			pending = Mnemonic.parse(typed);
			return password(null);
		} catch (IllegalArgumentException e) {
			return new RestoreView(boxes(typed), e.getMessage());
		}
	}

	@Action("phrase-back")
	public View onClickBackFromPassword() {
		return restoring ? new RestoreView(pending().words(), null) : verify();
	}

	@Action("phrase-save")
	public View onSave(final Form form) {
		final Mnemonic phrase = pending();
		final String password = Objects.requireNonNullElse(form.get(PASSWORD), "");

		if (password.length() < MINIMUM_PASSWORD) {
			return password("Use at least " + MINIMUM_PASSWORD + " characters");
		}

		if (!password.equals(form.get(REPEAT))) {
			return password("The two passwords differ");
		}

		final EvmWallet wallet = importing() ? EvmWallet.create(phrase, password, vault, legacyVault, imported)
			: keepingLegacyKeys(EvmWallet.create(phrase, password, vault));
		final AppView opened = wallets.open(store.save(wallet));

		forget();
		return opened;
	}

	/*
	 * A wallet saved again from its phrase, under a new password perhaps, replaces the file it was kept in.
	 * The legacy keys in it are sealed under a key the phrase gives, not the password, so the block is carried
	 * over as it is and nothing is lost by recovering.
	 */
	private EvmWallet keepingLegacyKeys(final EvmWallet wallet) {
		return store.find(wallet.addresses().getFirst()).map(EvmWallet::legacy).filter(Objects::nonNull)
			.map(wallet::withLegacy).orElse(wallet);
	}

	private PasswordView password(final String error) {
		return new PasswordView(restoring, importing(), error);
	}

	private boolean importing() {
		return !imported.isEmpty();
	}

	private SeedView begin() {
		final List<Integer> shuffled = new ArrayList<>(IntStream.range(0, Mnemonic.WORDS).boxed().toList());

		Collections.shuffle(shuffled, random);
		pending = Mnemonic.generate(random);
		order = List.copyOf(shuffled);
		return new SeedView(pending.words());
	}

	private synchronized VerifyView verify() {
		final List<String> words = pending().words();
		final List<String> chosen = picked.stream().map(words::get).toList();

		return new VerifyView(chosen, order.stream().map(index -> new VerifyView.Tile(index, words.get(index),
			picked.contains(index))).toList(), chosen.equals(words)
		);
	}

	private Mnemonic pending() {
		return Optional.ofNullable(pending).orElseThrow(
			() -> new IllegalStateException("No recovery phrase is in hand")
		);
	}

	private static List<String> boxes(final String typed) {
		final List<String> words = new ArrayList<>(List.of(typed.strip().split("\\s+")));

		words.removeIf(String::isEmpty);
		return IntStream.range(0, Mnemonic.WORDS).mapToObj(n -> n < words.size() ? words.get(n) : "").toList();
	}

	private synchronized void forget() {
		imported.forEach(LegacyKey::wipe);
		imported = List.of();
		pending = null;
		restoring = false;
		order = List.of();
		picked.clear();
	}
}
