package de.axxepta.tools;

import de.axxepta.exceptions.VersionatedException;

public class FileNameVersioned {//using Semantic Versioning(SemVer) schema

	private final static String PATTERN_FILE_VERSIONED = "^[0-9A-Za-z]+[0-9A-Za-z ]*\\.v\\d{1,3}(\\.\\d{1,3}){0,2}\\.[0-9A-Za-z]+";

	public static boolean isVersionatedFile(String fileName) {
		return fileName.matches(PATTERN_FILE_VERSIONED);
	}

	public static String getInitialFileName(String versionatedFileName, boolean validate) throws VersionatedException {
		if (validate) {
			if (!isVersionatedFile(versionatedFileName)) {
				throw new VersionatedException("Not a versionated file");
			}
		}

		String[] partsFileName = versionatedFileName.split("\\.");

		return partsFileName[0] + "." + partsFileName[partsFileName.length - 1];
	}

	public static String getFileNameWithInitialVersion(String fileName) throws Exception {
		String[] partsFileName = fileName.split("\\.");

		if (partsFileName.length != 2) {
			throw new Exception("Invalid file name to be versionated");
		}

		return partsFileName[0] + ".v0.0.1." + partsFileName[1];
	}

	public static String modifyVersionFileName(String versionatedFileName, byte shiftingVersionUnits, byte position,
			boolean validate) throws Exception {
		if (validate) {
			if (!isVersionatedFile(versionatedFileName)) {
				throw new VersionatedException("Not a versionated file");
			}
		}

		if (position < 0 || position > 2) {
			throw new Exception("Not valid possition of version index");
		}

		String[] partsFileName = versionatedFileName.split("\\.");

		if(partsFileName.length - 3 < position) {
			throw new Exception("Cannot be setted that position");
		}
		
		Byte [] version = getVersion(versionatedFileName, false); 
		
		byte sequenceVersion = version[position];
		
		byte newVersion = (byte) (sequenceVersion + shiftingVersionUnits);
		
		if(newVersion < 0 || newVersion > 127) {
			throw new Exception("Too large shift");
		}
		version[position] = newVersion;
		
		String versionString = "";
		for(int i = 0; i < version.length; i ++)
			if(version[i] != null)
				versionString += String.valueOf(version[i]) + ".";
			else break;
		versionString = ".v" + versionString;
		
		return partsFileName[0] + versionString +  partsFileName[partsFileName.length - 1] ;
	}

	public static String addSubVersion(String versionatedFileName, boolean validate) throws Exception {
		if (validate) {
			if (!isVersionatedFile(versionatedFileName)) {
				throw new VersionatedException("Not a versionated file");
			}
		}
		
		String[] partsFileName = versionatedFileName.split("\\.");
		
		if(partsFileName.length == 5)
			throw new Exception("File cannot be sub-versioned");
		
		String firstPart = versionatedFileName.substring(0, versionatedFileName.lastIndexOf("."));
		
		return firstPart + ".1." + partsFileName[partsFileName.length - 1];
	}

	public static Byte[] getVersion(String versionatedFileName, boolean validate) throws VersionatedException {
		if (validate) {
			if (!isVersionatedFile(versionatedFileName)) {
				throw new VersionatedException("Not a versionated file");
			}
		}
		
		String[] partsFileName = versionatedFileName.split("\\.");
		partsFileName[1] = partsFileName[1].substring(1);
		Byte[] version = new Byte[3];
		
		for(int i = 1; i < partsFileName.length - 1; i ++) {
			version[i - 1] = Byte.parseByte(partsFileName[i]);
		}
		
		return version;
	}
}
