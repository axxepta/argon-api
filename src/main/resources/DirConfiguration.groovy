package de.axxepta.configure.script

import org.codehaus.groovy.runtime.InvokerHelper

import org.slf4j.Logger;
import org.slf4j.LoggerFactory

class DirConfiguration extends Script {
	
	private final static Logger LOG = LoggerFactory.getLogger(this.class)
	
	private final static FileTreeBuilder TREE_BUILDER = new FileTreeBuilder(new File("."))
	
	def directoriesRoot() {
		def resources = ["data", "logs", "uploads"] as String[]
		for (directoryName in resources) {
			 def directory =  new File(directoryName)
			 if(!directory.exists()){
				 LOG.info(directoryName + " will be created")
				 TREE_BUILDER.dir(directoryName + " will be created")
			 }
			 else{
				 LOG.info(directoryName + " already exists")
			 }
		}
	}
	
	def directoryDatabase() {
		def xodusFolderRestModule = new File('RestServicesModule' + File.separator + 'xodus-db')
		def folderOxygenModule = new File('OxygenWebAuthorModule')
		
		if(!xodusFolderRestModule.exists()){
			LOG.info('xodus-db folder will be created in RestServicesModule')
			TREE_BUILDER.dir('RestServicesModule' + File.separator + 'xodus-db')
		}
		else{
			LOG.info('xodus-db folder exists in RestServicesModule')
		}
		
		if(!folderOxygenModule.exists()){
			LOG.info('Oxygen module is decoupled')
		}
		else{
			def xodusFolderOxygenModule = new File('OxygenWebAuthorModule' + File.separator + 'shiro-res')
			if(!xodusFolderOxygenModule.exists()){
				LOG.info('shiro-res folder will be created in OxygenWebAuthorModule')
				TREE_BUILDER.dir('OxygenWebAuthorModule' + File.separator + 'shiro-res')
			}
			else{
				LOG.info('xodus-res folder exists in OxygenWebAuthorModule')
			}
		}
	}
	
	def targetsDetails() {
		def targetFolder = new File('target')
		def targetFolderRest = new File('RestServicesModule' + File.separator + 'target')
		def targetFolderOxygen = new File('OxygenWebAuthorModule' + File.separator + 'target')
		if(targetFolder.exists()){
			LOG.info('target is created on : ' + (new
				Date(targetFolder.lastModified())))
		}
		else{
			LOG.info('target directory not exist yet')
		}
		
		if(targetFolderRest.exists()){
			LOG.info('target from Rest Service Module is created on : ' + (new Date(targetFolder.lastModified())))
		}
		else{
			LOG.info('target from Rest Service Module directory not exist yet')
		}
		
		if(targetFolderOxygen.exists()){
			LOG.info('target from Oxygen Service Module is created on : ' + (new
					Date(targetFolder.lastModified())))
		}
		else{
			LOG.info('target from Oxygen Service Module directory not exist yet')
		}
	}
	
	def run() {
		
		LOG.info('initial project artifact id: ' + project.artifactId)
		
		directoriesRoot()
		
		directoryDatabase()
		
		targetsDetails()
	}
	
	static void main(String[] args) {
		InvokerHelper.runScript(DirConfiguration, args)
	}
}