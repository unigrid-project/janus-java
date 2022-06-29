/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

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
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.beans.Introspector;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.unigrid.janus.controller.view.SplashScreenController;

@Dependent
public class StageProducer {
	private static final String FXML_SUFFIX = ".fxml";

	@Produces
	public Stage produce(final InjectionPoint point) {
		final Class<?> clazz = point.getMember().getDeclaringClass();
		FXMLLoader loader = new FXMLLoader();
		System.out.println("bajs producer");
		
		loader.setControllerFactory(controller -> {
			final BeanManager manager = CDI.current().getBeanManager();
			final Bean<?> bean = manager.getBeans(controller).iterator().next();
			System.out.println("Bean context " + manager.getContext(bean.getScope()).get(bean));
			return manager.getContext(bean.getScope()).get(bean);
			
			/*try {
				return controller.getDeclaredConstructor().newInstance();
				
			}
			catch(Exception e) {
				return null;
			}*/
		});

		final String name = Introspector.decapitalize(clazz.getSimpleName());
		loader.setClassLoader(getClass().getClassLoader());
		System.out.println(loader.getClassLoader());
		System.out.println(name.concat(FXML_SUFFIX));
		loader.setLocation(clazz.getResource(name.concat(FXML_SUFFIX)));
		System.out.println("SHITTTTTTTTTTTTTTTT " + point.getType());
		try{
			return loader.load();

		}
		catch(IOException e) {
			//TODO throw illegal state
			System.out.println(e.getMessage());
			System.out.println(e.getCause());
		}
		return null;
	}
}
