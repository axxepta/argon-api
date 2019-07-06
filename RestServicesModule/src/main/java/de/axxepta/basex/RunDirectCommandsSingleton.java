package de.axxepta.basex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RunDirectCommandsSingleton
{
    INSTANCE;

    private final RunDirectCommands commands;
    
    private final Logger logger = LoggerFactory.getLogger(RunDirectCommandsSingleton.class);
    
    private RunDirectCommandsSingleton(){
        commands = new RunDirectCommands();
        logger.info("Direct comand instance was created");
    }

    public RunDirectCommands getRunCommands(){
        return commands;
    }
    
}
