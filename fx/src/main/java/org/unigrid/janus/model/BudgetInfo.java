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

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class BudgetInfo {
	@JsonbProperty("Name")
	private String name;
	@JsonbProperty("URL")
	private String url;
	@JsonbProperty("Hash")
	private String hash;
	//private String feeHash;
	//private int blockStart;
	//private int blockEnd;
	//private int totalPaymentCount;
	//private int remainingPaymentCount;
	//private String paymentAddress;
	//private double ratio;
	@JsonbProperty("Yeas")
	private int yeas;
	@JsonbProperty("Nays")
	private int nays;
	//private int abstains;
	//private int totalPayment;
	//private int monthlyPayment;
	//private boolean isEstablished;
	//private boolean isValid;
	//private String isValidReason;
	//private boolean fValid;
}
