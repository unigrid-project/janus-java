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

import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Unigrid", description = "The Unigrid Control Center.")
public final class LaunchOptions {
	@Option(names = "--window-system", paramLabel = "<system>", defaultValue = "x11",
		description = "What the page is drawn on in a Linux desktop session: x11, wayland or auto "
			+ "(defaults to ${DEFAULT-VALUE}, which embeds it in the window on X11 and Wayland desktops alike)."
	)
	private WindowSystem windowSystem;

	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Show this help and exit.")
	private boolean help;

	private LaunchOptions() {
	}

	/** Empty when the help was asked for, which has then been printed instead. */
	public static Optional<LaunchOptions> of(final String... args) {
		final LaunchOptions options = new LaunchOptions();
		final CommandLine commandLine = new CommandLine(options).setCaseInsensitiveEnumValuesAllowed(true);

		return CommandLine.printHelpIfRequested(commandLine.parseArgs(args)) ? Optional.empty()
			: Optional.of(options);
	}

	public WindowSystem windowSystem() {
		return windowSystem;
	}
}
