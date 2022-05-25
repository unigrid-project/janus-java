/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

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

import org.apache.commons.configuration2.SystemConfiguration;

public class Preferences {
	private static final String ROOT_NODE = "janus";
	public static final SystemConfiguration PROPS = new SystemConfiguration();

	public static <T> void changePropertyDefault(Class<T> type, String key, T defaultValue) {
		PROPS.setProperty(key, PROPS.get(type, key, defaultValue));
	}

	public static java.util.prefs.Preferences get() {
		return java.util.prefs.Preferences.userRoot().node(ROOT_NODE);
		}
}
