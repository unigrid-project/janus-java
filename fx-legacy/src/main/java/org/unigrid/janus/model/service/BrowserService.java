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

package org.unigrid.janus.model.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.application.HostServices;
import lombok.RequiredArgsConstructor;
import org.unigrid.janus.model.cdi.Eager;

@Eager
@ApplicationScoped
@RequiredArgsConstructor
public class BrowserService {
	private static final String BASE_URL_TEMPLATE = "https://explorer.unigrid.org/%s/%s";
	private static final String ADDRESS_PART = "address";
	private static final String TX_PART = "tx";

	@Inject private DebugService debug;
	@Inject private HostServices hostServices;

	public void navigateAddress(String address) {
		navigate(String.format(BASE_URL_TEMPLATE, ADDRESS_PART, address));
	}

	public void navigateTransaction(String tx) {
		navigate(String.format(BASE_URL_TEMPLATE, TX_PART, tx));
	}

	public void navigate(String url) {
		try {
			hostServices.showDocument(url);
		} catch (NullPointerException e) {
			debug.print(e.getMessage(), BrowserService.class.getSimpleName());
		}
	}
}
