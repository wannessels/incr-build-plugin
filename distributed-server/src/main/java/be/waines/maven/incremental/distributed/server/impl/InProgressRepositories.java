package be.waines.maven.incremental.distributed.server.impl;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import be.waines.maven.model.BuildId;

@Singleton
public class InProgressRepositories {
	
	private ConcurrentMap<BuildId, BuildInProgressRepository> inProgressRepositories = new ConcurrentHashMap<BuildId, BuildInProgressRepository>();
	private File inProgressRootDir;
	
	@Inject
	InProgressRepositories(@Named(BuildServerConfig.REPOSITORY_ROOT_DIR) File rootDir) {
		this.inProgressRootDir = new File(rootDir, "inProgress");
		this.inProgressRootDir.mkdirs();
	}
	
	public BuildInProgressRepository getInProgressRepository(BuildId buildId) {
		BuildInProgressRepository inProgressRepository = inProgressRepositories.get(buildId);
		if (inProgressRepository == null) {
			File repoDir = new File(inProgressRootDir, buildId.toString());
			inProgressRepositories.putIfAbsent(buildId, new BuildInProgressRepository(repoDir, buildId));
		}
		return inProgressRepositories.get(buildId);
	}

	public void removeInProgressRepository(BuildId buildId) {
		inProgressRepositories.remove(buildId);
	}
	

}
