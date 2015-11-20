package be.waines.maven.model;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=ModuleIdFactory.class)
public class ModuleIdFactory {

	public ModuleId create(MavenProject project) {
		String groupId = project.getGroupId();
		String artifactId = project.getArtifactId();
		String version = project.getVersion();
		return new ModuleId(groupId, artifactId, version);
	}

	public ModuleId create(Dependency dependency) {
		String groupId = dependency.getGroupId();
		String artifactId = dependency.getArtifactId();
		String version = dependency.getVersion();
		return new ModuleId(groupId, artifactId, version);
	}
	
}
