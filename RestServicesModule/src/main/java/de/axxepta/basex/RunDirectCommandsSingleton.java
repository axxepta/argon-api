package de.axxepta.basex;

import org.apache.log4j.Logger;

public enum RunDirectCommandsSingleton
{
    INSTANCE;

    private final RunDirectCommands commands;
    
    private final Logger logger = Logger.getLogger(RunDirectCommandsSingleton.class);
    
    private RunDirectCommandsSingleton(){
        commands = new RunDirectCommands();
        logger.info("Direct comand instance was created");
    }

    public RunDirectCommands getRunCommands(){
        return commands;
    }
    
}
