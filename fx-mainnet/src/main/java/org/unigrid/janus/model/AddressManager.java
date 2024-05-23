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

package org.unigrid.janus.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AddressManager {

	private static final String FILE_PATH = "path_to_your_json_file.json";

	public static void main(String[] args) {
		String mnemonic = "your_mnemonic_here";
		addNewAddress(mnemonic);
	}

	public static JSONArray loadExistingAddresses() throws Exception {
		String content = new String(Files.readAllBytes(Paths.get(FILE_PATH)));
		return new JSONArray(content);
	}

	public static JSONObject generateNewAddress(String mnemonic, String derivationPath) {
		// Your logic to generate a new address using the mnemonic and derivation path
		// For demonstration purposes, I'm returning dummy data
		JSONObject newAddress = new JSONObject();
		newAddress.put("address", "new_address_here");
		newAddress.put("publicKey", "new_public_key_here");
		newAddress.put("derivationPath", derivationPath);
		newAddress.put("label", "Optional_label_or_description");
		return newAddress;
	}

	public static void addNewAddress(String mnemonic) {
		try {
			JSONArray addresses = loadExistingAddresses();

			// Determine the next derivation path based on the number of existing addresses
			String nextDerivationPath = "m/44'/0'/0'/" + addresses.length();

			JSONObject newAddress = generateNewAddress(mnemonic, nextDerivationPath);

			// Check if the address already exists
			boolean exists = false;
			for (int i = 0; i < addresses.length(); i++) {
				if (addresses.getJSONObject(i).getString("address")
					.equals(newAddress.getString("address"))) {
					exists = true;
					break;
				}
			}

			if (!exists) {
				addresses.put(newAddress);
				// Write updated addresses back to the JSON file
				Files.write(Paths.get(FILE_PATH), addresses.toString(4).getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
