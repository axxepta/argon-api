package de.axxepta.services.implementations;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import de.axxepta.dao.interfaces.IDocumentCacheDAO;
import de.axxepta.dao.interfaces.IDocumentDAO;
import de.axxepta.services.interfaces.IDatabaseResourceService;

@Service(name = "DatabaseBaseXServiceImplementation")
@Singleton
public class DatabaseResourceServiceImpl implements IDatabaseResourceService {

	private static final Logger LOG = Logger.getLogger(DatabaseResourceServiceImpl.class);

	@Inject
	@Named("BaseXDao")
	private IDocumentDAO documentDAO;

	@Inject
	@Named("DocumentMemoryCacheDAO")
	private IDocumentCacheDAO documentCacheDAO;

	private String defaultDatabaseName = "common-database";

	@Override
	public Boolean changeDatabaseName(String newDatabaseName) {

		boolean result = documentCacheDAO.setDatabaseName(newDatabaseName);
		if (!result) {
			LOG.info("Change database name in " + newDatabaseName);
			defaultDatabaseName = newDatabaseName;
			return true;
		} else {
			return false;
		}
	}

	private String getFileName(File file) {
		if (file == null) {
			return null;
		}

		return file.getName();
	}

	private String getFileContent(File file) {
		if (file == null) {
			return null;
		}

		try {
			return FileUtils.readFileToString(file);
		} catch (IOException e) {
			LOG.error("Exception in reading content of file " + e.getMessage());
			return null;
		}

	}

	@Override
	public boolean uploadFileToDatabase(File file) {
		String fileName = getFileName(file);

		if (fileName == null)
			return false;

		String content = getFileContent(file);

		if (content == null || content.isEmpty())
			return false;

		// nu e corect
		boolean fileExists = documentDAO.showDatabases().containsKey(fileName);

		if (fileExists) {
			LOG.info("Update file with name " + file.getName() + " in database " + defaultDatabaseName);
			boolean r = documentCacheDAO.update(fileName, content);
			return r;
		} else {
			LOG.info("Save file with name " + file.getName() + " in database " + defaultDatabaseName);
			boolean r = documentCacheDAO.save(fileName, content);
			return r;
		}

	}

	@Override
	public boolean deleteFileFromDatabase(File file) {
		String fileName = getFileName(file);

		if (fileName == null)
			return false;
		LOG.info("Delete file with name " + fileName + " from database " + defaultDatabaseName);
		
		return documentCacheDAO.delete(fileName);
	}

	@Override
	public boolean testDB(String resourceName) {
		LOG.info("test resource " + resourceName);
		return documentDAO.test(resourceName);
	}

	@Override
	public String showInfosDatabase(String databaseName) {
		return documentDAO.showInfoDatabase(databaseName);
	}

}
