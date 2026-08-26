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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.unigrid.janus.web.WindowControl;

public class BrowserWindow {
	private static final File INSTALL_DIR = new File(System.getProperty("user.home"), ".janus/jcef");
	private static final Dimension SIZE = new Dimension(1100, 720);
	private static final String TITLE = "Unigrid";

	private final JFrame frame = new JFrame(TITLE);
	private final FrameControl control = new FrameControl(frame);

	public BrowserWindow() {
		/* The title bar and its buttons are drawn by the page, so the frame contributes
		   nothing but a rectangle. Decoration has to be settled before the frame is
		   realised, which is why it happens here rather than alongside the sizing. */
		frame.setUndecorated(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent event) {
				CefApp.getInstance().dispose();
				frame.dispose();
			}
		});
	}

	public WindowControl control() {
		return control;
	}

	public void open(final URI uri) throws Exception {
		final CefAppBuilder builder = new CefAppBuilder();

		builder.setInstallDir(INSTALL_DIR);
		builder.setProgressHandler(new ConsoleProgressHandler());
		builder.getCefSettings().windowless_rendering_enabled = false;

		/* Chromium keeps running after the last window closes, so the process has to be
		   torn down explicitly or the wallet lingers with no way to reach it. */
		builder.setAppHandler(new MavenCefAppHandlerAdapter() {
			@Override
			public void stateHasChanged(final CefApp.CefAppState state) {
				if (state == CefApp.CefAppState.TERMINATED) {
					System.exit(0);
				}
			}
		});

		final CefClient client = builder.build().createClient();

		/* The browser has to be in the content pane before the frame is shown. Attaching it
		   afterwards leaves Chromium to open a top level window of its own, and the frame
		   stays empty. */
		frame.getContentPane().add(
			client.createBrowser(uri.toString(), false, false).getUIComponent(),
			BorderLayout.CENTER
		);

		SwingUtilities.invokeLater(() -> {
			frame.setSize(SIZE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
