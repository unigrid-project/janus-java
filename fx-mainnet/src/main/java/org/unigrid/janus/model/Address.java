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

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
	private String address;
	private Amount amount;

	@JsonbTypeDeserializer(AmountDeserializer.class)
	public static class Amount extends BigDecimal {
		private static final int DECIMALS = 8;

		public Amount(BigDecimal amount) {
			super(amount.toString());
		}

		public Amount(String amount) {
			super(amount);
		}

		@Override
		public String toString() {
			return this.setScale(DECIMALS, RoundingMode.UP).toPlainString();
		}
	}
}
