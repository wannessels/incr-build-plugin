package be.waines.maven.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;


class ArtifactReferenceFactory {
	
	private MavenProject project;
	private File buildDirectory;

	public ArtifactReferenceFactory(MavenProject project) {
		this.project = project;
		this.buildDirectory = new File(project.getBuild().getDirectory());
	}
	
	public ArtifactReference getArtifact() {
		Artifact projectArtifact = project.getArtifact();
		if (projectArtifact.getFile() != null) {
			return newLocalArtifactReference(projectArtifact);
		} else {
			return null;
		}
	}

	public List<ArtifactReference> getAttachedArtifacts() {
		List<ArtifactReference> artifacts = new ArrayList<ArtifactReference>();
		
		for (Artifact artifact : project.getAttachedArtifacts()) {
			artifacts.add(newLocalArtifactReference(artifact));
		}
		return artifacts;
	}
	
	private ArtifactReference newLocalArtifactReference(Artifact projectArtifact) {
		String relativePath =buildDirectory.toURI().relativize(projectArtifact.getFile().toURI()).getPath();
		String type = projectArtifact.getType();
		String classifier = projectArtifact.getClassifier();
		return new ArtifactReference(relativePath,type,classifier);
	}
}