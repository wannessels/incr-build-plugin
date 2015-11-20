package be.waines.maven.api;

import java.io.File;
import java.io.Serializable;


public class ArtifactReference implements Serializable {

	private static final long serialVersionUID = 2864034039450734295L;
	
	private String relativePath;
	private String type;
	private String classifier;

	public ArtifactReference(String relativePath, String type, String classifier) {
		this.relativePath = relativePath;
		this.type = type;
		this.classifier = classifier;
	}
	
	@SuppressWarnings("unused")
	private ArtifactReference() {
		
	}

	public String getRelativePath() {
		return relativePath;
	}
	
	public String getType() {
		return type;
	}
	
	public String getClassifier() {
		return classifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtifactReference other = (ArtifactReference) obj;
		if (classifier == null) {
			if (other.classifier != null)
				return false;
		} else if (!classifier.equals(other.classifier))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	public File getFile(File buildDirectory) {
		return new File(buildDirectory, relativePath);
	}
	
}
