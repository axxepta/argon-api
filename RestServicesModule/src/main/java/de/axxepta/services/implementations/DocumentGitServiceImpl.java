package de.axxepta.services.implementations;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Service;

import de.axxepta.models.UserAuthModel;
import de.axxepta.services.interfaces.IDocumentGitService;
import gitdb.integration.DataItem;
import gitdb.integration.GitDB;

@Service(name = "DocumentGitServiceImplementation")
@Singleton
public class DocumentGitServiceImpl implements IDocumentGitService {

	private static final Logger LOG = Logger.getLogger(DocumentGitServiceImpl.class);

	private Map<String, Repository> repositoryCloneMap;

	private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

	@Context
	private Application application;

	private int timeDifference;

	private Runnable clearRunnable = new Runnable() {
		public void run() {
			long now = Instant.now().toEpochMilli();

			int numberDeletedDir = 0;
			for (Entry<String, Repository> entry : repositoryCloneMap.entrySet()) {
				File dirGit = entry.getValue().getDirectory();
				if (now - timeDifference > dirGit.lastModified()) {
					try {
						FileUtils.forceDelete(dirGit.getParentFile());
						repositoryCloneMap.remove(entry.getKey());
						numberDeletedDir++;
					} catch (IOException e) {
						LOG.error("Directory " + dirGit.getParentFile().getAbsolutePath()
								+ " cannot be deleted, with exception " + e.getMessage());
					}
				}
			}

			LOG.info("Number of directory deleted is " + numberDeletedDir);
		}
	};

	@PostConstruct
	private void initService() {

		ResourceConfig resourceConfiguration = ResourceConfig.forApplication(application);

		String startTimeDelete = (String) resourceConfiguration.getProperty("start-time-delete-Git-clone");
		String cicleTimeDelete = (String) resourceConfiguration.getProperty("cicle-time-delete-Git-clone");
		String timeDifferenceDelete = (String) resourceConfiguration.getProperty("difference-time-delete-Git-clone");
		int startTime = 3;
		if (startTimeDelete != null && !startTimeDelete.isEmpty()) {

			try {
				startTime = Integer.parseInt(startTimeDelete);
			} catch (NumberFormatException e) {
				LOG.error(startTimeDelete + " is not a number");
			}
		}

		int cicleTime = 10;
		if (cicleTimeDelete != null && !cicleTimeDelete.isEmpty()) {
			try {
				Integer.parseInt(cicleTimeDelete);
			} catch (NumberFormatException e) {
				LOG.error(cicleTimeDelete + " is not a number");
			}
		}

		timeDifference = 100000;

		if (timeDifferenceDelete != null && !timeDifferenceDelete.isEmpty()) {

			try {
				timeDifference = Integer.parseInt(startTimeDelete);
			} catch (NumberFormatException e) {
				LOG.error(startTimeDelete + " is not a number");
			}
		}

		repositoryCloneMap = new HashMap<>();
		scheduledExecutorService.scheduleAtFixedRate(clearRunnable, startTime, cicleTime, TimeUnit.MINUTES);
	}

	@Override
	public List<String> getRemoteBranchesNames(String gitURL, UserAuthModel userAuth) {

		Collection<Ref> refs = getRefsFromURL(gitURL, userAuth);
		if (refs == null) {
			LOG.error("Refs collections for " + gitURL + " is null");
			return null;
		}
		List<String> branchesNameList = new ArrayList<>();
		for (Ref ref : refs) {
			branchesNameList.add(ref.getName().substring(ref.getName().lastIndexOf("/") + 1));
		}
		LOG.info("Head names list for repository have size " + branchesNameList.size());
		return branchesNameList;
	}

	@Override
	public Pair<List<String>, List<String>> getFileNamesFromCommit(String gitURL, UserAuthModel userAuth) {

		Repository repository = getRepository(gitURL, null, userAuth, true);

		if (repository == null) {
			LOG.info("Null repository for " + gitURL);
			return null;
		}

		List<String> listDirs = new ArrayList<>();
		List<String> listFiles = new ArrayList<>();
		try (RevWalk walk = new RevWalk(repository); TreeWalk treeWalk = new TreeWalk(repository);) {
			ObjectId objectId = repository.resolve(Constants.HEAD);
			RevCommit commit = walk.parseCommit(objectId);
			RevTree tree = commit.getTree();
			treeWalk.addTree(tree);
			treeWalk.setRecursive(false);
			while (treeWalk.next()) {
				if (treeWalk.isSubtree()) {
					listDirs.add(treeWalk.getPathString());
					treeWalk.enterSubtree();
				} else {
					listFiles.add(treeWalk.getPathString());
				}
			}

			repository.close();

		} catch (IOException e) {
			LOG.error(e.getMessage());
			return null;
		}
		
		LOG.info("Pair of list with size for dirs list " + listDirs.size() + " and size for files list " + listFiles.size());
		return Pair.of(listDirs, listFiles);
	}

	@Override
	public byte[] getDocumentFromRepository(String gitURL, String branchName, String pathFileName,
			UserAuthModel userAuth) {
		Repository repository;
		if (branchName == null) {
			repository = getRepository(gitURL, null, userAuth, true);
		} else {
			repository = getRepository(gitURL, branchName, userAuth, true);
		}

		if (repository == null)
			return null;

		try (RevWalk walk = new RevWalk(repository);) {
			ObjectId objectId = repository.resolve(Constants.HEAD);
			RevCommit commit = walk.parseCommit(objectId);
			RevTree tree = commit.getTree();
			TreeWalk treewalk = TreeWalk.forPath(repository, pathFileName, tree);
			byte[] content = repository.open(treewalk.getObjectId(0)).getBytes();
			LOG.info("Get content of document " + pathFileName);
			return content;
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return null;
		}
	}

	@Override
	public Boolean commitDocumentLocalToGit(String gitURL, String branchName, File localFile, String copyOnDir,
			String commitMessage, UserAuthModel userAuth) {
		Repository repository = getRepository(gitURL, branchName, userAuth, false);
		
		if(repository == null) {
			LOG.info("Repository for " + gitURL  + " is null");
			return false;
		}
		
		boolean isLocale = false;
		if (!gitURL.toLowerCase().startsWith("https"))
			isLocale = true;

		Boolean response = null;
		try {
			response = commitDocumentLocalToGit(repository, branchName, localFile, copyOnDir, commitMessage, userAuth,
					isLocale);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}

		if (!isLocale) {
			deleteTmpDir(repository.getDirectory().getParentFile());
		}

		LOG.info("Commit and eventually push is executed");
		return response;
	}

	private Boolean commitDocumentLocalToGit(Repository repository, String branchName, File localFile, String copyOnDir,
			String commitMessage, UserAuthModel userAuth, boolean isLocale) {
		if (repository == null) {
			LOG.error("Repository is null");
			return false;
		}
		String fileName = localFile.getName();
		LOG.info("File name " + fileName);
		String directoryRepositoryPath = repository.getDirectory().getAbsolutePath();
		String pathRepository = directoryRepositoryPath.substring(0,
				directoryRepositoryPath.lastIndexOf(File.separator));
		
		File directoryRepository = new File(pathRepository + File.separator + copyOnDir);

		if (!directoryRepository.exists()) {
			try {
				java.nio.file.Files.createDirectories(directoryRepository.toPath());
			} catch (IOException e) {
				LOG.error(e.getMessage());
				return false;
			}
		}

		LOG.info("Path is " + directoryRepository.getAbsolutePath());
		try {
			FileUtils.copyFileToDirectory(localFile, directoryRepository);
		} catch (IOException e1) {
			LOG.error("Copy to temp directory has not been achieved");
			return false;
		}

		try (RevWalk walk = new RevWalk(repository);
				Git git = new Git(repository);
				GitDB gitDB = new GitDB(repository);) {
			LOG.info("Try to commit file with name " + fileName + " from path " + pathRepository);

			DataItem dataItem;
			if (copyOnDir == null) {
				dataItem = new DataItem(fileName,
						FileUtils.readFileToString(new File(directoryRepository + File.separator + fileName)));
			} else {
				dataItem = new DataItem(copyOnDir + "/" + fileName,
						FileUtils.readFileToString(new File(directoryRepository + File.separator + fileName)));
			}
			LOG.info("Commit message " + commitMessage);
			Config config = repository.getConfig();
			String name = config.getString("user", null, "name");
			String email = config.getString("user", null, "email");
			LOG.info("Repository name " + name + " with email " + email);
			RevCommit commit = gitDB.commit(dataItem, commitMessage, name, email);

			LOG.info("SHA commit " + commit.getName());

			if (!isLocale) {
				PushCommand pushCommand = git.push();
				try {
					UsernamePasswordCredentialsProvider credentialsProvider = getCredetialsProvider(userAuth);
					pushCommand.setCredentialsProvider(credentialsProvider).call();
					LOG.info("Commit is push it");
				} catch (GitAPIException e) {
					LOG.error("Push on repository exception " + e.getMessage());
					return false;
				}
			}

		} catch (RevisionSyntaxException | IOException | InterruptedException | UnmergedPathsException
				| WrongRepositoryStateException | NoHeadException | ConcurrentRefUpdateException e) {
			LOG.error("Commit Exception " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public Date lastModify(String url, UserAuthModel userAuth) {
		LOG.info("last modification time for " + url);
		Repository repository = getRepository(url, null, userAuth, false);

		if (repository == null) {
			LOG.info("Repository for " + url + " is null");
			return null;
		}

		try (RevWalk revWalk = new RevWalk(repository);) {
			revWalk.sort(RevSort.COMMIT_TIME_DESC);
			List<Ref> refs = repository.getRefDatabase().getRefs();
			for (Ref ref : refs) {
				RevCommit commit = revWalk.parseCommit(ref.getLeaf().getObjectId());
				revWalk.markStart(commit);
			}
			RevCommit newest = revWalk.next();
			return newest.getAuthorIdent().getWhen();
		} catch (IOException e) {
			LOG.error(e.getMessage());
			return null;
		}
	}

	private UsernamePasswordCredentialsProvider getCredetialsProvider(UserAuthModel userAuth) {
		UsernamePasswordCredentialsProvider credentials = null;
		String username = userAuth.getUsername();
		String password = userAuth.getPassword();
		if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
			LOG.info("For username " + username);
			credentials = new UsernamePasswordCredentialsProvider(username, password);
		}

		return credentials;
	}

	private Collection<Ref> getRefsFromURL(String url, UserAuthModel userAuth) {
		LOG.info("Get refs for " + url);
		UsernamePasswordCredentialsProvider credentials = getCredetialsProvider(userAuth);

		long start = System.nanoTime();

		Collection<Ref> refsRepository = null;
		try {
			refsRepository = Git.lsRemoteRepository().setHeads(true).setTags(true).setRemote(url)
					.setCredentialsProvider(credentials).call();
		} catch (GitAPIException e) {
			LOG.error(e.getMessage());
			return null;
		}

		long end = System.nanoTime();

		LOG.info("Duration to obtain Ref from URL " + url + "  is " + (end - start) + " nano seconds");

		return refsRepository;
	}

	private Repository getRepository(String urlGitRepository, String branchName, UserAuthModel authUser,
			boolean withCache) {

		// is a local repository
		if (!urlGitRepository.startsWith("https")) {
			Repository repository = getLocalRepository(urlGitRepository);
			return repository;
		}

		UsernamePasswordCredentialsProvider credentials = getCredetialsProvider(authUser);

		Repository repository = null;
		if (!repositoryCloneMap.containsKey(urlGitRepository)) {

			File tempDirClone = null;

			tempDirClone = com.google.common.io.Files.createTempDir();

			long start = System.nanoTime();
			try {
				Git git = null;
				if (branchName == null)
					// clone all available branches
					git = Git.cloneRepository().setURI(urlGitRepository).setCredentialsProvider(credentials)
							.setDirectory(tempDirClone).setCloneAllBranches(true).call();
				else {
					git = Git.cloneRepository().setURI(urlGitRepository).setBranch(branchName)
							.setCredentialsProvider(credentials).setDirectory(tempDirClone).call();
					urlGitRepository += "/tree/" + branchName;
				}

				if (git != null)
					repository = git.getRepository();
				else
					return null;

				if (withCache) {
					LOG.info("URL " + urlGitRepository + " was registered to cache");
					repositoryCloneMap.put(urlGitRepository, repository);
				}

			} catch (GitAPIException e) {
				LOG.error(e.getMessage());
				return null;
			}

			long end = System.nanoTime();

			LOG.info("Duration to obtain Ref from URL " + urlGitRepository + "  is " + (end - start) + " nano seconds");
		} else {
			LOG.info(urlGitRepository + " is contained in hash map");
			if (branchName == null)
				repository = repositoryCloneMap.get(urlGitRepository);
			else
				repository = repositoryCloneMap.get(urlGitRepository + "/tree/" + branchName);
		}

		return repository;
	}

	private Repository getLocalRepository(String pathToGit) {
		File dotGitFile = new File(pathToGit + File.separator + ".git");
		if (!dotGitFile.exists() || !dotGitFile.isDirectory()) {
			LOG.info("Directory " + pathToGit + " not exists");
			return null;
		}
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		try {
			Repository repository = repositoryBuilder.setGitDir(dotGitFile).readEnvironment().findGitDir()
					.setMustExist(true).build();
			return repository;
		} catch (IOException e) {
			LOG.error("IOException " + e.getMessage());
			return null;
		}
	}

	@PreDestroy
	private void shutdownService() {
		for (Entry<String, Repository> entry : repositoryCloneMap.entrySet()) {
			File dirGit = entry.getValue().getDirectory();
			deleteTmpDir(dirGit.getParentFile());
		}
	}

	private void deleteTmpDir(File file) {
		try {
			FileUtils.forceDelete(file);
			LOG.info(file.getAbsolutePath() + " was succesfuly deleted");
		} catch (IOException e) {
			LOG.error("Directory " + file.getAbsolutePath() + " cannot be deleted, with exception " + e.getMessage()
					+ " in shutdown method");
		}
	}
}
