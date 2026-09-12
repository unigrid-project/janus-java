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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.unigrid.janus.web.WindowControl;

public class FrameControl implements WindowControl {
	private static final int FOLLOW_INTERVAL = 10;
	private static final Dimension MINIMUM = new Dimension(900, 640);

	private final JFrame frame;
	private final Timer follow = new Timer(FOLLOW_INTERVAL, event -> follow());

	private Consumer<Point> tracking;

	public FrameControl(final JFrame frame) {
		this.frame = frame;
	}

	@Override
	public void minimise() {
		onSwingThread(() -> frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED));
	}

	@Override
	public void toggleMaximise() {
		onSwingThread(() -> frame.setExtendedState(maximised() ? Frame.NORMAL : Frame.MAXIMIZED_BOTH));
	}

	@Override
	public void close() {
		/* Routed through the window listener so that closing from the page tears Chromium
		   down the same way closing from the desktop does. */
		onSwingThread(() -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
	}

	@Override
	public void beginMove() {
		onSwingThread(() -> {
			final Point pointer = pointer();

			if (pointer == null) {
				return;
			}

			/* A maximised window cannot be dragged anywhere useful, so pulling on the
			   title bar restores it first, the way a native one behaves. */
			if (maximised()) {
				frame.setExtendedState(Frame.NORMAL);
			}

			final Point grip = new Point(pointer.x - frame.getX(), pointer.y - frame.getY());

			track(at -> frame.setLocation(at.x - grip.x, at.y - grip.y));
		});
	}

	@Override
	public void endMove() {
		onSwingThread(follow::stop);
	}

	@Override
	public void beginResize(final Edge edge) {
		onSwingThread(() -> {
			final Point pointer = pointer();

			if (pointer == null || maximised()) {
				return;
			}

			final Rectangle origin = frame.getBounds();

			track(at -> frame.setBounds(
				ResizeBounds.resized(edge, origin, new Point(at.x - pointer.x, at.y - pointer.y), MINIMUM)
			));
		});
	}

	@Override
	public void endResize() {
		onSwingThread(follow::stop);
	}

	private void track(final Consumer<Point> placement) {
		tracking = placement;
		follow.start();
	}

	private void follow() {
		final Point pointer = pointer();

		if (pointer != null) {
			tracking.accept(pointer);
		}
	}

	private boolean maximised() {
		return (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	private Point pointer() {
		final PointerInfo info = MouseInfo.getPointerInfo();

		/* Absent whenever the pointer is on another screen or the display cannot be
		   queried, in which case there is nothing sensible to move towards. */
		return info == null ? null : info.getLocation();
	}

	private void onSwingThread(final Runnable action) {
		SwingUtilities.invokeLater(action);
	}
}
