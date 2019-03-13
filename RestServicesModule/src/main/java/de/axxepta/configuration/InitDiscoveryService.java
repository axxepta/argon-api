package de.axxepta.configuration;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.Populator;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.DuplicatePostProcessor;

public class InitDiscoveryService {

	private static final Logger LOG = Logger.getLogger(InitDiscoveryService.class);
			
	public static void initDiscoveryService(ServiceLocator locator, ClassLoader classLoader) {		
		DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);		
		Populator populator = dcs.getPopulator();
		try {
			List<ActiveDescriptor<?>> activeDescriptorList = populator.populate(new ClasspathDescriptorFileFinder(classLoader),
					new DuplicatePostProcessor());
			LOG.info("Size active descriptor list " + activeDescriptorList.size());
			LOG.info("Populate with " + activeDescriptorList.toString());
		} catch (IOException | MultiException ex) {
			LOG.error(ex.getMessage());
		}
	}
}
