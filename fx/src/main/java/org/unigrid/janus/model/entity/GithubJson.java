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

package org.unigrid.janus.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Data
//@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name="feed")
public class GithubJson {
 
	@XmlElement(name = "id")
	private String tag;

	@XmlElementWrapper
	@XmlElement(name = "entry")
	List<Entry> entries;

	public static class Entry {
		@Getter @Setter
		//@XmlElement(name="id")
		private String id;
	}
}
