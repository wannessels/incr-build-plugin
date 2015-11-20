package be.waines.maven.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=ArtifactManager.class)
public class ArtifactManager {

	@Requirement
	private MavenProjectHelper mavenProjectHelper;

	public void resetArtifacts(MavenProject project, BuildResult previousBuild) throws IOException {
		File buildDirectory = new File(project.getBuild().getDirectory());
		previousBuild.downloadArtifacts(buildDirectory);
		
		if (previousBuild.getArtifact() != null) {
			File artifactFile = previousBuild.getArtifact().getFile(buildDirectory);
			if (!artifactFile.exists()) {
				throw new FileNotFoundException(artifactFile.getAbsolutePath());
			}
			project.getArtifact().setFile(artifactFile);
		}
		
		for (ArtifactReference artifact : previousBuild.getAttachedArtifacts()) {
			File artifactFile = artifact.getFile(buildDirectory);
			if (!artifactFile.exists()) {
				throw new FileNotFoundException(artifactFile.getAbsolutePath());
			}
			mavenProjectHelper.attachArtifact(project, artifact.getType(), artifact.getClassifier(), artifactFile);
		}
	}
	
}