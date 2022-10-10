/*
	The Janus Wallet
	Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

	This program is free software: you can redistribute it and/or modify it under the terms of the
	addended GNU Affero General Public License as published by the Free Software Foundation, version 3
	of the License (see COPYING and COPYING.addendum).

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
	even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
	GNU Affero General Public License for more details.

	You should have received an addended copy of the GNU Affero General Public License with this program.
	If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.jqwik;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.CDIProvider;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.weld.environment.se.WeldContainer;

public class NamedCDIProvider implements CDIProvider {
	public static final AtomicReference<String> NAME_REFERENCE = new AtomicReference<>();

	@Override
	public CDI<Object> getCDI() {
		if (Objects.isNull(NAME_REFERENCE.get())) {
			throw new IllegalStateException("No namespace set for requested CDI instance");
		}

		return WeldContainer.instance(NAME_REFERENCE.get());
	}

	@Override
	public int getPriority() {
		return DEFAULT_CDI_PROVIDER_PRIORITY + 10; /* Bump ourselves up so we get precedence. */
	}
}
