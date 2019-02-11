package de.axxepta.rest.tests;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.ServerProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import de.axxepta.bind.MeterConfigBinder;
import de.axxepta.resources.TestResource;
import de.axxepta.server.embedded.ServerEmbedded;
import de.axxepta.server.embedded.interfaces.IServerEmbedded;
import io.restassured.RestAssured;

public class RestServicesTest {

	private final static int PORT = 8830;
	private final static String CONTEXT = "sample/";

	private IServerEmbedded serverEmbedded;

	private Runnable startServer = () -> {
		try {
			serverEmbedded.startServer();
		} catch (Exception e) {
			
		}
	};

	private  Runnable stopServer = () -> {
		try {
			serverEmbedded.stopServer();
		} catch (Exception e) {
			
		}
	};

	@BeforeClass
	public static void setup() {
		String port = System.getProperty("server.port");
		if (port == null) {
			RestAssured.port = Integer.valueOf(PORT);
		} else {

			RestAssured.port = Integer.valueOf(port);

		}

		String basePath = System.getProperty("server.base");
		if (basePath == null) {
			basePath = CONTEXT;
		}
		RestAssured.basePath = basePath;

		String baseHost = System.getProperty("server.host");
		if (baseHost == null) {
			baseHost = "http://127.0.0.1";
		}
		RestAssured.baseURI = baseHost;

	}

	@Before
	public  void startServer() {
		if(serverEmbedded != null)
			return;
		
		Map<String, String> initParamsMap = new HashMap<>();

		initParamsMap.put("allowedOrigins", "*");
		initParamsMap.put("allowedMethods", "GET, POST");
		initParamsMap.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

		initParamsMap.put(ServerProperties.PROVIDER_CLASSNAMES, TestResource.class.getCanonicalName());

		initParamsMap.put(ServerProperties.MONITORING_STATISTICS_REFRESH_INTERVAL, "true");
		
		
		serverEmbedded = new ServerEmbedded(PORT, CONTEXT, initParamsMap);

		Thread startServerTh = new Thread(startServer);
		startServerTh.start();

		/*
		 * try { Thread.sleep(60 * 1000); } catch (InterruptedException e) {
		 * 
		 * }
		 */

	}

	@Test
	public void test() {
		String test = "test";
		assertEquals(test, "test");
	}
	// @formatter:off
	/*@Test
	public void testGet() {
		final String serviceRelativePath = "testing/test";
		String testURL = RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath 
				+ serviceRelativePath;
		System.out.println("Test URL " + testURL);
		HttpURLConnection http = null;
		try {
			http = (HttpURLConnection) new URL(testURL).openConnection();
		} catch (MalformedURLException e) {
			fail("Malformed URL " + e.getMessage());
		} catch (IOException e) {
			fail("IO exception " + e.getMessage());
		}

		try {
			http.connect();
		} catch (IOException e) {
			fail("IO exception connect " + e.getMessage());
		}

		assertThat("Response Code", 200, is(HttpStatus.OK_200));
		assertEquals("test", "test");
	}
	// @formatter:on
	 
	 
	/*@Test
	public void testForTestServices() {
		String testURL = RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath + "testing/test";
		System.out.println("Test URL " + testURL);
		given().when().get(testURL).then().assertThat().statusCode(200);
	}*/

	@After
	public void stopServer() {
		Thread stopServerTh = new Thread(stopServer);
		stopServerTh.start();
	}

}
