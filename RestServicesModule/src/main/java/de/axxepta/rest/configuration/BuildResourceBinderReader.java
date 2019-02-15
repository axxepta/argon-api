package de.axxepta.rest.configuration;

import java.io.File;
import java.util.Locale;

public class BuildResourceBinderReader {

	private final ResourceBundleReader bundlerReader;
	
	public BuildResourceBinderReader(String fileName) {
		final File fileConfig = new File(fileName);
		final Locale locale = new Locale("en");
		
		bundlerReader = new ResourceBundleReader(fileConfig, locale);

	}
	
	public ResourceBundleReader getBundlerReader() {
		return bundlerReader;
	}

}
