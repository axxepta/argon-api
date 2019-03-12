package de.axxepta.configuration;

import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;

public class ArgonServerResourceConfig extends ResourceConfig {

	private static final Logger LOG = Logger.getLogger(ArgonServerResourceConfig.class);

	public ArgonServerResourceConfig(@Context ServiceLocator locator) {
		
		setApplicationName("REST Argon server application");
		
		packages(true, "de.axxepta");
		
		LOG.info(this.getApplicationName());
		
		InitDiscoveryService.initDiscoveryService(locator, getClass().getClassLoader());
		
		InitResourceConfig.initResourceConfig(this);

		InitResourceConfig.initRegisterMeterBinder(this);

		InitResourceConfig.initUtilitiesXML(this);

		register(MultiPartFeature.class);

		register(JacksonJaxbXMLProvider.class);

		InitResourceConfig.initSwaggerProvider(this);

		InitResourceConfig.initEncoding(this);
		
		InitResourceConfig.initLogger();
	}

}
