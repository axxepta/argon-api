package de.axxepta.resources;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.axxepta.dao.interfaces.IDocumentCacheDAO;
import de.axxepta.dao.interfaces.IDocumentDAO;
import de.axxepta.exceptions.ResponseException;
import de.axxepta.models.FileDescriptionModel;
import de.axxepta.models.FileDisplayModel;
import de.axxepta.services.interfaces.IFileResourceService;
import de.axxepta.tools.ExtractContentFile;
import de.axxepta.tools.GetContentOfURL;
import de.axxepta.tools.ValidationString;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("document-services")
public class DocumentsResource {

	private static final Logger LOG = Logger.getLogger(DocumentsResource.class);

	@Inject
	private Meter metricRegistry;

	@Inject
	@Named("FileServiceImplementation")
	private IFileResourceService fileService;

	@Inject
	@Named("DocumentMemoryCacheDAO")
	private IDocumentCacheDAO documentCacheDAO;
	
	@Context
	private HttpServletRequest servletRequest;

	@Operation(summary = "List uploaded files", description = "Provide a map of uploaded files", method = "GET", operationId = "#4_1")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "retrieve of json of uploaded files"),
			@ApiResponse(responseCode = "409", description = "uploaded directory stored as constant not exist") })
	@GET
	@Path("list-dir-uploaded-files")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listUploadedFiles() throws ResponseException {
		metricRegistry.mark();
		List<File> filesList = fileService.listUploadedFiles();
		if (filesList == null) {
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), "Upload directory not exist");
		}

		Map<String, FileDisplayModel> filesResponseMap = new HashMap<>();
		for (File file : filesList) {
			String key = FileUtils.basename(file.getName());
			key = key.substring(0, key.length() - 1);
			FileDisplayModel value = null;
			try {
				value = new FileDisplayModel(file);
				filesResponseMap.put(key, value);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}

		return Response.status(Status.OK).entity(filesResponseMap).build();
	}

	@Operation(summary = "set other name for used BaseX database", description = "Retrieve head for an repository", method = "PUT", operationId = "#4_2")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "change database"),
			@ApiResponse(responseCode = "406", description = "request is do it from address other than local address"),
			@ApiResponse(responseCode = "409", description = "not exist database with name wanted") })

	@PUT
	@Path("set-databaseName")
	public Response setDatabaseName(
			@Parameter(description = "name of the database", required = true) @QueryParam("database-name") String databaseName) {
		metricRegistry.mark();

		if (!servletRequest.getRemoteAddr().equals("127.0.0.1")) {
			LOG.error("The service has been attempted to be runned by " + servletRequest.getRemoteAddr());
			return Response.status(Status.NOT_ACCEPTABLE).entity("That service must be used only locally").build();
		}

		LOG.info("Trying to set new database with name " + databaseName);

		Boolean r = documentCacheDAO.setDatabaseName(databaseName);

		if (r == null) {
			return Response.status(Status.ACCEPTED).entity("Database with name " + databaseName + " is already setted")
					.build();
		}

		if (!r) {
			return Response.status(Status.CONFLICT).entity("Database with name " + databaseName + " not exists")
					.build();
		} else {
			LOG.info("Database name is changed in " + databaseName);
			return Response.status(Status.OK).entity("New database used have name " + databaseName).build();
		}
	}

	@Operation(summary = "Check if file exist", description = "Retrieve head for an repository", method = "GET", operationId = "#4_3")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "file exist"),
			@ApiResponse(responseCode = "202", description = "file not exist"),
			@ApiResponse(responseCode = "400", description = "name of file cannot be valided") })
	@GET
	@Path("exist-file")
	public Response existFile(
			@Parameter(description = "file name", required = true) @QueryParam("filename") String fileName,
			@Parameter(description = "is stored locally") @QueryParam("stored-as-temp") boolean storedAsTemp)
			throws ResponseException {
		LOG.info("test if file service");
		metricRegistry.mark();
		if (!ValidationString.validationString(fileName, "fileName")) {
			LOG.error("Name of file is null or empty");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"Name of file cannot be validated");
		}
		boolean fileExists = fileService.existFileStored(fileName);
		if (fileExists)
			return Response.status(Status.OK).entity("File  with name " + fileName + " exist").build();
		else
			return Response.status(Status.ACCEPTED).entity("File  with name " + fileName + " not exist").build();
	}

	@Operation(summary = "Upload file", description = "Upload file in directory and maybe also in database", method = "POST", operationId = "#4_4")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "upload file succes and return URL for temporal file and his type in JSON"),
			@ApiResponse(responseCode = "400", description = "at least one parameter in the request is missing"),
			@ApiResponse(responseCode = "409", description = "temporal file cannot be created") })
	@POST
	@Path("upload-file")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadFile(
			@Parameter(description = "URL of the file", required = true) @FormParam("file-url") String fileURLString,
			@Parameter(description = "is stored in the database") @FormParam("is-added-to-database") boolean isAddedToDatabase,
			@Parameter(description = "is stores as a temp file") @FormParam("is-temp-file") boolean isTmpFile)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(fileURLString, "file url")) {
			LOG.error("At least one of the parameters is missing");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"At least one of the parameters is missing");
		}

		URL fileURL = null;
		try {
			fileURL = new URL(fileURLString);
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"String does not determine a valid URL");
		}

		FileDescriptionModel fileDescription = fileService.uploadLocalFile(fileURL, isTmpFile);

		if (fileDescription == null)
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
					"An error occour in uploaded file in directory");

		if (isAddedToDatabase) {

			String content;
			try {
				content = GetContentOfURL.getContent.getContent(new URL(fileURLString));
			} catch (MalformedURLException e) {
				LOG.error(fileURLString + " malformed URL");
				throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), "Malformed URL");
			}

			String fileName = fileURLString.substring(fileURLString.lastIndexOf("/") + 1);

			if (content == null) {
				LOG.error("Exception during getting content of page");
				throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
						"Exception during getting content of page");
			}

			boolean result = documentCacheDAO.save(fileName, content);

			if (result) {
				fileDescription.setAddedToDatabase(true);
			}
		} else {
			LOG.info("File from URL " + fileURLString + " will not be added to the database");
		}

		return Response.ok(fileDescription).build();
	}

	@Operation(summary = "Delete a file", description = "Delete an file from upload directory and from database", method = "DELETE", operationId = "#4_5")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "the file was successfully deleted"),
			@ApiResponse(responseCode = "202", description = "file not exist"),
			@ApiResponse(responseCode = "409", description = "name of file cannot be validated") })
	@DELETE
	@Path("delete-file")
	@Produces(MediaType.TEXT_PLAIN)
	public Response deleteFile(
			@Parameter(description = "file name", required = true) @QueryParam("filename") String fileName,
			@Parameter(description = "is stored in the database") @QueryParam("is-from-database") @DefaultValue("false") boolean isFromDatabase)
			throws ResponseException {
		LOG.info("delete file service");
		metricRegistry.mark();

		if (!ValidationString.validationString(fileName, "fileName")) {
			LOG.error("Not valid file name");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "Not valid file name");
		}

		String response = "";
		boolean hasDeleted = false;

		if (!isFromDatabase) {
			LOG.info(fileName + " will not be deleted from database");
			hasDeleted = fileService.deleteFile(fileName);

			if (hasDeleted) {
				response = "File  with name " + fileName + " was deleted from upload directory";
			} else {
				response = "File with name " + fileName + " wasn't deleted from upload directory";
			}
		} else {
			LOG.info(fileName + " will be deleted from database");
			response = "file will be deteleted from database";
		}

		if (hasDeleted)
			return Response.status(Status.OK).entity(response).build();
		else
			return Response.status(Status.ACCEPTED).entity(response).build();
	}

	@Operation(summary = "Retrieve file", description = "Retrieve file as an binary one from upload directory or from database", method = "GET", operationId = "#4_6")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "return file content"),
			@ApiResponse(responseCode = "409", description = "name of file cannot be validated") })
	@GET
	@Path("retrieve-file")
	public Response retrieveFile(
			@Parameter(description = "file name", required = true) @QueryParam("filename") String fileName,
			@Parameter(description = "is from database") @QueryParam("is-from-database") boolean isFromDatabase)
			throws ResponseException {
		LOG.info("retrieve temp file service");
		metricRegistry.mark();

		if (!ValidationString.validationString(fileName, "fileName")) {
			LOG.error("File name is not valid");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "File name is not valid");
		}

		long startCall = System.nanoTime();
		String content = documentCacheDAO.getContentFile(fileName);
		long endCall = System.nanoTime();

		LOG.info("Time(in nanoseconds) for retrieving file " + (endCall - startCall) + " with bytes buffer size ");

		LOG.info("Value content " + content);
		
		if (content == null) {		
			return Response.ok("File " + fileName + " not found").build();
		}
		
		byte[] bytesFile = content.getBytes();

		return Response.ok(bytesFile, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = " + fileName).build();

	}
	
	@Operation(summary = "Retrieve list of files from one of database", description = "Retrieve list of files stored in setted BaseX database", method = "GET", operationId = "#4_7")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "return list of file names"),
		@ApiResponse(responseCode = "409", description = "list of file names cannot be obtain") })
	@GET
	@Path("list-file-names-from-database")
	public Response getListNameFileDatabase(@Parameter(description = "is from cache database") @QueryParam("is-from-cache") boolean isFromCacheDatabase) throws ResponseException {
		metricRegistry.mark();
		
		List<String> fileNameList = documentCacheDAO.getListFileName(isFromCacheDatabase);
		
		if(fileNameList == null) {
			LOG.error("list of files cannot be obtain from database");
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), "list of files cannot be obtain from database");
		}
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonFileNames = null;
		try {
			jsonFileNames = mapper.writeValueAsString(fileNameList);
		} catch (JsonProcessingException e) {
			
		}
		
		return Response.status(Status.OK).entity(jsonFileNames).build();
	}
	
}
