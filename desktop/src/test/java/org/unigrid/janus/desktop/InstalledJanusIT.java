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
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.unigrid.janus.shell.BrowserWindow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Installs the packages the build produced into clean containers and lets the installed Janus do what it is
 * for: open its window, serve its pages only to the browser it opened, draw the first page with the bundled
 * engine, and quit when the window is closed. On a Wayland desktop it also has to draw the page inside its own
 * window and open the file picker. It runs in the integration phase, after the packages exist, and is skipped
 * where Docker is not available. Its containers are Linux ones, so it runs on Linux only.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(value = 20, unit = TimeUnit.MINUTES)
public class InstalledJanusIT {
	private static final Path DIST = Path.of("target", "dist").toAbsolutePath();
	private static final Path CLOSE_SCRIPT = Path.of("src", "test", "resources", "close-window.py").toAbsolutePath();
	private static final String USER = "janus";
	private static final String LOG = "/tmp/janus.log";
	private static final String SCREEN = "DISPLAY=:99";
	private static final String WINDOW_TITLE = "Unigrid";
	private static final String WINDOW_CLASS = "org-unigrid-janus-shell-Janus";
	private static final String FIRST_PAGE_TEXT = "wallet";
	private static final Pattern SERVING = Pattern.compile("serving its interface at (http://\\S+)");
	private static final Pattern BROKEN_ENGINE = Pattern.compile(
		"UnsatisfiedLinkError|did not initialize|CefInitializationException"
	);
	private static final long INSTALL_SECONDS = 600;
	private static final long SHORT_SECONDS = 60;
	private static final long WAIT_SECONDS = 120;
	private static final long POLL_MILLIS = 2000;

	/* Only what the test itself needs. The libraries Janus runs on have to come in through the package, which
	   is what a person installing it depends on. */
	private static final String DEBIAN_SETUP = """
		export DEBIAN_FRONTEND=noninteractive
		apt-get update -qq
		apt-get install -y -qq xvfb xauth procps curl xdotool imagemagick tesseract-ocr python3-xlib >/dev/null
		""";
	private static final String FEDORA_SETUP = """
		dnf install -y -q xorg-x11-server-Xvfb xorg-x11-xauth procps-ng curl which xdotool ImageMagick \
			tesseract tesseract-langpack-eng python3-xlib >/dev/null
		""";
	private static final String DEBIAN_LIBRARIES = """
		apt-get install -y -qq libgtk-3-0 libnss3 libgbm1 libasound2 libxss1 libxtst6 libxi6 libcups2 \
			libatk-bridge2.0-0 libxkbcommon0 >/dev/null
		""";
	private static final String PACKAGED = "/opt/unigrid";
	private static final String PORTABLE = "/opt/with space/Unigrid";

	private static final String XWAYLAND_SCREEN = "DISPLAY=:$(ls /tmp/.X11-unix | sed s/^X//)";
	private static final Path CLICK_SCRIPT = Path.of("src", "test", "resources", "click.py").toAbsolutePath();
	private static final String PICKER_TITLE = "Choose a wallet.dat or wallet dump";

	/* GNOME on Wayland, the default session of Ubuntu, Fedora and the distributions built on them. GTK 4 comes
	   with every GNOME desktop, and the browser engine loads it there when it is left to run on Wayland. */
	private enum WaylandDesktop {
		UBUNTU("ubuntu:24.04", DEBIAN_SETUP + """
			apt-get install -y -qq weston xwayland libgtk-4-1 python3-websocket >/dev/null
			apt-get install -y -qq /r/unigrid_*.deb >/dev/null
			""", "ubuntu:GNOME"),
		FEDORA("fedora:40", FEDORA_SETUP + """
			dnf install -y -q weston xorg-x11-server-Xwayland gtk4 python3-websocket-client >/dev/null
			dnf install -y -q /r/unigrid-*.rpm >/dev/null
			""", "GNOME");

		private final String image;
		private final String install;
		private final String currentDesktop;

		WaylandDesktop(final String image, final String install, final String currentDesktop) {
			this.image = image;
			this.install = install;
			this.currentDesktop = currentDesktop;
		}
	}

	@BeforeAll
	static void needDockerAndPackages() {
		Assumptions.assumeTrue(Container.dockerAvailable(), "Docker is not available");
		Assumptions.assumeTrue(DIST.resolve("Unigrid").toFile().isDirectory(), "No application image in " + DIST);
	}

	/* The package is built on one release, and has to install on the releases around it whose libraries were
	   renamed in the meantime. */
	@ParameterizedTest
	@ValueSource(strings = { "debian:12", "debian:13", "ubuntu:22.04", "ubuntu:24.04" })
	public void shouldRunFromTheDebianPackage(final String image) throws Exception {
		verify(image, DEBIAN_SETUP + "apt-get install -y -qq /r/unigrid_*.deb >/dev/null", PACKAGED,
			"apt-get install -y -qq --reinstall /r/unigrid_*.deb >/dev/null", "apt-get remove -y -qq unigrid");
	}

	@Test
	public void shouldRunFromTheRpmPackage() throws Exception {
		verify("fedora:40", FEDORA_SETUP + "dnf install -y -q /r/unigrid-*.rpm >/dev/null", PACKAGED,
			"dnf reinstall -y -q /r/unigrid-*.rpm >/dev/null", "dnf remove -y -q unigrid");
	}

	@Test
	public void shouldRunFromAPortableFolderWhoseNameHasASpace() throws Exception {
		verify("debian:12", DEBIAN_SETUP + DEBIAN_LIBRARIES
			+ "mkdir -p '/opt/with space' && cp -r /r/Unigrid '/opt/with space/'", PORTABLE, "true", null);
	}

	/* The window itself is an X11 one even on Wayland, drawn through Xwayland, and the page has to show up
	   inside it rather than in a window of the browser engine's own. What the engine runs on is printed as
	   well, since that is what decides it. */
	@ParameterizedTest
	@EnumSource(WaylandDesktop.class)
	public void shouldDrawThePageInsideItsWindowOnAWaylandDesktop(final WaylandDesktop desktop) throws Exception {
		try (Container container = startOnWayland(desktop)) {
			awaitOutput(container, """
				pgrep -af '[j]cef_helper' | grep -o -- '--ozone-platform=[a-z0-9]*' | sort -u || true
				window=$(%s xdotool search --name '^%s$' | head -1)
				%s import -window "$window" -resize 200%% png:- | tesseract stdin stdout
				""".formatted(XWAYLAND_SCREEN, WINDOW_TITLE, XWAYLAND_SCREEN),
				output -> output.toLowerCase().contains(FIRST_PAGE_TEXT),
				"the first page drawn in the window");
		}
	}

	@ParameterizedTest
	@EnumSource(WaylandDesktop.class)
	public void shouldOpenTheFilePickerOnAWaylandDesktop(final WaylandDesktop desktop) throws Exception {
		try (Container container = startOnWayland(desktop)) {
			asUser(container, "python3 /tmp/click.py \"[hx-post='/action/import']\" '[data-choose-file]'",
				"asking for the file picker");
			awaitOutput(container, XWAYLAND_SCREEN + " xdotool search --name '^" + PICKER_TITLE + "$'",
				output -> true, "the file picker");
		}
	}

	private static Container startOnWayland(final WaylandDesktop desktop) throws IOException, InterruptedException {
		final Container container = Container.start(desktop.image, DIST);

		try {
			succeeds(container, "root", INSTALL_SECONDS, desktop.install + "mkdir -p -m 1777 /tmp/.X11-unix",
				"installing");
			container.copyIn(CLICK_SCRIPT, "/tmp/click.py");
			succeeds(container, "root", SHORT_SECONDS, "useradd -m " + USER, "creating the user");
			asUser(container, """
				export XDG_RUNTIME_DIR=/tmp/runtime-%1$s
				mkdir -p -m 0700 "$XDG_RUNTIME_DIR"
				setsid weston --backend=headless --renderer=pixman --xwayland --socket=wayland-1 \
					--width=1600 --height=1000 >/tmp/weston.log 2>&1 &
				for second in $(seq 30); do ls /tmp/.X11-unix/X* >/dev/null 2>&1 && break; sleep 1; done
				ls /tmp/.X11-unix/X* >/dev/null 2>&1 || { cat /tmp/weston.log; exit 1; }
				%2$s XDG_SESSION_TYPE=wayland WAYLAND_DISPLAY=wayland-1 XDG_CURRENT_DESKTOP=%3$s \
					JAVA_TOOL_OPTIONS=-D%4$s=9222 setsid nohup %5$s/bin/Unigrid >%6$s 2>&1 &
				""".formatted(USER, XWAYLAND_SCREEN, desktop.currentDesktop, BrowserWindow.DEBUGGING_PORT,
					PACKAGED, LOG),
				"starting Janus on Wayland");
			awaitAddress(container);
			return container;
		} catch (IOException | InterruptedException | RuntimeException | Error e) {
			container.close();
			throw e;
		}
	}

	/* The removal command is null where nothing was installed by a package, which has no menu entry to look for. */
	private void verify(final String image, final String install, final String home, final String reinstall,
		final String removal) throws Exception {

		try (Container container = Container.start(image, DIST)) {
			succeeds(container, "root", INSTALL_SECONDS, install, "installing");
			container.copyIn(CLOSE_SCRIPT, "/tmp/close-window.py");
			succeeds(container, "root", SHORT_SECONDS, "useradd -m " + USER, "creating the user");

			if (removal != null) {
				assertIsInTheApplicationMenu(container);
			}

			start(container, home + "/bin/Unigrid");

			assertRefusesRequestsWithoutTheToken(container, awaitAddress(container));
			assertRunsTheBundledEngine(container, home + "/lib/app/jcef");
			assertShowsTheFirstPage(container);

			if (removal != null) {
				assertWindowIsOfTheClassTheMenuEntryNames(container);
			}

			assertQuitsWhenTheWindowCloses(container);
			final String log = asUser(container, "cat " + LOG, "reading the log").output();

			assertFalse(BROKEN_ENGINE.matcher(log).find(), "The engine reported a fault");

			succeeds(container, "root", INSTALL_SECONDS, reinstall + "\ntest -x '" + home + "/bin/Unigrid'",
				"installing the same package again");

			if (removal != null) {
				assertRemovalLeavesNothingBehind(container, removal, home);
			}
		}
	}

	/* A container has no desktop menu to register the entry with, which is why the install does not insist on
	   it, so it is the entry the package ships that is looked at: filed under Network, opening the launcher and
	   naming the class of the window, so that a dock can tell which window belongs to it. */
	private static void assertIsInTheApplicationMenu(final Container container)
		throws IOException, InterruptedException {

		succeeds(container, "root", SHORT_SECONDS, """
			entry=%1$s/lib/unigrid-Unigrid.desktop
			grep -q '^Categories=Network;$' "$entry"
			grep -q '^Exec=%1$s/bin/Unigrid$' "$entry"
			grep -q '^Icon=%1$s/lib/Unigrid.png$' "$entry"
			grep -q '^StartupWMClass=%2$s$' "$entry"
			test -f %1$s/lib/Unigrid.png
			""".formatted(PACKAGED, WINDOW_CLASS), "finding the menu entry the package ships");
	}

	private static void assertWindowIsOfTheClassTheMenuEntryNames(final Container container)
		throws IOException, InterruptedException {

		awaitOutput(container, SCREEN + " xdotool search --class '^" + WINDOW_CLASS + "$'", output -> true,
			"a window of the class " + WINDOW_CLASS);
	}

	private static void assertRemovalLeavesNothingBehind(final Container container, final String removal,
		final String home) throws IOException, InterruptedException {

		succeeds(container, "root", INSTALL_SECONDS, removal, "removing the package");
		succeeds(container, "root", SHORT_SECONDS, """
			test ! -e %1$s/bin/Unigrid
			test ! -e %1$s/lib/unigrid-Unigrid.desktop
			""".formatted(home), "checking that the removal took the program and its menu entry");
	}

	private static void start(final Container container, final String launcher)
		throws IOException, InterruptedException {

		asUser(container, """
			setsid Xvfb :99 -screen 0 1600x1000x24 >/dev/null 2>&1 &
			sleep 2
			%s setsid nohup '%s' >%s 2>&1 &
			""".formatted(SCREEN, launcher, LOG), "starting Janus");
	}

	private static void assertRefusesRequestsWithoutTheToken(final Container container, final String address)
		throws IOException, InterruptedException {

		assertEquals("403", asUser(container, "curl -s -o /dev/null -w '%{http_code}' " + address + "/",
			"asking for the page without the token").output().trim());
	}

	private static void assertRunsTheBundledEngine(final Container container, final String engine)
		throws IOException, InterruptedException {

		awaitOutput(container, "pgrep -af '[j]cef_helper' || true", output -> output.contains(engine),
			"the browser engine running from " + engine);
		asUser(container, "test ! -e ~/.janus/jcef", "checking that no second engine was downloaded");
	}

	private static void assertShowsTheFirstPage(final Container container)
		throws IOException, InterruptedException {

		awaitOutput(container, SCREEN + " xdotool search --name '^" + WINDOW_TITLE + "$'", output -> true,
			"the window");
		awaitOutput(container, SCREEN + " import -window root -resize 200% png:- | tesseract stdin stdout",
			output -> output.toLowerCase().contains(FIRST_PAGE_TEXT), "the first page drawn in the window");
	}

	private static void assertQuitsWhenTheWindowCloses(final Container container)
		throws IOException, InterruptedException {

		/* The launcher runs Java inside its own process, so what there is to look for is the launcher's name. */
		asUser(container, "pgrep -x Unigrid", "finding Janus running before its window is closed");
		asUser(container, SCREEN + " python3 /tmp/close-window.py " + WINDOW_TITLE, "closing the window");
		awaitOutput(container, "pgrep -x Unigrid || echo gone", "gone\n"::equals,
			"Janus quitting after its window was closed");
	}

	private static String awaitAddress(final Container container) throws IOException, InterruptedException {
		final Matcher matcher = SERVING.matcher(awaitOutput(container, "cat " + LOG + " || true",
			output -> SERVING.matcher(output).find(), "Janus serving its interface"));

		matcher.find();
		return matcher.group(1).replaceAll("/+$", "");
	}

	private static Container.Result asUser(final Container container, final String script, final String doing)
		throws IOException, InterruptedException {

		return succeeds(container, USER, SHORT_SECONDS, script, doing);
	}

	private static Container.Result succeeds(final Container container, final String user, final long seconds,
		final String script, final String doing) throws IOException, InterruptedException {

		final Container.Result result = container.exec(user, seconds, script);

		if (result.exit() != 0) {
			fail("Failed " + doing + ":\n" + result.output() + "\nJanus log:\n" + log(container));
		}

		return result;
	}

	private static String log(final Container container) throws IOException, InterruptedException {
		return container.exec(USER, SHORT_SECONDS, "cat " + LOG + " || true").output();
	}

	private static String awaitOutput(final Container container, final String script, final Predicate<String> expected,
		final String what) throws IOException, InterruptedException {

		final long deadline = System.currentTimeMillis() + WAIT_SECONDS * 1000;
		String output = "";

		while (System.currentTimeMillis() < deadline) {
			final Container.Result result = container.exec(USER, SHORT_SECONDS, script);
			output = result.output();

			if (result.exit() == 0 && expected.test(output)) {
				return output;
			}

			Thread.sleep(POLL_MILLIS);
		}

		throw new AssertionError("Timed out waiting for " + what + ". Last output:\n" + output
			+ "\nJanus log:\n" + log(container));
	}
}
