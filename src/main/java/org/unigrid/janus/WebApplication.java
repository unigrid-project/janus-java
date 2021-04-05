package org.unigrid.janus;

/*import cloud.piranha.cdi.weld.WeldInitializer;
import cloud.piranha.faces.myfaces.MyFacesInitializer;
import cloud.piranha.http.api.HttpServer;
import cloud.piranha.http.impl.DefaultHttpServer;
import cloud.piranha.http.webapp.HttpWebApplicationServer;
import cloud.piranha.webapp.impl.DefaultWebApplication;*/

public class WebApplication {
	//private final HttpWebApplicationServer server;
	//private final HttpServer httpServer;

	public WebApplication() {
		/*System.getProperties().put("java.naming.factory.initial",
			"cloud.piranha.naming.impl.DefaultInitialContextFactory");

		server = new HttpWebApplicationServer();
		httpServer = new DefaultHttpServer(8180, server, false);

		final DefaultWebApplication application = new DefaultWebApplication();

		server.addWebApplication(application);
		//application.addResource(new IndexResource("/WEB-INF/classes"));
		application.addInitializer(WeldInitializer.class.getName());
		application.addInitializer(MyFacesInitializer.class.getName());

		server.initialize();
		server.start();
		httpServer.start();*/
	}

	public void stop() {
		//httpServer.stop();
		//server.stop();
	}
}
