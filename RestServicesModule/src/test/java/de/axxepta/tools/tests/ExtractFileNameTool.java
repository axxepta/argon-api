package de.axxepta.tools.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ExtractFileNameTool {

	public static List<File> extractListFile(String fileNameWithListPath) throws FileNotFoundException {
		List<String> listFileName = getStringListFromFile(fileNameWithListPath);
		String resourcesDirectoryPath = fileNameWithListPath.substring(0, fileNameWithListPath.lastIndexOf(File.separator));
		List<File> listFile = new ArrayList<>();
		for (String fileName : listFileName) {
			File file = new File(resourcesDirectoryPath + File.separator + fileName);
			if (!file.exists()) {
				throw new FileNotFoundException("File " + file.getName() + " not exists");
			}
			listFile.add(file);
		}
		return listFile; 
	}
	
	public static List<String> getStringListFromFile(String fileNameWithListPath) throws FileNotFoundException {
		File fileURL = new File(fileNameWithListPath);
		Scanner sc;
		List<String> listLines = new ArrayList<>();
		try {
			sc = new Scanner(fileURL);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if(line.startsWith("#"))
					continue;
				listLines.add(line);
			}
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("File " + fileURL.getName() + " not exists");
		}
		sc.close();
		return listLines;
	}

}
