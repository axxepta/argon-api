package de.axxepta.tools;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class DomFromStringContentParallel {

	private static final Logger LOG = LoggerFactory.getLogger(DomFromStringContentParallel.class);
	
	private ExecutorService executor;
	
	public DomFromStringContentParallel(int x) {
		executor = Executors.newWorkStealingPool(x);
	}
	
	public Document getDom(File file) throws InterruptedException, ExecutionException {
		 Callable<Document> callable = new Callable<>() {
		        @Override
		        public Document call() {
		        	String content;
					try {
						content = FileUtils.readFileToString(file);
					} catch (IOException e) {
						LOG.error(e.getMessage());
						return null;
					}
		            return DomFromStringContent.domFromString.getDOM(content);
		        }
		    };
		    
		    Future<Document> future = executor.submit(callable);
		    
		    return future.get();
	}
	
	public void shutdownProcessing() {
		executor.shutdown();
	}
	
}
