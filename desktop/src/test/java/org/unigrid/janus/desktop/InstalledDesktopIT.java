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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the installed package does for the desktop it was installed on, looked for after a real install on this
 * computer: the menu entry and the programs list on Windows, the bundle description and icon on macOS, and the
 * menu entry the package ships on Linux. A runner has no desktop menu to register that entry with, so it is the
 * file itself that is looked at. The removal comes last, and what it should take with it is looked for then. It
 * installs software, so it runs only where janus.installed-wallet is true, which only the workflow sets.
 */
@EnabledIfSystemProperty(named = "janus.installed-wallet", matches = "true")
@Timeout(value = 20, unit = TimeUnit.MINUTES)
public class InstalledDesktopIT {
	private static final Path DIST = Path.of("target", "dist").toAbsolutePath();
	private static final Path EVIDENCE = Path.of("target", "installed-desktop").toAbsolutePath();
	private static final String WINDOW_CLASS = "org-unigrid-janus-shell-Janus";
	private static final String UNINSTALL = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
	private static final int MINIMUM_ICON_SIZES = 7;

	@TempDir
	private static Path scratch;

	private static Path launcher;
	private static Path menuEntry;

	@BeforeAll
	static void install() throws Exception {
		Files.createDirectories(EVIDENCE);
		launcher = Installation.install(DIST, scratch, EVIDENCE);

		if (!Installation.WINDOWS && !Installation.MAC) {
			menuEntry = launcher.getParent().getParent().resolve("lib").resolve("unigrid-Unigrid.desktop");
		}
	}

	/* The removal is the last thing, once everything the install left has been looked at. */
	@AfterAll
	static void removeAndLookForWhatIsLeft() throws Exception {
		Installation.uninstall(DIST, EVIDENCE);

		if (!Installation.MAC) {
			assertFalse(Files.exists(launcher), "The program stayed after the removal");
		}

		if (Installation.WINDOWS) {
			assertFalse(Files.exists(startMenuShortcut()), "The Start menu shortcut stayed after the removal");
		}

		if (menuEntry != null) {
			assertFalse(Files.exists(menuEntry), "The menu entry stayed after the removal");
		}
	}

	private static Path startMenuShortcut() {
		return Path.of(System.getenv("APPDATA"), "Microsoft", "Windows", "Start Menu", "Programs", "Unigrid",
			"Unigrid.lnk");
	}

	private static String output(final String... command) throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		process.waitFor();
		return output;
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	public void shouldPutUnigridInTheStartMenu() {
		assertTrue(Files.isRegularFile(startMenuShortcut()), "No Start menu shortcut at " + startMenuShortcut());
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	public void shouldListUnigridAmongTheInstalledPrograms() throws Exception {
		final String listed = output("reg", "query", "HKCU" + UNINSTALL, "/s", "/f", "Unigrid")
			+ output("reg", "query", "HKLM" + UNINSTALL, "/s", "/f", "Unigrid");

		assertTrue(listed.contains("Stiftelsen The Unigrid Foundation"),
			"The programs list has no Unigrid:\n" + listed);
	}

	@Test
	@EnabledOnOs(OS.MAC)
	public void shouldDescribeTheBundleToFinder() throws Exception {
		final Path info = scratch.resolve("Unigrid.app").resolve("Contents").resolve("Info.plist");
		final String description = output("plutil", "-p", info.toString());

		assertTrue(description.contains("\"CFBundleIdentifier\" => \"org.unigrid.janus\""), description);
		assertTrue(description.contains("\"LSApplicationCategoryType\" => \"public.app-category.utilities\""),
			description);
		assertTrue(description.contains("\"CFBundleIconFile\" => \"Unigrid.icns\""), description);
	}

	@Test
	@EnabledOnOs(OS.MAC)
	public void shouldCarryAnIconDrawnForEverySize() throws Exception {
		final Path icons = scratch.resolve("Unigrid.iconset");
		final Path icon = scratch.resolve("Unigrid.app").resolve("Contents").resolve("Resources")
			.resolve("Unigrid.icns");

		output("iconutil", "--convert", "iconset", "--output", icons.toString(), icon.toString());

		try (Stream<Path> files = Files.list(icons)) {
			assertTrue(files.count() >= MINIMUM_ICON_SIZES, "The icon holds too few sizes");
		}
	}

	@Test
	@EnabledOnOs(OS.LINUX)
	public void shouldShipAMenuEntryUnderNetworkThatPointsAtTheProgram() throws Exception {
		assertTrue(Files.isRegularFile(menuEntry), "The package shipped no menu entry at " + menuEntry);

		final String entry = Files.readString(menuEntry);

		assertTrue(entry.contains("\nCategories=Network;\n"), entry);
		assertTrue(entry.contains("\nExec=" + launcher + "\n"), entry);
		assertTrue(entry.contains("\nStartupWMClass=" + WINDOW_CLASS + "\n"), entry);
	}
}
