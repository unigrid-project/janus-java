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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.stream.Stream;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.Release;
import org.unigrid.janus.core.WalletBackup;
import org.unigrid.janus.core.WalletChoice;
import org.unigrid.janus.ui.controller.AboutController;
import org.unigrid.janus.ui.controller.ImportController;
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
 * tests: its files live in a folder of its own and its window only takes notes. The controllers are
 * handed their collaborators directly, because the container would look for wallets in the real home.
 */
public class ControlCenterRig implements AutoCloseable {
	public static final String TITLE = "Unigrid Control Center";

	private final SessionToken token = SessionToken.random();
	private final RecordingWindow window = new RecordingWindow();
	private final Path home;
	private final Path data;
	private final Path backups;
	private final UiServer server;
	private final URI base;

	public ControlCenterRig() throws Exception {
		home = Files.createTempDirectory("janus-rig");
		data = Files.createDirectory(home.resolve("data"));
		backups = home.resolve("backups");

		final WalletChoice choice = new WalletChoice(new WalletBackup(backups, Clock.systemUTC()));
		final Actions actions = Actions.of(new WelcomeController(), new AboutController(new Release()),
			new ImportController(new DataDirectory(data), choice)
		);

		server = new UiServer(Routes.create(new Templates(false), token, window, actions, new IndexView(TITLE)));
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

	public Path data() {
		return data;
	}

	public Path backups() {
		return backups;
	}

	/** A wallet left behind in the data folder, where the legacy daemon kept it. */
	public Path leaveWalletBehind() throws IOException {
		return Files.createFile(data.resolve("wallet.dat"));
	}

	/** A wallet somewhere the person would have to point at themselves. */
	public Path keepWalletElsewhere() throws IOException {
		return Files.createFile(home.resolve("elsewhere.dat"));
	}

	@Override
	public void close() throws Exception {
		server.stop();

		try (Stream<Path> paths = Files.walk(home)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}
}
