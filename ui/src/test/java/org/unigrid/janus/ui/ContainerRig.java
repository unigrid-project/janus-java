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

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.unigrid.janus.core.ChosenWallet;
import org.unigrid.janus.core.DataDirectory;
import org.unigrid.janus.core.hedgehog.HedgehogStand;
import org.unigrid.janus.ui.controller.WalletController;
import org.unigrid.janus.ui.view.IndexView;
import org.unigrid.janus.web.RecordingWindow;
import org.unigrid.janus.web.Routes;
import org.unigrid.janus.web.SessionToken;
import org.unigrid.janus.web.Templates;
import org.unigrid.janus.web.UiServer;
import org.unigrid.janus.web.action.ActionExtension;
import org.unigrid.janus.web.action.Actions;

/**
 * The Control Center put together the way the shell puts it together: the container builds every controller
 * and finds every action, and only what reaches outside, Hedgehog and the window, is a stand-in. The home
 * folder is a temporary one for as long as the rig lives, so everything the container keeps there is the
 * rig's own.
 */
public class ContainerRig implements AutoCloseable {
	private static final String HOME = "user.home";

	private final String realHome = System.getProperty(HOME);
	private final SessionToken token = SessionToken.random();
	private final RecordingWindow window = new RecordingWindow();
	private final HedgehogStand stand = HedgehogStand.start().signed();
	private final Path home;
	private final Path data;
	private final List<String> addresses;
	private final SeContainer container;
	private final UiServer server;
	private final URI base;

	public ContainerRig() throws Exception {
		home = Files.createTempDirectory("janus-container");
		data = Files.createDirectory(home.resolve("data"));
		addresses = WalletFixture.addresses();

		System.setProperty(HOME, home.toString());
		RigBeans.provide(stand.service(), new DataDirectory(data));
		container = SeContainerInitializer.newInstance().addBeanClasses(RigBeans.class)
			.addExtensions(new RigBeans.Replacing()).initialize();

		final Actions actions = Actions.discovered(container.getBeanManager().getExtension(ActionExtension.class));
		final WalletController wallet = container.select(WalletController.class).get();

		server = new UiServer(Routes.create(new Templates(false), token, window, actions,
			() -> new IndexView(ControlCenterRig.TITLE, wallet.start())
		));
		base = server.start();
	}

	public URI base() {
		return base;
	}

	public URI entrance() {
		return base.resolve("/?" + SessionToken.PARAMETER + "=" + token.value());
	}

	public Screen open() throws Exception {
		return Screen.open(base, entrance());
	}

	public RecordingWindow window() {
		return window;
	}

	public HedgehogStand hedgehog() {
		return stand;
	}

	public SeContainer container() {
		return container;
	}

	public ChosenWallet chosen() {
		return container.select(ChosenWallet.class).get();
	}

	public List<String> addresses() {
		return addresses;
	}

	public Path data() {
		return data;
	}

	/** Where the container keeps the EVM wallets made or restored in it. */
	public Path wallets() {
		return home.resolve(".janus").resolve("wallets");
	}

	public Path home() {
		return home;
	}

	/** A wallet left behind in the folder the legacy daemon used, where the import card finds it. */
	public Path leaveWalletBehind() throws IOException {
		return WalletFixture.copyTo(data.resolve("wallet.dat"));
	}

	/** A wallet.dat in the folder the legacy daemon used that is cut short, so that it cannot be read. */
	public Path leaveDamagedWalletBehind() throws IOException {
		return WalletFixture.damagedTo(data.resolve("wallet.dat"));
	}

	/** A wallet somewhere the person has to point at themselves. */
	public Path keepWalletElsewhere() throws IOException {
		return WalletFixture.copyTo(home.resolve("elsewhere.dat"));
	}

	/** A wallet dump somewhere the person has to point at themselves. */
	public Path keepDumpElsewhere() throws IOException {
		return WalletFixture.copyDumpTo(home.resolve("elsewhere.dump"));
	}

	/** The addresses the keys of that dump give. */
	public List<String> dumpAddresses() throws IOException {
		return WalletFixture.dumpAddresses();
	}

	@Override
	public void close() throws Exception {
		try {
			server.stop();
			container.close();
			stand.close();
		} finally {
			System.setProperty(HOME, realHome);
		}

		try (Stream<Path> paths = Files.walk(home)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}
}
