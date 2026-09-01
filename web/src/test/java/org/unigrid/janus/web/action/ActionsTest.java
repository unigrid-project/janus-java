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

package org.unigrid.janus.web.action;

import net.jqwik.api.Example;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionsTest {
	public static class Padlock {
		@Action("lock")
		public void onClickLock() {
		}
	}

	public static class Bolt {
		@Action("lock")
		public void onClickBolt() {
		}
	}

	public static class Wallet {
		@Action("lock")
		public void onClickLock() {
		}

		@Action("unlock")
		public void onClickUnlock() {
		}
	}

	@Example
	public void shouldRefuseTwoMethodsClaimingTheSameAction() {
		final IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> Actions.of(new Padlock(), new Bolt())
		);

		assertTrue(thrown.getMessage().contains("lock"), thrown.getMessage());
	}

	@Example
	public void shouldBindEachActionOnce() {
		final Actions actions = Actions.of(new Wallet());

		assertTrue(actions.knows("lock"));
		assertTrue(actions.knows("unlock"));
	}
}
