package de.axxepta.dao.implementations;

import static jetbrains.exodus.env.StoreConfig.WITHOUT_DUPLICATES;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.basex.core.BaseXException;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import de.axxepta.basex.RunDirectCommands;
import de.axxepta.basex.RunDirectCommandsSingleton;
import de.axxepta.dao.interfaces.IDocumentCacheDAO;
import de.axxepta.dao.interfaces.IDocumentDAO;
import de.axxepta.properties.BuildResourceBinderReader;
import de.axxepta.properties.ResourceBundleReader;
import de.axxepta.services.interfaces.IHandlingFileDOM;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;

@Service(name = "DocumentDatabaseCacheDAO")
@Singleton
public class DocumentDBCacheDAOImpl implements IDocumentCacheDAO {

	private static final Logger LOG = LoggerFactory.getLogger(DocumentDBCacheDAOImpl.class);

	public static final String STORE_DOCUMENTS_PATH_DB = System.getProperty("user.dir") + File.separator
			+ "RestServicesModule" + File.separator + "xodus-db" + File.separator + ".store-documents-xodus";

	private final Environment env;

	private final Store storeDocuments;

	private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

	private String commonDatabaseName;

	int secondsDelayStop;
	
	@Inject
	@Named("BaseXDao")
	private IDocumentDAO documentDAO;

	@Inject
	@Named("HandlingFileDOM")
	private IHandlingFileDOM handlingFileDOM;

	private RunDirectCommands runDirectCommands;

	private Runnable transferRunnable = new Runnable() {

		@Override
		public void run() {
			LOG.info("Start executor for transfering file " + LocalDateTime.now());
			transferFiles();
		}
	};

	private void transferFiles() {
		List<String> fileNameList = getSavedFilesName();
		for (String fileName : fileNameList) {
			LOG.info("Transfer file " + fileName + " from Xodus to BaseX");
			String content = getContentFile(fileName);

			int resultTransfer;
			try {
				resultTransfer = saveInCommonDatabase(fileName, content);
				if (resultTransfer / 100 == 2)
					LOG.info("Result to transfer " + fileName + " to BaseX database " + resultTransfer);
				else
					LOG.error("Exception during transfer file content in BaseX with code " + +resultTransfer);
			} catch (IOException e) {
				LOG.error("Exception during stream close " + e.getMessage());
			}

			if (!delete(fileName))// delete also from Xodus
				LOG.error(fileName + " cannot be deleted from Xodus database");
			else
				LOG.info(fileName + " was deleted from Xodus");
		}
	}

	private int saveInCommonDatabase(String savedName, String content) throws IOException {
		InputStream stream = new ByteArrayInputStream(content.getBytes());	
		int code =  documentDAO.uploadXMLDocument(savedName, stream, commonDatabaseName);
		stream.close();		
		return code;
	}

	public DocumentDBCacheDAOImpl() {
		env = Environments.newInstance(STORE_DOCUMENTS_PATH_DB);
		storeDocuments = env.computeInTransaction(txn -> env.openStore("Sessions", WITHOUT_DUPLICATES, txn));
		LOG.info("Create DAO object for documents caching");

		runDirectCommands = RunDirectCommandsSingleton.INSTANCE.getRunCommands();
	}

	@PostConstruct
	private void initTransfer() {
		ResourceBundleReader resourceBundleReader = new BuildResourceBinderReader("ArgonServerConfig")
				.getBundlerReader();

		try {
			commonDatabaseName = (String) resourceBundleReader.getValueAsString("common-database-name");
		} catch (Exception e) {
			LOG.error("common-database-name property cannot be found");
			commonDatabaseName = "test-base";
		}

		LOG.info("Read common database name from configuration file");

		if (!runDirectCommands.existDatabase(commonDatabaseName)) {
			try {
				runDirectCommands.createDatabase(commonDatabaseName, null);
			} catch (BaseXException e) {
				LOG.error(e.getMessage());
			}
		}

		int durationStartExecutor = 3;
		int periodExecutor = 3;
		String unit = "min";

		try {
			String durationStartExecutorUnit = (String) resourceBundleReader
					.getValueAsString("duration-start-executor");
			if (durationStartExecutorUnit.length() > 3) {
				unit = durationStartExecutorUnit.substring(durationStartExecutorUnit.length() - 3);
				durationStartExecutor = Integer.parseUnsignedInt(
						durationStartExecutorUnit.substring(0, durationStartExecutorUnit.length() - 3));
				LOG.info("starting time of the executor will be set " + durationStartExecutor + unit);
			}
		} catch (Exception e) {
			LOG.error("number-minutes-start-executor wasn't found or isn't a number");
		}

		try {
			String periodMinutesExecutorUnit = (String) resourceBundleReader
					.getValueAsString("period-minutes-executor");
			if (periodMinutesExecutorUnit.length() > 3) {
				unit = periodMinutesExecutorUnit.substring(periodMinutesExecutorUnit.length() - 3);
				periodExecutor = Integer.parseUnsignedInt(
						periodMinutesExecutorUnit.substring(0, periodMinutesExecutorUnit.length() - 3));
				LOG.info("period time of the executor will be set " + periodExecutor + unit);
			}
		} catch (Exception e) {
			LOG.error("period-minutes-executor wasn't found or isn't a number");
		}

		if (unit.equals("min") || unit.equals("sec")) {
			unit = "min";
		}
		if (unit.equals("sec")) {
			scheduledExecutorService.scheduleAtFixedRate(transferRunnable, durationStartExecutor, periodExecutor,
					TimeUnit.SECONDS);
			secondsDelayStop = periodExecutor;
		} else {
			scheduledExecutorService.scheduleAtFixedRate(transferRunnable, durationStartExecutor, periodExecutor,
					TimeUnit.MINUTES);
			secondsDelayStop = periodExecutor * 60;
		}

	}

	@Override
	public Boolean setDatabaseName(String databaseName) {

		if (this.commonDatabaseName.equals(databaseName))
			return true;
		if (!runDirectCommands.existDatabase(databaseName))
			return false;

		transferFiles();

		this.commonDatabaseName = databaseName;

		return true;
	}

	@Override
	public List<String> getSavedFilesName() {
		List<String> listFileNames = new ArrayList<>();
		env.executeInTransaction(new TransactionalExecutable() {
			@Override
			public void execute(@NotNull final Transaction txn) {
				try (Cursor cursor = storeDocuments.openCursor(txn)) {
					while (cursor.getNext()) {
						final ByteIterable key = cursor.getKey();
						String fileName = StringBinding.entryToString(key);
						listFileNames.add(fileName);
					}
				}
			}
		});

		LOG.info("Saved " + listFileNames.size() + " files");
		return listFileNames;
	}

	@Override
	public String getContentFile(String fileName) {

		StringBuilder response = new StringBuilder();

		LOG.info("Try to get content of " + fileName);

		@NotNull
		final ByteIterable keyFileName = StringBinding.stringToEntry(fileName);
		env.executeInTransaction(new TransactionalExecutable() {
			@Override
			public void execute(@NotNull final Transaction txn) {
				final ByteIterable entry = storeDocuments.get(txn, keyFileName);

				if (entry == null) {
					LOG.info("entry for " + fileName + " not exist in Xodus database");
					response.append("");
				} else {
					LOG.info("Value of entry was found");
					String value = StringBinding.entryToString(entry);
					response.append(value);

				}
			}
		});

		LOG.info("Get content of the file " + fileName);

		if (response.length() == 0) {
			LOG.info("Content of file " + fileName + " not founded it in Xodus database");
			Document document = documentDAO.readXMLDocument(fileName, commonDatabaseName);// limited to XML documents

			if (document == null)
				return null;

			return handlingFileDOM.printDocument(document, "UTF-8", true);
		} else {
			return response.toString();
		}
	}

	@Override
	public boolean save(String fileName, String content) {
		if (fileName == null || fileName.isEmpty())
			return false;
		if (content == null || content.isEmpty())
			return false;

		@NotNull
		final ByteIterable keyFileName = StringBinding.stringToEntry(fileName);

		@NotNull
		final ByteIterable valueContent = StringBinding.stringToEntry(content);
		env.executeInTransaction((txn) -> {
			storeDocuments.put(txn, keyFileName, valueContent);
		});

		LOG.info("Save content of the file with name " + fileName);

		return true;
	}

	@Override
	public boolean update(String fileName, String content) {
		if (fileName == null || fileName.isEmpty())
			return false;
		if (content == null || content.isEmpty())
			return false;

		final ByteIterable keyFileName = StringBinding.stringToEntry(fileName);
		@NotNull
		final ByteIterable valueContent = StringBinding.stringToEntry(content);
		env.executeInTransaction((txn) -> {
			if (storeDocuments.get(txn, valueContent) != null) {
				storeDocuments.put(txn, keyFileName, valueContent);
				LOG.info("Update file with name" + fileName);
			}

		});

		LOG.info("Update content of file " + fileName);

		return true;
	}

	@Override
	public boolean delete(String fileName) {
		@NotNull
		final ByteIterable key = StringBinding.stringToEntry(fileName);
		AtomicBoolean success = new AtomicBoolean();
		success.set(true);
		env.executeInTransaction((txn) -> {
			boolean isDel = storeDocuments.delete(txn, key);
			if (!isDel) {
				LOG.error("Document with nane " + fileName + " wasn't deleted");
				success.set(false);
			}
		});

		LOG.info("Delete document with name " + fileName);

		String content = getContentFile(fileName);
		if (content != null && !content.isEmpty())
			LOG.error("Delete was in error for " + fileName);

		return success.get();
	}

	@Override
	public List<String> getListFileName(boolean isFromCache) {
		if (isFromCache)
			return getSavedFilesName();
		else
			return documentDAO.getAllFilesName(commonDatabaseName);
	}

	@PreDestroy
	private void stopChacheDAO() {
		runDirectCommands.close();
		scheduledExecutorService.shutdown();
		env.close();
	}

}
