"""Asks the window with the given title to close, the way a window manager does when its close
button is pressed: by sending WM_DELETE_WINDOW. Exits with 1 when no such window exists."""
import sys

from Xlib import X, display, protocol


def find(window, title):
    try:
        if window.get_wm_name() == title:
            return window
        children = window.query_tree().children
    except Exception:
        return None

    for child in children:
        found = find(child, title)

        if found:
            return found

    return None


screen = display.Display()
target = find(screen.screen().root, sys.argv[1])

if target is None:
    sys.exit(1)

event = protocol.event.ClientMessage(
    window=target,
    client_type=screen.intern_atom("WM_PROTOCOLS"),
    data=(32, [screen.intern_atom("WM_DELETE_WINDOW"), X.CurrentTime, 0, 0, 0]),
)
target.send_event(event, event_mask=X.NoEventMask)
screen.sync()
