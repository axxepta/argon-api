package de.axxepta.services.implementations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.castor.xmlctf.xmldiff.XMLDiff;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.common.base.Predicate;
import com.thaiopensource.xml.sax.Sax2XMLReaderCreator;

import de.axxepta.services.interfaces.IHandlingFileDOM;
import de.axxepta.tools.DomFromStringContentParallel;
import de.axxepta.tools.ValidationDocs;

@Service(name = "HandlingFileDOM")
@Singleton
public class HandlingFileDOM implements IHandlingFileDOM {

	private static final Logger LOG = LoggerFactory.getLogger(HandlingFileDOM.class);

	private DomFromStringContentParallel domFromStringParallel;

	@PostConstruct
	private void initHandlingFileDOM() {
		domFromStringParallel = new DomFromStringContentParallel(3);
	}

	@Override
	public Document getDocumentFile(File file) {
		try {
			return domFromStringParallel.getDom(file);
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getClass().getName() + " " + e.getMessage());
			return null;
		}

	}

	@Override
	public File getFileFromStructure(Document document, String outFileNamePath) {
		File outFile = new File(outFileNamePath);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();

		try {
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);
			FileWriter writer = new FileWriter(new File(outFileNamePath));
			StreamResult result = new StreamResult(writer);
			transformer.transform(domSource, result);
		} catch (TransformerException | IOException e) {
			LOG.error(e.getClass().getName() + " " + e.getMessage());
		}
		return outFile;
	}

	@Override
	public boolean validateDocument(File file) {
		return ValidationDocs.validateXMLWithDOM.isDocTypeValid(file);
	}

	@Override
	public boolean insertNode(Document document, String nodeNameLookedUp, String nodeName,
			Map<String, String> attributesWithValues, String nodeContent, int depth) {

		if (depth < 0) {
			LOG.error("negative depth");
			return false;
		}

		Predicate<Node> predicate = (Node node) -> node.getNodeName().equalsIgnoreCase(nodeNameLookedUp);
		VisitNodes visit = new VisitNodes();

		Node node = document.getDocumentElement();

		Node nodeFounded = visit.visitNodeReturnFirstly(node, 0, depth, predicate);

		if (nodeFounded == null) {
			LOG.info("Not founded node");
			return false;
		}

		Node insertNode = document.createElement(nodeName);
		for (Entry<String, String> attribValue : attributesWithValues.entrySet()) {
			((Element) insertNode).setAttribute(attribValue.getKey(), attribValue.getValue());
		}

		nodeFounded.appendChild(insertNode);

		document.normalize();

		return true;
	}

	@Override
	public int numberNodesContainsString(Document document, String nodeNameLookedUp, int depth) {

		if (depth < 0) {
			LOG.error("negative depth");
			return 0;
		}

		Predicate<Node> predicate = (Node node) -> node.getNodeName().equalsIgnoreCase(nodeNameLookedUp);
		VisitNodes visit = new VisitNodes();

		Node node = document.getDocumentElement();
		visit.visitNodeCount(node, 0, depth, predicate);
		return visit.count;
	}

	@Override
	public Integer modifyContentNode(Document document, String nodeNameLookedUp, String oldContent, String newContent,
			int maxNumberNodesToModify, int depth) {

		if (maxNumberNodesToModify < 0) {
			LOG.error("negative depth");
			return null;
		}

		Node rootNode = document.getDocumentElement();
		NodeList nodeList = rootNode.getChildNodes();
		int actualDepth = 0;
		List<NodeList> nodeListList = new ArrayList<>();
		nodeListList.add(nodeList);
		int numberNodesModificated = 0;

		Predicate<Node> predicate = (Node node) -> node.getNodeName().equalsIgnoreCase(nodeNameLookedUp);

		while (actualDepth <= depth) {
			for (int counterList = 0; counterList < nodeListList.size(); counterList++) {
				NodeList nodeListElem = nodeListList.get(counterList);
				for (int counterNodeList = 0; counterNodeList < nodeListElem.getLength(); counterNodeList++) {
					Node node = nodeListElem.item(counterNodeList);

					if (node != null && predicate.apply(node) && numberNodesModificated < maxNumberNodesToModify) {
						if (oldContent == null) {
							node.setTextContent(newContent);
						} else {
							if (node.getTextContent().equals(oldContent)) {
								node.setTextContent(newContent);
							}
						}
						numberNodesModificated++;

					}
				}

			}

			nodeListList = new ArrayList<>();

			for (int counterList = 0; counterList < nodeListList.size(); counterList++) {
				NodeList nodeListElem = nodeListList.get(counterList);
				for (int counterNodeList = 0; counterNodeList < nodeListElem.getLength(); counterNodeList++) {
					Node node = nodeListElem.item(counterNodeList);

					if (node != null && node.getNodeName() != null) {
						nodeListList.add(node.getChildNodes());
					}
				}
			}

			actualDepth++;
		}

		return numberNodesModificated;
	}

	@Override
	public boolean deleteNode(Document document, String nodeNameLookedUp, String nodeContent, int numberNodes,
			int depth) {
		if (depth < 0) {
			LOG.error("negative depth");
			return false;
		}

		VisitNodes visit = new VisitNodes();

		Node root = document.getDocumentElement();

		Predicate<Node> predicate = (Node node) -> node.getNodeName().equalsIgnoreCase(nodeNameLookedUp);
		Node nodeFounded = visit.visitNodeReturnFirstly(root, 0, depth, predicate);

		if (nodeFounded == null) {
			LOG.info("Is not founded some node with name " + nodeNameLookedUp + " in depth  " + depth);
			return false;
		}

		try {
			if (nodeContent == null) {
				nodeFounded.getParentNode().removeChild(nodeFounded);
			} else {
				if (nodeFounded.getTextContent().equals(nodeContent)) {
					nodeFounded.getParentNode().removeChild(nodeFounded);
				}
			}
		} catch (DOMException e) {
			LOG.error("DOMEXception " + e.getMessage());
			return false;
		}

		document.normalize();

		return true;
	}

	@Override
	public Integer differenceFiles(File firstFile, File secondFile) {

		XMLDiff diff;

		if (firstFile == null || !firstFile.exists()) {
			LOG.error("First file not exists");
			return null;
		}

		if (secondFile == null || !secondFile.exists()) {
			LOG.error("Second file not exists");
			return null;
		}

		LOG.info("First file URL " + firstFile.getAbsolutePath());
		LOG.info("Second file URL " + secondFile.getAbsolutePath());
		diff = new XMLDiff(firstFile.getAbsolutePath(), secondFile.getAbsolutePath());

		try {
			return diff.compare();
		} catch (IOException e) {
			LOG.error("IOException in compare " + e.getMessage());
			return null;
		}
	}

	@Override
	public String printDocument(Document document, String coding, boolean isIndent) {
		document.normalize();
		Transformer tf;
		try {
			tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.ENCODING, coding);
			if (isIndent)
				tf.setOutputProperty(OutputKeys.INDENT, "yes");
			Writer out = new StringWriter();
			tf.transform(new DOMSource(document), new StreamResult(out));
			LOG.info("apply pretty print");
			return out.toString();
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			LOG.error(e.getClass() + " " + e.getMessage());
			return null;
		}
	}

	@Override
	public String putAntetComment(String initialContent, String commentString) {
		return "<!-- \n" + commentString + "\n -->\n" + initialContent;
	}

	private XMLReader createSAXReader() throws SAXException {
		Sax2XMLReaderCreator readerCreator = new Sax2XMLReaderCreator();
		XMLReader reader = readerCreator.createXMLReader();

		LOG.info("Create XML Reader");

		return reader;
	}

	@PreDestroy
	private void shutdownService() {
		domFromStringParallel.shutdownProcessing();
	}

	class VisitNodes {

		public int count;

		public VisitNodes() {
			count = 0;
		}

		public void visitNodeCount(Node node, int level, final int levelMax, Predicate<Node> predicate) {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node childNode = list.item(i);

				if (level < levelMax) {
					visitNodeCount(childNode, level + 1, levelMax, predicate);
					if (predicate.apply(childNode))
						count++;
				} else
					return;
			}
		}

		public Node visitNodeReturnFirstly(Node node, int level, final int levelMax, Predicate<Node> predicate) {
			Node foundedNode = null;
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				Node childNode = list.item(i);

				if (level < levelMax) {

					if (predicate.apply(childNode)) {
						foundedNode = childNode;
						break;
					} else
						visitNodeCount(childNode, level + 1, levelMax, predicate);
				} else
					return null;
			}
			return foundedNode;
		}
	}

}
