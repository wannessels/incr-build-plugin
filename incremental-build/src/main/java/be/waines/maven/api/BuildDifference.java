package be.waines.maven.api;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import be.waines.maven.LogUtil;
import be.waines.maven.model.ModuleId;

public class BuildDifference {
	
	public static final BuildDifference NO_PREVIOUS_BUILD = new BuildDifference(null);
	
	private BuildResult previousBuild;
	
	private boolean sourcesChanged;
	
	private Set<ModuleId> changedDependencies = new LinkedHashSet<ModuleId>();

	private boolean invalid = false;

	private Set<BuildResultExtension> missingExtensions = new HashSet<BuildResultExtension>();
	private Set<BuildResultExtension> extraExtensions = new HashSet<BuildResultExtension>();
	
	public BuildDifference(BuildResult previousBuild) {
		this.previousBuild = previousBuild;
	}
	
	public void setSourcesChanged() {
		sourcesChanged = true;
	}
	
	public void addChangedDependency(ModuleId moduleId) {
		changedDependencies.add(moduleId);
	}
	
	public boolean isDifferent() {
		return previousBuild == null || invalid || sourcesChanged ||
				!changedDependencies.isEmpty() ||
				!missingExtensions.isEmpty() || !extraExtensions.isEmpty();
	}
	
	public void printTo(LogUtil log) {
		if (previousBuild == null) {
			log.info("no previous build");
		} else if (invalid) {
			log.info("build result marked as invalid");
		} else {
			log.info("using build result " + previousBuild.getSource());
			log.info("\t\tsources " + (sourcesChanged ? "changed" : "not changed"));
			log.info("\t\tdependencies " + (changedDependencies.isEmpty() ? "not changed" : "changed"));
			for (ModuleId dependencyId : changedDependencies) {
				log.info("\t\t\tchanged dependency " + dependencyId);
			}
			for (BuildResultExtension extension : missingExtensions) {
				log.info("\t\t\tmissing extension " + extension);
			}
			for (BuildResultExtension extension : extraExtensions) {
				log.info("\t\t\textra extension " + extension);
			}
			
		}
	}
	
	public BuildResult getPreviousBuild() {
		return previousBuild;
	}

	public void setInvalid() {
		invalid = true;
	}

	public void addMissingExtensions(Set<BuildResultExtension> missingExtensions) {
		this.missingExtensions  = missingExtensions;
	}

	public void addExtraExtensions(Set<BuildResultExtension> extraExtensions) {
		this.extraExtensions = extraExtensions;
	}

}
