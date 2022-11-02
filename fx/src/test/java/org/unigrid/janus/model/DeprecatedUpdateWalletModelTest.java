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

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Objects;
import net.jqwik.api.Example;
import mockit.Mock;
import mockit.MockUp;
import mockit.Invocation;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.jqwik.WeldSetup;
import org.unigrid.janus.model.entity.Feed;

@WeldSetup(DeprecatedUpdateWalletModel.class)
public class DeprecatedUpdateWalletModelTest extends BaseMockedWeldTest {

	@Inject
	private DeprecatedUpdateWalletModel deprecatedUpdateWalletModel;

	@Example
	public boolean shouldReturnTrueOnCheckUpdateBootstrap() {
		String versionSuffix = "_fx";
		String newVersion = Objects.isNull(System.getProperty("release.tag"))
			? "1.0.11" + versionSuffix : System.getProperty("release.tag").substring(1);
		String currentVersion = Objects.isNull(System.getProperty("current.tag"))
			? "1.0.10" + versionSuffix : System.getProperty("current.tag").substring(1);

		new MockUp<DeprecatedUpdateWalletModel>() {
			@Mock
			public Feed initWebTarget(Invocation invocation) {
				String id = "tag:github.com,2008:Repository/354793431/v" + newVersion;
				Feed.Entry entry = new Feed.Entry();
				entry.setId(id);
				Feed feed = new Feed();
				ArrayList<Feed.Entry> list = new ArrayList<Feed.Entry>();
				list.add(entry);
				feed.setEntry(list);

				return feed;
			}
		};

		new MockUp<BootstrapModel>() {
			@Mock
			public String getBootstrapVer() {
				return currentVersion;
			}
		};

		// Should not be the same version after parse
		// checkUpdateBootstrap Should return true
		// Broken code comparing 1.0.1 with 1.0.1, instead of 1.0.11 with 1.0.10
		return !deprecatedUpdateWalletModel.checkUpdateBootstrap();
	}
}
