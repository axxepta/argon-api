package de.axxepta.tools.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.axxepta.tools.FileNameVersioned;

public class VersioningTest {

	private List<String> fileNameUnversionedList;
	private List<String> fileNameVersionedList;
	
	@Before
	public void initTest() {
		fileNameUnversionedList = Arrays.asList("SampleDoc.doc", "Sample.txt", "Sample.xml");
		fileNameVersionedList = Arrays.asList("file1.v25.xml", "file.v1.2.txt", "file2.v1.2.2.txt", "file2.v3.doc");
	}
	
	@Test
	public void versionedFileName() {
		for(String fileName : fileNameVersionedList) {
			assertTrue(FileNameVersioned.isVersionatedFile(fileName));
		}
	}
	
	@Test
	public void versioningFiles() {
		for(String fileName : fileNameUnversionedList) {
			try {
				String fileNameFirstVersioned = FileNameVersioned.getFileNameWithInitialVersion(fileName);
				assertTrue(FileNameVersioned.isVersionatedFile(fileNameFirstVersioned));
			} catch (Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	@Test
	public void addSubVersionFile() {
		final String firstVersionedFileName = fileNameVersionedList.get(0);
		try {
			String subVersionedFileName = FileNameVersioned.addSubVersion(firstVersionedFileName, true);
			assertTrue(FileNameVersioned.isVersionatedFile(subVersionedFileName));
			assertTrue(subVersionedFileName.split("\\.").length == firstVersionedFileName.split("\\.").length + 1);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void getInitialFileName() {
		for(String fileName : fileNameVersionedList) {
			try {
				String initialFileName = FileNameVersioned.getInitialFileName(fileName, true);
				assertTrue(initialFileName.split("\\.").length == 2);
			} catch (Exception e) {
				fail(e.getMessage());
			}		
		}
	}
	
	@Test
	public void changeVersionFile() {
		final String firstVersionedFileName = fileNameVersionedList.get(0);
		try {
			String fileName = FileNameVersioned.modifyVersionFileName(firstVersionedFileName , (byte)-5, (byte) 0, true);
			assertTrue(FileNameVersioned.isVersionatedFile(fileName));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
