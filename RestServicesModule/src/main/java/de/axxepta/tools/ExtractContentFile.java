package de.axxepta.tools;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class ExtractContentFile {
	
	private final File file;
	
	private final String fileContent;
	
	public ExtractContentFile(String fileNamePath) throws IOException {
		file = new File(fileNamePath);
		fileContent = FileUtils.fileRead(file);
	}
	
	public File getFile() {
		return file;
	}
	
	public String getFileName() {
		return file.getName();
	}
	
	public String getFileContent() {
		return fileContent;
	}
	
}
