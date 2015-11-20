package be.waines.maven.incremental.distributed.server.impl;

import java.io.File;
import java.io.IOException;

import be.waines.maven.api.BuildResult;
import be.waines.maven.model.ModuleId;

public class RepositoryCommit {
	
	private BuildInProgressRepository inProgressRepository;
	private SharedBuildRepository sharedRepository;

	public RepositoryCommit(BuildInProgressRepository inProgressRepository, SharedBuildRepository sharedRepository) {
		this.inProgressRepository = inProgressRepository;
		this.sharedRepository = sharedRepository;
	}
	
	public void execute() throws IOException {
		inProgressRepository.halt();
		for (ModuleId moduleId : inProgressRepository.getBuildResults().keySet()) {
			BuildResult buildResult = inProgressRepository.getBuildResultInternal(moduleId);
			if (inProgressRepository.isValid(moduleId)) {
				File inProgressArtifactsDir = inProgressRepository.moduleDir(moduleId);
				sharedRepository.importBuildResultAndArtifacts(moduleId, inProgressRepository.getBuildId(),buildResult, inProgressArtifactsDir);
			} else {
				sharedRepository.markAsInvalid(moduleId, inProgressRepository.getBuildId(), buildResult.getChecksum());
			}
		}
	}

}
