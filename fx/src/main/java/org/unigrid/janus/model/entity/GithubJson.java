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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class GithubJson {
	@JsonbProperty("tag_name")
	private String tagName;
	private List<Asset> assets;
	
	@Data
	public static class Asset {
		@JsonbProperty("browser_download_url")
		private String browserDownloadUrl;
		@JsonProperty("name")
		private String name;
	}
}
