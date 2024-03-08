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

package org.unigrid.janus.controller;

import java.math.BigDecimal;
import java.math.BigInteger;

/* 1 atom = 1,000,000 uATOM
 	atom  = "atom"  // 1 (base denom unit)
	matom = "matom" // 10^-3 (milli)
	uatom = "uatom" // 10^-6 (micro)
	natom = "natom" // 10^-9 (nano)
 https://blog.cosmos.network/phase-ii-initiated-cosmos-atom-transfers-enabled-by-governance-831a7e555ab6 */

public class ATOMUnitUtil {

    public static BigDecimal microAtomToAtom(String uatomString) {
        BigDecimal uatom = new BigDecimal(uatomString);
        return uatom.movePointLeft(6).stripTrailingZeros();
    }

    public static BigDecimal microAtomToAtom(BigInteger uatomBigInteger) {
        BigDecimal uatom = new BigDecimal(uatomBigInteger);
        return uatom.movePointLeft(6).stripTrailingZeros();
    }

    public static BigDecimal atomToMicroAtom(String atomVal) {
        BigDecimal atom = new BigDecimal(atomVal);
        return atomToMicroAtom(atom);
    }

    public static BigDecimal atomToMicroAtom(BigDecimal atom) {
        return atom.movePointRight(6).stripTrailingZeros();
    }

    public static BigInteger atomToMicroAtomBigInteger(BigDecimal atom) {
        BigDecimal bigDecimal = atom.movePointRight(6);
        if (getNumberOfDecimalPlaces(bigDecimal) != 0) {
            throw new RuntimeException("atom to uAtom: " + bigDecimal);
        }
        return bigDecimal.toBigInteger();
    }

    public static int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
        String string = bigDecimal.stripTrailingZeros().toPlainString();
        int index = string.indexOf(".");
        return index < 0 ? 0 : string.length() - index - 1;
    }
}
