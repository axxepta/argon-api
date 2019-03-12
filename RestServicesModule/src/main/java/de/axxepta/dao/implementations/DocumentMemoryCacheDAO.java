package de.axxepta.dao.implementations;

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import de.axxepta.dao.interfaces.IDocumentCacheDAO;
import de.axxepta.properties.BuildResourceBinderReader;
import de.axxepta.properties.ResourceBundleReader;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

@Service(name = "DocumentMemoryCacheDAO")
@Singleton
public class DocumentMemoryCacheDAO implements IDocumentCacheDAO {

	private Cache cache;

	private static final Logger LOG = Logger.getLogger(DocumentMemoryCacheDAO.class);

	@Inject
	@Named("DocumentDatabaseCacheDAO")
	private IDocumentCacheDAO documentDatabaseCacheDAO;

	private ResourceBundleReader resourceBundleReader;
	
	@PostConstruct
	private void initCacheDAO() {
		
		CacheManager cacheManager = CacheManager.getInstance();
		cacheManager.addCache("documents");
		
		LOG.info("Caches " + Arrays.toString(cacheManager.getCacheNames()));
		
		if(!cacheManager.cacheExists("documents"))
			LOG.error("Cache documents not exists yet");
		else
			LOG.info("Cache documents was created");
		
		cache = cacheManager.getCache("documents");
				
		resourceBundleReader = new BuildResourceBinderReader("ArgonServerConfig").getBundlerReader();
		configureCache();
	}

	private void configureCache() {
		CacheConfiguration config = cache.getCacheConfiguration();
		config.setEternal(false);
		int maxElements = 1000;
		int timeLive = 100;

		LOG.info("Available keys " + resourceBundleReader.getKeys());
		
		try {
			maxElements = Integer
					.parseUnsignedInt((String) resourceBundleReader.getValueAsString("cache-max-elements-in-memory"));
		} catch (Exception e) {
			LOG.error("cachemax-elements-in-memory wasn't found or isn't a number");
		}

		try {
			timeLive = Integer
					.parseUnsignedInt((String) resourceBundleReader.getValueAsString("cache-seconds-time-to-live"));
		} catch (Exception e) {
			LOG.error("cache-seconds-time-to-live wasn't found or isn't a number");
		}

		LOG.info("Configure cache in memory : max elements " + maxElements + ", time live " + timeLive);

		config.setMaxElementsInMemory(maxElements);
		config.setTimeToLiveSeconds(timeLive);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getSavedFilesName() {
		return cache.getKeys();
	}

	@Override
	public Boolean setDatabaseName(String databaseName) {
		return documentDatabaseCacheDAO.setDatabaseName(databaseName);
	}

	@Override
	public String getContentFile(String fileName) {
		if (cache.isKeyInCache(fileName))
			return (String) cache.get(fileName).getValue();
		else
			return documentDatabaseCacheDAO.getContentFile(fileName);
	}

	@Override
	public boolean save(String fileName, String content) {
		Element element = new Element(fileName, content);
		cache.put(element);
		return documentDatabaseCacheDAO.save(fileName, content);
	}

	@Override
	public boolean update(String fileName, String content) {

		LOG.info("Update " + fileName);

		if (cache.isKeyInCache(fileName)) {
			Element element = new Element(fileName, content);
			cache.put(element);
			return documentDatabaseCacheDAO.update(fileName, content);
		} else
			return false;

	}

	@Override
	public boolean delete(String fileName) {

		LOG.info("Delete " + fileName);

		boolean response;
		if (cache.isKeyInCache(fileName)) {
			response = cache.remove(fileName);
			response = documentDatabaseCacheDAO.delete(fileName);
		} else
			response = false;

		return response;
	}

	@PreDestroy
	private void shutdownService() {
		CacheManager.getInstance().shutdown();
	}
}
