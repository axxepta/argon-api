package de.axxepta.resources;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;

import com.codahale.metrics.Meter;

import de.axxepta.exceptions.ResponseException;
import de.axxepta.services.interfaces.IFileResourceService;
import de.axxepta.services.interfaces.IHandlingFileDOM;
import de.axxepta.tools.ValidateURL;
import de.axxepta.tools.ValidationString;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("document-xml-services")
public class DocumentsXMLResource {

	private static final Logger LOG = Logger.getLogger(DocumentsXMLResource.class);

	@Inject
	private Meter metricRegistry;

	@Inject
	@Named("FileServiceImplementation")
	private IFileResourceService fileResurceService;

	@Inject
	@Named("HandlingFileDOM")
	private IHandlingFileDOM handlingFileDOM;

	@Operation(summary = "Validate", description = "Validate XML file", method = "GET", operationId = "#6_1")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "validate or inalidate with success"),
			@ApiResponse(responseCode = "400", description = "URL is incorrect") })
	@GET
	@Path("is-file-valid")
	@Produces(MediaType.TEXT_PLAIN)
	public Response fileIsValid(
			@Parameter(description = "String for file URL", required = true) @QueryParam("file-url") String fileURL)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(fileURL, "fileURL")) {
			LOG.error("URL string of file is null, empty or isn't valid");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"URL string of file cannot be validated");
		}

		File tmpFile = fileResurceService.createTempFileFromURL(fileURL);

		if (tmpFile == null) {
			return Response.status(Status.CONFLICT).entity("Temp file cannot be created").build();
		}

		String response = null;
		if (handlingFileDOM.validateDocument(tmpFile)) {
			response = "File from " + fileURL + " is valid";
		} else {
			response = "File from " + fileURL + " isn't valid";
		}

		LOG.info(response);
		return Response.status(Status.OK).entity(response).build();
	}

	@Operation(summary = "Number of nodes", description = "Determine number of nodes that have as a specifically value", method = "GET", operationId = "#6_2")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "validate or inalidate with success"),
			@ApiResponse(responseCode = "400", description = "URL string is incorrect"),
			@ApiResponse(responseCode = "409", description = "document is not valid or temp file cannod be created") })
	@GET
	@Path("number-nodes-that-contains")
	@Produces(MediaType.TEXT_PLAIN)
	public Response numberNodes(
			@Parameter(description = "String for file URL", required = true) @QueryParam("file-url") String fileURL,
			@Parameter(description = "string searched", required = true) @QueryParam("string-search") String string,
			@Parameter(description = "depth search") @QueryParam("depth") int depth) throws ResponseException {
		metricRegistry.mark();
		if (!ValidationString.validationString(fileURL, "firstFileURL")) {
			LOG.error("URL of file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"URL string of file cannot be validated");
		}

		if (!ValidationString.validationString(string, "search string")) {
			LOG.error("URL of file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL of file cannot be validated");
		}

		if (depth == 0) {
			depth = 1;
			LOG.info("Using a default value of depth, witch is it 1");
		}

		File tmpFile = fileResurceService.createTempFileFromURL(fileURL);

		if (tmpFile == null) {
			return Response.status(Status.CONFLICT).entity("Temp file cannot be created").build();
		}

		Document doc = handlingFileDOM.getDocumentFile(tmpFile);

		if (doc == null) {
			return Response.status(Status.CONFLICT).entity("Document is not an XML valid one").build();
		}

		int numberNodes = handlingFileDOM.numberNodesContainsString(doc, string, depth);
		return Response.status(Status.OK).entity("number of nodes that have " + string + " as values is " + numberNodes)
				.build();
	}

	@Operation(summary = "Difference from two files", description = "Determine if the files is same or exist some difference between", method = "GET", operationId = "#6_3")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "validate or inalidate with success"),
			@ApiResponse(responseCode = "400", description = "one of two URLs is incorrect"),
			@ApiResponse(responseCode = "409", description = "one of two documents is not valid or less oen temp file cannod be created") })
	@GET
	@Path("difference-files")
	@Produces(MediaType.TEXT_PLAIN)
	public Response differenceXMLFiles(
			@Parameter(description = "String for first file URL", required = true) @QueryParam("first-file-url") String firstFileURL,
			@Parameter(description = "String for second file URL", required = true) @QueryParam("second-file-url") String secondFileURL)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(firstFileURL, "firstFileURL")
				|| !ValidateURL.validateURL.isURLValid(firstFileURL)) {
			LOG.error("URL of first file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"URL of first file cannot be validated");
		}

		if (!ValidationString.validationString(secondFileURL, "secondFileURL")
				|| !ValidateURL.validateURL.isURLValid(secondFileURL)) {
			LOG.error("URL of first file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"URL of first file cannot be validated");
		}

		File firstTmpFile = fileResurceService.createTempFileFromURL(firstFileURL);
		File secondTmpFile = fileResurceService.createTempFileFromURL(secondFileURL);

		Integer response = handlingFileDOM.differenceFiles(firstTmpFile, secondTmpFile);

		if (response == null) {
			return Response.status(Status.CONFLICT).entity("At least one of two files is not valid one").build();
		} else {
			if (response == 0) {
				return Response.status(Status.OK).entity("Both files have same content").build();
			} else {
				return Response.status(Status.OK).entity("Between files exist a difference computed as " + response)
						.build();
			}
		}
	}

	@Operation(summary = "Modify nodes", description = "Modify a number of nodes from XML file for a certain depth", method = "DELETE", operationId = "#6_3")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "modify with succes from validated file or nodes with searched content not exist in file"),
			@ApiResponse(responseCode = "400", description = "one query parameter is not valid"),
			@ApiResponse(responseCode = "409", description = "exception in create a new file with modification of nodes") })
	@PUT
	@Path("modify-node-file")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_PLAIN)
	public Response modifyUploadedFile(
			@Parameter(description = "local file name URL", required = true) @FormParam("file-url") String fileURL,
			@Parameter(description = "node name", required = true) @FormParam("node-name") String nodeName,
			@Parameter(description = "old node content") @FormParam("new-node-content") String oldNodeContent,
			@Parameter(description = "new node content", required = true) @FormParam("new-node-content") String newNodeContent,
			@Parameter(description = "depth search") @QueryParam("depth") int depth,
			@Parameter(description = "number of nodes to modify") @FormParam("number-nodes") int numberNodes,
			@Parameter(description = "name of new file") @FormParam("name-new-file") String newNameFile)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(fileURL, "fileURL") || !ValidateURL.validateURL.isURLValid(fileURL)) {
			LOG.error("URL of file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL string cannot be validate");
		}

		if (!ValidationString.validationString(newNodeContent, "newNodeContent")) {
			LOG.error("New node content is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"Node content cannot be validated");
		}

		if (!ValidationString.validationString(nodeName, "nodeName")) {
			LOG.error("Node name is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "Node name cannot be validated");
		}

		if (depth == 0) {
			depth = 1;
			LOG.info("Using a default value of depth, witch is it 1");
		}

		if (numberNodes == 0) {
			numberNodes = 1;
			LOG.info("Using a default value of number of nodes that will have content changed, witch is it 1");
		}

		LOG.info("Change for " + fileURL + " first node with name " + nodeName + " with content " + newNodeContent);

		File tmpFile = fileResurceService.createTempFileFromURL(fileURL);

		if (tmpFile == null) {
			return Response.status(Status.CONFLICT).entity("Temp file cannot be created").build();
		}

		Document doc = handlingFileDOM.getDocumentFile(tmpFile);

		if (doc == null) {
			return Response.status(Status.CONFLICT).entity("Document is not an XML valid one").build();
		}

		Integer numberModifiedNodes = handlingFileDOM.modifyContentNode(doc, nodeName, oldNodeContent, newNodeContent,
				numberNodes, depth);

		if (numberModifiedNodes == null) {
			return Response.status(Status.CONFLICT)
					.entity("An conflict caused by a precondition or exception in modification of node contents")
					.build(); 
		} else {
			LOG.info("Number of modificated node is " + numberModifiedNodes);

			String fileName = fileURL.toString().substring(fileURL.toString().lastIndexOf("/"));
			String content = handlingFileDOM.printDocument(doc, "UTF-8", true);
			content = handlingFileDOM.putAntetComment(content,
					"Number of modificated node is from " + fileURL + " is " + numberModifiedNodes);
			String fileNamePath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator
					+ FilenameUtils.removeExtension(fileName) + "-modifyNodes." + FileUtils.extension(fileName);
			LOG.info("File modified was save in " + fileNamePath);
			File fileLocal = new File(fileNamePath);
			try {
				fileLocal.createNewFile();
				FileUtils.fileWrite(fileLocal, content);
			} catch (IOException e) {
				throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
			}

			return Response.status(Status.OK).entity("Number of modificated node is " + numberModifiedNodes + " and is "
					+ (numberModifiedNodes == numberNodes) + " that is equal to value from request , obtained the file with content : \n " + content).build();
		}
	}

	@Operation(summary = "Delete nodes", description = "Delete a number of nodes from XML file", method = "DELETE", operationId = "#6_3")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "delete with succes from validated file or nodes with searched content not exist in file"),
			@ApiResponse(responseCode = "400", description = "one query parameter is not valid"),
			@ApiResponse(responseCode = "409", description = "exception in create a new file with deleted nodes") })
	@DELETE
	@Path("delete-node")
	@Produces(MediaType.TEXT_PLAIN)
	public Response deleteNode(
			@Parameter(description = "file URL", required = true) @QueryParam("file-url") String fileURL,
			@Parameter(description = "node name", required = true) @QueryParam("node-name") String nodeName,
			@Parameter(description = "node content text") @QueryParam("node-content") String nodeContent,
			@Parameter(description = "depth search") @QueryParam("depth") int depth,
			@Parameter(description = "number of nodes") @QueryParam("number-nodes") int numberNodes,
			@Parameter(description = "save as a locally file") @QueryParam("saved-locally") boolean isSavedLocally)
			throws ResponseException {

		metricRegistry.mark();
		if (!ValidationString.validationString(fileURL, "firstFileURL")) {
			LOG.error("URL of file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL of file cannot be validated");
		}

		if (!ValidationString.validationString(nodeName, "search string")) {
			LOG.error("Node name is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "Node name is null or empty");
		}

		if (depth == 0) {
			depth = 1;
			LOG.info("Using a default value of depth, witch is it 1");
		}

		File tmpFile = fileResurceService.createTempFileFromURL(fileURL);

		if (tmpFile == null) {
			return Response.status(Status.CONFLICT).entity("Temp file cannot be created").build();
		}

		Document doc = handlingFileDOM.getDocumentFile(tmpFile);

		boolean result = handlingFileDOM.deleteNode(doc, nodeName, nodeContent, numberNodes, depth);

		String response;
		if (result) {
			response = "Delete node  " + nodeName;
			if (nodeContent != null) {
				response += " with content " + nodeContent;
			}
			response += " found in " + fileURL + " in depth " + depth;
			if (isSavedLocally) {

				String fileName = fileURL.toString().substring(fileURL.toString().lastIndexOf("/"));
				String content = handlingFileDOM.printDocument(doc, "UTF-8", true);
				content = handlingFileDOM.putAntetComment(content, response);
				String pathSaveFile = System.getProperty("user.dir") + File.separator + "uploads" + File.separator
						+ FilenameUtils.removeExtension(fileName) + "-deleteNodes." + FileUtils.extension(fileName);
				LOG.info("Modificated file will be saved locally in " + pathSaveFile);
				File fileLocal = new File(pathSaveFile);
				response += " obtained the file with content:  \n" + content;
				try {
					fileLocal.createNewFile();
					FileUtils.fileWrite(fileLocal, content);
				} catch (IOException e) {
					throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
				}
			}
		} else {
			response = "Node not found in " + fileURL + " in depth " + depth;
		}

		LOG.info(response);

		return Response.status(Status.OK).entity(response).build();
	}
}
