package be.waines.maven.impl;

import java.util.Collection;
import java.util.Collections;

import org.apache.maven.project.MavenProject;

import be.waines.maven.api.BuildResult;
import be.waines.maven.api.BuildResultRepository;

public class NoOpBuildResultRepository implements BuildResultRepository {

	@Override
	public Collection<BuildResult> readBuilds(MavenProject project) {
		return Collections.emptyList();
	}

	@Override
	public void writeBuild(MavenProject project, BuildResult buildResult) {
	}
	
	@Override
	public void markAsInvalid(MavenProject project) {
	}
	
	@Override
	public boolean isValid(MavenProject project) {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
