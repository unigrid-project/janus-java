package org.unigrid.bootstrap;

import java.lang.reflect.Method;
import javafx.application.Application;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class HeadlessApplication {

	public Application assignMonoclePlatform() throws Exception {
		//assignHeadlessPlatform();

		Class<?> platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
		System.out.println("platformFactoryClass: " + platformFactoryClass);
		Object platformFactoryImpl;
		try {
			Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory");
		} catch (ClassNotFoundException e) {
			System.out.println("com.sun.glass.ui.monocle.MonoclePlatformFactory not in classpath");
			e.printStackTrace();
		}
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

	private void assignHeadlessPlatform() throws Exception {
		Class<?> nativePlatformFactoryClass = Class.forName("com.sun.glass.ui.monocle.NativePlatformFactory");
		try {
			Constructor<?> nativePlatformCtor = Class.forName(
				"com.sun.glass.ui.monocle.HeadlessPlatform").getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		} catch (ClassNotFoundException exception) {
			// Before Java 8u40 HeadlessPlatform was located inside of a "headless" package.
			Constructor<?> nativePlatformCtor = Class.forName(
				"com.sun.glass.ui.monocle.headless.HeadlessPlatform").getDeclaredConstructor();
			nativePlatformCtor.setAccessible(true);
			assignPrivateStaticField(nativePlatformFactoryClass, "platform", nativePlatformCtor.newInstance());
		}
	}

	private void assignPrivateStaticField(Class<?> clazz, String name, Object value) throws Exception {
		Field field = clazz.getDeclaredField(name);
		field.setAccessible(true);
		field.set(clazz, value);
		field.setAccessible(false);
	}

}
