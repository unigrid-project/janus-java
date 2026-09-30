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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Installs the packages the build produced into clean containers and lets the installed Janus do what it is
 * for: open its window, serve its pages only to the browser it opened, draw the first page with the bundled
 * engine, and quit when the window is closed. It runs in the integration phase, after the packages exist,
 * and is skipped where Docker is not available.
 */
@Timeout(value = 20, unit = TimeUnit.MINUTES)
public class InstalledJanusIT {
	private static final Path DIST = Path.of("target", "dist").toAbsolutePath();
	private static final Path CLOSE_SCRIPT = Path.of("src", "test", "resources", "close-window.py").toAbsolutePath();
	private static final String USER = "janus";
	private static final String LOG = "/tmp/janus.log";
	private static final String SCREEN = "DISPLAY=:99";
	private static final String WINDOW_TITLE = "Unigrid";
	private static final String FIRST_PAGE_TEXT = "wallet";
	private static final Pattern SERVING = Pattern.compile("serving its interface at (http://\\S+)");
	private static final Pattern BROKEN_ENGINE = Pattern.compile(
		"UnsatisfiedLinkError|did not initialize|CefInitializationException"
	);
	private static final long INSTALL_SECONDS = 600;
	private static final long SHORT_SECONDS = 60;
	private static final long WAIT_SECONDS = 120;
	private static final long POLL_MILLIS = 2000;

	private static final String DEBIAN_SETUP = """
		apt-get update -qq
		apt-get install -y -qq xvfb xauth procps curl xdotool imagemagick tesseract-ocr python3-xlib \
			libgtk-3-0 libnss3 libgbm1 libasound2 libxss1 libxtst6 libxi6 libcups2 libatk-bridge2.0-0 \
			libxkbcommon0 >/dev/null
		""";
	private static final String FEDORA_SETUP = """
		dnf install -y -q xorg-x11-server-Xvfb xorg-x11-xauth procps-ng curl which xdotool ImageMagick \
			tesseract tesseract-langpack-eng python3-xlib gtk3 nss mesa-libgbm alsa-lib libXScrnSaver \
			libXtst >/dev/null
		""";
	private static final String PACKAGED = "/opt/unigrid";
	private static final String PORTABLE = "/opt/with space/Unigrid";

	@BeforeAll
	static void needDockerAndPackages() {
		Assumptions.assumeTrue(Container.dockerAvailable(), "Docker is not available");
		Assumptions.assumeTrue(DIST.resolve("Unigrid").toFile().isDirectory(), "No application image in " + DIST);
	}

	@Test
	public void shouldRunFromTheDebianPackage() throws Exception {
		verify("debian:12", DEBIAN_SETUP + "apt-get install -y -qq /r/unigrid_*.deb >/dev/null", PACKAGED,
			"apt-get install -y -qq --reinstall /r/unigrid_*.deb >/dev/null");
	}

	@Test
	public void shouldRunFromTheRpmPackage() throws Exception {
		verify("fedora:40", FEDORA_SETUP + "dnf install -y -q /r/unigrid-*.rpm >/dev/null", PACKAGED,
			"dnf reinstall -y -q /r/unigrid-*.rpm >/dev/null");
	}

	@Test
	public void shouldRunFromAPortableFolderWhoseNameHasASpace() throws Exception {
		verify("debian:12", DEBIAN_SETUP + "mkdir -p '/opt/with space' && cp -r /r/Unigrid '/opt/with space/'",
			PORTABLE, "true");
	}

	private void verify(final String image, final String install, final String home, final String reinstall)
		throws Exception {

		try (Container container = Container.start(image, DIST)) {
			succeeds(container, "root", INSTALL_SECONDS, install, "installing");
			container.copyIn(CLOSE_SCRIPT, "/tmp/close-window.py");
			succeeds(container, "root", SHORT_SECONDS, "useradd -m " + USER, "creating the user");

			start(container, home + "/bin/Unigrid");

			assertRefusesRequestsWithoutTheToken(container, awaitAddress(container));
			assertRunsTheBundledEngine(container, home + "/lib/app/jcef");
			assertShowsTheFirstPage(container);
			assertQuitsWhenTheWindowCloses(container);
			final String log = asUser(container, "cat " + LOG, "reading the log").output();

			assertFalse(BROKEN_ENGINE.matcher(log).find(), "The engine reported a fault");

			succeeds(container, "root", INSTALL_SECONDS, reinstall + "\ntest -x '" + home + "/bin/Unigrid'",
				"installing the same package again");
		}
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

		assertTrue(asUser(container, "pgrep -af '[j]cef_helper'", "finding the engine").output().contains(engine),
			"The browser engine must run from " + engine);
		asUser(container, "test ! -e ~/.janus/jcef", "checking that no second engine was downloaded");
	}

	private static void assertShowsTheFirstPage(final Container container)
		throws IOException, InterruptedException {

		asUser(container, SCREEN + " xdotool search --name '^" + WINDOW_TITLE + "$'", "finding the window");
		awaitOutput(container, SCREEN + " import -window root -resize 200% png:- | tesseract stdin stdout",
			output -> output.toLowerCase().contains(FIRST_PAGE_TEXT), "the first page drawn in the window");
	}

	private static void assertQuitsWhenTheWindowCloses(final Container container)
		throws IOException, InterruptedException {

		asUser(container, SCREEN + " python3 /tmp/close-window.py " + WINDOW_TITLE, "closing the window");
		awaitOutput(container, "pgrep -f '[l]ib/runtime/bin/java' || echo gone", "gone\n"::equals,
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

		assertEquals(0, result.exit(), "Failed " + doing + ":\n" + result.output());
		return result;
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

		final String log = container.exec(USER, SHORT_SECONDS, "cat " + LOG + " || true").output();
		throw new AssertionError("Timed out waiting for " + what + ". Last output:\n" + output
			+ "\nJanus log:\n" + log);
	}
}
