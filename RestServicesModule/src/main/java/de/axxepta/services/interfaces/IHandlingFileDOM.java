package de.axxepta.services.interfaces;

import java.io.File;
import java.util.Map;

import org.jvnet.hk2.annotations.Contract;
import org.w3c.dom.Document;

@Contract
public interface IHandlingFileDOM {

	public Document getDocumentFile(File file);

	public File getFileFromStructure(Document document, String outFileNamePath);

	public boolean validateDocument(File file);

	public boolean insertNode(Document document, String nodeNameLookedUp, String nodeName,
			Map<String, String> attributesWithValues, String nodeContent, int depth);
	
	public int numberNodesContainsString(Document document, String nodeNameLookedUp, int depth);
	
	public Integer modifyContentNode(Document document, String nodeNameLookedUp, String oldContent, String newContent,
			int maxNumberNodesToModify, int depth);
	
	public boolean deleteNode(Document document, String nodeNameLookedUp, String nodeContent, int numberNodes, int depth);
	
	public Integer differenceFiles(File firstFile, File secondFile);
	
	public String printDocument(Document document, String coding, boolean isIndent);
	
	public String putAntetComment(String initialContent, String commentString);
	
}
