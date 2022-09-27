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

import jakarta.enterprise.context.ApplicationScoped;
import org.unigrid.janus.model.cdi.Eager;

@Eager
@ApplicationScoped
public class BootstrapModel {

	private static BootstrapModel bootstrapModel = null;
	private String bootstrapVer = "0.0.0";
	private String downloadUrl = "https://github.com/unigrid-project/janus-java/releases/download/";
	private Boolean testing = false;

	public static BootstrapModel getInstance() {
		if (bootstrapModel == null) {
			bootstrapModel = new BootstrapModel();
		}
		return bootstrapModel;
	}

	public String getBootstrapVer() {
		return this.bootstrapVer;
	}

	public void setBootstrapVer(String ver) {
		this.bootstrapVer = ver;
	}

	public String getDownloadUrl() {
		return this.downloadUrl;
	}

	public void setDownloadUrl(String url) {
		this.downloadUrl = url;
	}

	public Boolean getTesting() {
		return this.testing;
	}

	public void setTesting(Boolean test) {
		this.testing = test;
	}
}
