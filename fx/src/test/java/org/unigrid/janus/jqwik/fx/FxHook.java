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

package org.unigrid.janus.jqwik.fx;

import jakarta.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import net.jqwik.api.lifecycle.AroundContainerHook;
import net.jqwik.api.lifecycle.AroundPropertyHook;
import net.jqwik.api.lifecycle.PropertyExecutionResult;
import net.jqwik.api.lifecycle.PropertyExecutor;
import net.jqwik.api.lifecycle.PropertyLifecycleContext;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.testfx.api.FxToolkit;
import org.unigrid.janus.model.cdi.CDIUtil;

public class FxHook implements AroundContainerHook, AroundPropertyHook {
	private static final int HIGH_PRIORITY = 1024;

	@RequiredArgsConstructor
	private class ApplicationAdapter extends Application {
		private final Object instance;

		private void forEachMethodWithAnnotation(Class type, Consumer<Method> consumer) {
			for (Method m : instance.getClass().getDeclaredMethods()) {
				if (Objects.nonNull(m.getAnnotation(type))) {
					consumer.accept(m);
				}
			}
		}

		@Override
		public void start(Stage stage) throws Exception {
			forEachMethodWithAnnotation(FxStart.class, m -> {
				try {
					if (m.getParameterCount() == 0) {
						m.invoke(instance);
					} else if (m.getParameterCount() == 1) {
						m.invoke(instance, stage);
					} else {
						throw new IllegalStateException("Invalid start method prototype.");
					}
				} catch (IllegalAccessException | InvocationTargetException ex) {
					System.err.print(ex);
					ex.printStackTrace();
				}
			});
		}

		@Override
		public void stop() throws Exception {
			forEachMethodWithAnnotation(FxStop.class, m -> {
				try {
					m.invoke(instance);
				} catch (IllegalAccessException | InvocationTargetException ex) {
					System.err.print(ex);
					ex.printStackTrace();
				}
			});
		}
	}

	private Application setupFx(Object instance, FxResource resource) {
		try {
			FxToolkit.registerPrimaryStage();

			Platform.runLater(() -> {
				try {
					FXMLLoader loader = new FXMLLoader();
					loader.setClassLoader(resource.clazz().getClassLoader());
					loader.setLocation(resource.clazz().getResource(resource.name()));

					loader.setControllerFactory(c -> {
						final Object o = CDI.current().select(c).get();

						if (o instanceof TargetInstanceProxy) {
							return CDIUtil.unproxy(o);
						}

						return o;
					});

					FxToolkit.toolkitContext().setRegisteredStage(loader.load());
					FxToolkit.showStage();
				} catch (IOException | TimeoutException ex) {
					ex.printStackTrace();
				}
			});

			return FxToolkit.setupApplication(() -> new ApplicationAdapter(instance));

		} catch (TimeoutException ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}

		throw new IllegalStateException("Unable to set up FX application.");
	}

	private void shutdownFx(Application application) {
		try {
			FxToolkit.cleanupStages();
			FxToolkit.cleanupApplication(application);
		} catch (TimeoutException ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
	}

	private FxResource findResource(PropertyLifecycleContext context) {
		final AtomicReference<FxResource> reference = new AtomicReference<>();

		context.optionalContainerClass().ifPresent(clazz -> {
			reference.set(clazz.getAnnotation(FxResource.class));

			if (Objects.isNull(reference.get())) {
				throw new IllegalStateException("FX tests require an FxResource annotation.");
			}
		});

		return reference.get();
	}

	@Override
	public PropertyExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) {
		final FxResource resource = findResource(context);
		Application application = setupFx(context.testInstance(), resource);

		final PropertyExecutionResult result = property.execute();
		shutdownFx(application);
		return result;
	}

	@Override
	public int proximity() {
		return HIGH_PRIORITY;
	}
}
