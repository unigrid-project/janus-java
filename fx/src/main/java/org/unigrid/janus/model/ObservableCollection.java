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
import java.util.Collection;
import java.util.List;
import org.unigrid.janus.model.function.VoidFunction;

public class ObservableCollection {
	final List<Collection> collections = new ArrayList<>();
	int cachedHashCode = 0;

	public ObservableCollection(Collection<? extends ObservableCollectionMember>... collections) {
		for (Collection c : collections) {
			this.collections.add(c);
		}
	}

	public void onChange(VoidFunction onChangeFunction, VoidFunction onUnchangedFunction) {
		final ArrayList<ObservableCollectionMember> collectionMembers = new ArrayList<>();

		for (Collection c : collections) {
			collectionMembers.addAll(c);
		}

		final int hashCode = collectionMembers.stream().map(c -> c.observableHashCode()).reduce(0, Integer::sum);
		System.out.println("hashcode: " + hashCode);
		System.out.println("cachedHashCode: " + cachedHashCode);

		if (hashCode != cachedHashCode) {
			cachedHashCode = hashCode;
			onChangeFunction.apply();
		} else {
			onUnchangedFunction.apply();
		}
	}
}
