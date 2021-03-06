package de.axxepta.dao.interfaces;

import java.util.List;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface IDocumentCacheDAO {
	
	public Boolean setDatabaseName(String databaseName);
	
	public List<String> getSavedFilesName();
	
	public String getContentFile(String fileName);
	
	public boolean save(String fileName, String content);
	
	public boolean update(String fileName, String content);
	
	public boolean delete(String fileName);
	
	public List<String> getListFileName(boolean isFromCache);
	
}
