/*
    The Janus Wallet
    Copyright © 2021-2022 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the The Unigrid Foundation and
    the Free Software Foundation, version 3 of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License and the addendum for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/hedgehog>.
 */

package org.unigrid.janus.model.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import java.util.ArrayList;
import java.util.List;

// TODO: EagerExtension unused, add it
public class EagerExtension implements Extension {
	private final List<Bean<?>> eagerBeansList = new ArrayList<>();

	public <T> void collect(@Observes ProcessBean<T> event) {
		if (event.getAnnotated().isAnnotationPresent(Eager.class)
			&& event.getAnnotated().isAnnotationPresent(ApplicationScoped.class)) {

			eagerBeansList.add(event.getBean());
		}
	}

	public void load(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		eagerBeansList.forEach((bean) -> {

			beanManager.getReference(bean, bean.getBeanClass(),
				beanManager.createCreationalContext(bean)
			).toString();
		});
	}
}
