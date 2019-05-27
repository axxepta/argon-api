package de.axxepta.tools.tests;

import org.junit.Test;

import de.axxepta.tools.TransformJSONToXML;
import de.axxepta.tools.ValidateURL;
import de.axxepta.tools.ValidationDocs;
import de.axxepta.tools.ValidationString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class RestToolsTest {

	private final static String PATH_RESOURCES_DIR = "src/test/resources";

	@Test
	public void test() {
		String test = "test";
		assertEquals(test, "test");
	}

	@Test
	public void existenceDirectoryResource() {
		File resourcesDirectory = new File(PATH_RESOURCES_DIR);
		if (!resourcesDirectory.exists())
			fail("Resource directory not exists");
	}

	@Test
	public void existenceFileURLResource() {
		final String fileURLResource = PATH_RESOURCES_DIR + File.separator + "TestResourceURL.txt";
		if (!new File(fileURLResource).exists())
			fail("Resource file with URLs not exists " + fileURLResource);
	}

	@Test
	public void testValidationURL() {
		List<String> listURL = null;
		try {
			listURL = ExtractFileNameTool
					.getStringListFromFile(PATH_RESOURCES_DIR + File.separator + "TestResourceURL.txt");
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

		for (String url : listURL) {
			if (!ValidateURL.validateURL.isURLValid(url)) {
				fail(url + " is not valid");
				break;
			}
		}
	}

	@Test
	public void testValidationXML() {
		List<File> listXMLFile = null;
		try {
			listXMLFile = ExtractFileNameTool
					.extractListFile(PATH_RESOURCES_DIR + File.separator + "TestResourceXML.txt");
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}
	
		for (File file : listXMLFile) {

			if (!ValidationDocs.validateXMLWithDOM.isDocTypeValid(file))
				fail(file.getPath() + " is not an XML file valid");
		}
	}

	@Test
	public void testValidationJSON() {
		List<File> listJSONFile = null;
		try {
			listJSONFile = ExtractFileNameTool
					.extractListFile(PATH_RESOURCES_DIR + File.separator + "TestResourceJson.txt");
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}

		for (File file : listJSONFile) {
			if (!file.exists()) {
				fail(file + " not exist");
			}

			if (!ValidationDocs.validateJSONDocs.isDocTypeValid(file))
				fail(file.getPath() + " is not an JSON file valid");
		}
	}

	@Test
	public void convertJsonToXML() {
		List<File> listJSONFile = null;
		try {
			listJSONFile = ExtractFileNameTool
					.extractListFile(PATH_RESOURCES_DIR + File.separator + "TestResourceJson.txt");
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}
		
		for (File file : listJSONFile) {
			String xmlContent = TransformJSONToXML.transformJSON.transformToXML(file);
			System.out.println(xmlContent);
			try {
				File tempFileXML = File.createTempFile("XMLTestFile", ".tmp");
				PrintWriter out = new PrintWriter(tempFileXML);
				out.println(xmlContent);
				out.close();

				if (!ValidationDocs.validateXMLWithDOM.isDocTypeValid(tempFileXML))
					fail("Generated  file from " + file.getPath() + " is not an XML file valid");

				if (!tempFileXML.delete()) {
					fail("Generated  file " + file.getPath() + " cannot be deleted");// <-- fail here
				}
			} catch (IOException e) {
				fail("Cannot create tempt file " + e.getMessage());
			}
		}
	}

	@Test
	public void validationStrings() {
		final String simple = "abcdefgh";
		if (!ValidationString.validationString(simple, "simple")) {
			fail("simple string cannot be validate");
		}

		if (ValidationString.validationString("", "")) {
			fail("that cannot be nether valid");
		}
	}

}
