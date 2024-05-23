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

package org.unigrid.janus.model.external;

import java.util.ArrayList;
import java.util.List;
import mockit.MockUp;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.unigrid.janus.model.entity.Feed;

public class ResponseMockUp extends MockUp<OutboundJaxrsResponse> {

	protected <T> T readEntities(Class<T> clazz) {
		switch (clazz.getSimpleName()) {
			case "Feed" -> {
				return (T) feed((Class<List>) clazz);
			}
			case "String" -> {
				return (T) "2088092";
			}
			default -> {
				return null;
			}
		}
	}

	public Feed feed(Class<List> clazz) {
		final Feed result = new Feed();
		List<Feed.Entry> list = new ArrayList<Feed.Entry>();
		Feed.Entry entry = new Feed.Entry();
		entry.setId("tag:github.com,2008:Repository/354793431/v1.0.7");
		list.add(entry);
		result.setEntry(list);
		return result;
	}
}
