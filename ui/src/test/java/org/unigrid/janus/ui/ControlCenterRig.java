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

package org.unigrid.janus.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.unigrid.janus.core.ChosenWallet;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.Release;
import org.unigrid.janus.core.WalletBackup;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.core.evm.EvmWalletStore;
import org.unigrid.janus.core.evm.SeedVault;
import org.unigrid.janus.core.hedgehog.HedgehogService;
import org.unigrid.janus.core.hedgehog.HedgehogStand;
import org.unigrid.janus.core.wallet.WalletLedger;
import org.unigrid.janus.ui.controller.AboutController;
import org.unigrid.janus.ui.controller.ImportController;
import org.unigrid.janus.ui.controller.PhraseController;
import org.unigrid.janus.ui.controller.WalletController;
import org.unigrid.janus.ui.controller.WelcomeController;
import org.unigrid.janus.ui.view.IndexView;
import org.unigrid.janus.web.RecordingWindow;
import org.unigrid.janus.web.Routes;
import org.unigrid.janus.web.SessionToken;
import org.unigrid.janus.web.Templates;
import org.unigrid.janus.web.UiServer;
import org.unigrid.janus.web.action.Actions;

/**
 * The Control Center served the way the shell serves it, but kept away from the person running the
 * tests: its files live in a folder of its own, its window only takes notes and its Hedgehog is a
 * stand-in. The controllers are handed their collaborators directly, because the container would look
 * for wallets in the real home.
 */
public class ControlCenterRig implements AutoCloseable {
	public static final String TITLE = "Unigrid Control Center";

	private static final String FIXTURE = "/org/unigrid/janus/core/legacy/plain-wallet";

	/* Sealing at the standard scrypt cost takes a second and 256 MB, which no test needs to pay. */
	private static final int CHEAP_SEAL = 16;

	private final SessionToken token = SessionToken.random();
	private final RecordingWindow window = new RecordingWindow();
	private final HedgehogStand stand = HedgehogStand.start().signed();
	private final HedgehogService hedgehog = stand.service();
	private final WalletLedger ledger = new WalletLedger(stand.client(), ZoneOffset.UTC);
	private final Path home;
	private final Path data;
	private final Path backups;
	private final ChosenWallet chosen;
	private final List<String> addresses;
	private final UiServer server;
	private final URI base;

	public ControlCenterRig() throws Exception {
		home = Files.createTempDirectory("janus-rig");
		data = Files.createDirectory(home.resolve("data"));
		backups = home.resolve("backups");
		chosen = new ChosenWallet(home.resolve("chosen"));

		final WalletChoice choice = new WalletChoice(new WalletBackup(backups, Clock.systemUTC()));
		final ImportController importer = new ImportController(new DataDirectory(data), choice);
		final WalletController wallet = new WalletController(chosen, choice, hedgehog, ledger, importer,
			ZoneOffset.UTC
		);
		final PhraseController phrase = new PhraseController(new EvmWalletStore(home.resolve("wallets")),
			new SeedVault(new SecureRandom(), CHEAP_SEAL), wallet, importer, new SecureRandom()
		);
		final Actions actions = Actions.of(new WelcomeController(), new AboutController(new Release()), importer,
			wallet, phrase
		);

		try (InputStream in = getClass().getResourceAsStream(FIXTURE + ".addresses")) {
			addresses = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
		}

		server = new UiServer(Routes.create(new Templates(false), token, window, actions,
			() -> new IndexView(TITLE, wallet.start())
		));
		base = server.start();
	}

	public URI base() {
		return base;
	}

	/** Where the shell first sends its window, carrying the token that is swapped for a cookie. */
	public URI entrance() {
		return base.resolve("/?" + SessionToken.PARAMETER + "=" + token.value());
	}

	public RecordingWindow window() {
		return window;
	}

	public HedgehogStand hedgehog() {
		return stand;
	}

	public ChosenWallet chosen() {
		return chosen;
	}

	/** The addresses the legacy wallet the rig hands out holds keys for. */
	public List<String> addresses() {
		return addresses;
	}

	public Path data() {
		return data;
	}

	public Path backups() {
		return backups;
	}

	/** Where the EVM wallets made or restored in the rig are kept. */
	public Path wallets() {
		return home.resolve("wallets");
	}

	/** A wallet left behind in the data folder, where the legacy daemon kept it. */
	public Path leaveWalletBehind() throws IOException {
		return copyFixture(data.resolve("wallet.dat"));
	}

	/** A wallet somewhere the person would have to point at themselves. */
	public Path keepWalletElsewhere() throws IOException {
		return copyFixture(home.resolve("elsewhere.dat"));
	}

	private Path copyFixture(final Path to) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(FIXTURE + ".dat")) {
			Files.copy(in, to);
			return to;
		}
	}

	@Override
	public void close() throws Exception {
		server.stop();
		ledger.stop();
		hedgehog.stop();
		stand.close();

		try (Stream<Path> paths = Files.walk(home)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}
}
