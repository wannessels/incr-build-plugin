package be.waines.maven.api;

import java.util.Collection;

import org.apache.maven.project.MavenProject;

public interface BuildResultRepository {

	Collection<BuildResult> readBuilds(MavenProject project);	
	void writeBuild(MavenProject project, BuildResult buildResult);
	
	boolean isValid(MavenProject project);
	void markAsInvalid(MavenProject project);	
	
	boolean isEnabled();
}