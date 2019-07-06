package de.axxepta.configuration;

import javax.ws.rs.core.Context;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgonServerResourceConfig extends ResourceConfig {

	private static final Logger LOG = LoggerFactory.getLogger(ArgonServerResourceConfig.class);
	
	public ArgonServerResourceConfig(@Context ServiceLocator locator) {
		
		setApplicationName("REST Argon server application");
		
		packages(true, "de.axxepta");
		
		LOG.info(this.getApplicationName());
		
		InitDiscoveryService.initDiscoveryService(locator, Thread.currentThread().getContextClassLoader());
		
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
