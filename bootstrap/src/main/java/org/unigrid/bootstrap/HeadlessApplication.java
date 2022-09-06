package org.unigrid.bootstrap;

import java.lang.reflect.Method;
import javafx.application.Application;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class HeadlessApplication {

	private static final String PLATFORM_FACTORY
		= "com.sun.glass.ui.PlatformFactory";
	private static final String MONOCLE_PLATFORM_FACTORY
		= "com.sun.glass.ui.monocle.MonoclePlatformFactory";

	private static final String NATIVE_PLATFORM_FACTORY
		= "com.sun.glass.ui.monocle.NativePlatformFactory";
	private static final String HEADLESS_NATIVE_PLATFORM
		= "com.sun.glass.ui.monocle.headless.HeadlessPlatform";
	private static final String HEADLESS_U40_NATIVE_PLATFORM
		= "com.sun.glass.ui.monocle.HeadlessPlatform";

	public void launch(Class<? extends Application> appClass, String... appArgs) {
		initMonocleHeadless();
		Application.launch(appClass, appArgs);
	}

	private void initMonocleHeadless() {
		//if (Boolean.getBoolean("testfx.headless")) {

		try {
			assignMonoclePlatform();
			assignHeadlessPlatform();
		} catch (ClassNotFoundException exception) {
			throw new IllegalStateException("monocle headless platform not found - did you forget to add "
				+ "a dependency on monocle (https://github.com/TestFX/Monocle)?", exception);
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		}
		//}
	}
/*
	private void assignMonoclePlatform() throws Exception {
		Class<?> platformFactoryClass = Class.forName(PLATFORM_FACTORY);
		Object platformFactoryImpl = Class.forName(MONOCLE_PLATFORM_FACTORY)
			.getDeclaredConstructor().newInstance();
		assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
	}

	private void assignHeadlessPlatform() throws Exception {
		Class<?> nativePlatformFactoryClass = Class.forName(NATIVE_PLATFORM_FACTORY);
		try {
			Constructor<?> nativePlatformCtor = Class.forName(
				HEADLESS_U40_NATIVE_PLATFORM).getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		} catch (ClassNotFoundException exception) {
			// Before Java 8u40 HeadlessPlatform was located inside of a "headless" package.
			Constructor<?> nativePlatformCtor = Class.forName(
				HEADLESS_NATIVE_PLATFORM).getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		}
	}*/

	public Application assignMonoclePlatform() throws Exception {
		//assignHeadlessPlatform();

		Class<?> platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
		System.out.println("platformFactoryClass: " + platformFactoryClass);
		Object platformFactoryImpl;
		Object clz;

		try {
			platformFactoryImpl = Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory")
				.getDeclaredConstructor().newInstance();
			assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
			System.out.println("platformFactoryImpl: " + platformFactoryImpl);
			Method m = platformFactoryClass.getMethod("createApplication");
			return (Application) m.invoke(platformFactoryImpl);
		} catch (Exception e) {
			System.out.println("error: " + e.getMessage());
			return null;
		}
	}

	public void assignMonoclePlatformTestFX() throws Exception {
		Class<?> platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
		System.out.println("platformFactoryClass: " + platformFactoryClass.getSimpleName());
		Object platformFactoryImpl = Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory")
			.getDeclaredConstructor().newInstance();
		assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
	}

	public void assignHeadlessPlatform()
		throws Exception {
		Class<?> nativePlatformFactoryClass = Class.forName(NATIVE_PLATFORM_FACTORY);
		try {
			Object nativePlatformImpl = Class.forName(HEADLESS_U40_NATIVE_PLATFORM)
				.getDeclaredConstructor().newInstance();
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformImpl);
		} catch (ClassNotFoundException exception) {
			Object nativePlatformImpl = Class.forName(HEADLESS_NATIVE_PLATFORM)
				.getDeclaredConstructor().newInstance();
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformImpl);
		}
	}

	private void assignPrivateStaticField(Class<?> clazz, String name, Object value) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		field.set(clazz, value);
		field.setAccessible(false);
	}

}
