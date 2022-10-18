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

package org.unigrid.janus.jqwik.fx;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.jqwik.api.lifecycle.AddLifecycleHook;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.PropagationMode;
import org.apache.commons.lang3.RandomStringUtils;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.model.service.RPCService;

@AddLifecycleHook(value = FxHook.class, propagateTo = PropagationMode.ALL_DESCENDANTS)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseFxTest extends BaseMockedWeldTest {
	@BeforeContainer
	public static void beforeContainer() {
		System.setProperty(RPCService.PROPERTY_USERNAME_KEY, RandomStringUtils.randomAlphabetic(20));
		System.setProperty(RPCService.PROPERTY_PASSWORD_KEY, RandomStringUtils.randomAlphabetic(20));
	}
}
