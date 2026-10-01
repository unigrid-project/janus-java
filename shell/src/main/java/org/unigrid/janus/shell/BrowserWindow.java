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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.time.Duration;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unigrid.janus.web.WindowControl;

public class BrowserWindow {
	public static final String DEBUGGING_PORT = "janus.remote-debugging-port";

	private static final Logger LOG = LoggerFactory.getLogger(BrowserWindow.class);
	private static final Dimension SIZE = new Dimension(1240, 800);
	private static final Duration SHUTDOWN_GRACE = Duration.ofSeconds(10);
	private static final String TITLE = "Unigrid";

	private final JFrame frame = new JFrame(TITLE);
	private final FrameControl control = new FrameControl(frame);

	public BrowserWindow() {
		/* The title bar and its buttons are drawn by the page, so the frame contributes
		   nothing but its outline. Decoration has to be settled before the frame is
		   realised, which is why it happens here rather than alongside the sizing. */
		frame.setUndecorated(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setIconImages(WindowIcon.images());
		fitShapeToFrame();
	}

	private void fitShapeToFrame() {
		if (!shapedWindowsSupported()) {
			return;
		}

		/* The state changes from the window manager as well as from the page, and the
		   resize that comes with it is not ordered against the state change, so the
		   shape follows both. */
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent event) {
				fitShape();
			}
		});
		frame.addWindowStateListener(event -> fitShape());
	}

	private void fitShape() {
		frame.setShape(WindowShape.of(frame.getExtendedState(), frame.getSize()).orElse(null));
	}

	private boolean shapedWindowsSupported() {
		return frame.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(
			GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT
		);
	}

	public WindowControl control() {
		return control;
	}

	public void open(final URI uri) throws Exception {
		final CefAppBuilder builder = new CefAppBuilder();
		final CefLocation location = new CefLocation();

		builder.setInstallDir(location.directory());

		/* The folder of an installed application is not ours to write to, so a missing or damaged engine
		   there has to fail with the engine's own error rather than trigger a download or a deletion. */
		builder.setSkipInstallation(location.isBundled());
		builder.setProgressHandler(new ConsoleProgressHandler());
		builder.getCefSettings().windowless_rendering_enabled = false;

		/* Lets the installer tests work the installed wallet's window; nothing listens unless a port is named. */
		final Integer debuggingPort = Integer.getInteger(DEBUGGING_PORT);

		if (debuggingPort != null) {
			builder.getCefSettings().remote_debugging_port = debuggingPort;
		}

		/* Chromium keeps running after the last window closes, so the process has to be
		   torn down explicitly or the wallet lingers with no way to reach it. */
		builder.setAppHandler(new MavenCefAppHandlerAdapter() {
			@Override
			public void stateHasChanged(final CefApp.CefAppState state) {
				LOG.info("The browser engine is {}", state);

				if (state == CefApp.CefAppState.TERMINATED) {
					System.exit(0);
				}
			}
		});

		final CefClient client = builder.build().createClient();
		final CefBrowser browser = client.createBrowser(uri.toString(), false, false);

		frame.addWindowListener(shutDownOnClose(browser));

		/* The browser has to be in the content pane before the frame is shown. Attaching it
		   afterwards leaves Chromium to open a top level window of its own, and the frame
		   stays empty. */
		frame.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);

		/* The browser engine creates the browser the first time its component is painted. On Windows the
		   engine's own native child covers the whole of the undecorated frame, which leaves Windows nothing
		   to ask the frame to paint, so the component is asked to paint itself. */
		SwingUtilities.invokeLater(() -> {
			frame.setSize(SIZE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			browser.getUIComponent().repaint();
		});
	}

	/* Chromium answers a close request by asking the window to close and cancels its own
	   unless the browser has been told that closing is allowed, so without that permission
	   the browser, and with it the process, lives on. The frame is hidden rather than
	   disposed because the shutdown completes on the event thread, which AWT retires once
	   the last window is gone. On macOS the engine now and then never finishes shutting
	   down, which would leave Janus running with no window, so it gets a deadline. */
	private WindowAdapter shutDownOnClose(final CefBrowser browser) {
		return new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent event) {
				LOG.info("The window is closing");
				frame.setVisible(false);
				browser.setCloseAllowed();
				CefApp.getInstance().dispose();
				ShutdownDeadline.start(SHUTDOWN_GRACE, () -> {
					LOG.warn("The browser engine did not shut down in time; ending anyway");
					System.exit(0);
				});
			}
		};
	}
}
