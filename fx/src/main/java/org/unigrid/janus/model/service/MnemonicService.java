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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.input.Clipboard;

public class MnemonicService {

	private final List<String> mnemonicWordList = new ArrayList<>();

	public List<String> getMnemonicWordList() {
		return mnemonicWordList;
	}

	public void setMnemonicFromClipboard() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasString()) {
			String mnemonic = clipboard.getString();
			String[] words = mnemonic.split("\\s+"); // Split by whitespace
			mnemonicWordList.clear();
			mnemonicWordList.addAll(Arrays.asList(words));
		}
	}

	// Other methods related to mnemonics...
}
