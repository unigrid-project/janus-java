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

package org.unigrid.janus.view.backing;

import java.util.ArrayList;
import java.util.Comparator;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn.SortType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.unigrid.janus.model.Address;

@Data
@EqualsAndHashCode(callSuper = false)
public class AddressList extends SimpleListProperty<Address> {
	private final ObservableList<Address> source;
	private boolean hideEmpty;
	private SortType sortType = SortType.DESCENDING;

	public AddressList() {
		super();
		source = FXCollections.observableArrayList(new ArrayList<Address>());

		set(source.sorted((l, r) -> {
			Comparator<Address> comparator = Comparator.comparing(Address::getAmount);

			if (sortType == SortType.ASCENDING) {
				comparator = comparator.reversed();
			}

			return comparator.compare(l, r);
		}).filtered(t -> !(hideEmpty && t.getAmount().doubleValue() == 0)));
	}

	public ObservableList<Address> getSource() {
		return source;
	}
}
