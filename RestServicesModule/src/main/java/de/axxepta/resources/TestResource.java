package de.axxepta.resources;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.annotation.Metric;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path("testing")
@Singleton
public class TestResource {

	private static final Logger LOG = LoggerFactory.getLogger(TestResource.class);

	private Meter metricRegistry;

	private HttpServletRequest request;

	@Inject
	public void setMetricRegistry(Meter metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	@Context
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	@Operation(summary = "Doing simple test", description = "Doing a test by returning a simple message", method = "GET", operationId = "#1_1")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "message test") })
	@GET
	@Path("test")
	@Produces(MediaType.TEXT_PLAIN)
	public Response test() {

		metricRegistry.mark();

		String token = request.getSession().getId();
		LOG.info("Session id " + token);

		LOG.info("Do a simple test on argon server");
		return Response.ok("Do a simple test on argon server").build();
	}

	@Operation(summary = "Doing simple test and adding timestamp", description = "Doing a test by returning a simple message with an timestamp", method = "GET", operationId = "#1_2")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "message test with date") })
	@GET
	@Path("test-date")
	@Produces(MediaType.TEXT_PLAIN)
	@Metric
	public Response date() {
		metricRegistry.mark();

		LocalDateTime dateTime = LocalDateTime.now();
		LOG.info("Do a simple test on argon server on " + dateTime);
		return Response.ok("Do a simple test on argon server on " + dateTime).build();
	}

}
