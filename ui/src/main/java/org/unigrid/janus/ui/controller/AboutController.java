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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.unigrid.janus.core.Release;
import org.unigrid.janus.ui.view.AboutView;
import org.unigrid.janus.web.action.Action;

@ApplicationScoped
public class AboutController {
	private final Release release;

	@Inject
	public AboutController(final Release release) {
		this.release = release;
	}

	@Action("about")
	public AboutView onClickAbout() {
		return new AboutView(release.version());
	}
}
