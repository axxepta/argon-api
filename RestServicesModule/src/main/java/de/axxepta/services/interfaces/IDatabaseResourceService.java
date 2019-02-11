package de.axxepta.services.interfaces;

import java.io.File;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface IDatabaseResourceService {
	
	public Boolean changeDatabaseName(String newDatabaseName);
	
	public boolean uploadFileToDatabase(File file);
	
	public boolean deleteFileFromDatabase(File file);
	
	public boolean testDB(String resourceName);
	
	public String showInfosDatabase(String databaseName);
}
