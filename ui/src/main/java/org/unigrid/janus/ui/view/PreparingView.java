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

package org.unigrid.janus.ui.view;

import java.util.List;
import java.util.Locale;
import org.unigrid.janus.web.action.View;

/** How far the wallet is from being shown, and what went wrong when it cannot be. */
public record PreparingView(List<Step> steps, String failure, boolean chooseAnother) implements View {
	/** One thing to get done, with how far it has got when it is a download whose progress is known. */
	public record Step(String label, State state, Integer percent) {
		public Step(final String label, final State state) {
			this(label, state, null);
		}

		public boolean measured() {
			return state == State.ACTIVE && percent != null;
		}

		public enum State {
			DONE,
			ACTIVE,
			PENDING;

			public String css() {
				return name().toLowerCase(Locale.ROOT);
			}
		}
	}

	public boolean failed() {
		return failure != null;
	}

	@Override
	public String template() {
		return "fragments/preparing :: preparing";
	}
}
