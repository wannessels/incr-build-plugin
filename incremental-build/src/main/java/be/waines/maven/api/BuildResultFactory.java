package be.waines.maven.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import be.waines.maven.impl.BuildResultExtensionCalculators;
import be.waines.maven.impl.NoOpIncrementalReactor;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ModuleId;
import be.waines.maven.model.ModuleIdFactory;

@Component(role=BuildResultFactory.class)
public class BuildResultFactory {
	
	@Requirement(hint="local")
	private IncrementalReactor incrementalReactor;
	
	@Requirement(hint="distributed", optional=true)
	private IncrementalReactor distributedIncrementalReactor;
	
	@Requirement
	private ModuleIdFactory moduleIdFactory;
	
	@Requirement
	private BuildResultExtensionCalculators extensionCalculators;
	
	private void initDistributedComponents() {
		if (distributedIncrementalReactor == null || !distributedIncrementalReactor.isEnabled()) {
			distributedIncrementalReactor = new NoOpIncrementalReactor();
		}
	}

	public BuildResult createBuildResult(ProjectExecutionEvent projectExecutionEvent) {
		initDistributedComponents();
		MavenProject project = projectExecutionEvent.getProject();
		ModuleId moduleId = moduleIdFactory.create(project);
		Checksum checksum = incrementalReactor.getChecksum(moduleId);
		
		ArtifactReferenceFactory factory = new ArtifactReferenceFactory(project);		
		List<ArtifactReference> attachedArtifacts = factory.getAttachedArtifacts();
		ArtifactReference artifact = factory.getArtifact();
		
		Map<ModuleId, Checksum> dependencyChecksums = getDependencyChecksums(project);
		
		Set<BuildResultExtension> extensions = extensionCalculators.calculate(projectExecutionEvent);

		return new BuildResult(moduleId,checksum, dependencyChecksums, artifact, attachedArtifacts, extensions, "(local) " + moduleId);
	}

	private Map<ModuleId, Checksum> getDependencyChecksums(MavenProject project) {
		Map<ModuleId, Checksum> dependencyChecksums = new HashMap<ModuleId, Checksum>();
		for (Dependency dependency : project.getDependencies()) {
			addIfChanged(moduleIdFactory.create(dependency), dependencyChecksums);
		}
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			addIfChanged(moduleIdFactory.create(parentProject), dependencyChecksums);
		}
		return dependencyChecksums;
	}

	private void addIfChanged(ModuleId dependencyId, Map<ModuleId, Checksum> dependencyChecksums) {
		Checksum dependencyChecksum = incrementalReactor.getChecksum(dependencyId);
		if (dependencyChecksum == null) {
			dependencyChecksum = distributedIncrementalReactor.getChecksum(dependencyId);
		}
		if (dependencyChecksum != null) {
			dependencyChecksums.put(dependencyId, dependencyChecksum);
		}
	}

}
