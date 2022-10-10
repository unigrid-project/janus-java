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

package org.unigrid.janus.model.external;

import jakarta.json.bind.JsonbBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.function.Supplier;
import org.unigrid.janus.model.rpc.entity.BaseResult;

public class JaxrsResponseHandler {

	public static <T extends BaseResult, R> T handle(Class<T> clazz, Type resultClazz, Supplier supplier) {
		try {
			final String jsonFile = (String) supplier.get();
			final T result = clazz.getDeclaredConstructor().newInstance();
			result.setResult(JsonbBuilder.create().fromJson(JaxrsResponseHandler.class
				.getResourceAsStream(jsonFile), resultClazz));

			return result;
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException
			| NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to parse response.");
		}
	}
}
