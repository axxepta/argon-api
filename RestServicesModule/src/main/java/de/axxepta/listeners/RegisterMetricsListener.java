package de.axxepta.listeners;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;

import de.axxepta.health.DatabaseHealth;

public class RegisterMetricsListener implements ServletContextListener {

	private static final Logger LOG = Logger.getLogger(RegisterMetricsListener.class);

	public static final MetricRegistry metric = new MetricRegistry();
	
	public final HealthCheckRegistry health = new HealthCheckRegistry();
	
	protected String nameApplication = "rest services";
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		LOG.info("Start register metrics listener for " + nameApplication);
		
		event.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, metric);
		event.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, health);	
		
		health.register("basex", new DatabaseHealth());
		final Map<String, HealthCheck.Result> results = health.runHealthChecks();
		for (Entry<String, HealthCheck.Result> entry : results.entrySet()) {
		    if (entry.getValue().isHealthy()) {
		        LOG.info(entry.getKey() + " is healthy");
		    } else {
		        LOG.error(entry.getKey() + " is unhealthy: " + entry.getValue().getMessage());
		    }
		}
		
		startReport();
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		LOG.info("Application " + nameApplication + " shutdown");
	}

	private void startReport() {
		final ConsoleReporter reporterConsole = ConsoleReporter.forRegistry(metric).
				convertRatesTo(TimeUnit.SECONDS).filter(MetricFilter.ALL)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();

		
		reporterConsole.start(30, TimeUnit.SECONDS);
		
		final JmxReporter reporter = JmxReporter.forRegistry(metric).build();
		reporter.start();
		
		LOG.info("Start metric collector");
	}
}
