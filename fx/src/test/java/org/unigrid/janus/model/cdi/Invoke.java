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

package org.unigrid.janus.model.cdi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import mockit.Invocation;

public class Invoke {

	public static <T, R> R invoke(String name, Invocation invocation, Object... args) {
		try {
			final List<Class> paramterTypes = new ArrayList<>();
			for (Object arg : args) {
				paramterTypes.add(arg.getClass());
			}
			final T invocationInstance = invocation.getInvokedInstance();
			final Method method = invocation.getInvokedInstance().getClass()
				.getDeclaredMethod(name, paramterTypes.toArray(new Class[0]));
			method.setAccessible(true);
			return (R) method.invoke(invocationInstance, args);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
}
