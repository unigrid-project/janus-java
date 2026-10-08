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

/* A failed action leaves the page as it was, which makes a click that failed look like one that was ignored.
   The reason is written to the log, so the person is only told that there is one. */
document.addEventListener("htmx:responseError", () => {
	const host = document.querySelector("main > .card") ?? document.querySelector("main");
	const notice = host.querySelector("[data-failure]") ?? host.appendChild(document.createElement("p"));

	notice.className = "step__error";
	notice.dataset.failure = "";
	notice.setAttribute("role", "alert");
	notice.textContent = "That did not work. The reason is in the log in the logs folder inside .janus in your home folder.";
});
