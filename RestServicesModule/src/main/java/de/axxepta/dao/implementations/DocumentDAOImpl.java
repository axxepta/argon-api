package de.axxepta.dao.implementations;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.basex.core.BaseXException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.axxepta.basex.RunDirectCommands;
import de.axxepta.dao.interfaces.IDocumentDAO;
import de.axxepta.tools.ValidationDocs;

@Service(name = "BaseXDao")
@Singleton
public class DocumentDAOImpl implements IDocumentDAO {

	private static final Logger LOG = Logger.getLogger(DocumentDAOImpl.class);

	@Context
	private HttpServletRequest request;

	private String scheme;
	private String hostName;
	private int port;
	private String baseURL;
	private String username;
	private String password;

	private Client client;

	private RunDirectCommands runDirectCommands;
	
	@PostConstruct
	public void initConnection() {
		scheme = request.getScheme();
		hostName = "localhost";
		port = request.getLocalPort();
		baseURL = "argon-server/argon-rest/rest";

		username = "admin";
		password = "admin";

		client = createClient(username, password);
		
		runDirectCommands = new RunDirectCommands();
	}

	private Client createClient(String usermane, String password) {
		LOG.info("Create a client for username " + username);
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().nonPreemptive()
				.credentials(username, password).build();

		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(feature);
		Client client = ClientBuilder.newClient(clientConfig);
		return client;
	}

	@Override
	public void executeQuery(String databaseName, String query) {
		LOG.info("Set actual connection for " + databaseName);
	}

	@Override
	public Map<String, Map<String, String>> showDatabases() {

		Map<String, Map<String, String>> infoDatabasesMap = new HashMap<>();
		String[] arrayDatabases = runDirectCommands.listDatabases();

		for (String nameDatabase : arrayDatabases) {
			Map<String, String> info;
			try {
				info = runDirectCommands.getDatabaseInfo(nameDatabase);
			} catch (BaseXException e) {
				LOG.error("Exception for database with name " + nameDatabase + " : " + e.getMessage());
				continue;
			}
			infoDatabasesMap.put(nameDatabase, info);
		}

		return infoDatabasesMap;
	}

	@Override
	public String showInfoDatabase(String databaseName) {
		if (!runDirectCommands.existDatabase(databaseName))
			return null;

		try {
			return runDirectCommands.showInfoDatabase(databaseName);
		} catch (BaseXException e) {
			LOG.error(e.getMessage());
			return null;
		}
	}

	@Override
	public boolean test(String resource) {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().nonPreemptive()
				.credentials(username, password).build();

		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(feature);
		String urlBaseX = composeURL(resource);
		WebTarget webTarget = client.target(urlBaseX);

		if (webTarget == null) {
			LOG.error("HTTP URL connection is null");
			return false;
		}
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		Response response = invocationBuilder.get();
		int code;
		try {
			code = response.getStatus();
		} catch (ProcessingException e) {
			LOG.error(e.getMessage());
			client.close();
			return false;
		}
		LOG.info("Response is " + code);
		client.close();
		if (code == 200)
			return true;
		else
			return false;

	}

	@Override
	public Document readXMLDocument(String documentName, String databaseName) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments(true);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setValidating(false);
		DocumentBuilder docBuilder;
		try {
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOG.error("Parser configuration exception " + e.getMessage());
			return null;
		}

		String databaseRESTfulURL = composeURL(databaseName);

		WebTarget webTarget = client.target(databaseRESTfulURL + '/' + documentName);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_XML);
		Response response = invocationBuilder.get();
		int code = response.getStatus();
		LOG.info("For upload XML file response is " + code);

		if (code == 200) {
			File readFile = response.readEntity(File.class);
			try {
				Document doc = docBuilder.parse(readFile);
				return doc;
			} catch (SAXException | IOException e) {
				LOG.error("Exception " + e.getMessage());
				return null;
			}
		} else {
			LOG.error("response code for downloading " + documentName + " is " + code);
			return null;
		}
	}
	
	@Override
	public int uploadXMLDocument(File file, boolean withSchemaValidation, String databaseName) {
		if (file == null || !file.exists() || !file.isFile()) {
			LOG.info("file not exists or is a directory");
			return 500;
		}

		if (withSchemaValidation) {
			if (ValidationDocs.validateXMLSchema.isDocTypeValid(file)) {
				LOG.error(file.getName() + " is not an XML document valid");
				return 500;
			}
		} else {
			if (ValidationDocs.validateXMLWithDOM.isDocTypeValid(file)) {
				LOG.error(file.getName() + " is not an XML document valid");
				return 500;
			}
		}

		String databasePath = composeDatabasePath(databaseName);

		if (databasePath == null)
			return 500;

		String databaseRESTfulURL = composeURL(databaseName);
		/*
		 * String fileName =
		 * nameFilePath.substring(nameFilePath.lastIndexOf(File.separator) + 1,
		 * nameFilePath.length()); File to = new File(databasePath + File.separator +
		 * fileName); try { Files.copy(file.toPath(), to.toPath()); } catch (IOException
		 * e) { LOG.error("IOException " + e.getMessage()); return 500; }
		 */

		WebTarget webTarget = client.target(databaseRESTfulURL);

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		Response response = invocationBuilder.put(Entity.xml(file));

		int code = response.getStatus();
		LOG.info("For upload XML file response is " + code);
		client.close();

		return code;
	}

	@Override
	public int deleteDocument(String fileName, String databaseName) {

		String databasePath = composeDatabasePath(databaseName);

		if (databasePath == null)
			return 500;

		String databaseRESTfulURL = composeURL(databaseName);

		WebTarget webTarget = client.target(databaseRESTfulURL + '/' + fileName);

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		Response response = invocationBuilder.delete();
		int code = response.getStatus();

		LOG.info("For delete XML file response is " + code);
		client.close();

		return code;

	}

	@Override
	public Boolean createDatabase(String databaseName, String fileURL) {
		if (runDirectCommands.existDatabase(databaseName)) {
			LOG.info("Database with name " + databaseName + " already exists");
			return null;
		}
		File file = null;
		if (fileURL != null) {
			String fileName = fileURL.substring(fileURL.lastIndexOf('/') + 1, fileURL.length());
			String prefixFile = FilenameUtils.removeExtension(fileName);
			try {
				file = File.createTempFile(prefixFile, ".tmp");
			} catch (IOException e) {
				LOG.error("Temp file cannot be created " + e.getMessage());
				return null;
			}
		}
		try {
			runDirectCommands.createDatabase(databaseName, file);
		} catch (BaseXException e) {
			LOG.error(e.getMessage());
			file.delete();
			return false;
		}
		if (file != null)
			file.delete();
		return true;
	}

	@Override
	public Boolean dropDatabase(String databaseName) {
		if (!runDirectCommands.existDatabase(databaseName))
			return null;
		try {
			runDirectCommands.dropDatabase(databaseName);
		} catch (BaseXException e) {
			LOG.error(e.getMessage());
			return false;
		}
		return true;
	}

	private String composeURL(String resourceDatabase) {
		return scheme + "://" + hostName + ':' + port + '/' + baseURL + '/' + resourceDatabase;
	}
	
	private String composeDatabasePath(String databaseName) {
		String databasePath = null;
		if (showDatabases().containsKey(databaseName)) {
			Map<String, String> propertiesDatabaseMap = showDatabases().get(databaseName);
			String databasePathName = propertiesDatabaseMap.get("inputpath");
			databasePath = databasePathName.substring(0, databasePathName.lastIndexOf(File.separator));
			LOG.info("For database " + databaseName + " path is " + databasePath);
			return databasePath;
		} else {
			LOG.info("database with name " + databaseName + " not exists");
			return null;
		}
	}

	@PreDestroy
	private void close() {
		runDirectCommands.close();
	}
	
}