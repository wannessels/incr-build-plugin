package be.waines.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import be.waines.maven.api.ArtifactManager;
import be.waines.maven.api.BuildDifference;
import be.waines.maven.api.BuildResult;
import be.waines.maven.api.BuildResultExtension;
import be.waines.maven.api.BuildResultFactory;
import be.waines.maven.api.BuildResultRepository;
import be.waines.maven.api.ChecksumConflictException;
import be.waines.maven.api.IncrementalReactor;
import be.waines.maven.impl.BuildResultExtensionCalculators;
import be.waines.maven.impl.NoOpBuildResultRepository;
import be.waines.maven.impl.NoOpIncrementalReactor;
import be.waines.maven.model.Checksum;
import be.waines.maven.model.ChecksumUtil;
import be.waines.maven.model.ModuleId;
import be.waines.maven.model.ModuleIdFactory;

import com.google.common.collect.Sets;

@Component(role=ProjectExecutionListener.class)
public class IncrementalBuilder implements ProjectExecutionListener {
	
	@Requirement
	private ArtifactManager artifactManager;
	
	@Requirement
	private LogUtil logUtil;
	
	@Requirement(hint="local")
	private IncrementalReactor localIncrementalReactor;
	
	@Requirement(hint="distributed", optional=true)
	private IncrementalReactor distributedIncrementalReactor;
	
	@Requirement
	private LifecycleDependencyResolver lifecycleDependencyResolver;

	@Requirement
	private BuildResultFactory buildResultFactory;
	
	@Requirement(hint="local")
	private BuildResultRepository localBuildResultRepository;
	
	@Requirement(hint="distributed", optional=true)
	private BuildResultRepository distributedBuildResultRepository;
	
	@Requirement
	private ModuleIdFactory moduleIdFactory;
	
	@Requirement
	private BuildResultExtensionCalculators extensionCalculators;
	
	public IncrementalBuilder() {
		//initDistributedComponents();
	}


	private void initDistributedComponents() {
		if (distributedIncrementalReactor == null || !distributedIncrementalReactor.isEnabled()) {
			distributedIncrementalReactor = new NoOpIncrementalReactor();
		}
		if (distributedBuildResultRepository == null || !distributedBuildResultRepository.isEnabled()) {
			distributedBuildResultRepository = new NoOpBuildResultRepository();
		}
	}
	
	
	@Override
	public void beforeProjectExecution(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
		initDistributedComponents();
		logUtil.logToFile(new File(projectExecutionEvent.getSession().getRequest().getBaseDirectory(),"incrementalbuild.log"));
		logUtil.logProject(projectExecutionEvent.getProject());
		
	}

	@Override
	public void beforeProjectLifecycleExecution(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
		if (!incrementalBuildEnabled(projectExecutionEvent)) {
			logUtil.info("incremental build disabled");
			return;
		}
		
		MavenProject project = projectExecutionEvent.getProject();	

		setDependencyChecksums(projectExecutionEvent);		
		
		Collection<BuildResult> previousBuilds = new ArrayList<BuildResult>();
		previousBuilds.addAll(localBuildResultRepository.readBuilds(project));
		previousBuilds.addAll(distributedBuildResultRepository.readBuilds(project));
		
		Checksum sourcesChecksum = calculateAndSetSourcesChecksum(project);
		BuildDifference buildDifference = BuildDifference.NO_PREVIOUS_BUILD;
		Set<BuildResultExtension> extensions = extensionCalculators.calculate(projectExecutionEvent);
		for (BuildResult previousBuild : previousBuilds) {
			buildDifference = getBuildDifference(previousBuild, sourcesChecksum, extensions, project);
			if (!buildDifference.isDifferent()) {
				break;
			}
		}
		
		buildDifference.printTo(logUtil);		
		if (!buildDifference.isDifferent()) {
			skipProject(projectExecutionEvent, buildDifference.getPreviousBuild());
		} else {
			logUtil.info("building...");
		}
	}


	private void setDependencyChecksums(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
		try {
			ensureDependenciesDownloaded(projectExecutionEvent);
			
			MavenProject project = projectExecutionEvent.getProject();
			for (Dependency dependency : project.getDependencies()) {
				ModuleId moduleId = moduleIdFactory.create(dependency);
				if (!localIncrementalReactor.contains(moduleId) && !distributedIncrementalReactor.contains(moduleId)) {
					Checksum dependencyChecksum = getDependencyChecksum(project, moduleId);
					localIncrementalReactor.setChecksum(moduleId, dependencyChecksum);
					distributedIncrementalReactor.setChecksum(moduleId, dependencyChecksum);
				}
			}
		} catch (IOException e) {
			throw new LifecycleExecutionException(e);
		}
	}
	
	private Checksum getDependencyChecksum(MavenProject project, ModuleId moduleId) throws IOException {
		for (Artifact dependencyArtifact : project.getDependencyArtifacts()) {
			if (moduleId.getGroupId().equals(dependencyArtifact.getGroupId()) &&
					moduleId.getArtifactId().equals(dependencyArtifact.getArtifactId()) &&
					moduleId.getVersion().equals(dependencyArtifact.getBaseVersion())) {
				return ChecksumUtil.calculateChecksum(dependencyArtifact.getFile());
			}
		}
		throw new IllegalStateException("Could not find dependencyArtifact for " + moduleId + ". This should not happen!");
	}


	private void ensureDependenciesDownloaded(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
		Collection<String> scopesToResolve = Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST);
		lifecycleDependencyResolver.resolveProjectDependencies(projectExecutionEvent.getProject(), Collections.<String>emptySet(), 
				scopesToResolve, projectExecutionEvent.getSession(), false, Collections.<Artifact> emptySet());
	}
	
	private BuildDifference getBuildDifference(BuildResult previousBuild, Checksum sourcesChecksum, Set<BuildResultExtension> extensions, MavenProject project) throws LifecycleExecutionException {
		BuildDifference buildDifference = new BuildDifference(previousBuild);
		if (!distributedBuildResultRepository.isValid(project)) { //local build might be invalid on remote, in case of distributed build
			buildDifference.setInvalid();
		} else if (previousBuild != null) { 
			calculateFilesChanged(sourcesChecksum, buildDifference);
			calculateDependenciesChanged(project, buildDifference);
			calculateExtensionsChanged(extensions, buildDifference);
		}
		return buildDifference;
	}


	private void calculateExtensionsChanged(Set<BuildResultExtension> extensions, BuildDifference buildDifference) {
		Set<BuildResultExtension> previousExtensions = buildDifference.getPreviousBuild().getExtensions();
		
		Set<BuildResultExtension> missingExtensions = Sets.difference(previousExtensions, extensions);
		Set<BuildResultExtension> extraExtensions = Sets.difference(extensions, previousExtensions);
		
		buildDifference.addMissingExtensions(missingExtensions);
		buildDifference.addExtraExtensions(extraExtensions);
	}


	@Override
	public void afterProjectExecutionFailure(ProjectExecutionEvent projectExecutionEvent) {
		MavenProject project = projectExecutionEvent.getProject();
		localBuildResultRepository.markAsInvalid(project);
		distributedBuildResultRepository.markAsInvalid(project);
	}

	@Override
	public void afterProjectExecutionSuccess(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
		if (!incrementalBuildEnabled(projectExecutionEvent)) {
			return;
		}
		
		MavenProject project = projectExecutionEvent.getProject();
		
		BuildResult buildResult = buildResultFactory.createBuildResult(projectExecutionEvent);		
		localBuildResultRepository.writeBuild(project,buildResult);
		distributedBuildResultRepository.writeBuild(project,buildResult);
	}
	
	private boolean incrementalBuildEnabled(ProjectExecutionEvent projectExecutionEvent) {
		String propertyValue = projectExecutionEvent.getSession().getSystemProperties().getProperty("enableIncrementalBuild","false");
		return propertyValue.isEmpty() || Boolean.valueOf(propertyValue);
	}
	
	
	private Checksum calculateAndSetSourcesChecksum(MavenProject project) throws LifecycleExecutionException {
		try {
			logUtil.debug("Calculating file checksums...");
			Checksum sourcesChecksum = ChecksumUtil.calculateSourcesChecksum(project);
			
			ModuleId moduleId = moduleIdFactory.create(project);
			localIncrementalReactor.setChecksum(moduleId, sourcesChecksum);
			distributedIncrementalReactor.setChecksum(moduleId, sourcesChecksum);
			
			return sourcesChecksum;
		} catch (IOException e) {
			throw new LifecycleExecutionException(e);
		} catch (ChecksumConflictException e) {
			logUtil.warn("checksum conflict!", e);
			throw e;
		}
	}


	private void skipProject(ProjectExecutionEvent projectExecutionEvent, BuildResult previousBuild) {
		logUtil.info("skipping build");
		boolean artifactsReset = false;
		try {
			artifactManager.resetArtifacts(projectExecutionEvent.getProject(), previousBuild);
			artifactsReset = true;
		} catch (RuntimeException e) {
			logUtil.warn("could not reset artifacts, can't skip build", e);
		} catch (IOException e) {
			logUtil.warn("could not reset artifacts, can't skip build", e);
		}
		if (artifactsReset) {
			projectExecutionEvent.getExecutionPlan().clear();
		}
	}
	
	private void calculateFilesChanged(Checksum checksum, BuildDifference buildDifference) throws LifecycleExecutionException {
		BuildResult previousBuild = buildDifference.getPreviousBuild();
		Checksum previousChecksum = previousBuild.getSourcesChecksum();
		if (previousChecksum != null && !previousChecksum.equals(checksum)) {
			logUtil.debug("checksum differs: " + checksum + " != " + previousChecksum);
			buildDifference.setSourcesChanged();
		}
	}

	private void calculateDependenciesChanged(MavenProject project, BuildDifference buildDifference) {
		List<Dependency> dependencies = project.getModel().getDependencies();
		BuildResult previousBuild = buildDifference.getPreviousBuild();
		
		List<ModuleId> dependencyIds = new ArrayList<ModuleId>();
		for (Dependency dependency : dependencies) {
			dependencyIds.add(moduleIdFactory.create(dependency));
		}
		MavenProject parentProject = project.getParent();
		if (parentProject != null) {
			dependencyIds.add(moduleIdFactory.create(parentProject));
		}
		
		//TODO compare enkel local/local en distributed/distributed?
		for (ModuleId dependencyId : dependencyIds) {
			if (dependencyChangedSince(previousBuild, dependencyId, localIncrementalReactor)) {
				buildDifference.addChangedDependency(dependencyId);
			} else if (dependencyChangedSince(previousBuild, dependencyId, distributedIncrementalReactor)) {
				buildDifference.addChangedDependency(dependencyId);
			}
		}
		logUtil.debug("no dependencies in reactor changed");
	}


	private boolean dependencyChangedSince(BuildResult previousBuild, ModuleId dependencyId, IncrementalReactor incrementalReactor) {
		if (previousBuild.hasDependency(dependencyId) && incrementalReactor.contains(dependencyId)) {
			Checksum previousDependencyChecksum = previousBuild.getDependencyChecksum(dependencyId);
			Checksum newDependencyChecksum = incrementalReactor.getChecksum(dependencyId);
			if (!previousDependencyChecksum.equals(newDependencyChecksum)) {
				logUtil.debug("depends on previously changed module " + dependencyId);
				return true;	
			}
		}
		return false;
	}
	
}
