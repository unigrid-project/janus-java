/*
	The Janus Wallet
	Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.unigrid.janus.model.AddressCosmos;
import org.unigrid.janus.model.DataDirectory;

@ApplicationScoped
public class AddressCosmosService {
	@Inject private DebugService debug;

	public void saveAddress(AddressCosmos newAddress) throws IOException {
		saveAddressToJSON(newAddress);
	}

	private void saveAddressToJSON(AddressCosmos newAddress) throws IOException {
		// Ensure directory exists
		DataDirectory.ensureDirectoryExists(DataDirectory.KEYRING_DIRECTORY);

		File file = DataDirectory.getCosmosAddresses();
		ObjectMapper objectMapper = new ObjectMapper();
		List<AddressCosmos> addresses;

		// Check if file exists and read existing data
		if (file.exists() && file.length() != 0) {
			addresses = objectMapper.readValue(file, new TypeReference<List<AddressCosmos>>() {
			});
		} else {
			addresses = new ArrayList<>();
		}

		// Add the new address to the list
		addresses.add(newAddress);
		if (debug != null) {
			debug.print("newAddress: " + newAddress, AddressCosmosService.class.getSimpleName());
		} else {
			System.out.println("newAddress: " + newAddress);
		}
		// Write the updated list back to the JSON file
		objectMapper.writeValue(file, addresses);
	}
}
