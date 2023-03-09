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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.Getter;

@Data
public class VersionNumber implements Comparable<String> {

	public VersionNumber() {

	}

	public VersionNumber(String version) {
		setVersionNumber(version);
	}

	@Getter
	private String versionNumber;

	private List<Integer> version = new ArrayList<>();

	private List<Integer> patternMatcher(String s) {
		Pattern pattern = Pattern.compile("\\d");
		Matcher matcher = pattern.matcher(s);
		List<Integer> list = new ArrayList<>();
		while (matcher.find()) {
			list.add(Integer.valueOf(matcher.group()));
		}
		return list;
	}

	private void setVersionNumber(String s) {
		version = patternMatcher(s);
	}

	@Override
	public int compareTo(String t) {
		List<Integer> otherVersion = patternMatcher(t);

		if (version.get(0) < otherVersion.get(0)
			|| version.get(1) < otherVersion.get(1)
			|| version.get(2) < otherVersion.get(2)) {
			return 1;
		} else {
			return 0;
		}

	}

}
