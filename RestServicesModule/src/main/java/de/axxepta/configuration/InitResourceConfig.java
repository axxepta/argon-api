package de.axxepta.configuration;

import java.io.File;
import java.util.Locale;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;

import de.axxepta.bind.MeterConfigBinder;
import de.axxepta.properties.ResourceBundleReader;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

public class InitResourceConfig {

	private static final Logger LOG = LoggerFactory.getLogger(InitResourceConfig.class);

	public static void initResourceConfig(ResourceConfig config) {
		final String fileName = "ArgonServerConfig";
		final File fileConfig = new File(fileName);
		final Locale locale = new Locale("en");

		ResourceBundleReader bundleReader = new ResourceBundleReader(fileConfig, locale);
		ResourceConfig resourceConfig = ResourceConfig.forApplication(config);
		for (String key : bundleReader.getKeys()) {
			LOG.info("Register property " + key);
			resourceConfig.property(key, bundleReader.getValueAsString(key));
		}
	}

	public static void initRegisterMeterBinder(ResourceConfig config) {
		LOG.info("Register meter config binder");
		config.register(new MeterConfigBinder());
	}

	public static void initUtilitiesXML(ResourceConfig config) {
		LOG.info("Register utilities related to XML");
		config.register(MultiPartFeature.class);
		config.register(JacksonJaxbXMLProvider.class);
	}

	public static void initSwaggerProvider(ResourceConfig config) {
		LOG.info("OpenApi for Swagger is initialized");
		ResourceConfig resourceConfig = ResourceConfig.forApplication(config);
		String value = (String) resourceConfig.getProperty("activation-swagger");
		if (value == null || value.isEmpty()) {
			LOG.error("Property for activation-swagger not exist");
		} else {
			value = value.trim();
			if (!value.equals("true") && !value.equals("false")) {
				LOG.error("Property activation-swagger have setting wrong value");
			} else if (value.equals("true")) {
				LOG.info("Swagger is registered");

				OpenApiResource openApiResource = new OpenApiResource();
				config.register(openApiResource);
			}
		}
	}

	public static void initEncoding(ResourceConfig config) {
		ResourceConfig resourceConfig = ResourceConfig.forApplication(config);
		String value = (String) resourceConfig.getProperty("encoding-activate");

		if (value == null || value.isEmpty()) {
			LOG.error("Property for activation encoding not exist");
		} else {
			value = value.trim();
			if (!value.equals("true") && !value.equals("false")) {
				LOG.error("Property activation encoding have setting wrong value");
			} else if (value.equals("true")) {
				LOG.info("Activated encoding");

				config.register(EncodingFilter.class);
				config.register(GZipEncoder.class);
				config.register(DeflateEncoder.class);
			}
		}
	}

	public static void initLogger() {
		LOG.info("logger config provider is initialized");
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

}
