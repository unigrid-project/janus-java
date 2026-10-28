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

package org.unigrid.janus.shell;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unigrid.janus.ui.controller.WalletController;
import org.unigrid.janus.ui.view.IndexView;
import org.unigrid.oriel.server.AppId;
import org.unigrid.oriel.server.Routes;
import org.unigrid.oriel.server.SessionToken;
import org.unigrid.oriel.server.UiServer;
import org.unigrid.oriel.server.action.ActionExtension;
import org.unigrid.oriel.server.action.Actions;
import org.unigrid.oriel.server.template.Templates;
import org.unigrid.oriel.window.Application;
import org.unigrid.oriel.window.BrowserWindow;
import org.unigrid.oriel.window.WindowIcon;
import org.unigrid.oriel.window.launch.LaunchOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public final class Janus {
	/* Logging starts when the logger below is made, and writes its files to the folder this makes. */
	static {
		LogFolder.prepare(Path.of(System.getProperty("user.home")));
	}

	private static final Logger LOG = LoggerFactory.getLogger(Janus.class);
	private static final String TITLE = "Unigrid Control Center";
	private static final int USAGE_ERROR = 2;

	private Janus() {
	}

	public static void main(final String[] args) throws Exception {
		final CommandLine command = command();
		final Optional<LaunchOptions> options;

		try {
			options = options(command, args);
		} catch (IllegalArgumentException | CommandLine.ParameterException e) {
			refuse(command, e);
			System.exit(USAGE_ERROR);
			return;
		}

		if (options.isPresent()) {
			start(application(), options.get());
		}
	}

	/*
	 * The command line is Janus's: Oriel takes out the window system and Janus reads what is left, refusing what
	 * it does not know so that a mistyped option is never quietly ignored. Empty when the help was asked for,
	 * which is printed plainly on the command's own output; an option Janus or Oriel does not take is thrown,
	 * so that main alone decides how Janus ends.
	 */
	static Optional<LaunchOptions> options(final CommandLine command, final String... args) {
		final LaunchOptions options = LaunchOptions.of(args);

		if (command.parseArgs(options.remaining().toArray(String[]::new)).isUsageHelpRequested()) {
			command.usage(command.getOut());
			command.getOut().flush();
			return Optional.empty();
		}

		return Optional.of(options);
	}

	/* A refused command line is answered on the command's error output, the way picocli answers it itself. */
	static void refuse(final CommandLine command, final RuntimeException refused) {
		command.getErr().println(refused.getMessage());
		command.usage(command.getErr());
		command.getErr().flush();
	}

	/* What Janus takes, with the window system Oriel takes listed in the help where it can be chosen. */
	static CommandLine command() {
		final CommandLine command = new CommandLine(new Arguments());

		LaunchOptions.help().ifPresent(line -> command.getCommandSpec().usageMessage().footer("", "Window:",
			"  " + line));
		return command;
	}

	/* The window and the message shown when it cannot open both go by this name. */
	private static Application application() {
		return new Application(AppId.of("janus"), "Unigrid", WindowIcon.images(Janus.class, "icons/unigrid-%d.png"),
			new Dimension(1240, 800), new Dimension(900, 640)
		);
	}

	private static void start(final Application application, final LaunchOptions options) throws Exception {
		final SeContainer container = SeContainerInitializer.newInstance().initialize();
		final Actions actions = Actions.discovered(
			container.getBeanManager().getExtension(ActionExtension.class)
		);

		final BrowserWindow window = new BrowserWindow(application, options.windowSystem());
		final SessionToken token = SessionToken.random();
		final WalletController wallet = container.select(WalletController.class).get();
		final UiServer server = new UiServer(Routes.create(application.id(), new Templates(false), token,
			window.control(), actions, () -> new IndexView(TITLE, wallet.start())
		));
		final URI uri = server.start();

		LOG.info("Janus is serving its interface at {}", uri);

		/* The token rides on the first navigation only; the server swaps it for a cookie and
		   redirects, so it does not linger in the address. */
		try {
			window.open(uri.resolve("/?" + SessionToken.PARAMETER + "=" + token.value()));
		} catch (Exception | LinkageError e) {
			giveUp(application, server, e);
		}
	}

	/* The server's threads would keep the process alive with no window to reach it, and a launcher from
	   a menu has no console to show the error in. */
	private static void giveUp(final Application application, final UiServer server, final Throwable cause)
		throws Exception {

		LOG.error("Janus could not open its window", cause);
		server.stop();

		if (!GraphicsEnvironment.isHeadless()) {
			final String message = EngineFailure.message(application, cause);

			JOptionPane.showMessageDialog(null, message, TITLE, JOptionPane.ERROR_MESSAGE);
		}

		System.exit(1);
	}

	/** What Janus itself takes on the command line, besides the window system Oriel takes. */
	@Command(name = "Unigrid", description = "The Unigrid Control Center.")
	private static final class Arguments {
		@Option(names = { "-h", "--help" }, usageHelp = true, description = "Show this help and exit.")
		private boolean help;
	}
}
