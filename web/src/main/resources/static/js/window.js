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

/* Chromium owns the window contents, so the only way to reach the frame around it
   is to ask the host that opened it. */
const command = (name) => fetch("/window/" + name, { method: "POST" });

document.addEventListener("click", (event) => {
	const button = event.target.closest("[data-window]");

	if (button) {
		command(button.dataset.window);
	}
});

const bar = document.querySelector("[data-drag]");

if (bar) {
	bar.addEventListener("pointerdown", (event) => {
		if (event.button !== 0 || event.target.closest("[data-window]")) {
			return;
		}

		/* Capturing keeps the release with the title bar even when the pointer has run
		   ahead of the window, which it always does during a fast drag. */
		bar.setPointerCapture(event.pointerId);
		document.body.classList.add("is-dragging");
		command("move/start");
	});

	const release = () => {
		document.body.classList.remove("is-dragging");
		command("move/end");
	};

	bar.addEventListener("pointerup", release);
	bar.addEventListener("pointercancel", release);
	bar.addEventListener("dblclick", () => command("maximise"));
}
