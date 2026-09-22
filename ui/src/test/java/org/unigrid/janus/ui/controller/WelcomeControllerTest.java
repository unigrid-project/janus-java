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

package org.unigrid.janus.ui.controller;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import net.jqwik.api.Example;
import org.unigrid.janus.ui.view.WelcomeView;
import org.unigrid.janus.web.action.ActionExtension;
import org.unigrid.janus.web.action.Actions;
import org.unigrid.janus.web.action.Form;
import org.unigrid.janus.web.action.View;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WelcomeControllerTest {
	@Example
	public void shouldLeadBackToTheWelcomeStep() {
		assertEquals("fragments/welcome :: welcome", new WelcomeController().onClickWelcome().template());
	}

	@Example
	public void shouldBeFoundAndWiredByTheContainer() {
		try (SeContainer container = SeContainerInitializer.newInstance().initialize()) {
			final Actions actions = Actions.discovered(
				container.getBeanManager().getExtension(ActionExtension.class)
			);
			final View view = actions.invoke("welcome", Form.parse("")).orElseThrow();

			assertEquals(new WelcomeView(), view);
		}
	}
}
