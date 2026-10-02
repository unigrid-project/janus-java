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

/* A password that is pasted is one the person has not typed twice by heart, so the second box proves nothing.
   Every way text reaches a field without a key press is refused on a password field: Ctrl+V and Shift+Insert,
   the context menu and its Paste, a middle click, and a drop. The listeners sit on the document because the
   cards that hold the fields are swapped in and out. */
const isPasswordField = (event) => event.target instanceof HTMLInputElement && event.target.type === "password";

for (const type of ["paste", "drop", "contextmenu"]) {
	document.addEventListener(type, (event) => {
		if (isPasswordField(event)) {
			event.preventDefault();
		}
	});
}

document.addEventListener("beforeinput", (event) => {
	if (isPasswordField(event) && ["insertFromPaste", "insertFromDrop", "insertFromYank"].includes(event.inputType)) {
		event.preventDefault();
	}
});
