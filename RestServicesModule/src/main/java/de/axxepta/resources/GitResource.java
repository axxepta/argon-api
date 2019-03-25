package de.axxepta.resources;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ManagedAsync;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.axxepta.exceptions.ResponseException;
import de.axxepta.models.UserAuthModel;
import de.axxepta.services.interfaces.IDocumentGitService;
import de.axxepta.tools.ValidationString;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("git-services")
public class GitResource {

	private static final Logger LOG = Logger.getLogger(GitResource.class);

	@Inject
	private Meter metricRegistry;

	@Inject
	@Named("DocumentGitServiceImplementation")
	private IDocumentGitService documentGitService;

	private int serviceTimeout = 1000;

	@Operation(summary = "List branches names for git repository", description = "Provide a map of uploaded files", method = "GET", operationId = "#5_1")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "retrieve name of the repository head"),
			@ApiResponse(responseCode = "400", description = "string from url cannot be validate"),
			@ApiResponse(responseCode = "409", description = "null set of names or exception in a list map can indicate that repository is not accesible") })
	@GET
	@Path("branches-names")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRepositoryRemoteNames(
			@Parameter(description = "repository URL", required = true) @QueryParam("repository-url") String url,
			@Parameter(description = "username", required = false) @QueryParam("username") String username,
			@Parameter(description = "password", required = false) @QueryParam("password") String password)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(url, "repository-url")) {
			LOG.error("URL string is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL string is not a valid one");
		}

		UserAuthModel userAuth = new UserAuthModel(username, password);

		List<String> setNames = null;

		Supplier<List<String>> requestTask = () -> {

			return documentGitService.getRemoteBranchesNames(url, userAuth);

		};

		Future<List<String>> future = CompletableFuture.supplyAsync(requestTask);

		try {
			setNames = future.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		if (setNames == null) {
			LOG.error("for URL " + url + " is obtain null set of remote names ");
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
					"for URL " + url + " is obtain null set of remote names");
		}

		final StringWriter stringWriter = new StringWriter();
		final ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(stringWriter, setNames);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		String response = stringWriter.toString();

		LOG.info(response);
		return Response.status(Status.OK).entity(response).build();
	}

	@Operation(summary = "Date for last modify", description = "Provide date for last modify", method = "GET", operationId = "#5_2")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "retrieve name of the repository head"),
			@ApiResponse(responseCode = "400", description = "string from url cannot be validate"),
			@ApiResponse(responseCode = "409", description = "null epoch time or exception  can indicate that repository is not accesible") })
	@GET
	@Path("time-modification")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLastModifyTimeRespository(
			@Parameter(description = "repository URL", required = true) @QueryParam("repository-url") String url,
			@Parameter(description = "username", required = false) @QueryParam("username") String username,
			@Parameter(description = "password", required = false) @QueryParam("password") String password)
			throws ResponseException {
		metricRegistry.mark();

		UserAuthModel userAuth = new UserAuthModel(username, password);

		if (!ValidationString.validationString(url, "repository-url")) {
			LOG.error("URL string is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL string is not a valid one");
		}

		Supplier<Date> requestTask = () -> {

			return documentGitService.lastModify(url, userAuth);

		};

		Date dateLastModification;

		Future<Date> future = CompletableFuture.supplyAsync(requestTask);

		try {
			dateLastModification = future.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		if (dateLastModification == null) {
			LOG.error("for URL " + url + " is obtain null set of remote names ");
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
					"for URL " + url + " is obtain null set of remote names");
		}
		;

		LOG.info("Last modification time " + dateLastModification);
		return Response.status(Status.OK).entity("Last modification time " + dateLastModification).build();
	}

	@Operation(summary = "Get lists of directories and files", description = "Provide a map of uploaded files", method = "GET", operationId = "#5_3")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "retrieve name of the repository head"),
			@ApiResponse(responseCode = "400", description = "string from url cannot be validate"),
			@ApiResponse(responseCode = "409", description = "exception in a list map") })
	@GET
	@ManagedAsync
	@Path("get-dirs-files")
	@Produces(MediaType.APPLICATION_JSON)
	public void getFilesNames(
			@Parameter(description = "repository URL", required = true) @QueryParam("repository-url") String url,
			@Parameter(description = "username", required = false) @QueryParam("username") String username,
			@Parameter(description = "password", required = false) @QueryParam("password") String password,
			@Suspended final AsyncResponse serviceAsyncResponse) throws ResponseException {
		metricRegistry.mark();

		LOG.info("Get async files and directory names");
		serviceAsyncResponse.setTimeout(serviceTimeout, TimeUnit.SECONDS);

		serviceAsyncResponse.setTimeoutHandler(res -> res.resume(Response.status(Status.SERVICE_UNAVAILABLE)
				.entity("Get files and directory operation timeout").build()));

		if (!ValidationString.validationString(url, "repository-url")) {
			LOG.error("URL string is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL string is not a valid one");
		}

		UserAuthModel userAuth = new UserAuthModel(username, password);

		Supplier<Pair<List<String>, List<String>>> requestTask = () -> {
			return documentGitService.getFileNamesFromCommit(url, userAuth);
		};

		Future<Pair<List<String>, List<String>>> future = CompletableFuture.supplyAsync(requestTask);
		Pair<List<String>, List<String>> dirsFilesLists = null;
		try {
			dirsFilesLists = future.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		if (dirsFilesLists == null) {
			LOG.error("for URL " + url + " is obtain null set of remote names ");
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
					"for URL " + url + " is obtain null set of remote names");
		}

		List<String> dirList = dirsFilesLists.getLeft();
		List<String> filesList = dirsFilesLists.getRight();
		StringWriter stringWriter = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();

		try {
			mapper.writeValue(stringWriter, dirList);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		String dirsString = "Directories " + stringWriter.toString();

		stringWriter = new StringWriter();
		mapper = new ObjectMapper();

		try {
			mapper.writeValue(stringWriter, filesList);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		String filesString = "Files " + stringWriter.toString();

		String response = dirsString + " " + filesString;

		LOG.info(response);
		serviceAsyncResponse.resume(Response.status(Status.OK).entity(response).build());
	}

	@Operation(summary = "Get file", description = "Download file from repository", method = "GET", operationId = "#5_4")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "retrieve file with succes"),
			@ApiResponse(responseCode = "400", description = "string from url or for path with file name cannot be validate"),
			@ApiResponse(responseCode = "409", description = "file not exist - content will be null") })
	@GET
	@Path("/get-file")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFile(
			@Parameter(description = "repository URL", required = true) @QueryParam("repository-url") String url,
			@Parameter(description = "branch-name", required = false) @QueryParam("branch-name") String branchName,
			@Parameter(description = "path-filename", required = true) @QueryParam("path-filename") String pathFileName,
			@Parameter(description = "username", required = false) @QueryParam("username") String username,
			@Parameter(description = "password", required = false) @QueryParam("password") String password)
			throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(url, "repository-url")) {
			LOG.error("URL string is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(), "URL string is not a valid one");
		}

		if (!ValidationString.validationString(pathFileName, "path-filename")) {
			LOG.error("URL string is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"path with file name string is not a valid one");
		}

		UserAuthModel userAuth = new UserAuthModel(username, password);

		LOG.info("Get file " + pathFileName);

		byte[] fileContent = documentGitService.getDocumentFromRepository(url, branchName, pathFileName, userAuth);

		if (fileContent == null) {
			LOG.error("for URL " + url + " is obtain null set of remote names ");
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(),
					"for URL " + url + " is obtain null set of remote names");
		}

		String fileName = pathFileName.substring(pathFileName.lastIndexOf(File.separator) + 1, pathFileName.length());
		return Response.status(Status.OK).header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
				.entity(fileContent).build();
	}

	@Operation(summary = "Commit or push a file", description = "Commit or push a file in repository", method = "PUT", operationId = "#5_5")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "commit with succes"),
			@ApiResponse(responseCode = "400", description = "string from url cannot be validate"),
			@ApiResponse(responseCode = "409", description = "exception in execution"),
			@ApiResponse(responseCode = "417", description = "commit with eventually push did not respect a condition to be executed") })
	@PUT
	@ManagedAsync
	@Path("/commit-push-file")
	@Produces(MediaType.TEXT_PLAIN)
	public void commitFile(
			@Parameter(description = "repository URL", required = true) @QueryParam("repository-url") String url,
			@Parameter(description = "branch-name", required = false) @QueryParam("branch-name") String branchName,
			@Parameter(description = "path-filename", required = true) @QueryParam("path-filename") String pathFileName,
			@Parameter(description = "on-dir", required = false) @QueryParam("on-dir") String onDir,
			@Parameter(description = "commit-message", required = false) @QueryParam("commit-message") String commitMessage,
			@Parameter(description = "username", required = false) @QueryParam("username") String username,
			@Parameter(description = "password", required = false) @QueryParam("password") String password,
			@Suspended final AsyncResponse serviceAsyncResponse) throws ResponseException {
		metricRegistry.mark();

		if (!ValidationString.validationString(url, "repository-url")
				|| (url.toLowerCase().startsWith("https") && (!ValidationString.validationString(username, "username")
						|| !ValidationString.validationString(password, "password")))) {
			LOG.error("Some string  from request is not a valid one");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"Some string from request is not a valid one");
		}

		if (!ValidationString.validationString(pathFileName, "pathFileName") || !(new File(pathFileName).exists())) {
			LOG.error("Path with file name from request is not a valid one ");
			throw new ResponseException(Response.Status.BAD_REQUEST.getStatusCode(),
					"Path with file name from request is not a valid one");
		}

		UserAuthModel userAuth = new UserAuthModel(username, password);

		Supplier<Boolean> requestTask = () -> {
			return documentGitService.commitDocumentLocalToGit(url, branchName, new File(pathFileName), onDir,
					commitMessage, userAuth);
		};

		Future<Boolean> future = CompletableFuture.supplyAsync(requestTask);

		Boolean response;
		try {
			response = future.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage());
			throw new ResponseException(Response.Status.CONFLICT.getStatusCode(), e.getMessage());
		}

		if (response == null) {
			serviceAsyncResponse.resume(Response.status(Status.CONFLICT)
					.entity("there was an internal error in executing the commit action").build());
		} else if (response) {
			serviceAsyncResponse.resume(Response.status(Status.OK)
					.entity("Commit and eventually push command executed successfully").build());
		} else {
			serviceAsyncResponse.resume(Response.status(Status.EXPECTATION_FAILED)
					.entity("Commit and eventually push command failed").build());
		}
	}
}
