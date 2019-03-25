package de.axxepta.services.interfaces;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jvnet.hk2.annotations.Contract;

import de.axxepta.models.UserAuthModel;

@Contract
public interface IDocumentGitService {

	public List<String> getRemoteBranchesNames(String gitURL, UserAuthModel userAuth);
	
	public Date lastModify(String gitURL, UserAuthModel userAuth);

	public Pair<List<String>, List<String>> getFileNamesFromCommit(String GitURL, UserAuthModel userAuth);

	public byte[] getDocumentFromRepository(String gitURL, String branchName, String pathFileName, UserAuthModel userAuth);

	public Boolean commitDocumentLocalToGit(String gitURL, String branchName, File localFile, String mountedOnDir, String commitMessage,
			UserAuthModel userAuth);

}
