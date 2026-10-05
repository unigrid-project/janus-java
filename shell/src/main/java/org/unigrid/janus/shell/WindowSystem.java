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

/**
 * What the browser engine draws the page on in a Linux desktop session. The frame around it is an X11 window
 * even on a Wayland desktop, drawn through Xwayland, and the engine can only be embedded in it from X11. Left
 * to choose, it runs on Wayland there and draws the page in a window of its own, and on GNOME it loads GTK 4,
 * which deadlocks the GTK 3 the file picker needs.
 */
public enum WindowSystem {
	X11("--ozone-platform=x11"),
	WAYLAND("--ozone-platform=wayland"),
	AUTO("--ozone-platform-hint=auto");

	/** Only a Linux desktop session leaves the engine a window system to choose. */
	static final boolean CHOOSABLE = System.getProperty("os.name").startsWith("Linux");

	private final String engineSwitch;

	WindowSystem(final String engineSwitch) {
		this.engineSwitch = engineSwitch;
	}

	public String engineSwitch() {
		return engineSwitch;
	}
}
