package de.axxepta.basex.tests;

import java.io.IOException;
import java.net.Socket;

import org.basex.BaseXServer;
import org.basex.api.client.ClientSession;
import org.basex.core.BaseXException;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.Test;

import de.axxepta.basex.RunDirectCommands;
import de.axxepta.basex.RunDirectCommandsSingleton;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommandsBasexTest {

	private static final int PORT = 1984; //default port
	private BaseXServer server;
	private ClientSession session;

	final String DATABASE_NAME = "test";

	private RunDirectCommands runDirectCommands;

	@Before
	public void initServer() {
		try (Socket ignored = new Socket("localhost", PORT)) {
			fail("BaseX object cannot be created, port " + PORT + " occupied");
		}
		catch(IOException e) {
			
		}

		try {
			server = new BaseXServer();
		} catch (IOException e) {
			fail("Exception server " + e.getMessage());
		}
		
		System.out.println("\n* Create a client session.");

		try {
			session = new ClientSession("localhost", PORT, "admin", "admin");
		} catch (IOException e) {
			fail("Client cannot be created");
		}

		runDirectCommands = RunDirectCommandsSingleton.INSTANCE.getRunCommands();
	}

	@Test
	public void display() {
		System.out.println("test message");
		assertTrue(true);
	}

	@Test
	public void createTest() {
		try {
			runDirectCommands.createDatabase(DATABASE_NAME, null);
			assertTrue(true);
		} catch (BaseXException e) {
			fail("Database cannot be created " + e.getMessage());
		}

		try {
			runDirectCommands.openDatabase(DATABASE_NAME);
		} catch (BaseXException e) {
			fail("Database cannot be open " + e.getMessage());
		}
	}

	@Test
	public void existDatabase() {
		assertTrue(runDirectCommands.existDatabase(DATABASE_NAME));
	}

	@Test
	public void displayDatabaseNames() {
		String[] listNamesDatabases = runDirectCommands.listDatabases();
		for (int i = 0; i < listNamesDatabases.length; i++) {
			System.out.println("Database " + (i + 1) + listNamesDatabases[i]);
		}
		assertTrue(listNamesDatabases.length > 0);
	}

	@Test
	public void xDeleteDatabase() {
		try {
			runDirectCommands.dropDatabase(DATABASE_NAME);
		} catch (BaseXException e) {
			fail("Database cannot be deleted " + e.getMessage());
		}
	}

	@After
	public void stop() {
		runDirectCommands.close();

		try {
			session.close();
		} catch (IOException e) {
			fail("Client session cannot be stopped " + e.getMessage());
		}

		if (BaseXServer.ping("localhost", PORT))
			server.stop();
	}

}
