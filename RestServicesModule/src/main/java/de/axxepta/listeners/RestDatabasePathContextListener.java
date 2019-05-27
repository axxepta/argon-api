package de.axxepta.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.apache.log4j.Logger;

public class RestDatabasePathContextListener implements ServletContextListener {

	private static final Logger LOG = Logger.getLogger(RestDatabasePathContextListener.class);

	private static String restPath;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletRegistration servletRegistration = event.getServletContext().getServletRegistration("REST");
		if (servletRegistration != null) {
			String restBaseXURLParam = (String) servletRegistration.getMappings().iterator().next();
			restPath = restBaseXURLParam.substring(1).substring(0, restBaseXURLParam.substring(1).lastIndexOf("/"));
			LOG.info("Rest path for BaseX " + restPath);
		} else {
			LOG.error("REST is not found in servlet mapping");
		}
	}

	public static String getRestPath() {
		return restPath;
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {

	}

}
