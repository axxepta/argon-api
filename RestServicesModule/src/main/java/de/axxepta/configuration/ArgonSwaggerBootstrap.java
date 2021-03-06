package de.axxepta.configuration;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

public class ArgonSwaggerBootstrap extends HttpServlet {

	private static final long serialVersionUID = 4736060928479268649L;

	private static final Logger LOG = LoggerFactory.getLogger(ArgonSwaggerBootstrap.class);

	private String packageName = "de.axxepta.sample";
	
	private String title = "Argon REST application";
	
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	
	public void setTitle(String title) {
		this.title = title;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		OpenAPI oas = new OpenAPI();
		Info info = new Info().title(title).description("Project of argon server")
				.contact(new Contact().email("apiteam@swagger.io"))
				.license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"));

		oas.info(info);
		SwaggerConfiguration oasConfig = new SwaggerConfiguration().openAPI(oas).prettyPrint(true)
				.resourcePackages(Stream.of(packageName).collect(Collectors.toSet()));

		LOG.info("Description " + oas.getInfo().getDescription() + " and title " + oas.getInfo().getTitle());
		LOG.info("Resource classes " + oasConfig.getResourcePackages());
		
		try {
			new JaxrsOpenApiContextBuilder().servletConfig(servletConfig).openApiConfiguration(oasConfig)
					.buildContext(true);
		} catch (OpenApiConfigurationException e) {
			throw new ServletException(e.getMessage(), e);
		}	
		
		LOG.info("swagger is initialized");
	}
}
