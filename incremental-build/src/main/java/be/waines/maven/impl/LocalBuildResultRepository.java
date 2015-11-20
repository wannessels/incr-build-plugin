package be.waines.maven.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import be.waines.maven.LogUtil;
import be.waines.maven.api.BuildResult;
import be.waines.maven.api.BuildResultRepository;


@Component(role=BuildResultRepository.class, hint="local")
public class LocalBuildResultRepository implements BuildResultRepository {
	
	public static final String BUILD_FILE = ".incrbld";
	
	@Requirement
	private LogUtil logUtil;
	
	@Override
	public Collection<BuildResult> readBuilds(MavenProject project) {
		File buildFile = getBuildFile(project);
		Collection<BuildResult> buildResults = new ArrayList<BuildResult>();
		if (buildFile.exists()) {
			try {
				BuildResult buildResult = BuildResultUtil.deserialize(buildFile);
				if (BuildResultUtil.hasArtifactReferencesForProject(buildResult, project) &&
						BuildResultUtil.hasArtifactFiles(buildResult, project)) {
					buildResults.add(buildResult);
				} else {
					logUtil.warn("Skipping BuildResult(" + buildResult.getChecksum() + ") because it doesn't have any artifacts");
				}
			} catch (IOException e) {
				logUtil.warn("could not read buildResult " + buildFile, e);
			}
		}
		return buildResults;
	}
	
	@Override
	public void writeBuild(MavenProject project, BuildResult incrementalBuild) {
		File buildFile = getBuildFile(project);
		BuildResultUtil.serialize(incrementalBuild, buildFile);
	}
	
	private File getBuildFile(MavenProject project) {
		return new File(getOrCreateBuildDirectory(project), BUILD_FILE);
	}
	
	private File getOrCreateBuildDirectory(MavenProject project) {
		File buildDirectory = new File(project.getBuild().getDirectory());
		if (!buildDirectory.exists()) {
			buildDirectory.mkdirs();
		}
		return buildDirectory;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void markAsInvalid(MavenProject project) {
		getBuildFile(project).delete();		
	}
	
	@Override
	public boolean isValid(MavenProject project) {
		return getBuildFile(project).exists();
	}

}
