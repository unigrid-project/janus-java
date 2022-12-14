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

package org.unigrid.janus.model;

import java.util.regex.Pattern;
import net.jqwik.api.Example;
import org.unigrid.janus.jqwik.WeldSetup;

@WeldSetup(Address.class)
public class NodeStatusTest {

	@Example
	public boolean shouldReturnTrueOnFindGridnodeOutput() {
		String str = "ugd_docker_1 88.131.213.107:51576 69ZH36wDddyp5xEdwU4UT16JR66B7gMwAvNnH121ypToeSgEkxG 149448f8c06cda10f1e7a30db5df0911cb7e3e6c1b8e3656c232f3caa3cb7965 0";
		String pattern[] = new String[] { "ugd_docker_[0-9]+", "[0-9]+(\\.[0-9]+){3}:[0-9]+", "[A-Za-z0-9]{51}", "[A-Za-z0-9]{64}", "[0-9]+" };
		String[] split = str.split(" ");

		boolean match = split.length == 5;

		for (int i = 0; i < split.length; i++) {
			match &= Pattern.matches(pattern[i], split[i]);
		}

		return match;
	}
}
