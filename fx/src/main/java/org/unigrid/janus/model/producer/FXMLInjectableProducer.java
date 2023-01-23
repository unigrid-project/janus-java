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

package org.unigrid.janus.model.producer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.beans.Introspector;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.unigrid.janus.model.cdi.CDIUtil;
import org.unigrid.janus.controller.Showable;
import org.unigrid.janus.model.FXMLInjectable;
import org.unigrid.janus.model.FXMLInjectableImpl;
import org.unigrid.janus.model.FXMLName;
import org.unigrid.janus.view.FxUtils;

@Dependent
public class FXMLInjectableProducer {
	private static final String FXML_SUFFIX = ".fxml";

	@Produces
	public <T, I> FXMLInjectable<T> produce(final InjectionPoint point) {
		final Class<?> clazz = point.getMember().getDeclaringClass();
		final FXMLLoader loader = new FXMLLoader();

		loader.setControllerFactory(c -> {
			final Object o = CDI.current().select(c).get();

			if (o instanceof TargetInstanceProxy) {
				return CDIUtil.unproxy(o);
			}

			return o;
		});

		if (point.getAnnotated().isAnnotationPresent(FXMLName.class)) {
			final String name = point.getAnnotated().getAnnotation(FXMLName.class).value();
			loader.setClassLoader(FxUtils.class.getClassLoader());
			loader.setLocation(FxUtils.class.getResource(name.concat(FXML_SUFFIX)));
		} else {
			final String name = Introspector.decapitalize(clazz.getSimpleName());
			loader.setClassLoader(clazz.getClassLoader());
			loader.setLocation(clazz.getResource(name.concat(FXML_SUFFIX)));
		}

		try {
			final I injectable = loader.load();

			if (loader.getController() instanceof Showable && injectable instanceof Stage stage) {
				final Showable showable = loader.getController();

				stage.setOnShown(e -> {
					showable.onShow(stage);
				});

				stage.setOnHidden(e -> {
					showable.onHide(stage);
				});
			}

			return (FXMLInjectable<T>) FXMLInjectableImpl.of(injectable);

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getCause());
			throw new IllegalStateException("Unable to load FXML file. " + e.getMessage());
		}
	}
}
