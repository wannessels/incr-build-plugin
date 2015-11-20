package be.waines.maven.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;

public class BuildResult implements Serializable {

	//changed to force incompatibility with previous (incorrect) buildresults
	private static final long serialVersionUID = 3L;
	
	private ModuleId moduleId;
	private Checksum sourcesChecksum;
	private Map<ModuleId, Checksum> dependencyChecksums = new HashMap<ModuleId, Checksum>();
	private Set<BuildResultExtension> extensions = new HashSet<BuildResultExtension>();

	//todo artifact (=result of build) separate from checksums (=input of build)?
	private ArtifactReference artifact;
	private List<ArtifactReference> attachedArtifacts;
	
	private String source;
	private Checksum checksum;
	
	
	public BuildResult(ModuleId moduleId, Checksum sourcesChecksum, Map<ModuleId, Checksum> dependencyChecksums, ArtifactReference artifact, List<ArtifactReference> attachedArtifacts, Set<BuildResultExtension> extensions, String source) {
		assert moduleId != null;
		assert sourcesChecksum != null;
		assert attachedArtifacts != null;
		assert dependencyChecksums != null;
		this.moduleId = moduleId;
		this.sourcesChecksum = sourcesChecksum;
		this.artifact = artifact;
		this.attachedArtifacts = attachedArtifacts;
		this.dependencyChecksums = dependencyChecksums;
		this.source = source;
		this.extensions = extensions;
		this.checksum = calculateChecksum(sourcesChecksum, dependencyChecksums.values(), extensions);
	}
	
	@SuppressWarnings("unused")
	private BuildResult() {}
		
	private Checksum calculateChecksum(Checksum sourcesChecksum, Collection<Checksum> dependencyChecksums, Set<BuildResultExtension> extensions) {
		Checksum result = sourcesChecksum;
		for (Checksum dependencyChecksum : dependencyChecksums) {
			result = result.xor(dependencyChecksum);
		}
		for (BuildResultExtension extension : extensions) {
			result = result.xor(extension.calculateChecksum());
		}
		return result;
	}

	public ModuleId getModuleId() {
		return moduleId;
	}
	
	public Checksum getSourcesChecksum() {
		return sourcesChecksum;
	}
	
	public Checksum getChecksum() {
		return checksum;
	}
	
	public ArtifactReference getArtifact() {
		return artifact;
	}
	
	public List<ArtifactReference> getAttachedArtifacts() {
		return new ArrayList<ArtifactReference>(new HashSet<ArtifactReference>(attachedArtifacts));
	}

	public boolean hasDependency(ModuleId dependencyId) {
		return dependencyChecksums.containsKey(dependencyId);
	}

	public Checksum getDependencyChecksum(ModuleId dependencyId) {
		return dependencyChecksums.get(dependencyId);
	}
	
	public Map<ModuleId, Checksum> getDependencyChecksums() {
		return dependencyChecksums;
	}
	
	public void downloadArtifacts(File buildDirectory) throws IOException {
	}

	
	public String getSource() {
		return source;
	}
	
	public Set<BuildResultExtension> getExtensions() {
		if (extensions == null) {
			extensions = new HashSet<BuildResultExtension>();
		}
		return extensions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ checksum.hashCode();
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
		BuildResult other = (BuildResult) obj;
		if (!checksum.equals(other.checksum))
			return false;
		return true;
	}
	
	

}
