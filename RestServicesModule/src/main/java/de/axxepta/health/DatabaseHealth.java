package de.axxepta.health;

import org.basex.core.BaseXException;

import com.codahale.metrics.health.HealthCheck;

import de.axxepta.basex.RunDirectCommands;

public class DatabaseHealth extends HealthCheck{

	private RunDirectCommands runDirectCommands = new RunDirectCommands();
	
	@Override
	protected Result check() throws Exception {
		try {
			runDirectCommands.showExistingDatabases();
			return HealthCheck.Result.healthy();
		}
		catch(BaseXException e){
			return HealthCheck.Result.unhealthy("Cannot obtain databases list from BaseX server " + e.getMessage());
		}
	}

}
