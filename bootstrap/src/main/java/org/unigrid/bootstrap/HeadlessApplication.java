package org.unigrid.bootstrap;

import java.lang.reflect.Method;
import javafx.application.Application;

public class HeadlessApplication {

	public Application assignMonoclePlatform() throws Exception {
		Class<?> platformFactoryClass = Class.forName("com.sun.glass.ui.PlatformFactory");
		Object platformFactoryImpl = Class.forName("com.sun.glass.ui.monocle.MonoclePlatformFactory")
			.getDeclaredConstructor().newInstance();
		Method m = platformFactoryClass.getMethod("createApplication");
		return (Application) m.invoke(platformFactoryImpl);
		//assignPrivateStaticField(platformFactoryClass, "instance", platformFactoryImpl);
	}

}
