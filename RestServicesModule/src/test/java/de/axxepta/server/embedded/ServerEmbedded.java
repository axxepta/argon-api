package de.axxepta.server.embedded;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import de.axxepta.configuration.ArgonServerResourceConfig;
import de.axxepta.configuration.InitResourceConfig;
import de.axxepta.server.embedded.interfaces.IServerEmbedded;


public class ServerEmbedded implements IServerEmbedded{

	private int port;

	public Server jettyServerEmbedded;
	private String path;
	private Map<String, String> initParametersMap;
	
	public ServerEmbedded(int port, String path, Map<String, String> initParametersMap) {
		this.port = port;
		this.path = path;
		this.initParametersMap = initParametersMap;
	}
	
	public void startServer() throws Exception {
		/*
		ResourceConfig config = new ResourceConfig();
		InitResourceConfig.initRegisterMeterBinder(config);
		
		ServletContainer servletContainer = new ServletContainer(config);
		*/
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/" + path);

		jettyServerEmbedded = new Server(port);
		
		Configuration.ClassList classlist = Configuration.ClassList
		        .setServerDefault(jettyServerEmbedded);
		 
		classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
		        "org.eclipse.jetty.plus.webapp.EnvConfiguration",
		        "org.eclipse.jetty.plus.webapp.PlusConfiguration");
		// Enable annotation/bytecode scanning and ServletContainerInitializer usages
		classlist.addBefore(
		        "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
		        "org.eclipse.jetty.annotations.AnnotationConfiguration");
		jettyServerEmbedded.setHandler(context);

		ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
		
		//context.addServlet(jerseyServletApp, "/*");
		
		jerseyServlet.setInitOrder(0);
		jerseyServlet.setDisplayName("Test service");
		
		 if(initParametersMap != null) {
			 for(Entry<String, String> initParam : initParametersMap.entrySet()) {
				 jerseyServlet.setInitParameter(initParam.getKey(), initParam.getValue());
			 }
		 }
		
		jettyServerEmbedded.start();
		jettyServerEmbedded.join();
	}

	public void stopServer() throws Exception {
		System.out.println("Server URI was " + jettyServerEmbedded.getURI());
		
		if (jettyServerEmbedded != null && jettyServerEmbedded.isStarted()) {	
				jettyServerEmbedded.stop();			
		}
	}
}
