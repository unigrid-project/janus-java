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
import org.unigrid.janus.model.external.ConfigUrlMockUp;
import net.jqwik.api.Example;
import mockit.Mock;
import mockit.MockUp;
import mockit.Invocation;
import org.unigrid.janus.jqwik.BaseMockedWeldTest;
import org.unigrid.janus.model.cdi.Invoke;
import org.unigrid.janus.model.entity.Feed;
import org.unigrid.janus.model.external.ConfigurationMockUp;

public class UpdateWalletTest extends BaseMockedWeldTest {
	private boolean testUpdateTrue;
	private String result;

	@Inject private UpdateWallet updateWallet;

	@Example
	public boolean shouldReturnTrueOnCheckUpdateBootstrap() {
		String versionSuffix = "_fx";
		String newVersion = Objects.isNull(System.getProperty("release.tag"))
			? "1.0.11" + versionSuffix : System.getProperty("release.tag").substring(1);
		String currentVersion = Objects.isNull(System.getProperty("current.tag"))
			? "1.0.10" + versionSuffix : System.getProperty("current.tag").substring(1);

		new MockUp<UpdateWallet>() {
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

		return updateWallet.checkUpdateBootstrap();
	}

	@Example
	public boolean checkUpdateIsTrue() {
		testUpdateTrue = false;
		new ConfigUrlMockUp();
		new ConfigurationMockUp();

		new MockUp<UpdateWallet>() {
			@Mock
			public void run(Invocation invocation) {
				System.out.println(invocation);
				if (Invoke.invoke("checkUpdate", invocation)) {
					testUpdateTrue = true;
					System.out.println("Testing testing");
				}
			}
		};

		updateWallet.run();
		return testUpdateTrue;
	}
/*
	@Example
	public boolean checkBootstrapUpdateIsTrue() {
		testUpdateTrue = false;
		new ConfigUrlMockUp();
		new MockUp<UpdateWallet>() {
			@Mock
			public void run(Invocation invocation) {
				if (Invoke.invoke("checkUpdateBootstrap", invocation).equals(false)) {
					testUpdateTrue = true;
				}
			}

			@Mock
			public Feed initWebTarget() {
				try {
					File file = new File(UpdateWalletTest.class
						.getResource("external/get_bootstrap_version_from_github.xml")
						.getFile());
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(file);
					doc.getDocumentElement().normalize();
					System.out.println(doc.getDocumentElement().getNodeName());
					Feed feed = new Feed();
					List<Feed.Entry> entrys = new ArrayList<Feed.Entry>();
					NodeList nList = doc.getElementsByTagName("entry");
					entrys.add(new Feed.Entry());
					System.out.println(entrys.size());
					Node node = nList.item(0);
					Element element = (Element) node;
					String id = element.getElementsByTagName("id").item(0).getTextContent();
					System.out.println(id);
					entrys.get(0).setId(id);
					feed.setEntry(entrys);
					return feed;
				} catch (Exception e) {
					System.out.println(e.getMessage());
					return null;
				}
			}

		};
		UpdateWallet updateWallet = new UpdateWallet();
		updateWallet.run();
		return testUpdateTrue;
	}
*/
}
