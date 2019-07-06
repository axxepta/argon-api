package de.axxepta.dao.implementations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
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
import org.basex.core.BaseXException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.axxepta.basex.RunDirectCommands;
import de.axxepta.basex.RunDirectCommandsSingleton;
import de.axxepta.dao.interfaces.IDocumentDAO;
import de.axxepta.listeners.RestDatabasePathContextListener;

@Service(name = "BaseXDao")
@Singleton
public class DocumentDAOImpl implements IDocumentDAO {

	private static final Logger LOG = LoggerFactory.getLogger(DocumentDAOImpl.class);

	@Context
	private HttpServletRequest request;

	private String scheme;
	private String hostName;
	private int port;
	private String baseURL;

	private Client client;

	private RunDirectCommands runDirectCommands;

	@PostConstruct
	public void initConnection() {
		scheme = request.getScheme();
		hostName = "localhost";
		port = request.getLocalPort();

		baseURL = RestDatabasePathContextListener.getRestPath();

		LOG.info("base URL for BaseX " + baseURL);

		client = ClientBuilder.newClient();
		
		runDirectCommands = RunDirectCommandsSingleton.INSTANCE.getRunCommands();

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
		
		String resourceURL= composeURL(resource);

		WebTarget webTarget = client.target(resourceURL);

		LOG.info("Web target URI " + webTarget.getUri());

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);

		int code;
		try {
			Response response = invocationBuilder.get();
			code = response.getStatus();
		} catch (WebApplicationException | ProcessingException e) {
			LOG.error(e.getMessage());
			return false;
		}
		LOG.info("Response is " + code);
		
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
		String resourceURL = databaseRESTfulURL + '/' + documentName;
		LOG.info("URL resource " + resourceURL);
		
		WebTarget webTarget = client.target(resourceURL);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_XML);
		Response response = invocationBuilder.get();
		int code = response.getStatus();
		LOG.info("For upload XML file response is " + code);
		
		if (code == 200) {
			File readFile = response.readEntity(File.class);
			try {
				Document doc = docBuilder.parse(readFile);

				Node rootNode = doc.getFirstChild();

				if (rootNode.getAttributes().getNamedItem("resources") != null
						&& rootNode.getAttributes().getNamedItem("resources").getNodeValue().equals("0")) {
					LOG.info("Document " + documentName + " not found in " + databaseName);
					return null;
				}
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
	public synchronized int uploadXMLDocument(String nameUsedSave, InputStream documentStream, String databaseName) {

		LOG.info("Try to upload in database file as " + nameUsedSave);

		String databasePath = composeDatabasePath(databaseName);

		if (databasePath == null) {
			LOG.info("Database path null");
			return 500;
		}

		String resourceURL = composeURL(databaseName);
		
		resourceURL +=  "/" + nameUsedSave;

		LOG.info("Database RESTful URL composed " + resourceURL);

		WebTarget webTarget = client.target("http://localhost:8801/argon-rest/rest/test-name/" + "pigs.xml");

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);
	
		Response response = null;
		
		try {			
			response = invocationBuilder.put(Entity.entity(documentStream, MediaType.APPLICATION_OCTET_STREAM_TYPE));
		} catch (ProcessingException e) {
			LOG.error("Exception  for obtain response " + e.getMessage());
			return 500;
		}

		int code = response.getStatus();
		LOG.info("For upload XML file response is " + code);
		return code;
	}

	@Override
	public int deleteDocument(String fileName, String databaseName) {

		String databasePath = composeDatabasePath(databaseName);

		if (databasePath == null) {
			LOG.info("Database path is null");
			return 500;
		}

		String databaseRESTfulURL = composeURL(databaseName);

		String urlDelete = databaseRESTfulURL + '/' + fileName;
		LOG.info("URL delete " + urlDelete);
		
		WebTarget webTarget = client.target(urlDelete);

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		Response response = invocationBuilder.delete();
		int code = response.getStatus();

		LOG.info("For delete XML file response is " + code);
		
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
			LOG.info("Try to run directly commands to create database");
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

	@Override
	public List<String> getAllFilesName(String databaseName) {
		LOG.info("Get all files name stored in database with name " + databaseName);
		String databaseURL = composeURL(databaseName);
		LOG.info("Database URL " + databaseURL);
		WebTarget webTarget = client.target(databaseURL);
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);

		Response response = invocationBuilder.get();

		if (response.getStatus() != 200) {
			LOG.info("Database " + databaseName + " cannot be open from REST");
			return null;
		}

		String responseString = (String) response.readEntity(String.class);

		LOG.info("response " + responseString);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document documentResponse = builder.parse(new ByteArrayInputStream(responseString.getBytes()));
			NodeList nodes = documentResponse.getElementsByTagName("rest:resource");

			LOG.info("Number of documents store in database " + databaseName + " " + nodes.getLength() + 1);
			List<String> fileNameList = new ArrayList<>();

			for (int i = 0; i < nodes.getLength(); i++) {
				fileNameList.add(nodes.item(i).getTextContent());
			}

			return fileNameList;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOG.error(e.getClass().getName() + " : " + e.getMessage());
			return null;
		}

	}

	private String composeURL(String resourceDatabase) {
		String urlDatabase = scheme + "://" + hostName + ':' + port + '/' + baseURL + '/' + resourceDatabase;
		LOG.info("Database URL " + urlDatabase);
		return urlDatabase;
	}

	private String composeDatabasePath(String databaseName) {// used to check directly if database exists
		String databasePath = runDirectCommands.getPathDatabases(databaseName);
		LOG.info("Database path " + databasePath);
		return databasePath;
	}

	@PreDestroy
	private void close() {
		client.close();
		runDirectCommands.close();
	}

}
